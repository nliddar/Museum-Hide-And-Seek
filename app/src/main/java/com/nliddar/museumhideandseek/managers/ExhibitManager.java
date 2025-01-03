package com.nliddar.museumhideandseek.managers;

import android.util.Log;

import com.nliddar.museumhideandseek.data.ExhibitData;
import com.nliddar.museumhideandseek.interfaces.ExhibitListenerInterface;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ExhibitManager {

    // ID of lobby in firestore Database
    String m_lobbyID;

    // ID of player in firestore Database
    String m_playerID;

    // Used for alternating exhibit list colours
    int m_colour1;

    // Used for alternating exhibit list colours
    int m_colour2;

    // The class implementing this interface will receive chat updates
    ExhibitListenerInterface m_exhibitListenerInterface;

    public ExhibitManager(String playerID, String lobbyID, int colour1, int colour2, ExhibitListenerInterface exhibitListenerInterface) {
        m_playerID = playerID;
        m_lobbyID = lobbyID;
        m_exhibitListenerInterface =  exhibitListenerInterface;
        m_colour1 = colour1;
        m_colour2 = colour2;

        // Start the exhibit listener
        exhibitListener();
    }

    // Constructor overloading if not displaying exhibits
    public ExhibitManager(String playerID, String lobbyID, ExhibitListenerInterface exhibitListenerInterface) {
        m_playerID = playerID;
        m_lobbyID = lobbyID;
        m_exhibitListenerInterface =  exhibitListenerInterface;

        // Start the exhibit listener
        exhibitListener();
    }

    public void getExhibitList() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ArrayList containing the exhibits
        ArrayList<ExhibitData> exhibitList = new ArrayList<>();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the exhibits collection within the lobby
        CollectionReference exhibitsCollection = lobbyDocument.collection("gameExhibits");

        exhibitsCollection.get()
                .addOnSuccessListener(exhibitsSnapshot -> {
                    // Used to alternate colours
                    boolean colourSwitch = true;

                    // For each exhibit
                    for (DocumentSnapshot exhibit : exhibitsSnapshot) {
                        // Get exhibit details
                        String name = exhibit.getString("name");
                        String desc = exhibit.getString("desc");
                        Long votes = exhibit.getLong("votes");
                        if (votes == null) {
                            votes = 0L;
                        }

                        // Alternate colours
                        colourSwitch = !colourSwitch;
                        int colour = m_colour1;
                        if(colourSwitch){
                            colour = m_colour2;
                        }

                        // Add exhibit to exhibit list
                        exhibitList.add(new ExhibitData(exhibit.getId(), name, desc, colour, Math.toIntExact(votes)));
                    }

                    // Update live data objects
                    m_exhibitListenerInterface.onExhibitListUpdate(exhibitList);
                })
                .addOnFailureListener( e -> Log.w("ExhibitManager", "getExhibitList: Could not find exhibits"));
    }

    public void setIsTarget(ExhibitData exhibitData) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyReference = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the exhibits collection within the lobby
        CollectionReference exhibitsCollection = lobbyReference.collection("gameExhibits");

        // Get the exhibit document
        DocumentReference exhibitsDocument = exhibitsCollection.document(exhibitData.getID());

        // Set isTarget to true
        exhibitsDocument.update("isTarget", true);
    }

    public void addVote(ExhibitData exhibitData) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyReference = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the players collection within the lobby
        CollectionReference playersCollection = lobbyReference.collection("players");

        // Get the player document
        DocumentReference playerDocument = playersCollection.document(m_playerID);

        playerDocument.get()
                .addOnSuccessListener(playerSnapshot -> {
                    // Get currently voted exhibit and new exhibit
                    String votedExhibit = playerSnapshot.getString("votedExhibit");
                    String newExhibit = exhibitData.getID();

                    // If the newly selected exhibit is different to the voted exhibit
                    if (votedExhibit == null || !votedExhibit.equals(newExhibit)) {
                        // If the user has already voted for an exhibit
                        if (votedExhibit != null) {
                            // Remove a vote from the currently voted exhibit
                            decrementVotes(votedExhibit);
                        }

                        // Add a vote to the newly selected exhibit
                        incrementVotes(newExhibit);

                        // Update the player document with the newly voted exhibit
                        playerDocument.update("votedExhibit", newExhibit);
                    }
                })
                .addOnFailureListener( e -> Log.w("ExhibitManager", "addVote: Could not find player"));
    }

    public void incrementVotes(String exhibitID) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyReference = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the exhibits collection within the lobby
        CollectionReference exhibitsCollection = lobbyReference.collection("gameExhibits");

        // Get the exhibit document
        DocumentReference exhibitDocument = exhibitsCollection.document(exhibitID);

        exhibitDocument.get()
                .addOnSuccessListener(exhibitSnapshot -> {
                    // Get current exhibit votes
                    Long currentVotes = exhibitSnapshot.getLong("votes");
                    if (currentVotes != null) {
                        // Increase votes by 1
                        exhibitDocument.update("votes", currentVotes + 1);
                    }
                })
                .addOnFailureListener( e -> Log.w("ExhibitManager", "incrementVotes: Could not find exhibit"));
    }

    public void decrementVotes(String exhibitID) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyReference = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the exhibits collection within the lobby
        CollectionReference exhibitsCollection = lobbyReference.collection("gameExhibits");

        // Get the exhibit document
        DocumentReference exhibitDocument = exhibitsCollection.document(exhibitID);

        exhibitDocument.get()
                .addOnSuccessListener(exhibitSnapshot -> {
                    // Get current exhibit votes
                    Long currentVotes = exhibitSnapshot.getLong("votes");
                    if (currentVotes != null) {
                        // Decrease votes by 1
                        exhibitDocument.update("votes", currentVotes - 1);
                    }
                })
                .addOnFailureListener( e -> Log.w("ExhibitManager", "decrementVotes: Could not find exhibit"));
    }

    public void getVotedExhibit(boolean doReset) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyReference = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the chat collection within the lobby
        CollectionReference exhibitsCollection = lobbyReference.collection("gameExhibits");

        // Get all exhibits
        exhibitsCollection.get()
                .addOnSuccessListener(exhibitsSnapshot -> {
                    // Holders for the chosenExhibit votes and isTarget
                    DocumentSnapshot chosenExhibit = null;
                    long chosenExhibitVotes = -1;
                    boolean chosenExhibitIsTarget = false;

                    // For each exhibit
                    for (DocumentSnapshot exhibit : exhibitsSnapshot) {

                        // Get the exhibit votes and isTarget
                        Long exhibitVotes = exhibit.getLong("votes");
                        Boolean exhibitIsTarget = exhibit.getBoolean("isTarget");

                        if (exhibitVotes != null && exhibitIsTarget != null) {
                            // If the current exhibit has the most votes
                            if (exhibitVotes > chosenExhibitVotes) {
                                // Save exhibit information
                                chosenExhibit = exhibit;
                                chosenExhibitVotes = exhibitVotes;
                                chosenExhibitIsTarget = exhibitIsTarget;
                            }
                        }
                        if (doReset){
                            // Set votes to zero for next round
                            exhibit.getReference().update("votes", 0);
                        }
                    }
                    if (chosenExhibit != null && m_exhibitListenerInterface != null) {
                        // Create ExhibitData object for the chosen exhibit
                        ExhibitData exhibitData = new ExhibitData(chosenExhibit.getId(), chosenExhibit.getString("name"), (int) chosenExhibitVotes, chosenExhibitIsTarget);
                        // Update listeners with the chosen exhibit
                        m_exhibitListenerInterface.onVotedExhibitReturn(exhibitData);
                    }
                })
                .addOnFailureListener( e -> Log.w("HiderService", "performChecks: Could not find gameExhibits"));
    }

    public void exhibitListener() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the exhibit collection within the lobby
        CollectionReference exhibitCollection = lobbyDocument.collection("gameExhibits");

        // Creates a listener that runs when any changes are made to the exhibit collection
        exhibitCollection.addSnapshotListener((chatSnapshot, error) -> {
            Log.d("exhibitListener", "onEvent: Event occurred");
            // Update the exhibit list
            getExhibitList();
        });
    }
}
