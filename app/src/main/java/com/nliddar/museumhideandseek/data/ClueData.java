package com.nliddar.museumhideandseek.data;

import android.annotation.SuppressLint;

public class ClueData {

    private final int m_number;
    private final String m_name;
    private final String m_message;
    private final boolean m_hasSeen;


    @SuppressLint("DefaultLocale")
    public ClueData(int number, String message, boolean hasSeen) {
        m_number = number;
        m_name = String.format("Clue %d", number);
        m_message = message;
        m_hasSeen = hasSeen;
    }

    public int getNumber() {
        return m_number;
    }

    public String getName() {
        return m_name;
    }

    public String getMessage() {
        return m_message;
    }

    public boolean isSeen() {
        return m_hasSeen;
    }
}
