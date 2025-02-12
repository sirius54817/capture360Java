package com.arashivision.sdk.demo.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.activity.LoadTrackMapActivity;
import com.arashivision.sdk.demo.activity.PathView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class loadMapFragment extends Fragment implements SensorEventListener {

    private static final int REQUEST_CODE = 1;
    private static final String API_URL = "https://1d7c-59-97-51-97.ngrok-free.app/building/getFloorPlan/";
    private static final String TAG = "LoadMapFragment";
    private static final float STEP_DISTANCE = 0.5f; // Distance moved per step

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor rotationSensor;
    private ProgressBar progressBar;
    private PathView pathView;
    private TextView timerTextView;

    private Button zoomInButton;
    private Button zoomOutButton;


    private int initialStepCount = -1;
    private int stepCount = 0;
    private int lastStepCount = 0;
    private float userX = 0, userY = 0;
    private int currentSegmentIndex = 0;
    private long startTime = 0;
    private boolean isMoving = false;
    private Handler handler = new Handler();
    private long elapsedTime = 0;



    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_load_track_map, container, false);

        // Initialize UI components
        progressBar = view.findViewById(R.id.progressBar);
        pathView = view.findViewById(R.id.pathView);
        timerTextView = view.findViewById(R.id.timerTextView);
        zoomInButton = view.findViewById(R.id.zoomInButton);  // Zoom In Button
        zoomOutButton = view.findViewById(R.id.zoomOutButton);

        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleListener());

        // Initialize sensor manager and check permissions
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        checkPermissions();

        // Load the map directly when the fragment is created
        loadMapFromApi(getArguments().getInt("MAP_ID", -1));

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

        return view;
    }

    private void checkPermissions() {
        Log.d(TAG, "Checking permissions for ACTIVITY_RECOGNITION...");
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_CODE);
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
    public void onResume() {
        super.onResume();
        registerSensors();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterSensors();
    }

    public boolean onTouch(View v, MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        return true;
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

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();
            scaleFactor *= scale;  // Update the scale factor

            // Make sure the scale factor stays within a reasonable range
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));  // Min zoom 0.1x, max zoom 5x

            // Zoom the map based on the scale factor
            zoomMap(scaleFactor);

            return true;
        }
    }

    // Function to zoom the map based on the scale factor
    private void zoomMap(float scaleFactor) {
        if (pathView != null) {
            pathView.setScaleFactor(scaleFactor);  // Assume `pathView` has a method to scale the path
            Log.d(TAG, "Zoom applied: scale factor = " + scaleFactor);
        } else {
            Log.e(TAG, "PathView is not initialized. Cannot apply zoom.");
        }
    }

    private void handleStepCounter(SensorEvent event) {
        if (initialStepCount == -1) {
            initialStepCount = (int) event.values[0];
        }
        stepCount = (int) event.values[0] - initialStepCount;

        if (stepCount > lastStepCount) {
            long stepTime = SystemClock.elapsedRealtime(); // Current time in milliseconds
            long timeElapsed = stepTime - startTime;

            // Log the step changes
            Log.d(TAG, "Step count updated: " + stepCount);
            Log.d(TAG, "Time from last step to current step: " + timeElapsed / 1000.0 + " seconds");

            // Update the yellow dot position based on user movement
            updateYellowDotPosition(STEP_DISTANCE);

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
        // Handle rotation changes if needed
    }

    private void updateYellowDotPosition(float distance) {
        Log.d(TAG, "Updating yellow dot position...");
        if (pathView != null) {
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
                    Log.d(TAG, "User moved to new position: X = " + userX + ", Y = " + userY);

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
            Log.d(TAG, "PathView updated with new user position.");
        } else {
            Log.e(TAG, "PathView is not initialized or visible. Cannot update position.");
        }
    }

    private void startTimer() {
        startTime = SystemClock.elapsedRealtime();
        handler.postDelayed(updateTimerRunnable, 1000);
        isMoving = true;
    }

    private void stopTimer() {
        elapsedTime = SystemClock.elapsedRealtime() - startTime;
        Log.d(TAG, "Timer stopped. Duration: " + elapsedTime + " milliseconds");
        Toast.makeText(getContext(), "Total time taken: " + elapsedTime / 1000 + " seconds", Toast.LENGTH_SHORT).show();
        isMoving = false;
        handler.removeCallbacks(updateTimerRunnable);
    }

    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            elapsedTime = SystemClock.elapsedRealtime() - startTime;
            int seconds = (int) (elapsedTime / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            handler.postDelayed(this, 1000);
        }
    };

    private void loadMapFromApi(int mapId) {
        Log.d(TAG, "Loading map from API with ID: " + mapId);
        if (mapId <= 0) {
            Log.e(TAG, "Invalid map ID: " + mapId);
            Toast.makeText(getContext(), "Invalid map ID", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String url = API_URL + mapId + "/"; // Constructing the URL with mapId
        new LoadMapTask().execute(url);
    }

    private class LoadMapTask extends AsyncTask<String, Void, List<LoadTrackMapActivity.Coordinate>> {
        @Override
        protected List<LoadTrackMapActivity.Coordinate> doInBackground(String... urls) {
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
                    return null;
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
        protected void onPostExecute(List<LoadTrackMapActivity.Coordinate> coordinates) {
            Log.d(TAG, "Parsing map data completed.");
            progressBar.setVisibility(View.GONE);
            if (coordinates != null) {
                Toast.makeText(getContext(), "Map loaded successfully", Toast.LENGTH_SHORT).show();
                List<List<Double>> coordinateLists = convertCoordinatesToLists(coordinates);
                if (pathView != null) {
                    pathView.setCoordinates(coordinateLists);
                    Log.d(TAG, "PathView coordinates set successfully.");
                } else {
                    Log.e(TAG, "PathView is not initialized or visible.");
                }

                currentSegmentIndex = 0;  // Reset to start of the path

                // Set the user's initial position to the starting point of the loaded map
                if (!coordinates.isEmpty()) {
                    userX = coordinates.get(0).getX();
                    userY = coordinates.get(0).getY();
                    if (pathView != null) {
                        pathView.updateUserPosition(userX, userY);
                        Log.d(TAG, "Initial user position set at X = " + userX + ", Y = " + userY);
                    }
                }
            } else {
                Toast.makeText(getContext(), "Failed to load map", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Map loading failed.");
            }
        }

        private List<LoadTrackMapActivity.Coordinate> parseJsonToCoordinates(String json) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                List<LoadTrackMapActivity.Coordinate> coordinates = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject mapObject = jsonArray.getJSONObject(i);
                    String data = mapObject.getString("data"); // Get the 'data' field as a string
                    JSONArray coordArray = new JSONArray(data); // Parse the 'data' string as a JSON array

                    // Convert the coordinates to a list of Coordinate objects
                    for (int j = 0; j < coordArray.length(); j++) {
                        JSONArray coordPair = coordArray.getJSONArray(j);
                        float x = (float) coordPair.getDouble(0);
                        float y = (float) coordPair.getDouble(1);
                        coordinates.add(new LoadTrackMapActivity.Coordinate(x, y));
                    }
                }
                return coordinates;
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
                return null;
            }
        }

        private List<List<Double>> convertCoordinatesToLists(List<LoadTrackMapActivity.Coordinate> coordinates) {
            List<List<Double>> list = new ArrayList<>();
            for (LoadTrackMapActivity.Coordinate coordinate : coordinates) {
                List<Double> coordList = new ArrayList<>();
                coordList.add((double) coordinate.getX());
                coordList.add((double) coordinate.getY());
                list.add(coordList);
            }
            return list;
        }
    }

    public static class Coordinate {
        private final float x;
        private final float y;

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
        // This method can remain empty for your use case if you don't need to handle accuracy changes
    }

}
