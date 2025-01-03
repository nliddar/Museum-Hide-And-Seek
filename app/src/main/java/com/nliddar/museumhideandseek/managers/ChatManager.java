package com.nliddar.museumhideandseek.managers;

import android.util.Log;

import com.nliddar.museumhideandseek.data.ChatData;
import com.nliddar.museumhideandseek.interfaces.ChatListenerInterface;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatManager {


    // ID of lobby in firestore Database
    String m_lobbyID;

    // ID of player in firestore Database
    String m_playerID;

    // The class implementing this interface will receive chat updates
    ChatListenerInterface m_chatListenerInterface;

    public ChatManager(String playerID, String lobbyID, ChatListenerInterface chatListenerInterface) {
        m_playerID = playerID;
        m_lobbyID = lobbyID;
        m_chatListenerInterface =  chatListenerInterface;

        // Start the chat listener
        chatListener();
    }

    public void getChatList() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ArrayList containing the chats
        ArrayList<ChatData> chatList = new ArrayList<>();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the chat collection within the lobby
        CollectionReference chatCollection = lobbyDocument.collection("chat");

        chatCollection.orderBy("number", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(chatSnapshot -> {
                    // For each chat
                    for (DocumentSnapshot chat : chatSnapshot) {
                        // Get chat details
                        String message = chat.getString("message");
                        String name = chat.getString("name");
                        Long number = chat.getLong("number");
                        Boolean hasSeen = chat.getBoolean("hasSeen");

                        if (message != null && name != null && number != null && hasSeen != null)
                        {
                            // Add chat to chat list
                            chatList.add(new ChatData(Math.toIntExact(number), name, message, hasSeen));
                        }
                    }

                    // Alert chatListenerInterface that the chat is has been updated
                    m_chatListenerInterface.onChatListUpdate(chatList);
                })
                .addOnFailureListener( e -> Log.w("ChatManager", "getChatList: Could not find chats"));
    }

    public void getChatCount() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the chat collection within the lobby
        CollectionReference chatCollection = lobbyDocument.collection("chat");

        chatCollection.get()
                .addOnSuccessListener(chatSnapshot -> {
                    // Update listener with number of chats
                    m_chatListenerInterface.onChatCountUpdate(chatSnapshot.size());
                })
                .addOnFailureListener( e -> Log.w("ChatManager", "getChatList: Could not find chats"));
    }

    public void createChat(String message) {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the player document
        DocumentReference playersDocument = lobbyDocument.collection("players").document(m_playerID);

        // Get reference to the chat collection within the lobby
        CollectionReference chatCollection = lobbyDocument.collection("chat");

        // Get the number of chats
        chatCollection.get()
                .addOnSuccessListener(chatSnapshot -> {
                    // Get the next chat number
                    int number = chatSnapshot.size() + 1;

                    // Get the player
                    playersDocument.get().addOnSuccessListener(playerSnapshot -> {
                        // Create chat information
                        Map<String, Object> chatInformation = new HashMap<>();
                        chatInformation.put("hasSeen", false);
                        chatInformation.put("id", m_playerID);
                        chatInformation.put("name", playerSnapshot.getString("name"));
                        chatInformation.put("message", message);
                        chatInformation.put("number",number);

                        // Add chat
                        chatCollection.add(chatInformation)
                            .addOnFailureListener( e -> Log.w("ChatManager", "createChat: Could not add chat"));
                    })
                    .addOnFailureListener( e -> Log.w("ChatManager", "createChat: Could not find player"));
                })
                .addOnFailureListener( e -> Log.w("ChatManager", "createChat: Could not find chats"));
    }

    public void setSeen() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyReference = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the chat collection within the lobby
        CollectionReference chatCollection = lobbyReference.collection("chat");

        // Get all chats
        chatCollection.get()
                .addOnSuccessListener(chatSnapshot -> {
                    // For each chat
                    for (DocumentSnapshot chat : chatSnapshot) {
                        DocumentReference chatDocument = chat.getReference();

                        // Get the ID of the player that created the chat
                        String chatPlayerID = chat.getString("id");

                        if (chatPlayerID != null)
                        {
                            // Update each chat as seen if the chat was not created by the local player
                            if (!chatPlayerID.equals(m_playerID)) {
                                // Update chat as seen
                                chatDocument.update("hasSeen", true);
                            }
                        }
                    }

                })
                .addOnFailureListener( e -> Log.w("ChatManager", "getChatList: Could not find chats"));
    }

    public void chatListener() {
        // Get Firestore database instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get reference to the lobby
        DocumentReference lobbyDocument = db.collection("gameLobbies").document(m_lobbyID);

        // Get reference to the chat collection within the lobby
        CollectionReference chatCollection = lobbyDocument.collection("chat");

        // Creates a listener that runs when any changes are made to the chat collection
        chatCollection.addSnapshotListener((chatSnapshot, error) -> {
            Log.d("chatListener", "onEvent: Event occurred");
            // Update the chat list
            getChatList();
        });
    }
}
