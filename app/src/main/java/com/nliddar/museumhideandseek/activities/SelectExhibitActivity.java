package com.nliddar.museumhideandseek.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.nliddar.museumhideandseek.data.ExhibitData;
import com.nliddar.museumhideandseek.adapters.ExhibitRecyclerViewAdapter;
import com.nliddar.museumhideandseek.interfaces.ExhibitRecyclerViewInterface;
import com.nliddar.museumhideandseek.R;
import com.nliddar.museumhideandseek.viewmodels.SelectExhibitViewModel;

import java.util.ArrayList;

public class SelectExhibitActivity extends AppCompatActivity implements ExhibitRecyclerViewInterface {

    // Object reference to ViewModel
    SelectExhibitViewModel m_selectExhibitViewModel;

    // ID of lobby in firestore Database
    String m_lobbyID;

    // ID of player in firestore Database
    String m_playerID;

    // An arraylist of 'ExhibitData' objects
    private ArrayList<ExhibitData> m_exhibitData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_exhibit);

        // Receives intent that communicates ID's
        Intent intent = getIntent();
        if(intent != null && intent.hasExtra("LobbyID") && intent.hasExtra("PlayerID")) {
            m_lobbyID = intent.getStringExtra("LobbyID");
            m_playerID = intent.getStringExtra("PlayerID");
            Log.d("SelectExhibitActivity", "onCreate: Intent received LobbyID: " + m_lobbyID);
        }

        // Create a new SelectExhibitViewModel, pass in colours and IDs
        m_selectExhibitViewModel = new SelectExhibitViewModel(
                m_playerID,
                m_lobbyID,
                ContextCompat.getColor(getApplicationContext(), R.color.red_2),
                ContextCompat.getColor(getApplicationContext(), R.color.blue_2));

        // Start observers
        observeExhibitList();

        // Gets the list of exhibits from the database
        m_selectExhibitViewModel.updateExhibitList();
    }


    // Listens for a change to the exhibitList live data object in m_lobbyViewModel
    public void observeExhibitList() {
        m_selectExhibitViewModel.getExhibitList().observe(this, exhibitList -> {
            // Update exhibit list
            m_exhibitData = exhibitList;

            // Display exhibits
            // Update recycler view for exhibits
            RecyclerView exhibitRecyclerView = findViewById(R.id.exhibitList);
            // Create and set ExhibitRecyclerViewAdapter and LinearLayoutManager to exhibitRecyclerView
            exhibitRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            ExhibitRecyclerViewAdapter exhibitAdapter = new ExhibitRecyclerViewAdapter(this, m_exhibitData, this);
            exhibitRecyclerView.setAdapter(exhibitAdapter);
        });
    }

    // Called when exhibit is clicked
    @Override
    public void onItemClick(int position) {
        ExhibitData exhibit = m_exhibitData.get(position);

        // Set an exhibit as target
        m_selectExhibitViewModel.selectExhibit(exhibit);

        // Sends intent that starts HiderActivity
        Intent intent = new Intent(this, HiderActivity.class);
        intent.putExtra("PlayerID", m_playerID);
        intent.putExtra("LobbyID", m_lobbyID);
        startActivity(intent);
    }
}