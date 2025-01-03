package com.nliddar.museumhideandseek.data;

import com.nliddar.museumhideandseek.R;

public class PlayerData {

    private final String m_playerName;
    private boolean m_isHider;
    private final boolean m_isPlayer;
    private final int m_colour;

    public PlayerData(String playerName, boolean isHider, boolean isPlayer, int colour) {
        m_playerName = playerName;
        m_isHider = isHider;
        m_isPlayer = isPlayer;
        m_colour = colour;
    }

    public int getHiderImage(){
        if (m_isHider) {
            return R.drawable.hider;
        } else {
            return R.drawable.search;
        }
    }

    public int getPlayerImage(){
        if (m_isPlayer) {
            return R.drawable.player;
        } else {
            return R.drawable.done_all;
        }
    }

    public String getPlayerName() {
        return m_playerName;
    }

    public boolean isHider() {
        return m_isHider;
    }

    public void setHider(boolean hider) {
        m_isHider = hider;
    }

    public int getColour() {
        return m_colour;
    }

}
