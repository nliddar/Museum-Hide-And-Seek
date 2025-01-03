package com.nliddar.museumhideandseek.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nliddar.museumhideandseek.data.ExhibitData;
import com.nliddar.museumhideandseek.interfaces.ExhibitListenerInterface;
import com.nliddar.museumhideandseek.managers.ExhibitManager;

import java.util.ArrayList;

public class SelectExhibitViewModel extends ViewModel implements ExhibitListenerInterface {

    // Used for alternating exhibit list colours
    int m_colour1;

    // Used for alternating exhibit list colours
    int m_colour2;

    // ID of lobby in firestore Database
    String m_lobbyID;

    // ID of player in firestore Database
    String m_playerID;

    // Object reference to the exhibit manager
    ExhibitManager m_exhibitManager;

    // Live data objects
    private final MutableLiveData<ArrayList<ExhibitData>> m_exhibitList = new MutableLiveData<>();

    public SelectExhibitViewModel(String playerID, String lobbyID, int colour1, int colour2) {
        m_playerID = playerID;
        m_lobbyID = lobbyID;
        m_colour1 = colour1;
        m_colour2 = colour2;

        // Initialise the exhibit manager
        m_exhibitManager = new ExhibitManager(playerID, lobbyID, colour1, colour2, this);
    }

    // Call exhibitManger to to get an updated version of the ExhibitList
    public void updateExhibitList() {
        m_exhibitManager.getExhibitList();
    }

    // Set an exhibit as the target
    public void selectExhibit(ExhibitData exhibitData) {
        m_exhibitManager.setIsTarget(exhibitData);
    }

    // Called when the exhibit list is updated
    @Override
    public void onExhibitListUpdate(ArrayList<ExhibitData> exhibitList) {
        // Update live data objects
        m_exhibitList.setValue(exhibitList);
    }

    public MutableLiveData<ArrayList<ExhibitData>> getExhibitList() {
        return m_exhibitList;
    }
}
