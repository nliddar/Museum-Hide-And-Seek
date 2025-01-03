package com.nliddar.museumhideandseek.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.nliddar.museumhideandseek.R;

public class LostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost);
    }

    public void onBackClick(View view) {
        // Sends intent that starts MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}