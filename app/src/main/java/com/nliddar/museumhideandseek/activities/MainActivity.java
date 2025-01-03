package com.nliddar.museumhideandseek.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.nliddar.museumhideandseek.data.GameData;
import com.nliddar.museumhideandseek.R;
import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    // Object reference to game data
    GameData m_gameData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialise firebase in the application (Used for online DB)
        FirebaseApp.initializeApp(this);

        // Initialise GameData object with default constructor
        m_gameData = new GameData();
    }

    public void onHiderClick(View view) {
        // Set player type to Hider
        m_gameData.setIsHider(true);

        // Sends intent that starts GameSelectActivity and sends the current GameData object
        Intent intent = new Intent(this, GameSelectActivity.class);
        intent.putExtra("GameData", m_gameData);
        startActivity(intent);
    }

    public void onSeekerClick(View view) {
        // Set player type to Seeker
        m_gameData.setIsHider(false);

        // Sends intent that starts GameSelectActivity and sends the current GameData object
        Intent intent = new Intent(this, GameSelectActivity.class);
        intent.putExtra("GameData", m_gameData);
        startActivity(intent);
    }
}