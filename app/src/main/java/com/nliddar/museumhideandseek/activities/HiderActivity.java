package com.nliddar.museumhideandseek.activities;

import androidx.appcompat.app.AppCompatActivity;
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

import com.nliddar.museumhideandseek.data.ChatData;
import com.nliddar.museumhideandseek.interfaces.ChatListenerInterface;
import com.nliddar.museumhideandseek.managers.ChatManager;
import com.nliddar.museumhideandseek.adapters.ChatRecyclerViewAdapter;
import com.nliddar.museumhideandseek.data.ClueData;
import com.nliddar.museumhideandseek.interfaces.ClueListenerInterface;
import com.nliddar.museumhideandseek.managers.ClueManager;
import com.nliddar.museumhideandseek.adapters.ClueRecyclerViewAdapter;
import com.nliddar.museumhideandseek.data.GuessData;
import com.nliddar.museumhideandseek.interfaces.GuessListenerInterface;
import com.nliddar.museumhideandseek.managers.GuessManager;
import com.nliddar.museumhideandseek.adapters.GuessRecyclerViewAdapter;
import com.nliddar.museumhideandseek.services.HiderService;
import com.nliddar.museumhideandseek.R;
import com.nliddar.museumhideandseek.interfaces.TimerListenerInterface;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class HiderActivity extends AppCompatActivity implements ClueListenerInterface, GuessListenerInterface, ChatListenerInterface, TimerListenerInterface {

    // Constant list of hints
    public final static String[] HINTS = {
            "Age", "Location", "Function", "Material",
            "Utility", "Climate", "Users", "Craftsmanship",
            "Ownership", "Aesthetics", "Value", "Rarity"};

    // Object reference to HiderService
    private HiderService m_hiderService;

    // Tracks whether the activity is currently bound to HiderService
    private boolean m_isBound = false;

    // Object reference to the clue manager
    private ClueManager m_clueManager;

    // Object reference to the guess manager
    private GuessManager m_guessManager;

    // Object reference to the chat manager
    private ChatManager m_chatManager;

    // ID of lobby in firestore Database
    private String m_lobbyID;

    // ID of player in firestore Database
    private String m_playerID;

    // Stores the number of clues that have currently been seen
    private int m_seenClueCount = 0;

    // Stores the number of guesses that have currently been seen
    private int m_seenGuessCount = 0;

    // Stores the number of chats that have currently been seen
    private int m_seenChatCount = 0;

    // Stores current hint
    private String m_currentHint;

    // Used to perform actions on the main UI thread
    private final Handler mainHandler = new Handler(Objects.requireNonNull(Looper.myLooper()));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hider);

        // Receives intent that communicates ID's
        Intent intent = getIntent();
        if(intent != null && intent.hasExtra("LobbyID") && intent.hasExtra("PlayerID")) {
            m_lobbyID = intent.getStringExtra("LobbyID");
            m_playerID = intent.getStringExtra("PlayerID");

            // Keep previous hint
            if(intent.hasExtra("Hint")) {
                TextView hintText = findViewById(R.id.hintText);
                hintText.setText(intent.getStringExtra("Hint"));
                m_currentHint = intent.getStringExtra("Hint");
            }

            // Keep seen counts
            if(intent.hasExtra("Clue") && intent.hasExtra("Guess") && intent.hasExtra("Chat")) {
                m_seenClueCount = intent.getIntExtra("Clue", 0);
                m_seenGuessCount = intent.getIntExtra("Guess", 0);
                m_seenChatCount = intent.getIntExtra("Chat", 0);
            }
        }
        else {
            Log.w("HiderActivity", "onCreate: failed to receive intents");
            return;
        }

        // Sends intent to service that communicates ID's
        Intent intentService = new Intent(this, HiderService.class);
        intentService.putExtra("PlayerID", m_playerID);
        intentService.putExtra("LobbyID", m_lobbyID);
        // Start then bind to the service
        startService(intentService);
        bindService(intentService, serviceConnection, Context.BIND_AUTO_CREATE);

        // Initialise the ClueManager
        m_clueManager = new ClueManager(m_playerID, m_lobbyID, this);

        // Initialise the GuessManager
        m_guessManager = new GuessManager(m_playerID, m_lobbyID, this);

        // Initialise the ChatManager
        m_chatManager = new ChatManager(m_playerID, m_lobbyID, this);

        // Get the clues stored in the db, automatically initialises recycler view
        m_clueManager.getClueList();

        // Get the guesses stored in the db, automatically initialises recycler view
        m_guessManager.getGuessList();
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
            m_hiderService.setTimerListenerInterface(HiderActivity.this);

            // Bound to service, so start the game
            m_hiderService.startGame();
        }
        // Handles behaviour if service disconnects
        @Override
        public void onServiceDisconnected(ComponentName name) {
            m_isBound = false;
        }
    };

    @Override
    public void onClueListUpdate(ArrayList<ClueData> clueList) {
        // Update the list of clues
        updateClueCardList(clueList);

        // Update notifications if clue count has increased
        if (clueList.size() > m_seenClueCount) {
            // Update notifications with clue count difference
            updateClueNotifications(clueList.size() - m_seenClueCount);
        }
    }

    @Override
    public void onClueCountUpdate(int clueCount) {
        // Update the seen clue count
        m_seenClueCount = clueCount;
    }

    public void updateClueCardList(ArrayList<ClueData> clueList) {
        // Update recycler view for Clues
        RecyclerView clueRecyclerView = findViewById(R.id.clueList);
        // Create and set ClueRecyclerViewAdapter and LinearLayoutManager to clueRecyclerView
        clueRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ClueRecyclerViewAdapter clueAdapter = new ClueRecyclerViewAdapter(this, clueList);
        clueRecyclerView.setAdapter(clueAdapter);
    }

    public void updateClueNotifications(int notificationCount) {
        TextView notificationText = findViewById(R.id.cluesNotificationText);

        // Update notification number
        notificationText.setText(String.valueOf(notificationCount));
    }

    // Called when user clears notifications
    public void onClueCardClick(View view) {
        // Update m_seenClueCount to reflect that all current clues have been seen
        m_clueManager.getClueCount();

        // Update clue notifications to reflect that all current clues have been seen
        updateClueNotifications(0);
    }

    @Override
    public void onGuessListUpdate(ArrayList<GuessData> guessList) {
        // Update the list of guesses
        updateGuessCardList(guessList);

        // Update notifications if guess count has increased
        if (guessList.size() > m_seenGuessCount) {
            // Update notifications with guess count difference
            updateGuessNotifications(guessList.size() - m_seenGuessCount);
        }
    }

    @Override
    public void onGuessCountUpdate(int guessCount) {
        // Update the seen clue count
        m_seenGuessCount = guessCount;
    }

    public void updateGuessCardList(ArrayList<GuessData> guessList) {
        // Update recycler view for Guesses
        RecyclerView guessRecyclerView = findViewById(R.id.guessList);
        // Create and set GuessRecyclerViewAdapter and LinearLayoutManager to guessRecyclerView
        guessRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        GuessRecyclerViewAdapter guessAdapter = new GuessRecyclerViewAdapter(this, guessList);
        guessRecyclerView.setAdapter(guessAdapter);
    }

    public void updateGuessNotifications(int notificationCount) {
        TextView notificationText = findViewById(R.id.guessNotificationText);

        // Update notification number
        notificationText.setText(String.valueOf(notificationCount));
    }

    // Called when user clears notifications
    public void onGuessCardClick(View view) {
        // Update m_seenGuessCount to reflect that all current guesses have been seen
        m_guessManager.getGuessCount();

        // Update clue notifications to reflect that all current guesses have been seen
        updateGuessNotifications(0);

        // Alert DB that this player has seen all guesses
        m_guessManager.setSeen();
    }

    @Override
    public void onChatListUpdate(ArrayList<ChatData> chatList) {
        // Update the list of chats
        updateChatCardList(chatList);

        // Update notifications if chat count has increased
        if (chatList.size() > m_seenChatCount) {
            // Update notifications with chat count difference
            updateChatNotifications(chatList.size() - m_seenChatCount);
        }
    }

    @Override
    public void onChatCountUpdate(int chatCount) {
        // Update the seen chat count
        m_seenChatCount = chatCount;
    }

    public void updateChatCardList(ArrayList<ChatData> chatList) {
        // Update recycler view for Chats
        RecyclerView chatRecyclerView = findViewById(R.id.chatList);
        // Create and set ChatRecyclerViewAdapter and LinearLayoutManager to chatRecyclerView
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ChatRecyclerViewAdapter chatAdapter = new ChatRecyclerViewAdapter(this, chatList);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    public void updateChatNotifications(int notificationCount) {
        TextView notificationText = findViewById(R.id.chatNotificationText);

        // Update notification number
        notificationText.setText(String.valueOf(notificationCount));
    }

    // Called when user clears notifications
    public void onChatCardClick(View view) {
        // Update m_seenChatCount to reflect that all current chats have been seen
        m_chatManager.getChatCount();

        // Update chat notifications to reflect that all current chats have been seen
        updateChatNotifications(0);
    }

    @Override
    @SuppressLint("DefaultLocale")
    public void onTimerUpdate(int timePassed) {
        TextView timerText = findViewById(R.id.timerText);
        TextView newClueText = findViewById(R.id.newClueInput);

        // Convert seconds to a string containing minutes and seconds
        String timeString = String.format("Submit: %02d:%02d", timePassed / 60, timePassed % 60);

        // Update timer TextView
        // Ensure UI updates occur on main thread
        mainHandler.post(() -> timerText.setText(timeString));

        // Update service with current clue state
        m_hiderService.setLastClue(newClueText.getText().toString());
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
    @SuppressLint("DefaultLocale")
    public void onRoundUpdate(int roundNum) {
        TextView roundText = findViewById(R.id.guessLeftText);

        String guessLeftString = String.format("Guesses Left: %d", roundNum);

        // Update round TextView
        // Ensure UI updates occur on main thread
        mainHandler.post(() -> roundText.setText(guessLeftString));
    }

    @Override
    public void onNextRound() {
        TextView hintText = findViewById(R.id.hintText);

        // Create a Random object
        Random random = new Random();


        // Update hintText with a random hint
        m_currentHint = String.format("Hint: " + HINTS[random.nextInt(12)]);

        hintText.setText(m_currentHint);
    }

    // Runs when the Exhibit Information button is pressed
    public void onInfoClick(View view) {
        // Unbind from service
        if (m_isBound) {
            // Unbind as timer listener
            m_hiderService.setTimerListenerInterface(null);
            unbindService(serviceConnection);
            m_isBound = false;
        }

        // Sends intent that starts ExhibitInfoActivity
        Intent intent = new Intent(this, ExhibitInfoActivity.class);
        intent.putExtra("PlayerID", m_playerID);
        intent.putExtra("LobbyID", m_lobbyID);
        intent.putExtra("Hint", m_currentHint);
        intent.putExtra("Clue", m_seenClueCount);
        intent.putExtra("Guess", m_seenGuessCount);
        intent.putExtra("Chat", m_seenChatCount);
        startActivity(intent);
    }
}