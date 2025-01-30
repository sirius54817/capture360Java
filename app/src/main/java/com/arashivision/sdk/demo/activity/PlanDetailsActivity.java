package com.arashivision.sdk.demo.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arashivision.sdk.demo.adapter.PlanDetailsAdapter; // Adjust the package as necessary
import com.arashivision.sdk.demo.model.YourDataModel1; // Adjust the package as necessary
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

public class PlanDetailsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PlanDetailsAdapter adapter;
    private List<YourDataModel1> dataList = new ArrayList<>();
    private Button buttonGoToMapping;
    private String projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up the adapter with a click listener
        adapter = new PlanDetailsAdapter(this, dataList, new PlanDetailsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(YourDataModel1 data) {
                // Navigate to MainActivity2 with selected map details
                Intent intent = new Intent(PlanDetailsActivity.this, MainActivity2.class);
                intent.putExtra("MAP_ID", data.getId()); // Pass the ID of the selected map
                intent.putExtra("PLAN_NAME", data.getName()); // Pass the name of the plan
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        // Initialize button for mapping
        buttonGoToMapping = findViewById(R.id.buttonGoToMapping);
        buttonGoToMapping.setOnClickListener(v -> {
            Log.d("PlanDetailsActivity", "Go to Map button clicked");
            Intent intent = new Intent(PlanDetailsActivity.this, GenerateSaveMapActivity.class);
            intent.putExtra("project", projectId);  // Pass the projectId to GenerateSaveMapActivity
            intent.putExtra("PLAN_NAME", "Your Plan Name"); // Replace with actual plan name from fetched data
            intent.putExtra("MAP_ID", 1); // Replace with actual plan ID based on your data
            startActivity(intent);
        });

        // Get project ID from intent passed from MainActivity
        Intent intent = getIntent();
        projectId = intent.getStringExtra("project");
        Log.d("PlanDetailsActivity", "Received project ID: " + projectId);

        // Fetch data from the API
        fetchDataFromApi("https://api.capture360.ai/building/getFloorPlan/");
    }

    private void fetchDataFromApi(String apiUrl) {
        Log.d("PlanDetailsActivity", "Fetching data from API: " + apiUrl);
        new FetchDataTask().execute(apiUrl);
    }

    private List<YourDataModel1> parseData(String responseData) {
        List<YourDataModel1> dataList = new ArrayList<>();
        if (responseData == null || responseData.isEmpty()) {
            Log.e("parseData", "Response data is empty");
            return dataList;
        }

        try {
            JSONArray jsonArray = new JSONArray(responseData);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int id = jsonObject.getInt("id");
                String name = jsonObject.getString("name");
                String data = jsonObject.getString("data"); // Capture the data

                YourDataModel1 dataModel = new YourDataModel1(id, name, data, true);
                dataList.add(dataModel);

                // Log the details about the map
                Log.d("parseData", "Map ID: " + id + ", Name: " + name + ", Data: " + data);
            }
            Log.d("parseData", "Parsed " + dataList.size() + " items.");
        } catch (JSONException e) {
            Log.e("parseData", "Error parsing data: " + e.getMessage());
        }
        return dataList;
    }

    private void displayData(List<YourDataModel1> dataList) {
        // Update the adapter with the fetched data
        adapter.updateData(dataList);
    }

    private class FetchDataTask extends AsyncTask<String, Void, List<YourDataModel1>> {

        @Override
        protected List<YourDataModel1> doInBackground(String... urls) {
            String url = urls[0];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            try {
                Log.d("FetchDataTask", "Fetching data from URL: " + url);

                URL urlObject = new URL(url);
                urlConnection = (HttpURLConnection) urlObject.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream == null) {
                    Log.e("FetchDataTask", "Input stream is null");
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }

                String responseData = buffer.toString();
                Log.d("FetchDataTask", "Response Data: " + responseData);

                return parseData(responseData);
            } catch (Exception e) {
                Log.e("FetchDataTask", "Error fetching data: " + e.getMessage());
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        Log.e("FetchDataTask", "Error closing stream: " + e.getMessage());
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(List<YourDataModel1> result) {
            if (result != null && !result.isEmpty()) {
                displayData(result);
            } else {
                Toast.makeText(PlanDetailsActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
