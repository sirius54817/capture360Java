package com.arashivision.sdk.demo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.recyclerview.widget.RecyclerView;

import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.model.FloorDetailsModel;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FloorDetailsAdapter extends RecyclerView.Adapter<FloorDetailsAdapter.FloorViewHolder> {

    private List<FloorDetailsModel> floorList;
    private OnItemClickListener onItemClickListener;
    private Context context;
    private int lastPosition = -1;  // Variable to track the last position for animation

    public interface OnItemClickListener {
        void onItemClick(int floorId, String imageUrl);
    }

    public FloorDetailsAdapter(Context context, List<FloorDetailsModel> floorList, OnItemClickListener listener) {
        this.context = context;
        this.floorList = floorList;
        this.onItemClickListener = listener;
    }

    @Override
    public FloorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_floor_details, parent, false);
        return new FloorViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(FloorViewHolder holder, int position) {
        FloorDetailsModel floor = floorList.get(position);
        holder.floorNameTextView.setText(floor.getName());
        Picasso.get().load(floor.getImageUrl()).into(holder.floorImageView);

        // Call setAnimation() to trigger animation when the item is bound
        setAnimation(holder.itemView, position);

        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(floor.getId(), floor.getImageUrl()));
    }

    @Override
    public int getItemCount() {
        return floorList.size();
    }

    public void updateData(List<FloorDetailsModel> newFloorList) {
        this.floorList = newFloorList;
        notifyDataSetChanged();
    }

    // Set animation on the item
    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            // Apply animation if position is greater than lastPosition
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.item_animation);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;  // Update lastPosition after animation
        }
    }

    public static class FloorViewHolder extends RecyclerView.ViewHolder {
        public TextView floorNameTextView;
        public ImageView floorImageView;

        public FloorViewHolder(View view) {
            super(view);
            floorNameTextView = view.findViewById(R.id.floorNameTextView);
            floorImageView = view.findViewById(R.id.floorImageView);
        }
    }
}
