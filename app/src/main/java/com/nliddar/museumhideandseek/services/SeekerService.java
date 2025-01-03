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

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.nliddar.museumhideandseek.R;
import com.nliddar.museumhideandseek.activities.SeekerActivity;
import com.nliddar.museumhideandseek.interfaces.TimerListenerInterface;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class SeekerService extends Service {

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

    // Stores the rounds completed (points to the next round)
    private int m_roundCounter = 1;

    // Object reference to the class that listens to timer updates
    private TimerListenerInterface m_timerListenerInterface;

    // Allows access to SeekerService's instance
    public class LocalBinder extends Binder {
        public SeekerService getSeekerService() {
            return SeekerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return m_binder;
    }

    // Creates a new SeekerService if not already created
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Receives intent that communicates ID's
        if(intent != null && intent.hasExtra("LobbyID") && intent.hasExtra("PlayerID")) {
            m_lobbyID = intent.getStringExtra("LobbyID");
            m_playerID = intent.getStringExtra("PlayerID");

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
        int notificationColour = ContextCompat.getColor(getApplicationContext(), R.color.blue_2);

        // Create the notification string
        String notificationString = String.format("Vote: %02d:%02d. Guesses Left: %d", timePassed / 60, timePassed % 60, roundNum);

        // Set up notification channel
        NotificationChannel notificationChannel =
                new NotificationChannel("seeker_1", "Seeker Game",
                        NotificationManager.IMPORTANCE_LOW);

        notificationChannel.setSound(null, null);
        notificationChannel.setDescription("Notification that a Seekers game is in progress.");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);

        // Create an intent that starts SeekerActivity when notification is clicked
        Intent intentOnClick = new Intent(this, SeekerActivity.class);
        intentOnClick.putExtra("PlayerID", m_playerID);
        intentOnClick.putExtra("LobbyID", m_lobbyID);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intentOnClick,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Creates the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "seeker_1")
                .setSmallIcon(R.drawable.search)
                .setContentTitle("Hide and Seek")
                .setContentText(notificationString)
                .setContentIntent(pendingIntent)
                .setColor(notificationColour);

        Notification seekerNotification = builder.build();
        // SeekerService is now foregrounded as game is running
        startForeground(1, seekerNotification);
    }

    // Timer used only to sync seekerActivities and update UI
    public void cosmeticTimer() {
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
                    // Create/Update notification
                    createGameNotification(TIMERLENGTH - secondCount, MAXROUND - (m_roundCounter - 1));
                }
            }
        }).start();
    }

    public void gameContinue() {
        // Increment roundCounter
        m_roundCounter++;

        // Start the cosmetic timer that will update UI listeners
        cosmeticTimer();
    }

    public void gameSuccess() {
        // Service is not foregrounded as game has stopped, removes the notification
        stopForeground(true);

        // Update listener
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

    // Listens for changes to the start flag
    public void startListener() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Creates a lobby listener that runs when an event occurs
        lobbyDocument.addSnapshotListener((lobbySnapshot, error) -> {
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

    // Listens for changes to the end flag
    public void endListener() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Creates a lobby listener that runs when an event occurs
        lobbyDocument.addSnapshotListener((lobbySnapshot, error) -> {
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

    public void setTimerListenerInterface(TimerListenerInterface timerListenerInterface) {
        m_timerListenerInterface = timerListenerInterface;
    }
}