package com.nliddar.museumhideandseek.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.nliddar.museumhideandseek.data.ExhibitData;
import com.nliddar.museumhideandseek.interfaces.ExhibitRecyclerViewInterface;
import com.nliddar.museumhideandseek.R;

import java.util.ArrayList;

public class ExhibitRecyclerViewAdapter extends RecyclerView.Adapter<ExhibitRecyclerViewAdapter.ExhibitViewHolder> {

    private final Context m_context;

    // An arraylist of 'ExhibitData' objects
    private final ArrayList<ExhibitData> m_exhibitData;

    // A reference to the activity (class) that implements the ExhibitRecyclerViewInterface, allows
    // that activity to handle on click behaviour
    private final ExhibitRecyclerViewInterface m_exhibitRecyclerViewInterface;

    // Constructor for ExhibitRecyclerViewAdapter, initialises the context, ExhibitData and interface
    public ExhibitRecyclerViewAdapter(Context context, ArrayList<ExhibitData> exhibitData, ExhibitRecyclerViewInterface exhibitRecyclerViewInterface){
        m_context = context;
        m_exhibitData = exhibitData;
        m_exhibitRecyclerViewInterface = exhibitRecyclerViewInterface;
    }

    // Applies the exhibit_item layout to each row
    @NonNull
    @Override
    public ExhibitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(m_context);
        // Create a itemView object that represents the exhibit_item layout and return a new ExhibitViewHolder
        View itemView = inflater.inflate(R.layout.exhibit_item, parent, false);
        return new ExhibitViewHolder(itemView, m_exhibitRecyclerViewInterface);
    }

    // Sets the exhibit name and description to a item
    @Override
    public void onBindViewHolder(@NonNull ExhibitViewHolder holder, int position) {
        holder.exhibitName.setText(m_exhibitData.get(position).getName());
        holder.exhibitDesc.setText(m_exhibitData.get(position).getDesc());
        holder.exhibitCard.setCardBackgroundColor(m_exhibitData.get(position).getColour());

        // Adds vote number
        int votes = m_exhibitData.get(position).getVotes();
        // If the exhibit has votes
        if (votes > 0) {
            // Show the notification card and vote number
            holder.exhibitVotesCard.setVisibility(View.VISIBLE);
            holder.exhibitVotesText.setText(String.valueOf(votes));
        }
    }

    // Returns the number of items to display
    @Override
    public int getItemCount() {
        return m_exhibitData.size();
    }


    public static class ExhibitViewHolder extends RecyclerView.ViewHolder {
        // Reference to the TextView that displays the exhibits name
        public TextView exhibitName;

        // Reference to the TextView that displays the exhibits Description
        public TextView exhibitDesc;

        // Reference to the CardView to change the background colour
        public CardView exhibitCard;

        // Reference to the CardView to show votes
        public CardView exhibitVotesCard;

        // Reference to the TextView to show vote number
        public TextView exhibitVotesText;


        // Constructor for ExhibitViewHolder, initialises the itemView (layout) and interface for each activity
        public ExhibitViewHolder(@NonNull View itemView, ExhibitRecyclerViewInterface exhibitRecyclerViewInterface) {
            super(itemView);

            exhibitName = itemView.findViewById(R.id.exhibitName);
            exhibitDesc = itemView.findViewById(R.id.exhibitDesc);
            exhibitCard = itemView.findViewById(R.id.exhibitCard);
            exhibitVotesCard = itemView.findViewById(R.id.exhibitNotificationCard);
            exhibitVotesText = itemView.findViewById(R.id.exhibitNotificationText);

            // Handles exhibit item on click behaviour
            itemView.setOnClickListener(v -> {
                // If an activity has implemented exhibitRecyclerViewInterface
                if (exhibitRecyclerViewInterface != null){
                    // Get position of the item clicked
                    int position = getAdapterPosition();

                    // If position is valid run onItemClick in the activity that implemented exhibitRecyclerViewInterface
                    if (position != RecyclerView.NO_POSITION) {
                        exhibitRecyclerViewInterface.onItemClick(position);
                    }
                }
            });
        }

    }
}
