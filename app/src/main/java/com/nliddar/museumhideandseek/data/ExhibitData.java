package com.nliddar.museumhideandseek.data;

public class ExhibitData {

    private final String m_id;
    private final String m_name;
    private String m_desc;
    private int m_colour;
    private final int m_votes;
    private boolean m_isTarget;

    public ExhibitData(String id, String name, String desc, int colour, int votes) {
        m_id = id;
        m_name = name;
        m_desc = desc;
        m_colour = colour;
        m_votes = votes;
    }

    // Constructor overloading for calculating and displaying voted exhibit
    public ExhibitData(String id, String name, int votes, boolean isTarget) {
        m_id = id;
        m_name = name;
        m_votes = votes;
        m_isTarget = isTarget;
    }

    public String getID() {
        return m_id;
    }

    public String getName() {
        return m_name;
    }

    public String getDesc() {
        return m_desc;
    }

    public int getColour() {
        return m_colour;
    }

    public int getVotes() {
        return m_votes;
    }

    public boolean isTarget() {
        return m_isTarget;
    }
}
