package com.arashivision.sdk.demo.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import com.arashivision.sdk.demo.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class GenerateSaveMapActivity extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_CODE = 1;
    private static final float STEP_DISTANCE = 0.40f; // Distance moved per step
    private static final String BASE_API_URL = "https://1d7c-59-97-51-97.ngrok-free.app/building/generate_floor_plan/";
    private static final String TAG = "GenerateSaveMapActivity";

    private SensorManager sensorManager;
    private Sensor stepSensor, rotationSensor;
    private PathView pathView;
    private Button saveButton, clearButton, zoomInButton, zoomOutButton;
    private ProgressBar progressBar;

    private int stepCount = 0;
    private int initialStepCount = -1;
    private float direction = 0;
    private float lastStepCount = 0;
    private ArrayList<String> mapNames = new ArrayList<>();
    private int project; // Use "project" instead of "projectId"
    private int floorId; // Variable to hold the floor ID

    private long startTime = 0; // Timer for the start of the map drawing
    private long lastStepTime = 0; // Last time step was taken
    private ArrayList<Float> stepDurations = new ArrayList<>(); // Store time between steps in seconds

    private float scaleFactor = 1.0f; // Initial scale factor for zooming

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_save_map);

        // Retrieve the floorId from the intent
        floorId = getIntent().getIntExtra("FLOOR_ID", -1); // Default value if not found
        Log.d(TAG, "Received floorId: " + floorId);

        // Fetch the current project from the API
        fetchProjectId();

        initializeViews();
        setupButtons();
        checkPermissions();
    }

    private void fetchProjectId() {
        new FetchProjectIdTask().execute("https://1d7c-59-97-51-97.ngrok-free.app/building/projectlist/");
    }

    private class FetchProjectIdTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... urls) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(urls[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    return parseProjectId(response.toString());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error fetching project ID: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing reader: " + e.getMessage());
                    }
                }
            }
            return -1; // Return -1 if there's an error
        }

        @Override
        protected void onPostExecute(Integer project) {
            if (project != -1) {
                GenerateSaveMapActivity.this.project = project; // Update the project variable
                Log.d(TAG, "Fetched Project: " + project);
            } else {
                Toast.makeText(GenerateSaveMapActivity.this, "Failed to fetch project ID", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity if unable to fetch project ID
            }
        }

        private Integer parseProjectId(String jsonResponse) {
            try {
                JSONArray projects = new JSONArray(jsonResponse);
                if (projects.length() > 0) {
                    JSONObject firstProject = projects.getJSONObject(0);
                    return firstProject.getInt("project"); // Use "project" instead of "id"
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error: " + e.getMessage());
            }
            return -1; // Return -1 if parsing fails
        }
    }

    private void initializeViews() {
        pathView = findViewById(R.id.pathView);
        saveButton = findViewById(R.id.saveButton);
        clearButton = findViewById(R.id.clearButton);
        progressBar = findViewById(R.id.progressBar);
        zoomInButton = findViewById(R.id.zoomInButton);  // Zoom In Button
        zoomOutButton = findViewById(R.id.zoomOutButton); // Zoom Out Button
    }

    private void setupButtons() {
        saveButton.setOnClickListener(v -> showSaveMapDialog());
        clearButton.setOnClickListener(v -> {
            pathView.clearPath();
            Toast.makeText(this, "Path cleared", Toast.LENGTH_SHORT).show();
        });

        // Zoom In button functionality
        zoomInButton.setOnClickListener(v -> {
            scaleFactor *= 1.1f; // Zoom In by 10%
            scaleFactor = Math.min(scaleFactor, 5.0f); // Ensure max zoom is 5x
            zoomMap(scaleFactor);
        });

        // Zoom Out button functionality
        zoomOutButton.setOnClickListener(v -> {
            scaleFactor *= 0.9f; // Zoom Out by 10%
            scaleFactor = Math.max(scaleFactor, 0.1f); // Ensure min zoom is 0.1x
            zoomMap(scaleFactor);
        });
    }

    private void zoomMap(float scaleFactor) {
        // Implement the zoom functionality for pathView
        pathView.setScaleFactor(scaleFactor);
        Log.d(TAG, "Zoom level: " + scaleFactor);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_CODE);
        } else {
            initSensors();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initSensors();
            } else {
                Toast.makeText(this, "Permission required for step counting", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        if (stepSensor == null) Log.d(TAG, "No step counter sensor available");
        if (rotationSensor == null) Log.d(TAG, "No rotation vector sensor available");
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensors();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterSensors();
    }

    private void registerSensors() {
        if (stepSensor != null) sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        if (rotationSensor != null) sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
    }

    private void unregisterSensors() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            handleStepCounter(event);
        } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            handleRotationVector(event);
        }
    }

    private void handleStepCounter(SensorEvent event) {
        if (initialStepCount == -1) {
            initialStepCount = (int) event.values[0];
            startTime = SystemClock.elapsedRealtime(); // Start timer when we start drawing the map
            Log.d(TAG, "Step counting started at time: " + startTime);
        }
        stepCount = (int) event.values[0] - initialStepCount;

        if (stepCount > lastStepCount) {
            long currentStepTime = SystemClock.elapsedRealtime();
            if (lastStepTime != 0) {
                // Calculate the time between the last step and the current step in milliseconds
                long stepDurationMillis = currentStepTime - lastStepTime;
                float stepDurationSeconds = stepDurationMillis / 1000.0f; // Convert milliseconds to seconds
                stepDurations.add(stepDurationSeconds); // Store the duration in seconds
                Log.d(TAG, "Step " + stepCount + " took " + String.format("%.3f", stepDurationSeconds) + " s");
            }
            lastStepTime = currentStepTime;

            updatePath();
            pathView.moveUserAlongPath(STEP_DISTANCE);
        }
        lastStepCount = stepCount;
    }

    private void handleRotationVector(SensorEvent event) {
        float[] rotationMatrix = new float[9];
        float[] orientation = new float[3];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        SensorManager.getOrientation(rotationMatrix, orientation);
        direction = (float) Math.toDegrees(orientation[0]);
    }

    private void updatePath() {
        float dx = (float) (STEP_DISTANCE * Math.cos(Math.toRadians(direction)));
        float dy = (float) (STEP_DISTANCE * Math.sin(Math.toRadians(direction)));
        pathView.updatePath(dx, dy);
        Log.d(TAG, "Updated path with dx: " + dx + ", dy: " + dy);
    }

    private void showSaveMapDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Map");

        // Create layout for dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText mapNameInput = new EditText(this);
        mapNameInput.setHint("Map Name");
        layout.addView(mapNameInput);

        builder.setView(layout);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String mapName = mapNameInput.getText().toString().trim();
            if (validateMapName(mapName)) {
                Log.d(TAG, "Saving map with floorId: " + floorId);
                saveMapToApi(mapName, pathView.getPathAsJson());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private boolean validateMapName(String mapName) {
        if (mapName.isEmpty()) {
            Toast.makeText(this, "Map name cannot be empty.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mapNames.contains(mapName)) {
            Toast.makeText(this, "Map name already exists.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveMapToApi(String mapName, String jsonData) {
        JSONObject mapJson = new JSONObject();
        try {
            mapJson.put("name", mapName);
            mapJson.put("data", jsonData);
            mapJson.put("plan", floorId); // Use the floorId for the "plan" field

            // Add timing information (total duration and step durations)
            long totalDurationMillis = SystemClock.elapsedRealtime() - startTime;
            float totalDurationSeconds = totalDurationMillis / 1000.0f; // Convert to seconds
            mapJson.put("total_duration", totalDurationSeconds);
            Log.d(TAG, "Total duration: " + String.format("%.3f", totalDurationSeconds) + " s");

            JSONArray durationsArray = new JSONArray();
            for (Float duration : stepDurations) {
                durationsArray.put(duration);
            }
            mapJson.put("step_durations", durationsArray);

            // Construct API URL with the "project" variable
            String apiUrl = BASE_API_URL + project + "/"; // Use project instead of projectId
            new SaveTask(mapName, mapJson.toString(), apiUrl, this).execute();
        } catch (JSONException e) {
            Log.e(TAG, "JSONException: " + e.getMessage());
            Toast.makeText(this, "Error creating JSON data.", Toast.LENGTH_SHORT).show();
        }
    }


    private static class SaveTask extends AsyncTask<Void, Void, Integer> {
        private final String jsonData;
        private final String mapName;
        private final String apiUrl;
        private final WeakReference<GenerateSaveMapActivity> activityReference;

        public SaveTask(String mapName, String jsonData, String apiUrl, GenerateSaveMapActivity activity) {
            this.mapName = mapName;
            this.jsonData = jsonData;
            this.apiUrl = apiUrl;
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            GenerateSaveMapActivity activity = activityReference.get();
            if (activity != null) {
                activity.progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                return sendHttpRequest("POST", apiUrl, jsonData);
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            GenerateSaveMapActivity activity = activityReference.get();
            if (activity != null) {
                activity.progressBar.setVisibility(View.GONE);
                if (responseCode != null && (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED)) {
                    Toast.makeText(activity, "Map saved successfully", Toast.LENGTH_SHORT).show();
                    activity.mapNames.add(mapName);
                    Log.d(TAG, "Map saved successfully: " + responseCode);

                    String savedMapLocation = activity.BASE_API_URL + activity.project + "/" + mapName; // Adjust as needed
                    Log.d(TAG, "Saved map location: " + savedMapLocation);
                } else {
                    Toast.makeText(activity, "Failed to save map: " + (responseCode != null ? responseCode : "Unknown error"), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to save map, response code: " + responseCode);
                }
            }
        }

        private int sendHttpRequest(String method, String apiUrl, String jsonData) throws IOException {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                // Write JSON data
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(jsonData.getBytes());
                }

                // Check response code
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
                    // Read error stream
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        StringBuilder errorMessage = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            errorMessage.append(line);
                        }
                        Log.e(TAG, "Error response: " + errorMessage.toString());
                    }
                }
                return responseCode;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this use case
    }
}
