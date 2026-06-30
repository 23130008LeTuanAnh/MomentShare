package com.example.momentshare.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.model.Moment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<Moment> historyList;
    private final OnItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(Moment moment);
    }

    public HistoryAdapter(List<Moment> historyList, OnItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_moment, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Moment moment = historyList.get(position);

        if (moment.getImageUrl() != null && !moment.getImageUrl().trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(moment.getImageUrl())
                    .centerCrop()
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(holder.imgHistoryMoment);
        } else {
            holder.imgHistoryMoment.setImageResource(R.mipmap.ic_launcher);
        }

        String caption = moment.getCaption() == null ? "" : moment.getCaption().trim();
        holder.txtHistoryCaption.setText(caption.isEmpty() ? "Không có caption" : caption);

        if (moment.getCreatedAt() != null) {
            holder.txtHistoryDate.setText(dateFormat.format(moment.getCreatedAt().toDate()));
        } else {
            holder.txtHistoryDate.setText("Không rõ thời gian");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(moment);
        });
    }

    @Override
    public int getItemCount() {
        return historyList == null ? 0 : historyList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgHistoryMoment;
        TextView txtHistoryCaption;
        TextView txtHistoryDate;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgHistoryMoment = itemView.findViewById(R.id.imgHistoryMoment);
            txtHistoryCaption = itemView.findViewById(R.id.txtHistoryCaption);
            txtHistoryDate = itemView.findViewById(R.id.txtHistoryDate);
        }
    }
}
