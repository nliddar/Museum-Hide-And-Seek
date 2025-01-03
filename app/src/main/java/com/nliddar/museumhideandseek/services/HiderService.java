package com.nliddar.museumhideandseek.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.nliddar.museumhideandseek.R;
import com.nliddar.museumhideandseek.activities.HiderActivity;
import com.nliddar.museumhideandseek.data.ExhibitData;
import com.nliddar.museumhideandseek.interfaces.ExhibitListenerInterface;
import com.nliddar.museumhideandseek.interfaces.TimerListenerInterface;
import com.nliddar.museumhideandseek.managers.ClueManager;
import com.nliddar.museumhideandseek.managers.ExhibitManager;
import com.nliddar.museumhideandseek.managers.GuessManager;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class HiderService extends Service implements ExhibitListenerInterface {

    // Constant describing the number of seconds the timer should last
    private final int TIMERLENGTH = 180;

    // Constant describing the number of rounds the game should last
    private final int MAXROUND = 3;

    // For communication between service and activities
    private final IBinder m_binder = new LocalBinder();

    // ID of lobby in firestore Database
    String m_lobbyID;

    // ID of player in firestore Database
    String m_playerID;

    // Stores the last received clue
    private String m_lastClue;

    // Stores the last received guess's ID
    private String m_lastGuessID;

    // Stores whether the game has been started
    private boolean m_hasStarted = false;

    // Stores the rounds completed (points to the next round)
    private int m_roundCounter = 1;

    // Object reference to the class that listens to timer updates
    private TimerListenerInterface m_timerListenerInterface;

    // Object reference to the clue manager
    private ClueManager m_clueManager;

    // Object reference to the guess manager
    private GuessManager m_guessManager;

    // Object reference to the exhibit manager
    private ExhibitManager m_exhibitManager;

    // Allows access to HiderService's instance
    public class LocalBinder extends Binder {
        public HiderService getHiderService() {
            return HiderService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return m_binder;
    }

    // Creates a new HiderService if not already created
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Receives intent that communicates ID's
        if(intent != null && intent.hasExtra("LobbyID") && intent.hasExtra("PlayerID")) {
            m_lobbyID = intent.getStringExtra("LobbyID");
            m_playerID = intent.getStringExtra("PlayerID");

            // Initialise the ClueManager
            m_clueManager = new ClueManager(m_playerID, m_lobbyID, null);

            // Initialise the GuessManager
            m_guessManager = new GuessManager(m_playerID, m_lobbyID, null);

            // Initialise the ExhibitManager
            m_exhibitManager = new ExhibitManager(m_playerID, m_lobbyID, this);

            // Create Listeners
            startListener();
            endListener();
        }

        return START_STICKY;
    }

    // Creates notification alerting a user that a game is in progress
    @SuppressLint("DefaultLocale")
    private void createGameNotification(int timePassed, int roundNum) {
        // Get Notification colour
        int notificationColour = ContextCompat.getColor(getApplicationContext(), R.color.red_2);

        // Create the notification string
        String notificationString = String.format("Submit: %02d:%02d. Guesses Left: %d", timePassed / 60, timePassed % 60, roundNum);

        // Set up notification channel
        NotificationChannel notificationChannel =
                new NotificationChannel("hider_1", "Hider Game",
                        NotificationManager.IMPORTANCE_LOW);

        notificationChannel.setSound(null, null);
        notificationChannel.setDescription("Notification that a Hiders game is in progress.");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);

        // Create an intent that starts HiderActivity when notification is clicked
        Intent intentOnClick = new Intent(this, HiderActivity.class);
        intentOnClick.putExtra("PlayerID", m_playerID);
        intentOnClick.putExtra("LobbyID", m_lobbyID);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intentOnClick,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Creates the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "hider_1")
                .setSmallIcon(R.drawable.hider)
                .setContentTitle("Hide and Seek")
                .setContentText(notificationString)
                .setContentIntent(pendingIntent)
                .setColor(notificationColour);

        Notification hiderNotification = builder.build();
        // HiderService is now foregrounded as game is running
        startForeground(1, hiderNotification);
    }

    // Master timer updates UI and invokes game logic checks
    public void masterTimer() {
        new Thread(new Runnable() {

            // Stores the number of seconds that have passed
            int secondCount = 0;

            @Override
            public void run() {
                // Breaks when location is unchanged for TIMERLENGTH seconds
                while(secondCount <= TIMERLENGTH){
                    // Wait 1 second
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    secondCount++;

                    // Update listeners
                    if (m_timerListenerInterface != null) {
                        // Update time counting down
                        m_timerListenerInterface.onTimerUpdate(TIMERLENGTH - secondCount);

                        // Update rounds left
                        m_timerListenerInterface.onRoundUpdate(MAXROUND - (m_roundCounter - 1));
                    }
                    // Create notification
                    createGameNotification(TIMERLENGTH - secondCount, MAXROUND - (m_roundCounter - 1));
                }

                // Get the voted exhibit and reset all votes to 0
                m_exhibitManager.getVotedExhibit(true);
            }
        }).start();
    }

    // Called when the chosen exhibit is returned
    @Override
    public void onVotedExhibitReturn(ExhibitData chosenExhibit) {
        // Save the ID of the chosen exhibit
        m_lastGuessID = chosenExhibit.getID();

        // Check if the chosen exhibit is the target
        if (chosenExhibit.isTarget()) {
            // Seekers have voted for the correct target
            updateEnd(1);
        }
        else {
            // Seekers have not voted for the correct target so continue
            checkContinue();
        }
    }

    // Called to start the game
    public void startGame() {
        // If not already started
        if (!m_hasStarted) {
            // Start the game
            updateStart(1);
            m_hasStarted = true;
        }
    }

    public void gameContinue() {
        // Increment roundCounter
        m_roundCounter++;

        // Update listener that its the next round
        if (m_timerListenerInterface != null) {
            // Update round ended
            m_timerListenerInterface.onNextRound();
        }

        // Start the master timer that will update UI listeners and re-invoke game logic checks
        masterTimer();
    }

    public void gameSuccess() {
        // Service is not foregrounded as game has stopped, removes the notification
        stopForeground(true);

        // Update listeners
        if (m_timerListenerInterface != null) {
            m_timerListenerInterface.onGameEnd(true);
        }
        // Game ended so stop service
        stopSelf();
    }

    public void gameFailed() {
        // Service is not foregrounded as game has stopped, removes the notification
        stopForeground(true);

        // Update listeners
        if (m_timerListenerInterface != null) {
            m_timerListenerInterface.onGameEnd(false);
        }
        // Game ended so stop service
        stopSelf();
    }

    public void checkContinue() {
        // If there are rounds left
        if (m_roundCounter <= MAXROUND) {
            // Start next round
            updateStart(m_roundCounter);

            // Add new clue to db
            m_clueManager.createClue(m_lastClue);

            // Add new guess to db
            m_guessManager.createGuess(m_lastGuessID);

            // Clear seeker votes
            clearPlayerVotes();
        }
        // Else the last round has been played
        else {
            // Seekers have run out of rounds
            updateEnd(-1);
        }
    }

    public void updateStart(int round) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Set the lobbies start flag
        lobbyDocument.update("start", round)
                .addOnSuccessListener(unused -> Log.d("updateStart", "onSuccess: start updated"))
                .addOnFailureListener(e -> Log.w("updateStart", "onFailure: failed to update start"));
    }

    // Listens for changes to the start flag
    public void startListener() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Creates a lobby listener that runs when an event occurs
        lobbyDocument.addSnapshotListener((lobbySnapshot, error) -> {
            Log.d("startListener", "onEvent: Event occurred");

            if (lobbySnapshot != null) {
                // Get start field
                Long start = lobbySnapshot.getLong("start");

                // If start > 1 and start is not equal to the current round then continue game
                if (start != null && start > 0 && start != m_roundCounter - 1) {
                    gameContinue();
                }
            }
        });
    }

    public void updateEnd(int flag) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Set the lobbies end flag
        lobbyDocument.update("end", flag)
                .addOnSuccessListener(unused -> Log.d("updateEnd", "onSuccess: end updated"))
                .addOnFailureListener(e -> Log.w("updateEnd", "onFailure: failed to update end"));
    }

    // Listens for changes to the end flag
    public void endListener() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Creates a lobby listener that runs when an event occurs
        lobbyDocument.addSnapshotListener((lobbySnapshot, error) -> {
            Log.d("endListener", "onEvent: Event occurred");

            if (lobbySnapshot != null) {
                // Get end field
                Long end = lobbySnapshot.getLong("end");

                if (end != null) {
                    // If end = 1 players win
                    if (end == 1) {
                        gameSuccess();
                    }
                    // If end = -1 players lose
                    if (end == -1) {
                        gameFailed();
                    }
                }
            }
        });
    }

    // Used to reset player votes each round
    public void clearPlayerVotes() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyReference = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the exhibits collection within the lobby
        CollectionReference playersCollection = lobbyReference.collection("players");

        playersCollection.get()
                .addOnSuccessListener(playersSnapshot -> {
                    // For each player
                    for (DocumentSnapshot player : playersSnapshot) {
                        // Set votedExhibit to null
                        player.getReference().update("votedExhibit", null);
                    }
                })
                .addOnFailureListener( e -> Log.w("HiderService", "clearPlayerVotes: Could not find players"));
    }

    public void setLastClue(String lastClue) {
        m_lastClue = lastClue;
    }

    public void setTimerListenerInterface(TimerListenerInterface timerListenerInterface) {
        m_timerListenerInterface = timerListenerInterface;
    }
}