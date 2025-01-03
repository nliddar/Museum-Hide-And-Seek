package com.nliddar.museumhideandseek.interfaces;

import com.nliddar.museumhideandseek.data.GuessData;

import java.util.ArrayList;

public interface GuessListenerInterface {

    // Called when guessList is updated
    void onGuessListUpdate(ArrayList<GuessData> guessList);

    // Called when guessCount is updated
    void onGuessCountUpdate(int guessCount);
}
