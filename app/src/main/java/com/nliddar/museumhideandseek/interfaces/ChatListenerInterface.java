package com.nliddar.museumhideandseek.interfaces;

import com.nliddar.museumhideandseek.data.ChatData;

import java.util.ArrayList;

public interface ChatListenerInterface {

    // Called when chatList is updated
    void onChatListUpdate(ArrayList<ChatData> chatList);

    // Called when chatCount is updated
    void onChatCountUpdate(int chatCount);
}
