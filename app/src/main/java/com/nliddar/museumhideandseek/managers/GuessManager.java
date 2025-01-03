package com.nliddar.museumhideandseek.managers;

import android.util.Log;

import com.nliddar.museumhideandseek.data.GuessData;
import com.nliddar.museumhideandseek.interfaces.GuessListenerInterface;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GuessManager {

    // ID of lobby in firestore Database
    String m_lobbyID;

    // ID of player in firestore Database
    String m_playerID;

    // The class implementing this interface will receive guess updates
    GuessListenerInterface m_guessListenerInterface;

    public GuessManager(String playerID, String lobbyID, GuessListenerInterface guessListenerInterface) {
        m_playerID = playerID;
        m_lobbyID = lobbyID;
        m_guessListenerInterface =  guessListenerInterface;

        // Start the guess listener
        guessListener();
    }

    public void getGuessList() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ArrayList containing the guesses
        ArrayList<GuessData> guessList = new ArrayList<>();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the guess collection within the lobby
        CollectionReference guessCollection = lobbyDocument.collection("guess");

        guessCollection.orderBy("number", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(guessSnapshot -> {
                    // For each guess
                    for (DocumentSnapshot guess : guessSnapshot) {
                        // Get guess details
                        String exhibit = guess.getString("exhibit");
                        Long number = guess.getLong("number");
                        Boolean hasSeen = guess.getBoolean("hasSeen");

                        if (exhibit != null && number != null && hasSeen != null)
                        {
                            // Add guess to guess list
                            guessList.add(new GuessData(Math.toIntExact(number), exhibit, hasSeen));
                        }
                    }

                    if (m_guessListenerInterface != null) {
                        // Alert guessListenerInterface that the guess is has been updated
                        m_guessListenerInterface.onGuessListUpdate(guessList);
                    }
                })
                .addOnFailureListener( e -> Log.w("GuessManager", "getGuessList: Could not find guesss"));
    }

    public void getGuessCount() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the guess collection within the lobby
        CollectionReference guessCollection = lobbyDocument.collection("guess");

        guessCollection.get()
                .addOnSuccessListener(guessSnapshot -> {
                    if (m_guessListenerInterface != null) {
                        // Alert guessListenerInterface that the guess count has been updated
                        m_guessListenerInterface.onGuessCountUpdate(guessSnapshot.size());
                    }
                })
                .addOnFailureListener( e -> Log.w("GuessManager", "getGuessList: Could not find guesses"));
    }

    public void createGuess(String exhibitID) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the guess collection within the lobby
        CollectionReference guessCollection = lobbyDocument.collection("guess");

        // Get reference to the exhibits collection within the lobby
        CollectionReference exhibitsCollection = lobbyDocument.collection("gameExhibits");

        // Get the exhibit document
        DocumentReference exhibitsDocument = exhibitsCollection.document(exhibitID);

        // Get the number of guesses
        guessCollection.get()
                .addOnSuccessListener(guessSnapshot -> {
                    // Get the next guess number
                    int number = guessSnapshot.size() + 1;

                    exhibitsDocument.get()
                            .addOnSuccessListener(exhibitSnapshot -> {
                                // Get the exhibits name
                                String name = exhibitSnapshot.getString("name");

                                // Create guess information
                                Map<String, Object> guessInformation = new HashMap<>();
                                guessInformation.put("hasSeen", false);
                                guessInformation.put("exhibit", name);
                                guessInformation.put("number", number);

                                // Add guess to db
                                guessCollection.add(guessInformation)
                                        .addOnFailureListener( e -> Log.w("GuessManager", "createGuess: Could not add guess"));

                            })
                            .addOnFailureListener( e -> Log.w("GuessManager", "createGuess: Could not find exhibit"));
                })
                .addOnFailureListener( e -> Log.w("GuessManager", "createGuess: Could not find guesses"));
    }

    public void setSeen() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the guess collection within the lobby
        CollectionReference guessCollection = lobbyDocument.collection("guess");

        // Get all guesses
        guessCollection.get()
                .addOnSuccessListener(guessSnapshot -> {
                    // For each guess
                    for (DocumentSnapshot guess : guessSnapshot) {
                        DocumentReference guessReference = guess.getReference();

                        // Update guess as seen
                        guessReference.update("hasSeen", true);
                    }

                })
                .addOnFailureListener( e -> Log.w("GuessManager", "getGuessList: Could not find guesses"));
    }


    public void guessListener() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the guess collection within the lobby
        CollectionReference guessCollection = lobbyDocument.collection("guess");

        // Creates a listener that runs when any changes are made to the guess collection
        guessCollection.addSnapshotListener((guessSnapshot, error) -> {
            Log.d("guessListener", "onEvent: Event occurred");
            // Update the guess list
            getGuessList();
        });
    }
}