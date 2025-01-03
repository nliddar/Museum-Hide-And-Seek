package com.nliddar.museumhideandseek.viewmodels;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nliddar.museumhideandseek.data.GameData;
import com.nliddar.museumhideandseek.data.PlayerData;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class LobbyViewModel extends ViewModel {

    // Default lobby data object reference used as a basis to create new lobbies
    Map<String, Object> lobbyDefault = new HashMap<>();

    // Player data object reference used to create new player documents
    Map<String, Object> m_playerInformation = new HashMap<>();

    // Object reference to game data
    GameData m_gameData;

    // ID of lobby in firestore Database
    String m_lobbyID;

    // ID of player in firestore Database
    String m_playerID;

    // Stores the colour for the hider
    int m_hiderColour;

    // Stores the colour for the seeker
    int m_seekerColour;

    // Live data objects
    private final MutableLiveData<Boolean> m_lobbyStart = new MutableLiveData<>();

    private final MutableLiveData<Long> m_lobbyCode= new MutableLiveData<>();

    private final MutableLiveData<Boolean> m_lobbyIsPrivate = new MutableLiveData<>();

    private final MutableLiveData<ArrayList<PlayerData>> m_hiderList = new MutableLiveData<>();

    private final MutableLiveData<ArrayList<PlayerData>> m_seekerList = new MutableLiveData<>();

    private final MutableLiveData<Boolean> m_failureExit = new MutableLiveData<>();

    private final MutableLiveData<String> m_lobbyIDLiveData = new MutableLiveData<>();

    private final MutableLiveData<String> m_playerIDLiveData = new MutableLiveData<>();

    public LobbyViewModel(GameData gameData, int hiderColour, int seekerColour) {
        m_gameData = gameData;
        m_hiderColour = hiderColour;
        m_seekerColour = seekerColour;
    }

    public void lobbySelector() {
        // Used to create random game codes
        Random random = new Random();

        // Initialise default lobby information
        // Random game code for a new lobby
        lobbyDefault.put("lobbyCode", random.nextInt(999999));
        lobbyDefault.put("isPrivate", false);
        lobbyDefault.put("start", -1);
        lobbyDefault.put("hasHider", false);
        lobbyDefault.put("end", 0);

        // Create player information
        m_playerInformation.put("isHider", m_gameData.isHider());
        m_playerInformation.put("name", m_gameData.getPlayerName());

        // If the player is hosting a private lobby
        if (m_gameData.isHost()) {
            // Set lobby as private
            Map<String, Object> lobby = lobbyDefault;
            lobby.put("isPrivate", m_gameData.isPrivate());

            // Create and join lobby
            createLobby(lobby, m_playerInformation);
        }
        // If the player is joining a private lobby
        else if (m_gameData.isPrivate()) {
            // Join private lobby
            joinPrivateLobby(m_gameData.getGameCode(), m_playerInformation);
        }
        // Else the player is joining a public lobby
        else {
            joinPublicLobby(m_playerInformation);
        }
    }

    public void joinLobby(String lobbyID, Map<String, Object> playerInfo) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(lobbyID);

        // Get reference to the players collection within the lobby
        CollectionReference playersCollection = lobbyDocument.collection("players");

        // Add the new player to collection
        playersCollection.add(playerInfo)
                .addOnSuccessListener(playerReference -> {
                    Log.d("joinLobby", "onSuccess: player joined");

                    // Get isHider
                    Boolean isHider = (Boolean) playerInfo.get("isHider");
                    if (isHider == null){
                        Log.w("joinPrivateLobby", "joinPrivateLobby: isHider is null");
                        return;
                    }

                    // Update hasHider if the player is a hider
                    if(isHider){
                        // Set hasHider
                        lobbyDocument.update("hasHider", true);
                    }

                    // Joined the lobby so save ID's
                    m_lobbyID = lobbyDocument.getId();
                    m_playerID = playerReference.getId();
                    // Update LiveData objects
                    m_lobbyIDLiveData.setValue(m_lobbyID);
                    m_playerIDLiveData.setValue(m_playerID);

                    // Update the lobby UI
                    // Update the game code
                    updateLobbyInformation();

                    // Update the player lists
                    updatePlayerList();

                    // Listen for game start
                    startListener();

                    // Listener for player changes
                    playerListener();
                })
                .addOnFailureListener(e -> Log.w("joinLobby", "onFailure: player failed to join"));
    }

    public void joinPrivateLobby(int lobbyCode, Map<String, Object> playerInfo) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get isHider
        Boolean isHider = (Boolean) playerInfo.get("isHider");
        if (isHider == null){
            Log.w("joinPrivateLobby", "joinPrivateLobby: isHider is null");
            return;
        }

        // Query the gameLobbies table
        db.collection("gameLobbies")
                // Find the correct lobby
                .whereEqualTo("lobbyCode", lobbyCode)
                // Only return one lobby
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("joinPrivateLobby", "onSuccess: Lobby Getting");

                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Get the returned lobby
                        DocumentSnapshot lobby = queryDocumentSnapshots.getDocuments().get(0);
                        // Check if a hider has already joined
                        Boolean hasHider = lobby.getBoolean("hasHider");

                        if (hasHider != null) {
                            // If a hider is trying to join a lobby with another hider in it
                            if (hasHider && isHider) {
                                Log.w("joinPrivateLobby", "onFailure: Two hiders in lobby");
                                // Update live data flag failure exit
                                m_failureExit.setValue(true);
                            }
                            // Else join the lobby
                            else {
                                joinLobby(lobby.getId(), playerInfo);
                            }
                        }
                    }
                    else {
                        Log.w("joinPrivateLobby", "onFailure: Lobby does not exist");
                        // Update live data flag failure exit
                        m_failureExit.setValue(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("joinPrivateLobby", "onFailure: Lobby not joined");
                    // Update live data flag failure exit
                    m_failureExit.setValue(true);
                });
    }

    public void joinPublicLobby(Map<String, Object> playerInfo) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get isHider
        Boolean isHider = (Boolean) playerInfo.get("isHider");
        if (isHider == null){
            Log.w("joinPublicLobby", "joinPublicLobby: isHider is null");
            return;
        }

        // Query the gameLobbies table
        db.collection("gameLobbies")
                // Find an available lobbies
                .whereEqualTo("start", -1)
                .whereEqualTo("isPrivate", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("joinPublicLobby", "onSuccess: Lobby Getting");

                    // If joining player is a hider
                    if (isHider) {
                        // Check all available lobbies
                        for (DocumentSnapshot lobby : queryDocumentSnapshots) {
                            Boolean hasHider = lobby.getBoolean("hasHider");

                            if (hasHider != null){
                                // If the lobby does not have a hider
                                if (!hasHider) {
                                    // Join lobby
                                    joinLobby(lobby.getId(), playerInfo);
                                    return;
                                }
                            }
                        }

                        // No available lobbies for a hider
                        Log.d("joinPublicLobby", "onSuccess: No public lobbies with hider availability");

                        // Create and join a new public lobby
                        createLobby(lobbyDefault, playerInfo);

                    }
                    // Else the joining player is a seeker
                    else {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Seeker joins a random public lobby
                            int randomLobby = 0;
                            // If there are two or more lobbies pick a random one
                            if (queryDocumentSnapshots.size() >= 2)
                            {
                                Random random = new Random();
                                randomLobby = random.nextInt(queryDocumentSnapshots.size());
                            }

                            // Get random lobby
                            DocumentSnapshot lobby = queryDocumentSnapshots.getDocuments().get(randomLobby);

                            // Join lobby
                            joinLobby(lobby.getId(), playerInfo);
                        }
                        else {
                            Log.d("joinPublicLobby", "onSuccess: No public lobbies");

                            // Create and join a new public lobby
                            createLobby(lobbyDefault, playerInfo);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w("joinPrivateLobby", "onFailure: Lobby not joined"));
    }

    public void createLobby(Map<String, Object> lobbyInfo, Map<String, Object> playerInfo) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Access the gameLobbies table
        db.collection("gameLobbies")
                // Add the given lobby
                .add(lobbyInfo)
                .addOnSuccessListener(lobbyReference -> {
                    Log.d("Create Lobby", "onSuccess: Lobby Created, ID: " + lobbyReference.getId());

                    // Create a collection of exhibits within the lobby
                    createExhibits(lobbyReference);

                    // Join created lobby
                    joinLobby(lobbyReference.getId(), playerInfo);
                })
                .addOnFailureListener(e -> Log.w("Create Lobby", "onFailure: Lobby Not Created"));
    }

    private void createExhibits(DocumentReference lobbyReference) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the exhibits collection within the lobby
        CollectionReference gameExhibitsCollection= lobbyReference.collection("gameExhibits");

        // Get reference to the exhibits collection
        CollectionReference exhibitsCollection = db.collection("exhibits");

        exhibitsCollection.get()
                .addOnSuccessListener(exhibitsSnapshot -> {
                    // For each original exhibit
                    for (DocumentSnapshot exhibit : exhibitsSnapshot) {
                        // Add a new game exhibit
                        gameExhibitsCollection.add(Objects.requireNonNull(exhibit.getData()));
                    }
                })
                .addOnFailureListener( e -> Log.w("LobbyViewModel", "createExhibits: Could not find exhibits"));
    }

    public void updatePlayerList() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ArrayLists containing the hiders and seekers
        ArrayList<PlayerData> hiderList = new ArrayList<>();
        ArrayList<PlayerData> seekerList = new ArrayList<>();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the players collection within the lobby
        CollectionReference playersCollection = lobbyDocument.collection("players");

        // Get the players
        playersCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("updatePlayerList", "onSuccess: got players");

                    // For each player
                    for (DocumentSnapshot player : queryDocumentSnapshots) {
                        // Get player details
                        Boolean isHider = player.getBoolean("isHider");
                        String name = player.getString("name");
                        boolean isPlayer = false;

                        if (isHider != null && name != null){
                            // Check if this player is the local player
                            if(player.getId().equals(m_playerID)) {
                                isPlayer = true;
                            }

                            // If the player is a hider
                            if (isHider) {
                                // Add player to the hider list
                                hiderList.add(new PlayerData(name, true, isPlayer, m_hiderColour));
                            }
                            // Else the player is a seeker
                            else {
                                // Add player to the seeker list
                                seekerList.add(new PlayerData(name, false, isPlayer, m_seekerColour));
                            }
                        }
                    }

                    // Update live data objects
                    m_hiderList.setValue(hiderList);
                    m_seekerList.setValue(seekerList);
                })
                .addOnFailureListener(e -> Log.w("updatePlayerList", "onFailure: failed to get players"));
    }

    public void updateLobbyInformation() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get the lobby
        lobbyDocument.get()
                .addOnSuccessListener(lobbySnapshot -> {
                    Log.d("getLobbyInformation", "onSuccess: Lobby found");

                    Long lobbyCode = lobbySnapshot.getLong("lobbyCode");
                    Boolean isPrivate = lobbySnapshot.getBoolean("isPrivate");

                    if (lobbyCode != null && isPrivate != null)
                    {
                        // Update live data
                        m_lobbyCode.setValue(lobbyCode);
                        m_lobbyIsPrivate.setValue(isPrivate);
                    }
                })
                .addOnFailureListener(e -> Log.w("getLobbyInformation", "onFailure: Lobby not found"));
    }


    public void deleteCurrentPlayer() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the players collection within the lobby
        CollectionReference playerCollection = lobbyDocument.collection("players");

        // Get reference to the player
        DocumentReference playerDocument = playerCollection.document(m_playerID);

        // Remove the player from collection
        playerDocument.delete()
                .addOnSuccessListener(unused -> {
                    Log.d("deleteCurrentPlayer", "onSuccess: player deleted");

                    // Get isHider
                    Boolean isHider = (Boolean) m_playerInformation.get("isHider");
                    if (isHider == null){
                        Log.w("deleteCurrentPlayer", "onSuccess: isHider is null");
                        return;
                    }

                    // Update hasHider
                    if(isHider){
                        Log.d("deleteCurrentPlayer", "onSuccess: Hider left");
                        // Set hasHider
                        lobbyDocument.update("hasHider", false);
                    }
                })
                .addOnFailureListener(e -> Log.w("deleteCurrentPlayer", "onFailure: player failed to delete"));
    }

    public void updateStart(Boolean start) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the players collection within the lobby
        CollectionReference playerCollection = lobbyDocument.collection("players");

        playerCollection.get()
                .addOnSuccessListener(playersSnapshot -> {
                    // Default start value (Game not started)
                    int startValue = -1;

                    // Count how many players are in the lobby
                    int playerCount = 0;
                    for (DocumentSnapshot ignored : playersSnapshot) {
                        playerCount++;
                    }

                    // If there are atleast 2 players and start is true
                    if (playerCount >= 2 && start){
                        // Set start value to game start
                        startValue = 0;
                    }

                    // Set the lobbies start flag
                    lobbyDocument.update("start", startValue)
                            .addOnSuccessListener(unused -> Log.d("updateStart", "onSuccess: start updated"))
                            .addOnFailureListener(e -> Log.w("updateStart", "onFailure: failed to update start"));
                })
                .addOnFailureListener(e -> Log.w("updateStart", "onFailure: failed to get players"));
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

                // If start == 0 start game
                if (start != null && start == 0) {
                    // Update live data start flag
                    m_lobbyStart.setValue(true);
                }
            }
        });
    }

    // Listens for changes to the players collection
    public void playerListener() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the players collection within the lobby
        CollectionReference playerCollection = lobbyDocument.collection("players");

        // Creates a listener that runs when any changes are made to the players collection
        playerCollection.addSnapshotListener((playerSnapshot, error) -> {
            Log.d("playerListener", "onEvent: Event occurred");
            // Update the players list
            updatePlayerList();
        });
    }

    public MutableLiveData<Boolean> getLobbyStart() {
        return m_lobbyStart;
    }

    public MutableLiveData<Long> getLobbyCode() {
        return m_lobbyCode;
    }

    public MutableLiveData<Boolean> getLobbyIsPrivate() {
        return m_lobbyIsPrivate;
    }

    public MutableLiveData<ArrayList<PlayerData>> getHiderList() {
        return m_hiderList;
    }

    public MutableLiveData<ArrayList<PlayerData>> getSeekerList() {
        return m_seekerList;
    }

    public MutableLiveData<Boolean> getFailureExit() {
        return m_failureExit;
    }

    public MutableLiveData<String> getLobbyIDLiveData() {
        return m_lobbyIDLiveData;
    }

    public MutableLiveData<String> getPlayerIDLiveData() {
        return m_playerIDLiveData;
    }
}
