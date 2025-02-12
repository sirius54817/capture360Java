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
    private int projectId; // Variable to hold the project ID

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
        projectId = getIntent().getIntExtra("PROJECT_ID", -1); // Default value if not found

        Log.d(TAG, "Received project: " + project + " and projectId: " + projectId);

        // Setup the adapter with an empty list and an onItemClickListener
        adapter = new FloorDetailsAdapter(this, new ArrayList<>(), new FloorDetailsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int floorId, String imageUrl) {
                navigateToPlanDetails(floorId, imageUrl);
            }
        });

        recyclerView.setAdapter(adapter);

        // Start fetching floor details for the given project ID
        new FetchFloorDetailsTask().execute("https://1d7c-59-97-51-97.ngrok-free.app/building/plan_details/");
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
                    String floorName = jsonObject.getString("floor_or_name");
                    String image = "https://1d7c-59-97-51-97.ngrok-free.app/" + jsonObject.getString("image");

                    // Add the floor details to the list
                    dataList.add(new FloorDetailsModel(id, floorName, image));
                }
            }

            if (!foundMatchingProject) {
                dataList.clear(); // Clear the list if no matching project found
                dataList.add(new FloorDetailsModel(-1, "Add new data", "")); // Show "Add new data" message
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
        String planDetailsUrl = "https://1d7c-59-97-51-97.ngrok-free.app/building/plans/project/" + project + "/" + floorId;

        // Create an intent to launch PlanDetailsActivity
        Intent intent = new Intent(FloorDetailsActivity.this, PlanDetailsActivity.class);
        intent.putExtra("FLOOR_ID", floorId); // Pass the floorId to PlanDetailsActivity
        intent.putExtra("IMAGE_URL", imageUrl); // Pass the image URL
        intent.putExtra("PLAN_DETAILS_URL", planDetailsUrl);  // Pass the URL for plan details
        intent.putExtra("project", project); // Pass the project
        intent.putExtra("PROJECT_ID", projectId); // Pass the projectId

        // Start the PlanDetailsActivity
        startActivity(intent);
    }
    private void navigateToGenerateSaveDetails(int floorId, String imageUrl) {
        // Log for debugging
        Log.d(TAG, "navigateToGenerateSaveDetails: floorId=" + floorId + ", imageUrl=" + imageUrl);

        // Construct the URL to pass to PlanDetailsActivity
        String planDetailsUrl = "https://1d7c-59-97-51-97.ngrok-free.app/building/plans/project/" + project + "/" + floorId;

        Intent intent = new Intent(FloorDetailsActivity.this, GenerateSaveMapActivity.class);
        intent.putExtra("FLOOR_ID", floorId); // Pass the floorId to PlanDetailsActivity
        intent.putExtra("IMAGE_URL", imageUrl); // Pass the image URL
        intent.putExtra("PLAN_DETAILS_URL", planDetailsUrl);  // Pass the URL for plan details
        intent.putExtra("project", project); // Pass the project
        intent.putExtra("PROJECT_ID", projectId); // Pass the projectId


        // Start the PlanDetailsActivity
        startActivity(intent);
    }

}
