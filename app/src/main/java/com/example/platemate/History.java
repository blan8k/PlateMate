package com.example.platemate;

import android.os.Bundle;

public class History extends BaseActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpNavigationDrawer(R.layout.base);

        // Inflate History-specific layout into the content frame
        getLayoutInflater().inflate(R.layout.historypage, findViewById(R.id.content_frame));

    }
}