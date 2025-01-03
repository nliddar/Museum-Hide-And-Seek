package com.nliddar.museumhideandseek.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.nliddar.museumhideandseek.data.PlayerData;
import com.nliddar.museumhideandseek.R;

import java.util.ArrayList;

public class PlayerRecyclerViewAdapter extends RecyclerView.Adapter<PlayerRecyclerViewAdapter.PlayerViewHolder> {

    // Object reference to context
    private final Context m_context;

    // An arraylist of 'PlayerData' objects
    private final ArrayList<PlayerData> m_playerData;

    // Constructor for PlayerRecyclerViewAdapter, initialises the context and PlayerData
    public PlayerRecyclerViewAdapter(Context context, ArrayList<PlayerData> playerData){
        m_context = context;
        m_playerData = playerData;
    }

    // Applies the player_lobby_item layout to each row
    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(m_context);
        // Create a itemView object that represents the player_lobby_item layout and return a new PlayerViewHolder
        View itemView = inflater.inflate(R.layout.player_lobby_item, parent, false);
        return new PlayerViewHolder(itemView);
    }

    // Sets the player name and images to a item
    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        holder.playerName.setText(m_playerData.get(position).getPlayerName());
        holder.playerHiderImage.setImageResource(m_playerData.get(position).getHiderImage());
        holder.playerImage.setImageResource(m_playerData.get(position).getPlayerImage());
        holder.playerCard.setCardBackgroundColor(m_playerData.get(position).getColour());
    }

    // Returns the number of items to display
    @Override
    public int getItemCount() {
        return m_playerData.size();
    }


    public static class PlayerViewHolder extends RecyclerView.ViewHolder {
        // Reference to the ImageView that displays whether the player is a hider or seeker
        public ImageView playerHiderImage;

        // Reference to the ImageView that displays the player image
        public ImageView playerImage;

        // Reference to the TextView that displays the players name
        public TextView playerName;

        // Reference to the CardView to change the background colour
        public CardView playerCard;

        // Constructor for PlayerViewHolder, initialises the itemView (layout)
        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);

            playerName = itemView.findViewById(R.id.inputName);
            playerHiderImage = itemView.findViewById(R.id.image_hider);
            playerImage = itemView.findViewById(R.id.image_player);
            playerCard = itemView.findViewById(R.id.playerCard);
        }

    }
}
