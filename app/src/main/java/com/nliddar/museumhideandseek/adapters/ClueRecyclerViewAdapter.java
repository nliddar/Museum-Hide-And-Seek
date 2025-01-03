package com.nliddar.museumhideandseek.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nliddar.museumhideandseek.data.ClueData;
import com.nliddar.museumhideandseek.R;

import java.util.ArrayList;

public class ClueRecyclerViewAdapter extends RecyclerView.Adapter<ClueRecyclerViewAdapter.ClueViewHolder>  {

    private final Context m_context;

    // An arraylist of 'ClueData' objects
    private final ArrayList<ClueData> m_clueData;

    // Constructor for ClueRecyclerViewAdapter, initialises the context and ClueData
    public ClueRecyclerViewAdapter(Context context, ArrayList<ClueData> clueData){
        m_context = context;
        m_clueData = clueData;
    }

    // Applies the clue_item layout to each row
    @NonNull
    @Override
    public ClueRecyclerViewAdapter.ClueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(m_context);
        // Create a itemView object that represents the clue_item layout and return a new ClueViewHolder
        View itemView = inflater.inflate(R.layout.clue_item, parent, false);
        return new ClueRecyclerViewAdapter.ClueViewHolder(itemView);
    }

    // Sets the clue name and message to a item
    @Override
    public void onBindViewHolder(@NonNull ClueRecyclerViewAdapter.ClueViewHolder holder, int position) {
        holder.clueName.setText(m_clueData.get(position).getName());
        holder.clueMessage.setText(m_clueData.get(position).getMessage());

        // If the clue has not been seen do not display seen image
        if (!m_clueData.get(position).isSeen()) {
            holder.clueSeen.setVisibility(View.INVISIBLE);
        }
    }

    // Returns the number of items to display
    @Override
    public int getItemCount() {
        return m_clueData.size();
    }


    public static class ClueViewHolder extends RecyclerView.ViewHolder {
        // Reference to the TextView that displays the clue name
        public TextView clueName;

        // Reference to the TextView that displays the clue message
        public TextView clueMessage;

        // Reference to the ImageView that displays whether the clue has been seen
        public ImageView clueSeen;


        // Constructor for ClueViewHolder, initialises the itemView (layout)
        public ClueViewHolder(@NonNull View itemView) {
            super(itemView);

            clueName = itemView.findViewById(R.id.clueName);
            clueMessage = itemView.findViewById(R.id.clueDesc);
            clueSeen = itemView.findViewById(R.id.clueSeen);
        }

    }
}
