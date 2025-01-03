package com.nliddar.museumhideandseek.activities;

import static com.nliddar.museumhideandseek.activities.HiderActivity.HINTS;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.nliddar.museumhideandseek.services.HiderService;
import com.nliddar.museumhideandseek.R;
import com.nliddar.museumhideandseek.interfaces.TimerListenerInterface;

import java.util.Random;

public class ExhibitInfoActivity extends AppCompatActivity implements TimerListenerInterface {

    // Object reference to HiderService
    private HiderService m_hiderService;

    // Tracks whether the activity is currently bound to HiderService
    private boolean m_isBound = false;

    // ID of lobby in firestore Database
    String m_lobbyID;

    // ID of player in firestore Database
    String m_playerID;

    // Stores the number of clues that have currently been seen
    private int m_seenClueCount = 0;

    // Stores the number of guesses that have currently been seen
    private int m_seenGuessCount = 0;

    // Stores the number of chats that have currently been seen
    private int m_seenChatCount = 0;

    // Stores current hint
    private String m_currentHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exhibit_info);

        // Receives intent that communicates ID's
        Intent intent = getIntent();
        if(intent != null && intent.hasExtra("LobbyID") && intent.hasExtra("PlayerID")) {
            m_lobbyID = intent.getStringExtra("LobbyID");
            m_playerID = intent.getStringExtra("PlayerID");

            // Keep previous hint
            if(intent.hasExtra("Hint")) {
                m_currentHint = intent.getStringExtra("Hint");
            }

            // Keep seen counts
            if(intent.hasExtra("Clue") && intent.hasExtra("Guess") && intent.hasExtra("Chat")) {
                m_seenClueCount = intent.getIntExtra("Clue", 0);
                m_seenGuessCount = intent.getIntExtra("Guess", 0);
                m_seenChatCount = intent.getIntExtra("Chat", 0);
            }
        }

        // Sends intent to service that communicates ID's
        Intent intentService = new Intent(this, HiderService.class);
        intentService.putExtra("PlayerID", m_playerID);
        intentService.putExtra("LobbyID", m_lobbyID);
        // Start then bind to the service
        startService(intentService);
        bindService(intentService, serviceConnection, Context.BIND_AUTO_CREATE);

        // Set the webView
        WebView webView = findViewById(R.id.webInfo);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://en.wikipedia.org/wiki/Rosetta_Stone");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from service if destroyed
        if (m_isBound) {
            // Unbind as timer listener
            m_hiderService.setTimerListenerInterface(null);
            unbindService(serviceConnection);
            m_isBound = false;
        }
    }

    // Handles behaviour when first bound to service
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("Library", "onServiceConnected: Successful connection to hider service");

            // Get reference to HiderService, set isBound to true
            HiderService.LocalBinder binder = (HiderService.LocalBinder) service;
            m_hiderService = binder.getHiderService();
            m_isBound = true;

            // Set HiderActivity as a timer listener
            m_hiderService.setTimerListenerInterface(ExhibitInfoActivity.this);
        }
        // Handles behaviour if service disconnects
        @Override
        public void onServiceDisconnected(ComponentName name) {
            m_isBound = false;
        }
    };

    public void onBackClick(View view) {
        // Unbind from service
        if (m_isBound) {
            // Unbind as timer listener
            m_hiderService.setTimerListenerInterface(null);
            unbindService(serviceConnection);
            m_isBound = false;
        }

        // Sends intent that starts HiderActivity
        Intent intent = new Intent(this, HiderActivity.class);
        intent.putExtra("PlayerID", m_playerID);
        intent.putExtra("LobbyID", m_lobbyID);
        intent.putExtra("Hint", m_currentHint);
        intent.putExtra("Clue", m_seenClueCount);
        intent.putExtra("Guess", m_seenGuessCount);
        intent.putExtra("Chat", m_seenChatCount);
        startActivity(intent);
    }

    @Override
    public void onTimerUpdate(int timePassed) {
        // Not Used
    }

    // Change the hint
    @Override
    public void onNextRound() {
        // Create a Random object
        Random random = new Random();

        // Update hintText with a random hint
        m_currentHint = String.format("Hint: " + HINTS[random.nextInt(12)]);
    }

    @Override
    public void onGameEnd(boolean isSuccess) {
        // Unbind from service
        if (m_isBound) {
            // Unbind as timer listener
            m_hiderService.setTimerListenerInterface(null);
            unbindService(serviceConnection);
            m_isBound = false;
        }

        if (isSuccess) {
            // Sends intent that starts WonActivity
            Intent intent = new Intent(this, WonActivity.class);
            startActivity(intent);
        }
        else {
            // Sends intent that starts LostActivity
            Intent intent = new Intent(this, LostActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onRoundUpdate(int roundNum) {
        // Not Used
    }
}