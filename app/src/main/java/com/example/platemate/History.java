package com.example.platemate;

import android.os.Bundle;

public class History extends BaseActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpNavigationDrawer(R.layout.historypage);
        setupNavigationHeader();
    }
}
