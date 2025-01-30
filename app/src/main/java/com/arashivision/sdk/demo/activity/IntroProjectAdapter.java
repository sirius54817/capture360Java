package com.arashivision.sdk.demo.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arashivision.sdk.demo.R;

import java.util.List;

public class IntroProjectAdapter extends RecyclerView.Adapter<IntroProjectAdapter.ProjectViewHolder> {

    private List<IntroModel> projectList;
    private OnItemClickListener onItemClickListener;

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_intro_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        IntroModel project = projectList.get(position);
        holder.bind(project);
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView projectName, companyName, location;

        ProjectViewHolder(View itemView) {
            super(itemView);
            projectName = itemView.findViewById(R.id.projectName);
            companyName = itemView.findViewById(R.id.companyName);
            location = itemView.findViewById(R.id.location);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener.onItemClick(projectList.get(position).getId());
                }
            });
        }

        void bind(IntroModel project) {
            projectName.setText(project.getProjectName());
            companyName.setText(project.getCompanyName());
            location.setText(project.getLocation());
        }
    }
}
