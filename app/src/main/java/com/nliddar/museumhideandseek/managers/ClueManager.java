package com.nliddar.museumhideandseek.managers;

import android.util.Log;

import com.nliddar.museumhideandseek.data.ClueData;
import com.nliddar.museumhideandseek.interfaces.ClueListenerInterface;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClueManager {

    // ID of lobby in firestore Database
    String m_lobbyID;

    // ID of player in firestore Database
    String m_playerID;

    // The class implementing this interface will receive clue updates
    ClueListenerInterface m_clueListenerInterface;

    public ClueManager(String playerID, String lobbyID, ClueListenerInterface clueListenerInterface) {
        m_playerID = playerID;
        m_lobbyID = lobbyID;
        m_clueListenerInterface =  clueListenerInterface;

        // Start the clue listener
        clueListener();
    }

    public void getClueList() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ArrayList containing the clues
        ArrayList<ClueData> clueList = new ArrayList<>();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the clue collection within the lobby
        CollectionReference clueCollection = lobbyDocument.collection("clue");

        clueCollection.orderBy("number", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(clueSnapshot -> {
                    // For each clue
                    for (DocumentSnapshot clue : clueSnapshot) {
                        // Get clue details
                        String message = clue.getString("message");
                        Long number = clue.getLong("number");
                        Boolean hasSeen = clue.getBoolean("hasSeen");

                        if (message != null && number != null && hasSeen != null)
                        {
                            // Add clue to clue list
                            clueList.add(new ClueData(Math.toIntExact(number), message, hasSeen));
                        }
                    }

                    if (m_clueListenerInterface != null) {
                        // Alert clueListenerInterface that the clue is has been updated
                        m_clueListenerInterface.onClueListUpdate(clueList);
                    }
                })
                .addOnFailureListener( e -> Log.w("ClueManager", "getClueList: Could not find clues"));
    }

    public void getClueCount() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the clue collection within the lobby
        CollectionReference clueCollection = lobbyDocument.collection("clue");

        clueCollection.get()
                .addOnSuccessListener(clueSnapshot -> {
                    if (m_clueListenerInterface != null) {
                        // Alert clueListenerInterface that the clue count has been updated
                        m_clueListenerInterface.onClueCountUpdate(clueSnapshot.size());
                    }
                })
                .addOnFailureListener( e -> Log.w("ClueManager", "getClueList: Could not find clues"));
    }

    public void createClue(String message) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the clue collection within the lobby
        CollectionReference clueCollection = lobbyDocument.collection("clue");

        // Get the number of clues
        clueCollection.get()
                .addOnSuccessListener(clueSnapshot -> {
                    // Get the next clue number
                    int number = clueSnapshot.size() + 1;

                    // Create clue information
                    Map<String, Object> clueInformation = new HashMap<>();
                    clueInformation.put("hasSeen", false);
                    clueInformation.put("message", message);
                    clueInformation.put("number", number);

                    // Add clue
                    clueCollection.add(clueInformation)
                            .addOnFailureListener( e -> Log.w("ClueManager", "createClue: Could not add clue"));

                })
                .addOnFailureListener( e -> Log.w("ClueManager", "createClue: Could not find clues"));
    }

    public void setSeen() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the clue collection within the lobby
        CollectionReference clueCollection = lobbyDocument.collection("clue");

        // Get all clues
        clueCollection.get()
                .addOnSuccessListener(clueSnapshot -> {
                    // For each clue
                    for (DocumentSnapshot clue : clueSnapshot) {
                        DocumentReference clueReference = clue.getReference();

                        // Update clue as seen
                        clueReference.update("hasSeen", true);
                    }

                })
                .addOnFailureListener( e -> Log.w("ClueManager", "getClueList: Could not find clues"));
    }


    public void clueListener() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the clue collection within the lobby
        CollectionReference clueCollection = lobbyDocument.collection("clue");

        // Creates a listener that runs when any changes are made to the clue collection
        clueCollection.addSnapshotListener((clueSnapshot, error) -> {
            Log.d("clueListener", "onEvent: Event occurred");
            // Update the clue list
            getClueList();
        });
    }
}
