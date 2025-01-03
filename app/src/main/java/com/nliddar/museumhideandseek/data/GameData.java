package com.nliddar.museumhideandseek.data;

import java.io.Serializable;

// GameTypeData is serialized so it can be passed through intents
public class GameData implements Serializable {

    private static final int PUBLIC_CODE = -1;
    private boolean m_isHost;
    private boolean m_isPrivate;
    private boolean m_isHider;
    private String m_playerName;
    private int m_gameCode;

    public GameData() {
        // GameTypeData Constructor Default
        m_isPrivate = true;
        m_isHider = false;
        m_isHost = false;
        m_gameCode = PUBLIC_CODE;
        m_playerName = "Player";
    }

    public boolean isHost() {
        return m_isHost;
    }

    public void setIsHost(boolean isHost) {
        m_isHost = isHost;
    }

    public boolean isPrivate() {
        return m_isPrivate;
    }

    public void setIsPrivate(boolean isPrivate) {
        m_isPrivate = isPrivate;
    }

    public boolean isHider() {
        return m_isHider;
    }

    public void setIsHider(boolean isHider) {
        m_isHider = isHider;
    }

    public int getGameCode() {
        return m_gameCode;
    }

    public void setGameCode(int gameCode) {
        m_gameCode = gameCode;
    }

    public String getPlayerName() {
        return m_playerName;
    }

    public void setPlayerName(String m_playerName) {
        this.m_playerName = m_playerName;
    }
}
