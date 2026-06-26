package com.example.momentshare.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.momentshare.model.Reaction;

import java.util.List;

public class ReactionAdapter extends RecyclerView.Adapter<ReactionAdapter.ReactionViewHolder> {

    private List<Reaction> reactionList;

    public ReactionAdapter(List<Reaction> reactionList) {
        this.reactionList = reactionList;
    }

    public void updateData(List<Reaction> newReactions) {
        this.reactionList = newReactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ReactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReactionViewHolder holder, int position) {
        Reaction reaction = reactionList.get(position);
        holder.txtUserId.setText(reaction.getUserId());
        holder.txtEmoji.setText(reaction.getEmoji());
        holder.txtEmoji.setTextSize(24f);
    }

    @Override
    public int getItemCount() {
        return reactionList != null ? reactionList.size() : 0;
    }

    public static class ReactionViewHolder extends RecyclerView.ViewHolder {
        TextView txtUserId;
        TextView txtEmoji;

        public ReactionViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserId = itemView.findViewById(android.R.id.text1);
            txtEmoji = itemView.findViewById(android.R.id.text2);
        }
    }
}