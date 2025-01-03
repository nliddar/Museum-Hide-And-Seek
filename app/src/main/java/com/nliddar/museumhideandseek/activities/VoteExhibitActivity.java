package com.nliddar.museumhideandseek.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.nliddar.museumhideandseek.data.ExhibitData;
import com.nliddar.museumhideandseek.interfaces.ExhibitListenerInterface;
import com.nliddar.museumhideandseek.managers.ExhibitManager;
import com.nliddar.museumhideandseek.adapters.ExhibitRecyclerViewAdapter;
import com.nliddar.museumhideandseek.interfaces.ExhibitRecyclerViewInterface;
import com.nliddar.museumhideandseek.R;
import com.nliddar.museumhideandseek.services.SeekerService;
import com.nliddar.museumhideandseek.interfaces.TimerListenerInterface;

import java.util.ArrayList;
import java.util.Objects;

public class VoteExhibitActivity extends AppCompatActivity implements TimerListenerInterface, ExhibitListenerInterface, ExhibitRecyclerViewInterface {

    // Object reference to SeekerService
    private SeekerService m_seekerService;

    // Tracks whether the activity is currently bound to SeekerService
    private boolean m_isBound = false;

    // ID of lobby in firestore Database
    private String m_lobbyID;

    // ID of player in firestore Database
    private String m_playerID;

    // An arraylist of 'ExhibitData' objects
    private ArrayList<ExhibitData> m_exhibitData;

    // Object reference to the exhibit manager
    private ExhibitManager m_exhibitManager;

    // Stores the number of clues that have currently been seen
    private int m_seenClueCount = 0;

    // Stores the number of guesses that have currently been seen
    private int m_seenGuessCount = 0;

    // Stores the number of chats that have currently been seen
    private int m_seenChatCount = 0;

    // Used to perform actions on the main UI thread
    private final Handler mainHandler = new Handler(Objects.requireNonNull(Looper.myLooper()));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote_exhibit);

        // Receives intent that communicates ID's
        Intent intent = getIntent();
        if(intent != null && intent.hasExtra("LobbyID") && intent.hasExtra("PlayerID")) {
            m_lobbyID = intent.getStringExtra("LobbyID");
            m_playerID = intent.getStringExtra("PlayerID");

            // Keep seen counts
            if(intent.hasExtra("Clue") && intent.hasExtra("Guess") && intent.hasExtra("Chat")) {
                m_seenClueCount = intent.getIntExtra("Clue", 0);
                m_seenGuessCount = intent.getIntExtra("Guess", 0);
                m_seenChatCount = intent.getIntExtra("Chat", 0);
            }
        }

        // Sends intent to service that communicates ID's
        Intent intentService = new Intent(this, SeekerService.class);
        intentService.putExtra("PlayerID", m_playerID);
        intentService.putExtra("LobbyID", m_lobbyID);
        // Start then bind to the service
        startService(intentService);
        bindService(intentService, serviceConnection, Context.BIND_AUTO_CREATE);

        // Initialise the exhibit manager
        m_exhibitManager = new ExhibitManager(
                m_playerID,
                m_lobbyID,
                ContextCompat.getColor(getApplicationContext(), R.color.red_2),
                ContextCompat.getColor(getApplicationContext(), R.color.blue_2),
                this);

        // Gets the list of exhibits from the database
        m_exhibitManager.getExhibitList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from service if destroyed
        if (m_isBound) {
            // Unbind as timer listener
            m_seekerService.setTimerListenerInterface(null);
            unbindService(serviceConnection);
            m_isBound = false;
        }
    }

    // Handles behaviour when first bound to service
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("VoteExhibitActivity", "onServiceConnected: Successful connection to seeker service");

            // Get reference to SeekerService, set isBound to true
            SeekerService.LocalBinder binder = (SeekerService.LocalBinder) service;
            m_seekerService = binder.getSeekerService();
            m_isBound = true;

            // Set VoteExhibitActivity as a timer listener
            m_seekerService.setTimerListenerInterface(VoteExhibitActivity.this);
        }
        // Handles behaviour if service disconnects
        @Override
        public void onServiceDisconnected(ComponentName name) {
            m_isBound = false;
        }
    };

    // Listens for a change to the exhibitList in m_exhibitManager
    @Override
    public void onExhibitListUpdate(ArrayList<ExhibitData> exhibitList) {
        // Update exhibit list
        m_exhibitData = exhibitList;

        // Display exhibits
        // Update recycler view for exhibits
        RecyclerView exhibitRecyclerView = findViewById(R.id.exhibitList);
        // Create and set ExhibitRecyclerViewAdapter and LinearLayoutManager to exhibitRecyclerView
        exhibitRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ExhibitRecyclerViewAdapter exhibitAdapter = new ExhibitRecyclerViewAdapter(this, m_exhibitData, this);
        exhibitRecyclerView.setAdapter(exhibitAdapter);
    }

    // Called when an exhibit is clicked
    @Override
    public void onItemClick(int position) {
        ExhibitData exhibit = m_exhibitData.get(position);
        // Add a vote to the exhibit
        m_exhibitManager.addVote(exhibit);
    }

    public void onBackClick(View view) {
        // Unbind from service
        if (m_isBound) {
            // Unbind as timer listener
            m_seekerService.setTimerListenerInterface(null);
            unbindService(serviceConnection);
            m_isBound = false;
        }

        // Sends intent that starts SeekerActivity
        Intent intent = new Intent(this, SeekerActivity.class);
        intent.putExtra("PlayerID", m_playerID);
        intent.putExtra("LobbyID", m_lobbyID);
        intent.putExtra("Clue", m_seenClueCount);
        intent.putExtra("Guess", m_seenGuessCount);
        intent.putExtra("Chat", m_seenChatCount);
        startActivity(intent);
    }

    // Called when the timer updates
    @Override
    @SuppressLint("DefaultLocale")
    public void onTimerUpdate(int timePassed) {
        TextView timerText = findViewById(R.id.voteTitleText);

        // Convert seconds to a string containing minutes and seconds
        String timeString = String.format("Vote: %02d:%02d", timePassed / 60, timePassed % 60);

        // Update timer TextView
        // Ensure UI updates occur on main thread
        mainHandler.post(() -> timerText.setText(timeString));
    }

    // Called when the game ends
    @Override
    public void onGameEnd(boolean isSuccess) {
        // Unbind from service
        if (m_isBound) {
            // Unbind as timer listener
            m_seekerService.setTimerListenerInterface(null);
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
        // Not needed
    }
}