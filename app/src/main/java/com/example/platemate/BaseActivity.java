package com.example.platemate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base);
        // Initialize DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);

        // Initialize Toolbar and set it as the ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // IMPORTANT: This sets the Toolbar as the ActionBar

        // Initialize NavigationView
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);

        // Set up ActionBar toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Check if ActionBar is available
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Only redirect to takePhoto on the initial app launch
        if (getIntent().getBooleanExtra("FROM_NAVIGATION", false)) {
            // Skip redirection if this is a navigation action
            return;
        }
        if (!(this instanceof takePhoto) && savedInstanceState == null) {
            first();
        }

    }

    private void first() {
        Intent intent = new Intent(this, takePhoto.class);
        intent.putExtra("FROM_NAVIGATION", true); // Add a flag to prevent repeated redirection
        startActivity(intent);
        finish();

    }

    // This method must be overridden in child activities to provide their specific layout
    protected int getContentViewId() {
        throw new UnsupportedOperationException("You must override getContentViewId() in child classes.");
    }

    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.takePic) {
            Intent intent = new Intent(this, takePhoto.class);
            intent.putExtra("FROM_NAVIGATION", true); // Add flag to prevent redirection loop
            startActivity(intent);

        } else if (id == R.id.pastHistory) {
            Intent intent = new Intent(this, History.class);
            intent.putExtra("FROM_NAVIGATION", true); // Add flag to prevent redirection loop
            startActivity(intent);
        } else if(id == R.id.daily){
            Intent intent = new Intent(this, Daily.class);
            intent.putExtra("FROM_NAVIGATION", true); // Add flag to prevent redirection loop
            startActivity(intent);
        }else if (id == R.id.loggingOut) {
            FirebaseAuth.getInstance().signOut();
            GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)) // Use your web client ID
                    .requestEmail().build();

            // Build GoogleSignInClient
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                // Redirect to login screen or update UI
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });

            Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "HOW?", Toast.LENGTH_SHORT).show();
        }

        // Close the navigation drawer after handling the click
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void setUpNavigationDrawer(int layoutResID) {
        // Set the activity's layout
        setContentView(layoutResID);

        // Initialize the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.navigation_view);
        if (navigationView == null) {
            Log.e("BaseActivity", "NavigationView is null. Check your layout.");
        }
        navigationView.setNavigationItemSelectedListener(this);
    }
}
