package com.nliddar.museumhideandseek.interfaces;

public interface TimerListenerInterface {

    // Called when a second passes
    void onTimerUpdate(int timePassed);

    // Called when the game ends
    void onGameEnd(boolean isSuccess);

    // Called to update round UI every second
    void onRoundUpdate(int roundNum);

    // Called when a round ends
    // Uses default as its only used by HiderActivity
    default void onNextRound() {

    }
}
