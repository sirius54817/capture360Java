package com.arashivision.sdk.demo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.model.FloorDetailsModel;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FloorDetailsAdapter extends RecyclerView.Adapter<FloorDetailsAdapter.FloorViewHolder> {

    private List<FloorDetailsModel> floorList;
    private OnItemClickListener onItemClickListener;
    private Context context;

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
