package com.nliddar.museumhideandseek.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nliddar.museumhideandseek.data.GameData;
import com.nliddar.museumhideandseek.viewmodels.LobbyViewModel;
import com.nliddar.museumhideandseek.adapters.PlayerRecyclerViewAdapter;
import com.nliddar.museumhideandseek.R;

public class LobbyActivity extends AppCompatActivity {

    // Object reference to LobbyViewModel
    LobbyViewModel m_lobbyViewModel;

    // Game data object
    GameData m_gameData;

    // ID of lobby in firestore Database
    String m_lobbyID;

    // ID of player in firestore Database
    String m_playerID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        // Create a default game data object
        m_gameData = new GameData();

        // Receives intent that communicates GameData
        Intent intent = getIntent();
        if(intent != null && intent.hasExtra("GameData")) {
            // Get the GameData Object
            m_gameData = (GameData) intent.getSerializableExtra("GameData");
        }
        else {
            Log.d("LobbyActivity", "onCreate: Error no GameData");
        }

        // Create a new LobbyViewModel, pass in hider and seeker playerCard colours
        m_lobbyViewModel = new LobbyViewModel(
                m_gameData,
                ContextCompat.getColor(getApplicationContext(), R.color.red_1),
                ContextCompat.getColor(getApplicationContext(), R.color.blue_1));

        // Start observers
        observeCode();
        observeHiderList();
        observeSeekerList();
        observeIsPrivate();
        observeFailureExit();
        observeStart();
        observeLobbyID();
        observePlayerID();

        // Select and create/join a lobby
        m_lobbyViewModel.lobbySelector();
    }

    public void onBackClick(View view) {
        // Call deleteCurrentPlayer in the viewModel
        m_lobbyViewModel.deleteCurrentPlayer();

        // Sends intent that starts MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void startOnClick(View view) {
        // Call updateStart in the viewModel
        m_lobbyViewModel.updateStart(true);
    }

    // Listens for a change to the hiderList live data object in m_lobbyViewModel
    public void observeHiderList() {
        m_lobbyViewModel.getHiderList().observe(this, hiderList -> {
            // Update recycler view for Hiders
            RecyclerView hiderRecyclerView = findViewById(R.id.hiderList);
            // Create and set PlayerRecyclerViewAdapter and LinearLayoutManager to hiderRecyclerView
            hiderRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            PlayerRecyclerViewAdapter hiderAdapter = new PlayerRecyclerViewAdapter(this, hiderList);
            hiderRecyclerView.setAdapter(hiderAdapter);
        });
    }

    // Listens for a change to the seekerList live data object in m_lobbyViewModel
    public void observeSeekerList() {
        m_lobbyViewModel.getSeekerList().observe(this, seekerList -> {
            // Update recycler view for Seekers
            RecyclerView seekerRecyclerView = findViewById(R.id.seekerList);
            // Create and set ActivityRecyclerViewAdapter and LinearLayoutManager to activityRecyclerView
            seekerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            PlayerRecyclerViewAdapter seekerAdapter = new PlayerRecyclerViewAdapter(this, seekerList);
            seekerRecyclerView.setAdapter(seekerAdapter);
        });
    }

    // Listens for a change to the lobbyCode live data object in m_lobbyViewModel
    public void observeCode() {
        m_lobbyViewModel.getLobbyCode().observe(this, lobbyCode -> {
            // Code TextView
            TextView codeText = findViewById(R.id.codeText);

            // Update Code
            String gameCodeString = String.format("Code: " + lobbyCode.toString());
            codeText.setText(gameCodeString);
        });
    }

    // Listens for a change to the IsPrivate live data object in m_lobbyViewModel
    public void observeIsPrivate() {
        m_lobbyViewModel.getLobbyIsPrivate().observe(this, isPrivate -> {
            // Title TextView
            TextView titleText = findViewById(R.id.lobbyText);
            // Background Layout
            ConstraintLayout backgroundLayout = findViewById(R.id.background);

            // Update UI based on isPrivate
            if (isPrivate) {
                // Update Lobby Title
                titleText.setText(R.string.lobby_private_game);

                // Update Background Colour
                int colour = ContextCompat.getColor(getApplicationContext(), R.color.red_2);
                backgroundLayout.setBackgroundColor(colour);
            }
            else {
                // Update Lobby Title
                titleText.setText(R.string.lobby_public_game);

                // Update Background Colour
                int colour = ContextCompat.getColor(getApplicationContext(), R.color.blue_2);
                backgroundLayout.setBackgroundColor(colour);
            }
        });
    }

    // Listens for a change to the start live data object in m_lobbyViewModel
    public void observeStart() {
        m_lobbyViewModel.getLobbyStart().observe(this, start -> {
            // If the player is a hider
            if(m_gameData.isHider()) {
                // Sends intent that starts SelectExhibitActivity
                Intent intent = new Intent(this, SelectExhibitActivity.class);
                intent.putExtra("PlayerID", m_playerID);
                intent.putExtra("LobbyID", m_lobbyID);
                startActivity(intent);
            }
            // If the player is a seeker
            else {
                // Sends intent that starts SeekerActivity
                Intent intent = new Intent(this, SeekerActivity.class);
                intent.putExtra("PlayerID", m_playerID);
                intent.putExtra("LobbyID", m_lobbyID);
                startActivity(intent);
            }
        });
    }

    // Listens for a change to the failureExit live data object in m_lobbyViewModel
    public void observeFailureExit() {
        m_lobbyViewModel.getFailureExit().observe(this, failureExit -> {
            if (failureExit) {
                Log.w("observeFailureExit", "Failure Exit");

                // Sends intent that starts MainActivity
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    // Listens for a change to the playerID live data object in m_lobbyViewModel
    public void observePlayerID() {
        m_lobbyViewModel.getPlayerIDLiveData().observe(this, playerID -> {
            m_playerID = playerID;
        });
    }

    // Listens for a change to the lobbyID live data object in m_lobbyViewModel
    public void observeLobbyID() {
        m_lobbyViewModel.getLobbyIDLiveData().observe(this, lobbyID -> {
            m_lobbyID = lobbyID;
        });
    }

}