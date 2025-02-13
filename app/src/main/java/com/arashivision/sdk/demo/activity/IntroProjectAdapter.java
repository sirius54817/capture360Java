package com.arashivision.sdk.demo.activity;

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

import java.util.List;

public class IntroProjectAdapter extends RecyclerView.Adapter<IntroProjectAdapter.ProjectViewHolder> {

    private List<IntroModel> projectList;
    private OnItemClickListener onItemClickListener;
    private int lastPosition = -1;  // To track the last position that was animated
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(int projectId);
    }

    public IntroProjectAdapter(List<IntroModel> projectList, OnItemClickListener onItemClickListener) {
        this.projectList = projectList;
        this.onItemClickListener = onItemClickListener;
    }

    public void updateData(List<IntroModel> newProjectList) {
        this.projectList = newProjectList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_intro_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        IntroModel project = projectList.get(position);
        holder.bind(project);

        // Apply animation if the position is greater than lastPosition
        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    // Method to apply animation on item
    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            // Load the animation from XML
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.item_animation);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;  // Update lastPosition after animation
        }
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView projectName, companyName, location;

        ProjectViewHolder(View itemView) {
            super(itemView);
            projectName = itemView.findViewById(R.id.projectName);
            companyName = itemView.findViewById(R.id.companyName);
            location = itemView.findViewById(R.id.location);

            // Set click listener for the item view
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener.onItemClick(projectList.get(position).getId());
                }
            });
        }

        // Method to bind data to views
        void bind(IntroModel project) {
            projectName.setText(project.getProjectName());
            companyName.setText(project.getCompanyName());
            location.setText(project.getLocation());
        }
    }
}
