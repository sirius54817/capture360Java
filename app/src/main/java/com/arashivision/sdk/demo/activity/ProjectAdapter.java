package com.arashivision.sdk.demo.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arashivision.sdk.demo.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<YourDataModel> projectList;
    private final OnItemClickListener onItemClickListener;
    private int lastPosition = -1; // Track the last position for animation

    // Define interface for item click callback
    public interface OnItemClickListener {
        void onItemClick(int projectId, int buildingId);  // Updated to include buildingId
    }

    // Constructor for the adapter
    public ProjectAdapter(List<YourDataModel> projectList, OnItemClickListener onItemClickListener) {
        this.projectList = projectList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the card item layout for each item in the list
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        YourDataModel project = projectList.get(position);

        // Set the text values for the project
        holder.projectTextView.setText("Project " + project.getProject());
        holder.floorsTextView.setText("Total Floors: " + (project.getTotalFloors() != null ? project.getTotalFloors() : "N/A"));
        holder.employeesTextView.setText("Employees: " + (project.getNoOfEmployees() != null ? project.getNoOfEmployees() : "N/A"));

        // Load the image asynchronously using Glide
        Glide.with(holder.itemView.getContext())
                .load(project.getImage()) // URL or resource ID of the image
                .apply(new RequestOptions()
                        .placeholder(R.drawable.placeholder) // Placeholder image while loading
                        .error(R.drawable.error) // Error image if loading fails
                )
                .into(holder.imageView);

        // Set up the click listener for the card item
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                // Use the correct methods to pass both projectId and buildingId
                onItemClickListener.onItemClick(project.getProject(), project.getId()); // Pass both projectId and buildingId
            }
        });

        // Call setAnimation to apply animation when the item is bound
        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        // Return the size of the projectList, default to 0 if the list is null or empty
        return projectList != null ? projectList.size() : 0;
    }

    // Method to update data in the adapter and refresh the view
    public void updateData(List<YourDataModel> newDataList) {
        this.projectList = newDataList;
        notifyDataSetChanged();
    }

    // ViewHolder class to hold views for each card item
    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView projectTextView;
        TextView floorsTextView;
        TextView employeesTextView;
        ImageView imageView;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize the views
            projectTextView = itemView.findViewById(R.id.projectTextView);
            floorsTextView = itemView.findViewById(R.id.floorsTextView);
            employeesTextView = itemView.findViewById(R.id.employeesTextView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

    // Method to apply animation when an item is bound
    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            // Use the context from the view's itemView
            Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), R.anim.item_animation);
            viewToAnimate.startAnimation(animation);
            lastPosition = position; // Update lastPosition to the current position
        }
    }
}
