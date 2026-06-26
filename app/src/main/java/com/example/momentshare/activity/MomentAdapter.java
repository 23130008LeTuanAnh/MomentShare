package com.example.momentshare.activity;

import android.content.Context;
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
import java.util.List;

public class MomentAdapter extends RecyclerView.Adapter<MomentAdapter.MomentViewHolder> {

    private Context context;
    private List<Moment> momentList;
    private OnMomentClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Moment moment);
    }

    public MomentAdapter(Context context, List<Moment> momentList, OnMomentClickListener listener) {
        this.context = context;
        this.momentList = momentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MomentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_moment_feed, parent, false);
        return new MomentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MomentViewHolder holder, int position) {
        Moment moment = momentList.get(position);

        holder.txtUsername.setText("Người gửi: " + moment.getSenderId());
        holder.txtCaption.setText(moment.getCaption());

        // Sử dụng thư viện Glide để tải ảnh từ URL vào ImageView
        if (moment.getImageUrl() != null && !moment.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(moment.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.imgMoment);
        }

        // Bắt sự kiện click vào item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMomentClick(moment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return momentList != null ? momentList.size() : 0;
    }

    // Hàm cập nhật danh sách khi có dữ liệu mới từ Firebase
    public void updateData(List<Moment> newMoments) {
        this.momentList = newMoments;
        notifyDataSetChanged();
    }

    public interface OnMomentClickListener {
        void onMomentClick(Moment moment);
    }

    public static class MomentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgMoment;
        TextView txtUsername, txtCaption;

        public MomentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            imgMoment = itemView.findViewById(R.id.imgMoment);
            txtCaption = itemView.findViewById(R.id.txtCaption);
        }
    }
}
