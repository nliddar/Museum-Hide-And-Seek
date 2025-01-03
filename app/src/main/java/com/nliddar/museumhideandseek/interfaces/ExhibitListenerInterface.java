package com.nliddar.museumhideandseek.interfaces;

import com.nliddar.museumhideandseek.data.ExhibitData;

import java.util.ArrayList;

public interface ExhibitListenerInterface {

    // Called when exhibitList is updated
    default void onExhibitListUpdate(ArrayList<ExhibitData> exhibitList) {

    }

    // Called when the chosen exhibit is returned
    default void onVotedExhibitReturn(ExhibitData exhibitData) {

    }
}
