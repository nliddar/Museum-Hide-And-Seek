package com.nliddar.museumhideandseek.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nliddar.museumhideandseek.data.GuessData;
import com.nliddar.museumhideandseek.R;

import java.util.ArrayList;

public class GuessRecyclerViewAdapter extends RecyclerView.Adapter<GuessRecyclerViewAdapter.GuessViewHolder>{

    private final Context m_context;

    // An arraylist of 'GuessData' objects
    private final ArrayList<GuessData> m_guessData;

    // Constructor for GuessRecyclerViewAdapter, initialises the context and GuessData
    public GuessRecyclerViewAdapter(Context context, ArrayList<GuessData> guessData){
        m_context = context;
        m_guessData = guessData;
    }

    // Applies the guess_item layout to each row
    @NonNull
    @Override
    public GuessRecyclerViewAdapter.GuessViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(m_context);
        // Create a itemView object that represents the guess_item layout and return a new GuessViewHolder
        View itemView = inflater.inflate(R.layout.guess_item, parent, false);
        return new GuessRecyclerViewAdapter.GuessViewHolder(itemView);
    }

    // Sets the guess name and message to a item
    @Override
    public void onBindViewHolder(@NonNull GuessRecyclerViewAdapter.GuessViewHolder holder, int position) {
        holder.guessName.setText(m_guessData.get(position).getName());
        holder.guessExhibit.setText(m_guessData.get(position).getExhibit());

        // If the guess has not been seen do not display seen image
        if (!m_guessData.get(position).isSeen()) {
            holder.guessSeen.setVisibility(View.INVISIBLE);
        }
    }

    // Returns the number of items to display
    @Override
    public int getItemCount() {
        return m_guessData.size();
    }


    public static class GuessViewHolder extends RecyclerView.ViewHolder {
        // Reference to the TextView that displays the guess name
        public TextView guessName;

        // Reference to the TextView that displays the guess exhibit
        public TextView guessExhibit;

        // Reference to the ImageView that displays whether the guess has been seen
        public ImageView guessSeen;


        // Constructor for GuessViewHolder, initialises the itemView (layout)
        public GuessViewHolder(@NonNull View itemView) {
            super(itemView);

            guessName = itemView.findViewById(R.id.guessName);
            guessExhibit = itemView.findViewById(R.id.guessDesc);
            guessSeen = itemView.findViewById(R.id.guessSeen);
        }

    }
}
