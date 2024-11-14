package com.example.platemate;

import android.os.Bundle;

public class Daily extends BaseActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpNavigationDrawer(R.layout.base);

        // Inflate History-specific layout into the content frame
        getLayoutInflater().inflate(R.layout.daily, findViewById(R.id.content_frame));

    }
}
