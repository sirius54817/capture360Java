package com.arashivision.sdk.demo.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.model.FloorDetailsModel;
import com.arashivision.sdk.demo.adapter.FloorDetailsAdapter;

import android.view.View;
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

public class FloorDetailsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloorDetailsAdapter adapter;
    private static final String TAG = "FloorDetailsActivity";
    private int project; // Variable to hold the project ID
    private int buildingId;
    private int lastPosition = -1;

    // Add the project_list variable
    private int projectListValue = -1;  // Default value if not found

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floor_details);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Retrieve the project and projectId from the intent
        project = getIntent().getIntExtra("project", -1); // Default value if not found
        buildingId = getIntent().getIntExtra("buildingId", -1);

        Log.d(TAG, "Received project: " + project + " and buildingid: " + buildingId);

        // Setup the adapter with an empty list and an onItemClickListener
        adapter = new FloorDetailsAdapter(this, new ArrayList<>(), new FloorDetailsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int floorId, String imageUrl) {
                navigateToPlanDetails(floorId, imageUrl);
            }
        });

        recyclerView.setAdapter(adapter);

        // Start fetching floor details for the given project ID
        new FetchFloorDetailsTask().execute("https://api.capture360.ai/building/plan_details/");
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            // Apply the item animation
            Animation animation = AnimationUtils.loadAnimation(FloorDetailsActivity.this, R.anim.item_animation);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;  // Update the lastPosition after animation
        }
    }

    private class FetchFloorDetailsTask extends AsyncTask<String, Void, List<FloorDetailsModel>> {

        @Override
        protected List<FloorDetailsModel> doInBackground(String... urls) {
            String urlString = urls[0];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try {
                // Create the URL connection
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

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
                String responseData = buffer.toString();

                return parseFloorDetails(responseData);
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
        protected void onPostExecute(List<FloorDetailsModel> result) {
            if (result != null && !result.isEmpty()) {
                adapter.updateData(result);
            } else {
                Toast.makeText(FloorDetailsActivity.this, "Failed to fetch data or no matching project", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private List<FloorDetailsModel> parseFloorDetails(String responseData) {
        List<FloorDetailsModel> dataList = new ArrayList<>();
        try {
            // Parse the JSON response
            JSONArray jsonArray = new JSONArray(responseData);
            boolean foundMatchingProject = false; // Flag to check if project matches

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int fetchedProjectId = jsonObject.getInt("project");

                // Ensure the fetched project ID matches the one passed from the previous activity
                if (fetchedProjectId == project) {
                    foundMatchingProject = true; // Set flag to true if project matches
                    int id = jsonObject.getInt("id");
                    String floorName = jsonObject.getString("floor"); // Correct field name
                    String image = "https://api.capture360.ai/" + jsonObject.getString("image");

                    // Extract the project_list value from the JSON object
                    projectListValue = jsonObject.optInt("project_list", -1);  // Default to -1 if not found

                    // Filter based on buildingId and project_list value
                    if (projectListValue == buildingId) {
                        // Add the floor details to the list if it matches the buildingId and project_list value
                        dataList.add(new FloorDetailsModel(id, floorName, image));
                    }
                }
            }

            // If no matching floor details are found, show an "Add new data" option
            if (dataList.isEmpty()) {
                dataList.add(new FloorDetailsModel(-1, "Add new data", "")); // Show "Add new data" message if no match
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing data: " + e.getMessage());
        }
        return dataList;
    }

    private void navigateToPlanDetails(int floorId, String imageUrl) {
        // Log for debugging
        Log.d(TAG, "navigateToPlanDetails: floorId=" + floorId + ", imageUrl=" + imageUrl);

        // Construct the URL to pass to PlanDetailsActivity
        String planDetailsUrl = "https://api.capture360.ai/building/plans/";

        // Create an intent to launch PlanDetailsActivity
        Intent intent = new Intent(FloorDetailsActivity.this, PlanDetailsActivity.class);
        intent.putExtra("FLOOR_ID", floorId); // Pass the floorId to PlanDetailsActivity
        intent.putExtra("IMAGE_URL", imageUrl); // Pass the image URL
        intent.putExtra("PLAN_DETAILS_URL", planDetailsUrl);  // Pass the URL for plan details
        intent.putExtra("project", project); // Pass the project
        intent.putExtra("PROJECT_LIST", projectListValue); // Pass the project_list value
        intent.putExtra("buildingId", buildingId);

        Log.d(TAG, "navigateToPlanDetails: project=" + project + ", projectListValue=" + projectListValue + ", buildingId=" + buildingId);

        // Start the PlanDetailsActivity
        startActivity(intent);
    }

    private void navigateToGenerateSaveDetails(int floorId, String imageUrl) {
        // Log for debugging
        Log.d(TAG, "navigateToGenerateSaveDetails: floorId=" + floorId + ", imageUrl=" + imageUrl);

        // Construct the URL to pass to PlanDetailsActivity
        String planDetailsUrl = "https://api.capture360.ai/building/plans/";

        Intent intent = new Intent(FloorDetailsActivity.this, GenerateSaveMapActivity.class);
        intent.putExtra("FLOOR_ID", floorId); // Pass the floorId to PlanDetailsActivity
        intent.putExtra("IMAGE_URL", imageUrl); // Pass the image URL
        intent.putExtra("PLAN_DETAILS_URL", planDetailsUrl);  // Pass the URL for plan details
        intent.putExtra("project", project); // Pass the project
        intent.putExtra("PROJECT_LIST", projectListValue); // Pass the project_list value
        intent.putExtra("buildingId", buildingId);

        Log.d(TAG, "navigateToGenerateSaveDetails: project=" + project + ", projectListValue=" + projectListValue + ", buildingId=" + buildingId);

        // Start the PlanDetailsActivity
        startActivity(intent);
    }
}
