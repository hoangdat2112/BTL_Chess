package com.example.btl_chess;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Adapter cho RecyclerView
public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {
    private List<RoomActivity.Room> rooms;
    private OnRoomSelectedListener listener;

    public interface OnRoomSelectedListener {
        void onRoomSelected(RoomActivity.Room room);
    }

    public RoomAdapter(List<RoomActivity.Room> rooms, OnRoomSelectedListener listener) {
        this.rooms = rooms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        RoomActivity.Room room = rooms.get(position);
        holder.roomNameText.setText(room.getName());
        holder.roomIdText.setText("ID: " + room.getId());

        holder.itemView.setOnClickListener(v -> listener.onRoomSelected(room));
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView roomNameText;
        TextView roomIdText;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            roomNameText = itemView.findViewById(android.R.id.text1);
            roomIdText = itemView.findViewById(android.R.id.text2);
        }
    }
}