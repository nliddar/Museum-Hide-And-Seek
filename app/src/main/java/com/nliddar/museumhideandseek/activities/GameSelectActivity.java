package com.nliddar.museumhideandseek.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.nliddar.museumhideandseek.data.GameData;
import com.nliddar.museumhideandseek.R;
import com.google.android.material.textfield.TextInputEditText;

public class GameSelectActivity extends AppCompatActivity {

    // Object reference to game data
    GameData m_gameData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_select);

        // Receives intent that communicates GameData
        Intent intent = getIntent();
        if(intent != null && intent.hasExtra("GameData")){
            // Get the GameData Object
            m_gameData = (GameData) intent.getSerializableExtra("GameData");
        }
        else {
            // Create a default game data object
            m_gameData = new GameData();
            Log.d("GameSelectActivity", "onCreate: Error no GameData");
        }
    }

    public void onHostPrivateClick(View view) {
        // Set GameData as host
        m_gameData.setIsHost(true);
        // Set GameType to private
        m_gameData.setIsPrivate(true);
        // Get player name
        updateName();

        // Sends intent that starts LobbyActivity and sends the current GameData object
        Intent intent = new Intent(this, LobbyActivity.class);
        intent.putExtra("GameData", m_gameData);
        startActivity(intent);
    }

    public void onJoinPublicClick(View view) {
        // Set GameData as join
        m_gameData.setIsHost(false);
        // Set GameType to public
        m_gameData.setIsPrivate(false);
        // Get player name
        updateName();

        // Sends intent that starts LobbyActivity and sends the current GameData object
        Intent intent = new Intent(this, LobbyActivity.class);
        intent.putExtra("GameData", m_gameData);
        startActivity(intent);
    }

    public void onJoinPrivateClick(View view) {
        // Set GameData as join
        m_gameData.setIsHost(false);
        // Set GameType to private
        m_gameData.setIsPrivate(true);
        // Get player name
        updateName();

        // Sends intent that starts GameCodeActivity and sends the current GameData object
        Intent intent = new Intent(this, GameCodeActivity.class);
        intent.putExtra("GameData", m_gameData);
        startActivity(intent);
    }

    public void updateName() {
        // Update name
        TextInputEditText inputName = findViewById(R.id.inputName);
        m_gameData.setPlayerName(String.valueOf(inputName.getText()));
    }

}