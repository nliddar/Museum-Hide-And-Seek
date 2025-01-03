package com.nliddar.museumhideandseek.data;

import android.annotation.SuppressLint;

public class GuessData {

    private final int m_number;
    private final String m_name;
    private final String m_exhibit;
    private final boolean m_hasSeen;


    @SuppressLint("DefaultLocale")
    public GuessData(int number, String exhibit, boolean hasSeen) {
        m_number = number;
        m_name = String.format("Guess %d", number);
        m_exhibit = exhibit;
        m_hasSeen = hasSeen;
    }

    public int getNumber() {
        return m_number;
    }

    public String getName() {
        return m_name;
    }

    public String getExhibit() {
        return m_exhibit;
    }

    public boolean isSeen() {
        return m_hasSeen;
    }
}
