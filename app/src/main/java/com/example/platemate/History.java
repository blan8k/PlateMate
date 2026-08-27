package com.example.platemate;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class History extends BaseActivity{
    private EditText goalCaloriesInput;
    private TextView trendSummary;
    private TextView latestRecordedDate;
    private TextView latestRecordedCalories;
    private TextView sevenDayAverage;
    private TextView trendDirection;
    private ListView historyList;
    private List<Double> dailyTotals = new ArrayList<>();
    private List<String> dayLabels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpNavigationDrawer(R.layout.historypage);
        setupNavigationHeader();
        goalCaloriesInput = findViewById(R.id.goalCaloriesInput);
        trendSummary = findViewById(R.id.trendSummary);
        latestRecordedDate = findViewById(R.id.latestRecordedDate);
        latestRecordedCalories = findViewById(R.id.latestRecordedCalories);
        sevenDayAverage = findViewById(R.id.sevenDayAverage);
        trendDirection = findViewById(R.id.trendDirection);
        historyList = findViewById(R.id.historyList);
        Button predictButton = findViewById(R.id.predictButton);

        predictButton.setOnClickListener(v -> predictTimeline());
        loadHistoricalData();
    }

    private void loadHistoricalData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to view history.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("meal_entries")
                .orderBy("timestamp")
                .limit(120)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Double> totalsByDay = new LinkedHashMap<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Timestamp ts = doc.getTimestamp("timestamp");
                        if (ts == null) {
                            continue;
                        }
                        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(ts.toDate());
                        Double calories = getNumber(doc.get("calories"));
                        if (calories == null) {
                            continue;
                        }
                        totalsByDay.put(day, totalsByDay.getOrDefault(day, 0.0) + calories);
                    }

                    dayLabels = new ArrayList<>(totalsByDay.keySet());
                    dailyTotals = new ArrayList<>(totalsByDay.values());
                    List<String> rows = new ArrayList<>();
                    for (Map.Entry<String, Double> entry : totalsByDay.entrySet()) {
                        rows.add(entry.getKey() + " : "
                                + String.format(Locale.getDefault(), "%.0f kcal", entry.getValue()));
                    }
                    if (rows.isEmpty()) {
                        rows.add("No historical nutrition data yet.");
                    }
                    historyList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
                    updateSnapshotMetrics();
                    updateTrendMessage();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load history: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateSnapshotMetrics() {
        if (dailyTotals.isEmpty() || dayLabels.isEmpty()) {
            latestRecordedDate.setText("Last recorded day: --");
            latestRecordedCalories.setText("Last recorded calories: --");
            sevenDayAverage.setText("7-day average: --");
            trendDirection.setText("Trend direction: --");
            trendDirection.setTextColor(ContextCompat.getColor(this, R.color.trend_flat));
            return;
        }

        int lastIndex = dailyTotals.size() - 1;
        double latestCalories = dailyTotals.get(lastIndex);
        String latestDay = dayLabels.get(lastIndex);

        int start = Math.max(0, dailyTotals.size() - 7);
        List<Double> recentValues = dailyTotals.subList(start, dailyTotals.size());
        double rollingAverage = average(recentValues);

        latestRecordedDate.setText(String.format(Locale.getDefault(), "Last recorded day: %s", latestDay));
        latestRecordedCalories.setText(String.format(
                Locale.getDefault(),
                "Last recorded calories: %.0f kcal",
                latestCalories
        ));
        sevenDayAverage.setText(String.format(
                Locale.getDefault(),
                "7-day average: %.0f kcal/day",
                rollingAverage
        ));

        if (dailyTotals.size() < 2) {
            trendDirection.setText("Trend direction: Need more data");
            trendDirection.setTextColor(ContextCompat.getColor(this, R.color.trend_flat));
            return;
        }

        double slope = linearRegression(dailyTotals)[0];
        if (slope > 5) {
            trendDirection.setText(String.format(Locale.getDefault(), "Trend direction: Rising (%.1f kcal/day)", slope));
            trendDirection.setTextColor(ContextCompat.getColor(this, R.color.trend_up));
        } else if (slope < -5) {
            trendDirection.setText(String.format(Locale.getDefault(), "Trend direction: Falling (%.1f kcal/day)", slope));
            trendDirection.setTextColor(ContextCompat.getColor(this, R.color.trend_down));
        } else {
            trendDirection.setText(String.format(Locale.getDefault(), "Trend direction: Stable (%.1f kcal/day)", slope));
            trendDirection.setTextColor(ContextCompat.getColor(this, R.color.trend_flat));
        }
    }

    private void updateTrendMessage() {
        if (dailyTotals.size() < 2) {
            trendSummary.setText("Log meals over multiple days to generate a trend.");
            return;
        }

        double[] regression = linearRegression(dailyTotals);
        double slope = regression[0];
        double average = average(dailyTotals);
        trendSummary.setText(String.format(
                Locale.getDefault(),
                "Trend: %.1f kcal/day change. Average intake: %.0f kcal/day.",
                slope,
                average
        ));
    }

    private void predictTimeline() {
        if (dailyTotals.size() < 2) {
            Toast.makeText(this, "Need at least 2 days of data for prediction.", Toast.LENGTH_SHORT).show();
            return;
        }

        String input = goalCaloriesInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Enter a target daily calorie goal.", Toast.LENGTH_SHORT).show();
            return;
        }

        double targetCalories;
        try {
            targetCalories = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid number.", Toast.LENGTH_SHORT).show();
            return;
        }

        double[] regression = linearRegression(dailyTotals);
        double slope = regression[0];
        double intercept = regression[1];
        int currentIndex = dailyTotals.size() - 1;
        double currentPredicted = slope * currentIndex + intercept;

        if (Math.abs(currentPredicted - targetCalories) < 10) {
            trendSummary.setText("You are already close to your calorie goal.");
            return;
        }

        if (Math.abs(slope) < 0.0001) {
            trendSummary.setText("Current trend is flat; prediction is not reliable yet.");
            return;
        }

        double direction = (targetCalories - currentPredicted) * slope;
        if (direction <= 0) {
            trendSummary.setText("Current trend is moving away from your goal. Adjust intake to improve trajectory.");
            return;
        }

        double daysNeeded = (targetCalories - currentPredicted) / slope;
        if (daysNeeded < 0 || daysNeeded > 3650) {
            trendSummary.setText("Prediction is outside a realistic range with current data.");
            return;
        }

        Calendar eta = Calendar.getInstance();
        eta.add(Calendar.DAY_OF_YEAR, (int) Math.ceil(daysNeeded));
        String etaDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(eta.getTime());

        trendSummary.setText(String.format(
                Locale.getDefault(),
                "Estimated goal timeline: %.0f days (around %s). Trend: %.1f kcal/day.",
                Math.ceil(daysNeeded),
                etaDate,
                slope
        ));
    }

    private double[] linearRegression(List<Double> values) {
        int n = values.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        double denominator = (n * sumXX) - (sumX * sumX);
        if (denominator == 0) {
            return new double[]{0, sumY / n};
        }

        double slope = ((n * sumXY) - (sumX * sumY)) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        return new double[]{slope, intercept};
    }

    private double average(List<Double> values) {
        double total = 0;
        for (Double value : values) {
            total += value;
        }
        return total / values.size();
    }

    private Double getNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
}
