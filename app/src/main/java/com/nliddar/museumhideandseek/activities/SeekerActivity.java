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
import android.widget.EditText;
import android.widget.TextView;

import com.nliddar.museumhideandseek.data.ChatData;
import com.nliddar.museumhideandseek.interfaces.ChatListenerInterface;
import com.nliddar.museumhideandseek.managers.ChatManager;
import com.nliddar.museumhideandseek.adapters.ChatRecyclerViewAdapter;
import com.nliddar.museumhideandseek.data.ClueData;
import com.nliddar.museumhideandseek.interfaces.ClueListenerInterface;
import com.nliddar.museumhideandseek.managers.ClueManager;
import com.nliddar.museumhideandseek.adapters.ClueRecyclerViewAdapter;
import com.nliddar.museumhideandseek.data.ExhibitData;
import com.nliddar.museumhideandseek.interfaces.ExhibitListenerInterface;
import com.nliddar.museumhideandseek.managers.ExhibitManager;
import com.nliddar.museumhideandseek.data.GuessData;
import com.nliddar.museumhideandseek.interfaces.GuessListenerInterface;
import com.nliddar.museumhideandseek.managers.GuessManager;
import com.nliddar.museumhideandseek.adapters.GuessRecyclerViewAdapter;
import com.nliddar.museumhideandseek.R;
import com.nliddar.museumhideandseek.services.SeekerService;
import com.nliddar.museumhideandseek.interfaces.TimerListenerInterface;

import java.util.ArrayList;
import java.util.Objects;

public class SeekerActivity extends AppCompatActivity implements ClueListenerInterface, GuessListenerInterface, ChatListenerInterface, TimerListenerInterface, ExhibitListenerInterface {

    // Object reference to SeekerService
    private SeekerService m_seekerService;

    // Tracks whether the activity is currently bound to SeekerService
    private boolean m_isBound = false;

    // Object reference to the clue manager
    private ClueManager m_clueManager;

    // Object reference to the guess manager
    private GuessManager m_guessManager;

    // Object reference to the chat manager
    private ChatManager m_chatManager;

    // Object reference to the exhibit manager
    private ExhibitManager m_exhibitManager;

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

    // Used to perform actions on the main UI thread
    private final Handler mainHandler = new Handler(Objects.requireNonNull(Looper.myLooper()));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seeker);

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

        // Initialise the ClueManager
        m_clueManager = new ClueManager(m_playerID, m_lobbyID, this);

        // Initialise the GuessManager
        m_guessManager = new GuessManager(m_playerID, m_lobbyID, this);

        // Initialise the ChatManager
        m_chatManager = new ChatManager(m_playerID, m_lobbyID, this);

        // Initialise the ExhibitManager
        m_exhibitManager = new ExhibitManager(m_playerID, m_lobbyID, this);

        // Get the clues stored in the db, automatically initialises recycler view
        m_clueManager.getClueList();

        // Get the guesses stored in the db, automatically initialises recycler view
        m_guessManager.getGuessList();

        // Get the voted exhibit
        m_exhibitManager.getVotedExhibit(false);
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
            Log.d("SeekerActivity", "onServiceConnected: Successful connection to seeker service");

            // Get reference to SeekerService, set isBound to true
            SeekerService.LocalBinder binder = (SeekerService.LocalBinder) service;
            m_seekerService = binder.getSeekerService();
            m_isBound = true;

            // Set SeekerActivity as a timer listener
            m_seekerService.setTimerListenerInterface(SeekerActivity.this);
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

        // Alert DB that this player has seen all clues
        m_clueManager.setSeen();
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

        // Alert DB that this player has seen all chats
        m_chatManager.setSeen();
    }

    public void onChatSendClick(View view) {
        // Get the typed message
        EditText chatInput = findViewById(R.id.chatInput);

        // Create (send) the chat
        m_chatManager.createChat(chatInput.getText().toString());
    }

    // Called every time the exhibit list is updated
    @Override
    public void onExhibitListUpdate(ArrayList<ExhibitData> exhibitList) {
        // Get the voted exhibit
        m_exhibitManager.getVotedExhibit(false);
    }

    // Called when the voted exhibit is returned
    @Override
    public void onVotedExhibitReturn(ExhibitData exhibitData) {
        TextView votedText = findViewById(R.id.selectedExhibitText);

        // Convert seconds to a string containing minutes and seconds
        String votedString = String.format("Selected Exhibit: " + exhibitData.getName());

        // Display the chosen exhibit
        votedText.setText(votedString);
    }

    @Override
    @SuppressLint("DefaultLocale")
    public void onTimerUpdate(int timePassed) {
        TextView timerText = findViewById(R.id.voteText);

        // Convert seconds to a string containing minutes and seconds
        String timeString = String.format("Vote: %02d:%02d", timePassed / 60, timePassed % 60);

        // Update timer TextView
        // Ensure UI updates occur on main thread
        mainHandler.post(() -> timerText.setText(timeString));
    }

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
    @SuppressLint("DefaultLocale")
    public void onRoundUpdate(int roundNum) {
        TextView roundText = findViewById(R.id.guessLeftText);

        String guessLeftString = String.format("Guesses Left: %d", roundNum);

        // Update TextView
        // Ensure UI updates occur on main thread
        mainHandler.post(() -> roundText.setText(guessLeftString));
    }

    // Called when the vote button is pressed
    public void onVoteClick(View view) {
        // Unbind from service
        if (m_isBound) {
            // Unbind as timer listener
            m_seekerService.setTimerListenerInterface(null);
            unbindService(serviceConnection);
            m_isBound = false;
        }

        // Sends intent that starts VoteExhibitActivity
        Intent intent = new Intent(this, VoteExhibitActivity.class);
        intent.putExtra("PlayerID", m_playerID);
        intent.putExtra("LobbyID", m_lobbyID);
        intent.putExtra("Clue", m_seenClueCount);
        intent.putExtra("Guess", m_seenGuessCount);
        intent.putExtra("Chat", m_seenChatCount);
        startActivity(intent);
    }
}