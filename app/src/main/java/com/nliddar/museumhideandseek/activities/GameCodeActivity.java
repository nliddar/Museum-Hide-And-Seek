package com.nliddar.museumhideandseek.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.nliddar.museumhideandseek.data.GameData;
import com.nliddar.museumhideandseek.R;

public class GameCodeActivity extends AppCompatActivity {

    // Object reference to game data
    GameData m_gameData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_code);

        // Receives intent that communicates GameData
        Intent intent = getIntent();
        if(intent != null && intent.hasExtra("GameData")){
            // Get the GameData Object
            m_gameData = (GameData) intent.getSerializableExtra("GameData");
        }
        else {
            // Create a default game data object
            m_gameData = new GameData();
            Log.d("GameCodeActivity", "onCreate: Error no GameData");
        }
    }

    public void onBackClick(View view) {
        // Sends intent that starts MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void submitOnClick(View view) {
        // Get code
        EditText inputCode = findViewById(R.id.input_code);
        String code = inputCode.getText().toString();
        if(code.isEmpty()) {
            Log.d("GameCodeActivity", "submitOnClick: Empty Code");
            // Tells the user to enter a code
            code = "Enter Code";
            inputCode.setHint(code);
            // Exit Function
            return;
        }

        try {
            int codeInt = Integer.parseInt(code);

            // Set the game code
            m_gameData.setGameCode(codeInt);

            // Sends intent that starts LobbyActivity and sends the current GameData object
            Intent intent = new Intent(this, LobbyActivity.class);
            intent.putExtra("GameData", m_gameData);
            startActivity(intent);

        } catch (NumberFormatException e) {
            e.printStackTrace();

            Log.d("GameCodeActivity", "submitOnClick: Invalid Code");

            // Tells the user their code was invalid
            code = "Invalid Code";
            inputCode.setText("");
            inputCode.setHint(code);
        }


    }
}