package com.example.platemate;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Daily extends BaseActivity{

    private TextView date;
    private ListView listView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpNavigationDrawer(R.layout.daily);
        setupNavigationHeader();
        date = findViewById(R.id.date);
        listView = findViewById(R.id.meals);
        String currentDate = new SimpleDateFormat("MM/dd/YYYY", Locale.getDefault()).format(new Date());
        date.setText("Today is " + currentDate);
    }
}
