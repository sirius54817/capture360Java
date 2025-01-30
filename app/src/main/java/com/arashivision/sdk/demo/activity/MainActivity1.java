package com.arashivision.sdk.demo.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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

public class MainActivity1 extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProjectAdapter adapter;
    private static final String TAG = "MainActivity1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);

        recyclerView = findViewById(R.id.recyclerView);

        // Initialize RecyclerView with correct adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Corrected ProjectAdapter initialization with a single OnItemClickListener
        adapter = new ProjectAdapter(new ArrayList<>(), new ProjectAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int projectId) {
                navigateToPlanDetails(projectId);
            }
        });

        recyclerView.setAdapter(adapter);

        // Fetch data directly when the activity is created
        new FetchDataTask().execute("https://api.capture360.ai/building/projectlist/");
    }

    // AsyncTask to fetch data and images
    private class FetchDataTask extends AsyncTask<String, Void, List<YourDataModel>> {

        @Override
        protected List<YourDataModel> doInBackground(String... urls) {
            String urlString = urls[0];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
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

                // Parse JSON response to extract data and images
                return parseDataAndImages(responseData);
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
        protected void onPostExecute(List<YourDataModel> result) {
            if (result != null && !result.isEmpty()) {
                // Update RecyclerView with fetched data
                adapter.updateData(result);
            } else {
                Toast.makeText(MainActivity1.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to parse data and images from JSON response
    private List<YourDataModel> parseDataAndImages(String responseData) {
        List<YourDataModel> dataList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(responseData);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int projectId = jsonObject.getInt("project");
                String image = "https://api.capture360.ai/" + jsonObject.getString("image");
                String totalFloors = jsonObject.isNull("total_floors") ? null : jsonObject.getString("total_floors");
                String noOfEmployees = jsonObject.isNull("no_of_employees") ? null : jsonObject.getString("no_of_employees");
                String planDetailsUrl = jsonObject.isNull("plan_details_url") ? null : jsonObject.getString("plan_details_url");

                YourDataModel dataModel = new YourDataModel(projectId, image, totalFloors, noOfEmployees);
                dataModel.setPlanDetailsUrl(planDetailsUrl); // Set planDetailsUrl
                dataList.add(dataModel);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing data and images: " + e.getMessage());
        }
        return dataList;
    }

    // Method to navigate to PlanDetailsActivity
    private void navigateToPlanDetails(int projectId) {
        // Base URL for plan details
        String basePlanDetailsUrl = "https://api.capture360.ai/building/plans/project/";
        Log.d(TAG, "navigateToPlanDetails: " + basePlanDetailsUrl);
        Log.d(TAG, "navigateToPlanDetails: " + projectId);

        // Construct the complete URL with projectId
        String planDetailsUrl = basePlanDetailsUrl + projectId + "/";

        // Create an intent to launch PlanDetailsActivity
        Intent intent = new Intent(MainActivity1.this, PlanDetailsActivity.class);
        intent.putExtra("PLAN_DETAILS_URL", planDetailsUrl); // Pass the complete URL as extra

        // Start the activity
        startActivity(intent);
    }

    // Method to navigate to MainActivity2
    private void navigateToMainActivity2(int projectId) {
        // Create an intent to launch MainActivity2/IntroPageActivity
        Intent intent = new Intent(MainActivity1.this, IntroPageActivity.class);
        intent.putExtra("project", projectId); // Pass the projectId
        Log.d(TAG, "navigateToMainActivity2: " + projectId);

        // Start the activity
        startActivity(intent);  
    }
}
