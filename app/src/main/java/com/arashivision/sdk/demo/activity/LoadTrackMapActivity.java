package com.arashivision.sdk.demo.activity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.util.CameraBindNetworkManager;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdk.demo.osc.OscManager;
import com.arashivision.sdk.demo.osc.callback.IOscCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LoadTrackMapActivity extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_CODE = 1;
    private static final String API_URL = "https://8044-59-97-51-97.ngrok-free.app/building/getFloorPlan/";
    private static final String TAG = "LoadTrackMapActivity";
    private static final float STEP_DISTANCE = 0.5f;  // Define the distance per step (in meters)

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor rotationSensor;
    private ProgressBar progressBar;
    private com.arashivision.sdk.demo.activity.PathView pathView; // PathView instance to display the map path

    // Variables for step counting and yellow dot movement
    private int initialStepCount = -1;  // To store initial step count value
    private int stepCount = 0;          // Current step count
    private int lastStepCount = 0;      // Last step count to track change in steps
    private float userX = 0, userY = 0; // Initial position of the user on the map
    private int currentSegmentIndex = 0; // To track the current segment of the path
    private long startTime = 0;         // Timer start time
    private long elapsedTime = 0;       // Total elapsed time
    private boolean isMoving = false;   // To track whether the user is moving
    private Handler handler = new Handler();  // To update the timer UI periodically

    private TextView timerTextView;  // Timer TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_track_map);

        // Initialize UI components
        progressBar = findViewById(R.id.progressBar);
        pathView = findViewById(R.id.pathView); // Find PathView in layout
        timerTextView = findViewById(R.id.timerTextView);  // Timer TextView

        // Retrieve MAP_ID (int) passed from MainActivity2
        int mapId = getIntent().getIntExtra("MAP_ID", -1); // Default to -1 if not found

        // Error handling if MAP_ID is missing
        if (mapId == -1) {
            Log.e(TAG, "MAP_ID is missing or invalid!");
            Toast.makeText(this, "MAP_ID is missing or invalid!", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if MAP_ID is invalid
            return;
        }

        // Initialize sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        checkPermissions();

        // Load the map directly when the activity is created
        loadMapFromApi(mapId);

        // Initialize buttons
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        findViewById(R.id.btn_connect_by_wifi).setOnClickListener(v -> connectCameraByWifi());

        findViewById(R.id.btn_normal_record_start).setOnClickListener(v -> startNormalRecord());

        findViewById(R.id.btn_normal_record_stop).setOnClickListener(v -> stopNormalRecord());

        findViewById(R.id.btn_start_record).setOnClickListener(v -> startVideoRecording());

        findViewById(R.id.btn_stop_record).setOnClickListener(v -> stopVideoRecording());
    }

    private void connectCameraByWifi() {
        CameraBindNetworkManager.getInstance().bindNetwork(errorCode -> {
            InstaCameraManager.getInstance().openCamera(InstaCameraManager.CONNECT_TYPE_WIFI);
        });
    }

    private void startNormalRecord() {
        Log.d(TAG, "Starting normal recording...");
        Toast.makeText(this, "Normal recording started", Toast.LENGTH_SHORT).show();
    }

    private void stopNormalRecord() {
        Log.d(TAG, "Stopping normal recording...");
        Toast.makeText(this, "Normal recording stopped", Toast.LENGTH_SHORT).show();
    }

    private void startVideoRecording() {
        if (isCameraConnected()) {
            String options = "\"captureMode\":\"video\""; // Set capture mode to video
            OscManager.getInstance().startRecord(options, new IOscCallback() {
                @Override
                public void onSuccessful(Object object) {
                    Log.d(TAG, "Video recording started successfully.");
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Error starting video recording: " + message);
                }
            });
        } else {
            promptToConnectCamera();
        }
    }

    private void stopVideoRecording() {
        if (isCameraConnected()) {
            OscManager.getInstance().stopRecord(new IOscCallback() {
                @Override
                public void onSuccessful(Object object) {
                    Log.d(TAG, "Video recording stopped successfully.");
                    Toast.makeText(LoadTrackMapActivity.this, "Video recording stopped", Toast.LENGTH_SHORT).show();

                    // Retrieve video file path from object if available
                    String videoFilePath = getVideoFilePath(object);
                    Log.d(TAG, "Video recording path: " + videoFilePath);

                    if (videoFilePath != null) {
                        saveVideoToGallery(videoFilePath);
                        saveMapDataToStorage(); // Save map data to storage
                        sendMapDataToServer(); // Send map data to server

                        // Log the location where the video is saved
                        Log.d(TAG, "Video saved to: " + videoFilePath);

                        // Move to MainActivity after the video is saved
                        Intent intent = new Intent(LoadTrackMapActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish(); // Close the current activity
                    } else {
                        Log.e(TAG, "Video file path is not available.");
                    }
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Error stopping video recording: " + message);
                }
            });
        } else {
            promptToConnectCamera();
        }
    }

    private void saveVideoToGallery(String fileName) {
        // Check and request permissions if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;  // Wait for permission result before proceeding
            }
        }

        // Define the directory and file path
        File capture360Dir = new File(Environment.getExternalStorageDirectory(), "Capture360");
        if (!capture360Dir.exists()) {
            boolean dirCreated = capture360Dir.mkdirs();
            if (!dirCreated) {
                Log.e("SaveVideo", "Failed to create directory Capture360");
                return; // Exit if directory creation fails
            }
        }

        // Define the video file
        File videoFile = new File(capture360Dir, fileName);
        String filePath = videoFile.getAbsolutePath();

        // Attempt to create the file if it doesn’t exist
        if (!videoFile.exists()) {
            try {
                boolean fileCreated = videoFile.createNewFile();
                if (!fileCreated) {
                    Toast.makeText(this, "Failed to create video file", Toast.LENGTH_SHORT).show();
                    Log.e("SaveVideo", "File creation failed for path: " + filePath);
                    return;
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error creating video file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("SaveVideo", "IOException while creating file: " + e.getMessage());
                return;
            }
        }

        // Insert file into MediaStore so it appears in the gallery
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        // Attempt to insert into MediaStore
        try {
            getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            Toast.makeText(this, "Video saved to gallery in Capture360 folder", Toast.LENGTH_SHORT).show();
            Log.d("SaveVideo", "Video saved successfully to: " + filePath);
        } catch(Exception e){
            Toast.makeText(this, "Error saving video to gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("SaveVideo", "Error inserting video into MediaStore: " + e.getMessage());
        }
    }

    private String getVideoFilePath(Object object) {
        if (object instanceof String) {
            try {
                JSONObject jsonResponse = new JSONObject((String) object);
                JSONArray fileUrls = jsonResponse.getJSONObject("results").getJSONArray("fileUrls");
                if (fileUrls.length() > 0) {
                    return fileUrls.getString(0);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON for video file path: " + e.getMessage());
            }
        }
        return null;
    }

    private void saveMapDataToStorage() {
        String mapDataJson = getMapDataAsJson();

        File mapDir = new File(Environment.getExternalStorageDirectory(), "Capture360");
        if (!mapDir.exists()) {
            boolean dirCreated = mapDir.mkdirs();
            if (!dirCreated) {
                Log.e(TAG, "Failed to create directory Capture360 for map data");
                return;
            }
        }

        File mapFile = new File(mapDir, "map_data.json");
        try (FileOutputStream fos = new FileOutputStream(mapFile)) {
            fos.write(mapDataJson.getBytes());
            fos.flush();
            Log.d(TAG, "Map data saved to: " + mapFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving map data to storage: " + e.getMessage());
        }
    }

    private void sendMapDataToServer() {
        // Code to send map data to a server
        // This could involve making an HTTP request to a server endpoint
        // and sending the map data as part of the request
        // For example:
        String mapDataJson = getMapDataAsJson();
        String serverUrl = "https://example.com/api/mapdata"; // Replace with your server URL

        // Create a new thread to send the request
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Write the map data to the request body
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = mapDataJson.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Check the response code
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Map data sent to server successfully.");
                } else {
                    Log.e(TAG, "Error sending map data to server. Response code: " + responseCode);
                }

                connection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Error sending map data to server: " + e.getMessage());
            }
        }).start();
    }

    private String getMapDataAsJson() {
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray.put(new JSONObject().put("x", userX).put("y", userY)); // Example of saving user position
        } catch (JSONException e) {
            e.printStackTrace();  // Log the exception or handle it as needed
            return "[]";  // Return an empty JSON array as a fallback in case of error
        }
        return jsonArray.toString(); // Return the JSON array as a string
    }

    private boolean isCameraConnected() {
        return InstaCameraManager.getInstance().getCameraConnectedType() != InstaCameraManager.CONNECT_TYPE_NONE;
    }

    private void promptToConnectCamera() {
        Toast.makeText(this, R.string.osc_toast_connect_camera, Toast.LENGTH_SHORT).show();
    }

    private void checkPermissions() {
        Log.d(TAG, "Checking permissions for ACTIVITY_RECOGNITION...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_CODE);
        } else {
            initSensors();
        }
    }

    private void initSensors() {
        Log.d(TAG, "Initializing sensors...");
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        if (stepCounterSensor == null) {
            Log.d(TAG, "No step counter sensor available");
        } else {
            Log.d(TAG, "Step counter sensor initialized");
        }

        if (rotationSensor == null) {
            Log.d(TAG, "No rotation vector sensor available");
        } else {
            Log.d(TAG, "Rotation vector sensor initialized");
        }
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
        Log.d(TAG, "Registering sensors...");
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
            Log.d(TAG, "Step counter sensor registered.");
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
            Log.d(TAG, "Rotation sensor registered.");
        }
    }

    private void unregisterSensors() {
        Log.d(TAG, "Unregistering sensors...");
        if (stepCounterSensor != null) {
            sensorManager.unregisterListener(this, stepCounterSensor);
            Log.d(TAG, "Step counter sensor unregistered.");
        }

        if (rotationSensor != null) {
            sensorManager.unregisterListener(this, rotationSensor);
            Log.d(TAG, "Rotation sensor unregistered.");
        }
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
            initialStepCount = (int) event.values[0];  // Capture the initial step count
        }
        stepCount = (int) event.values[0] - initialStepCount;

        if (stepCount > lastStepCount) {
            long stepTime = SystemClock.elapsedRealtime(); // Current time in milliseconds
            long timeElapsed = stepTime - startTime;

            // Log the step changes
            Log.d(TAG, "Step count updated: " + stepCount);
            Log.d(TAG, "Time from last step to current step: " + timeElapsed / 1000.0 + " seconds");

            // Update the yellow dot position based on user movement
            updateYellowDotPosition(STEP_DISTANCE); // Call the consolidated function

            // Log the yellow dot movement
            Log.d(TAG, "Yellow dot moved to new position: X = " + userX + ", Y = " + userY);

            startTime = stepTime; // Update start time for the next
            // Start the timer if the user starts moving
            if (!isMoving) {
                startTimer();
            }
        }
        lastStepCount = stepCount;
    }

    private void handleRotationVector(SensorEvent event) {
        // Handle rotation changes here if needed for other purposes
    }

    private void updateYellowDotPosition(float distance) {
        Log.d(TAG, "Updating yellow dot position...");
        // Move the yellow dot based on the step count
        // Assuming that each step corresponds to STEP_DISTANCE meters

        // Calculate the new position of the yellow dot on the map
        if (currentSegmentIndex < pathView.getPoints().size() - 1) {
            List<float[]> points = pathView.getPoints();
            float[] startCoord = points.get(currentSegmentIndex);
            float[] endCoord = points.get(currentSegmentIndex + 1);

            // Calculate the direction vector
            float directionX = endCoord[0] - startCoord[0];
            float directionY = endCoord[1] - startCoord[1];
            float segmentLength = (float) Math.sqrt(directionX * directionX + directionY * directionY);

            // Normalize the direction vector
            if (segmentLength > 0) {
                directionX /= segmentLength;
                directionY /= segmentLength;

                // Move the user position along the segment
                userX += directionX * distance;
                userY += directionY * distance;

                // Log the user's updated position
                Log.d(TAG, "User  moved to new position: X = " + userX + ", Y = " + userY);

                // Check if the user has reached the end of the segment
                if (Math.sqrt(Math.pow(userX - endCoord[0], 2) + Math.pow(userY - endCoord[1], 2)) < STEP_DISTANCE) {
                    userX = endCoord[0];
                    userY = endCoord[1];
                    currentSegmentIndex++; // Move to the next segment

                    // Stop the timer when the dot reaches the end of the segment
                    if (currentSegmentIndex == pathView.getPoints().size() - 1) {
                        stopTimer();
                    }
                }
            }
        }

        // Update the PathView to reflect the new position
        pathView.updateUserPosition(userX, userY); // Update the yellow dot position
    }

    private void startTimer() {
        Log.d(TAG, "Starting timer...");
        isMoving = true;
        startTime = SystemClock.elapsedRealtime(); // Start the timer
        handler.postDelayed(updateTimerRunnable, 1000); // Start updating the timer every second
    }

    private void stopTimer() {
        if (isMoving) {
            elapsedTime = SystemClock.elapsedRealtime() - startTime; // Calculate elapsed time
            Log.d(TAG, "Timer stopped. Duration: " + elapsedTime + " milliseconds");

            // Calculate and display the total duration
            Toast.makeText(this, "Total time taken: " + elapsedTime / 1000 + " seconds", Toast.LENGTH_SHORT).show();
            isMoving = false;
        }
    }

    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isMoving) {
                elapsedTime = SystemClock.elapsedRealtime() - startTime;
                int seconds = (int) (elapsedTime / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void loadMapFromApi(int mapId) {
        Log.d(TAG, "Loading map from API with ID: " + mapId);
        // Checking if mapId is valid
        if (mapId <= 0) {
            Log.e(TAG, "Invalid map ID: " + mapId);
            Toast.makeText(this, "Invalid map ID", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String url = API_URL + mapId + "/"; // Constructing the URL with mapId
        new LoadMapTask().execute(url);
    }

    private class LoadMapTask extends AsyncTask<String, Void, List<Coordinate>> {
        @Override
        protected List<Coordinate> doInBackground(String... urls) {
            Log.d(TAG, "Fetching map data from URL: " + urls[0]);
            String jsonResponse = "";
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP error code: " + responseCode + " for URL: " + urls[0]);
                    return null;  // Return null if the response is not OK
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                jsonResponse = stringBuilder.toString();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching map data", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return parseJsonToCoordinates(jsonResponse);
        }

        @Override
        protected void onPostExecute(List<Coordinate> coordinates) {
            Log.d(TAG, "Parsing map data completed.");
            progressBar.setVisibility(View.GONE);
            if (coordinates != null) {
                Toast.makeText(LoadTrackMapActivity.this, "Map loaded successfully", Toast.LENGTH_SHORT).show();
                List<List<Double>> coordinateLists = convertCoordinatesToLists(coordinates);
                pathView.setCoordinates(coordinateLists); // Pass coordinates to PathView
                currentSegmentIndex = 0;  // Reset to start of the path

                // Set the user's initial position to the starting point of the loaded map
                if (!coordinates.isEmpty()) {
                    userX = coordinates.get(0).getX(); // Set userX to the first coordinate's X
                    userY = coordinates.get(0).getY(); // Set userY to the first coordinate's Y
                    pathView.updateUserPosition(userX, userY); // Update the yellow dot position
                }
            } else {
                Toast.makeText(LoadTrackMapActivity.this, "Failed to load map", Toast.LENGTH_SHORT).show();
            }
        }

        private List<Coordinate> parseJsonToCoordinates(String json) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                List<Coordinate> coordinates = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject mapObject = jsonArray.getJSONObject(i);
                    String data = mapObject.getString("data"); // Get the 'data' field as a string
                    JSONArray coordArray = new JSONArray(data); // Parse the 'data' string as a JSON array

                    // Convert the coordinates to a list of Coordinate objects
                    for (int j = 0; j < coordArray.length(); j++) {
                        JSONArray coordPair = coordArray.getJSONArray(j);
                        float x = (float) coordPair.getDouble(0);
                        float y = (float) coordPair.getDouble(1);
                        coordinates.add(new Coordinate(x, y));
                    }
                }
                return coordinates;
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
                return null;
            }
        }

        private List<List<Double>> convertCoordinatesToLists(List<Coordinate> coordinates) {
            List<List<Double>> list = new ArrayList<>();
            for (Coordinate coordinate : coordinates) {
                List<Double> coordList = new ArrayList<>();
                coordList.add((double) coordinate.getX());
                coordList.add((double) coordinate.getY());
                list.add(coordList);
            }
            return list;
        }
    }

    public static class Coordinate {
        private float x;
        private float y;

        public Coordinate(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No implementation needed
    }
}

