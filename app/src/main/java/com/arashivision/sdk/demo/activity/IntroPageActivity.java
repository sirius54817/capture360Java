package com.arashivision.sdk.demo.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arashivision.sdk.demo.R;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class IntroPageActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private IntroProjectAdapter adapter;
    private static final String TAG = "IntroPageActivity";
    private int userId;
    private int lastPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_page);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Getting the user_id passed from LoginActivity1
        userId = getIntent().getIntExtra("user_id", -1);

        if (userId == -1) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if user_id is invalid
            return;
        }

        // Initialize the adapter and set it to the RecyclerView
        adapter = new IntroProjectAdapter(new ArrayList<>(), new IntroProjectAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int projectId) {
                navigateToMainActivity1(projectId);
            }
        });

        recyclerView.setAdapter(adapter);

        // Apply slide-in and slide-out animations for the activity transition
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

        // Fetch the projects data filtered by the user_id
        new FetchProjectsTask().execute("https://fd84-59-97-51-97.ngrok-free.app/building/project/");
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            // Apply the item animation
            Animation animation = AnimationUtils.loadAnimation(IntroPageActivity.this, R.anim.item_animation);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;  // Update the lastPosition after animation
        }
    }

    private class FetchProjectsTask extends AsyncTask<String, Void, List<IntroModel>> {

        @Override
        protected List<IntroModel> doInBackground(String... urls) {
            String urlString = urls[0];

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }

                return parseProjects(buffer.toString());
            } catch (Exception e) {
                Log.e(TAG, "Error fetching data: " + e.getMessage());
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Error closing stream: " + e.getMessage());
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(List<IntroModel> result) {
            // Check if result is null or empty and handle appropriately
            if (result == null || result.isEmpty()) {
                Toast.makeText(IntroPageActivity.this, "No projects found for this user", Toast.LENGTH_SHORT).show();
                adapter.updateData(new ArrayList<>()); // Update with an empty list
                return;
            }

            // Filter the projects by checking if user_id matches the user field in the project
            List<IntroModel> filteredProjects = new ArrayList<>();
            for (IntroModel project : result) {
                // Ensure that the project is not null and contains valid fields
                if (project != null &&
                        project.getUser() != null &&
                        project.getUser() == userId &&
                        project.getProjectName() != null && !project.getProjectName().isEmpty() &&
                        project.getCompanyName() != null && !project.getCompanyName().isEmpty() &&
                        project.getLocation() != null && !project.getLocation().isEmpty()) {
                    filteredProjects.add(project);
                }
            }

            // If no projects were found for this user, show a message
            if (filteredProjects.isEmpty()) {
                Toast.makeText(IntroPageActivity.this, "No projects found for this user", Toast.LENGTH_SHORT).show();
            }

            // Update the adapter with the filtered projects list
            adapter.updateData(filteredProjects);
        }
    }

    private List<IntroModel> parseProjects(String jsonData) {
        List<IntroModel> projectList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int id = jsonObject.getInt("id");
                Integer user = jsonObject.isNull("user") ? null : jsonObject.getInt("user");
                String projectName = jsonObject.isNull("project_name") ? null : jsonObject.getString("project_name");
                String companyName = jsonObject.isNull("company_name") ? null : jsonObject.getString("company_name");
                String location = jsonObject.isNull("location") ? null : jsonObject.getString("location");

                // Create project model only if necessary fields are not null
                if (user != null && projectName != null && companyName != null && location != null) {
                    IntroModel project = new IntroModel(id, projectName, companyName, location, user);
                    projectList.add(project);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing project data: " + e.getMessage());
        }
        return projectList;
    }

    private void navigateToMainActivity1(int projectId) {
        Intent intent = new Intent(IntroPageActivity.this, MainActivity1.class);
        intent.putExtra("PROJECT_ID", projectId);
        startActivity(intent);
    }
}
