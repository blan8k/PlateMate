package com.example.platemate;

import android.os.Bundle;

public class Daily extends BaseActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpNavigationDrawer(R.layout.daily);
        setupNavigationHeader();

    }
}