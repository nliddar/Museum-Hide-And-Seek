package com.nliddar.museumhideandseek.interfaces;

import com.nliddar.museumhideandseek.data.ClueData;

import java.util.ArrayList;

public interface ClueListenerInterface {

    // Called when clueList is updated
    void onClueListUpdate(ArrayList<ClueData> clueList);

    // Called when clueCount is updated
    void onClueCountUpdate(int clueCount);

}
