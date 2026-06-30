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
import com.example.momentshare.model.User;
import com.example.momentshare.repository.ReactionRepository;
import com.example.momentshare.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MomentAdapter hiển thị item khoảnh khắc trên Home.
 *
 * Đã chỉnh:
 * - Tải tên người đăng chắc hơn thông qua UserRepository fallback.
 * - Tránh bind nhầm tên khi RecyclerView tái sử dụng ViewHolder.
 */
public class MomentAdapter extends RecyclerView.Adapter<MomentAdapter.MomentViewHolder> {

    private final Context context;
    private List<Moment> momentList;
    private final OnMomentClickListener listener;
    private final UserRepository userRepository = new UserRepository();
    private final Map<String, User> userCache = new HashMap<>();
    private final ReactionRepository reactionRepository = new ReactionRepository();

    public MomentAdapter(Context context, List<Moment> momentList, OnMomentClickListener listener) {
        this.context = context;
        this.momentList = momentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MomentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_moment_feed, parent, false);
        return new MomentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MomentViewHolder holder, int position) {
        Moment moment = momentList.get(position);
        String senderId = moment.getSenderId() == null ? "" : moment.getSenderId();
        holder.boundSenderId = senderId;

        holder.txtUsername.setText("Đang tải...");
        holder.txtCaption.setText(moment.getCaption() == null ? "" : moment.getCaption());
        holder.imgAvatar.setImageResource(R.mipmap.ic_launcher);
        holder.txtReactionCount.setText("0 reaction");

        if (moment.getMomentId() != null && !moment.getMomentId().isEmpty()) {
            reactionRepository.countReactionsForMoment(moment.getMomentId(), new ReactionRepository.ReactionCountCallback() {
                @Override
                public void onSuccess(int count) {
                    holder.txtReactionCount.setText(count + (count == 1 ? " reaction" : " reactions"));
                }

                @Override
                public void onError(Exception e) {
                    holder.txtReactionCount.setText("0 reaction");
                }
            });
        }

        if (moment.getImageUrl() != null && !moment.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(moment.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.imgMoment);
        } else {
            holder.imgMoment.setImageResource(R.drawable.ic_launcher_background);
        }

        loadSenderInfo(holder, senderId);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMomentClick(moment);
        });
    }

    private void loadSenderInfo(@NonNull MomentViewHolder holder, String senderId) {
        if (senderId == null || senderId.trim().isEmpty()) {
            holder.txtUsername.setText("Người dùng MomentShare");
            return;
        }

        if (userCache.containsKey(senderId)) {
            bindSender(holder, senderId, userCache.get(senderId));
            return;
        }

        userRepository.getUserById(senderId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                userCache.put(senderId, user);
                bindSender(holder, senderId, user);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (senderId.equals(holder.boundSenderId)) {
                    holder.txtUsername.setText("Người dùng MomentShare");
                }
            }
        });
    }

    private void bindSender(@NonNull MomentViewHolder holder, String senderId, User user) {
        if (!senderId.equals(holder.boundSenderId)) {
            return;
        }

        if (user == null) {
            holder.txtUsername.setText("Người dùng MomentShare");
            return;
        }

        String name = user.getFullName();
        String username = user.getUsername();

        if (name != null && !name.trim().isEmpty()) {
            holder.txtUsername.setText(name);
        } else if (username != null && !username.trim().isEmpty()) {
            holder.txtUsername.setText("@" + username);
        } else {
            holder.txtUsername.setText("Người dùng MomentShare");
        }

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getAvatarUrl())
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .circleCrop()
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.mipmap.ic_launcher);
        }
    }

    @Override
    public int getItemCount() {
        return momentList != null ? momentList.size() : 0;
    }

    public void updateData(List<Moment> newMoments) {
        this.momentList = newMoments;
        notifyDataSetChanged();
    }

    public interface OnMomentClickListener {
        void onMomentClick(Moment moment);
    }

    public static class MomentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgMoment, imgReactionIcon;
        TextView txtUsername, txtCaption, txtReactionCount;
        String boundSenderId = "";

        public MomentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            imgMoment = itemView.findViewById(R.id.imgMoment);
            txtCaption = itemView.findViewById(R.id.txtCaption);
            imgReactionIcon = itemView.findViewById(R.id.imgReactionIcon);
            txtReactionCount = itemView.findViewById(R.id.txtReactionCount);
        }
    }
}
