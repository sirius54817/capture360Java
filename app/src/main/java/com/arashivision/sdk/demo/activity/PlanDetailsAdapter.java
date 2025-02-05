package com.arashivision.sdk.demo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.model.YourDataModel1;

import java.util.List;

public class PlanDetailsAdapter extends RecyclerView.Adapter<PlanDetailsAdapter.ViewHolder> {

    private Context context;
    private List<YourDataModel1> dataList;
    private OnItemClickListener listener;
    private int lastPosition = -1; // Initialize lastPosition

    public interface OnItemClickListener {
        void onItemClick(YourDataModel1 data);
    }

    public PlanDetailsAdapter(Context context, List<YourDataModel1> dataList, OnItemClickListener listener) {
        this.context = context;
        this.dataList = dataList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_plan_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YourDataModel1 data = dataList.get(position);
        holder.textViewTitle.setText(data.getName());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(data));

        // Call setAnimation to animate the item
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.item_animation);
            viewToAnimate.startAnimation(animation);
            lastPosition = position; // Update lastPosition
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void updateData(List<YourDataModel1> newDataList) {
        this.dataList.clear();
        this.dataList.addAll(newDataList);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;
        TextView textViewDescription;

        ViewHolder(View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.itemTitle);
            textViewDescription = itemView.findViewById(R.id.itemDescription);
        }
    }
}