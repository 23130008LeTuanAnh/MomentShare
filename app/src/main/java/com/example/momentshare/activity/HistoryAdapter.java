package com.example.momentshare.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.model.Moment;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<Moment> historyList;
    private OnItemClickListener listener;

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

        // Xử lý nạp ảnh an toàn, có ảnh giữ chỗ (placeholder) để View không bị sập chiều cao
        if (moment.getImageUrl() != null && !moment.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(moment.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(holder.imgHistoryMoment);
        } else {
            holder.imgHistoryMoment.setImageResource(R.drawable.ic_launcher_background);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(moment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgHistoryMoment;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgHistoryMoment = itemView.findViewById(R.id.imgHistoryMoment);
        }
    }
}
