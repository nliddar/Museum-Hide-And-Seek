package com.nliddar.museumhideandseek.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nliddar.museumhideandseek.data.ChatData;
import com.nliddar.museumhideandseek.R;

import java.util.ArrayList;

public class ChatRecyclerViewAdapter extends RecyclerView.Adapter<ChatRecyclerViewAdapter.ChatViewHolder> {

    private final Context m_context;

    // An arraylist of 'ChatData' objects
    private final ArrayList<ChatData> m_chatData;

    // Constructor for ChatRecyclerViewAdapter, initialises the context and ChatData
    public ChatRecyclerViewAdapter(Context context, ArrayList<ChatData> chatData){
        m_context = context;
        m_chatData = chatData;
    }

    // Applies the chat_item layout to each row
    @NonNull
    @Override
    public ChatRecyclerViewAdapter.ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(m_context);
        // Create a itemView object that represents the chat_item layout and return a new ChatViewHolder
        View itemView = inflater.inflate(R.layout.chat_item, parent, false);
        return new ChatRecyclerViewAdapter.ChatViewHolder(itemView);
    }

    // Sets the chat name and message to a item
    @Override
    public void onBindViewHolder(@NonNull ChatRecyclerViewAdapter.ChatViewHolder holder, int position) {
        holder.chatName.setText(m_chatData.get(position).getName());
        holder.chatMessage.setText(m_chatData.get(position).getMessage());

        // If the chat has not been seen do not display seen image
        if (!m_chatData.get(position).isSeen()) {
            holder.chatSeen.setVisibility(View.INVISIBLE);
        }
    }

    // Returns the number of items to display
    @Override
    public int getItemCount() {
        return m_chatData.size();
    }


    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        // Reference to the TextView that displays the chat name
        public TextView chatName;

        // Reference to the TextView that displays the chat message
        public TextView chatMessage;

        // Reference to the ImageView that displays whether the chat has been seen
        public ImageView chatSeen;


        // Constructor for ChatViewHolder, initialises the itemView (layout)
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            chatName = itemView.findViewById(R.id.chatName);
            chatMessage = itemView.findViewById(R.id.chatMessage);
            chatSeen = itemView.findViewById(R.id.chatSeen);
        }

    }
}
