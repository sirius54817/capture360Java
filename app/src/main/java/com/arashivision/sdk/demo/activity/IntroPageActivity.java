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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_page);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new IntroProjectAdapter(new ArrayList<>(), new IntroProjectAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int projectId) {
                navigateToMainActivity1(projectId);
            }
        });

        recyclerView.setAdapter(adapter);

        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

        new FetchProjectsTask().execute("https://1d7c-59-97-51-97.ngrok-free.app/building/project/");

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
            if (result != null && !result.isEmpty()) {
                adapter.updateData(result);
            } else {
                Toast.makeText(IntroPageActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private List<IntroModel> parseProjects(String jsonData) {
        List<IntroModel> projectList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int id = jsonObject.getInt("id");
                String projectName = jsonObject.getString("project_name");
                String companyName = jsonObject.getString("company_name");
                String location = jsonObject.getString("location");

                Log.d(TAG, "Parsed project: ID=" + id + ", Name=" + projectName + ", Company=" + companyName + ", Location=" + location);
                IntroModel project = new IntroModel(id, projectName, companyName, location);
                projectList.add(project);
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
