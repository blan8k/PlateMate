package com.example.platemate;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Daily extends BaseActivity{

    private TextView date;
    private ListView listView;
    private TextView totalCaloriesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpNavigationDrawer(R.layout.daily);
        setupNavigationHeader();
        date = findViewById(R.id.date);
        listView = findViewById(R.id.meals);
        totalCaloriesView = findViewById(R.id.totalCalories);
        String currentDate = new SimpleDateFormat("MM/dd/YYYY", Locale.getDefault()).format(new Date());
        date.setText("Today is " + currentDate);
        loadTodayMeals();
    }

    private void loadTodayMeals() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to view daily history.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 1);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("meal_entries")
                .whereGreaterThanOrEqualTo("timestamp", start.getTime())
                .whereLessThan("timestamp", end.getTime())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> rows = new ArrayList<>();
                    double totalCalories = 0.0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String summary = doc.getString("summary");
                        Double calories = getNumber(doc.get("calories"));
                        String time = "";
                        Timestamp timestamp = doc.getTimestamp("timestamp");
                        if (timestamp != null) {
                            time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    .format(timestamp.toDate());
                        }

                        if (calories != null) {
                            totalCalories += calories;
                        }

                        rows.add(time + " - " + (summary == null ? "Meal" : summary)
                                + " (" + (calories == null ? "N/A" : String.format(Locale.getDefault(), "%.0f kcal", calories)) + ")");
                    }

                    if (rows.isEmpty()) {
                        rows.add("No meals logged today.");
                    }

                    listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
                    totalCaloriesView.setText(String.format(Locale.getDefault(), "Total Calories Today: %.0f kcal", totalCalories));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load meals: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private Double getNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
}
