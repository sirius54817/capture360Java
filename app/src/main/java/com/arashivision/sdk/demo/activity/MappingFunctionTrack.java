/*
package com.arashivision.sdk.demo.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout; // Import for LinearLayout
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.TextView; // Import for TextView
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.ScaleGestureDetector;
import android.os.Handler; // Import for Handler

import com.arashivision.sdk.demo.R;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;

import java.lang.reflect.Type;

public class MappingFunctionTrack extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_CODE = 1;
    private static final float STEP_DISTANCE = 0.75f; // Distance moved per step
    private static final String API_URL = "https://14ce-59-97-51-97.ngrok-free.app/building/"; // Update with your actual API URL
    private static final String TAG = "MappingFunctionTrack";

    private SensorManager sensorManager;
    private Sensor stepSensor, rotationSensor;
    private PathView pathView;
    private Button saveButton, loadButton, clearButton;
    private ProgressBar progressBar;

    private int stepCount = 0;
    private int initialStepCount = -1;
    private float direction = 0;
    private float lastStepCount = 0;
    private ArrayList<String> mapNames = new ArrayList<>();
    private Map<String, String> mapIdMap = new HashMap<>();

    // To hold project details
    private String currentProjectId;
    private String currentProjectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapping_function_track);

        pathView = findViewById(R.id.pathView);
        saveButton = findViewById(R.id.saveButton);
        loadButton = findViewById(R.id.loadButton);
        clearButton = findViewById(R.id.clearButton);
        progressBar = findViewById(R.id.progressBar);

        setupButtons();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        checkPermissions();

        // Fetch current project details
        fetchCurrentProjectDetails();
    }

    private void setupButtons() {
        saveButton.setOnClickListener(v -> showSaveMapDialog());
        loadButton.setOnClickListener(v -> loadMapListFromApi());
        clearButton.setOnClickListener(v -> pathView.clearPath());
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
        }
        stepCount = (int) event.values[0] - initialStepCount;

        if (stepCount > lastStepCount) {
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
                saveMapToApi(mapName, currentProjectName, currentProjectId, pathView.getPathAsJson());
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

    private void saveMapToApi(String mapName, String projectName, String projectId, String jsonData) {
        JSONObject mapJson = new JSONObject();
        try {
            mapJson.put("name", mapName);
            mapJson.put("data", jsonData);
            mapJson.put("project_name", projectName); // Add project name
            mapJson.put("project_id", projectId);     // Add project ID

            new SaveTask(mapName, mapJson.toString()).execute(); // Pass both mapName and json data
        } catch (JSONException e) {
            Log.e(TAG, "JSONException: " + e.getMessage());
            Toast.makeText(this, "Error creating JSON data.", Toast.LENGTH_SHORT).show();
        }
    }

    private class SaveTask extends AsyncTask<String, Void, Integer> {
        private String jsonData;
        private String mapName; // Hold the map name

        public SaveTask(String mapName, String jsonData) {
            this.mapName = mapName; // Store the map name
            this.jsonData = jsonData; // Store the JSON data
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(String... params) {
            try {
                return sendHttpRequest("POST", API_URL + "generate_floor_plan/", jsonData);
            } catch (IOException e) {
                Log.e (TAG, "IOException: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            progressBar.setVisibility(View.GONE);
            if (responseCode != null && (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED )) {
                Toast.makeText(MappingFunctionTrack.this, "Map saved successfully", Toast.LENGTH_SHORT).show();
                mapNames.add(mapName); // Now mapName is accessible
                Log.d(TAG, "Map saved successfully: " + responseCode);
            } else {
                Toast.makeText(MappingFunctionTrack.this, "Failed to save map: " + (responseCode != null ? responseCode : "No response"), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Save failed: " + (responseCode != null ? responseCode : "No response"));
            }
        }
    }

    private void loadMapListFromApi() {
        new LoadMapListTask().execute();
    }

    private class LoadMapListTask extends AsyncTask<Void, Void, ArrayList<String>> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(API_URL + "getFloorPlan/");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONArray jsonArray = new JSONArray(response.toString());
                    ArrayList<String> mapNames = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject mapObject = jsonArray.getJSONObject(i);
                        String name = mapObject.getString("name");
                        mapNames.add(name);
                        mapIdMap.put(name, mapObject.getString("id")); // Store map ID if needed
                    }
                    return mapNames;
                } else {
                    Log.e(TAG, "Error response code: " + responseCode);
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error loading map list: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<String> mapNames) {
            progressBar.setVisibility(View.GONE);
            if (mapNames != null) {
                showMapSelectionDialog(mapNames);
            } else {
                Toast.makeText(MappingFunctionTrack.this, "Failed to load maps", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showMapSelectionDialog(ArrayList<String> mapNames) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Map");
        builder.setItems(mapNames.toArray(new String[0]), (dialog, which) -> {
            String selectedMapName = mapNames.get(which);
            loadMapData(mapIdMap.get(selectedMapName));
        });
        builder.show();
    }

    private void loadMapData(String mapId) {
        new LoadMapDataTask().execute(mapId);
    }

    private class LoadMapDataTask extends AsyncTask<String, Void, List<List<Double>>> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<List<Double>> doInBackground(String... params) {
            String mapId = params[0];
            HttpURLConnection connection = null;
            try {
                URL url = new URL(API_URL + "getFloorPlan/" + mapId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONArray jsonArray = new JSONArray(response.toString());
                    if (jsonArray.length() > 0) {
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        String dataString = jsonObject.getString("data");

                        // Convert string representation of JSON array to a JSONArray
                        JSONArray jsonData = new JSONArray(dataString);
                        if (jsonData.length() > 0) {
                            List<List<Double>> coordinates = new ArrayList<>();
                            for (int i = 0; i < jsonData.length(); i++) {
                                JSONArray point = jsonData.getJSONArray(i);
                                List<Double> coordinate = new ArrayList<>();
                                coordinate.add(point.getDouble(0));
                                coordinate.add(point.getDouble(1));
                                coordinates.add(coordinate);
                            }
                            return coordinates;
                        } else {
                            Log.e(TAG, " Error: JSON data array is empty");
                        }
                    } else {
                        Log.e(TAG, "Error: JSON array is empty");
                    }
                } else {
                    Log.e(TAG, "Error response code: " + responseCode);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading map data: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<List<Double>> coordinates) {
            progressBar.setVisibility(View.GONE);
            if (coordinates != null) {
                pathView.setCoordinates(coordinates);
                pathView.setShowUserDot(true); // Show the user dot
                Toast.makeText(MappingFunctionTrack.this, "Map loaded successfully", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to load map data.");
                Toast.makeText(MappingFunctionTrack.this, "Failed to load map data.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int sendHttpRequest(String requestMethod, String urlString, String jsonInput) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(requestMethod);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            if (jsonInput != null) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInput.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                return responseCode; // Return the response code
            } else {
                // Read the error response
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    errorResponse.append(inputLine);
                }
                in.close();
                Log.e(TAG, "Error response code: " + responseCode + ", Message: " + errorResponse.toString());
                throw new IOException("Unexpected response code: " + responseCode);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
                connection.disconnect();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed here
    }

    private void fetchCurrentProjectDetails() {
        // Implement API call to fetch current project details
        // For demonstration purposes, assume the project details are fetched successfully
        currentProjectId = "12345"; // Example project ID
        currentProjectName = "My Project"; // Example project name
    }
}

class PathView extends View {
    private Paint paint;
    private Paint arrowPaint;
    private Paint userDotPaint;
    private Paint startDotPaint;
    private List<float[]> points;
    private float scaleFactor = 1.0f;
    private ScaleGestureDetector scaleGestureDetector;
    private float userX, userY;
    private float startX, startY;
    private boolean showUserDot = false;
    private boolean canDrawPath = true;
    private boolean isMapLoaded = false;
    private boolean showArrow = true;
    private boolean showStartDot = false;
    private int currentSegmentIndex = 0;
    private float progressAlongSegment = 0; // Ranges from 0 to 1
    private Handler handler = new Handler();
    private Runnable moveUserRunnable;

    public PathView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.STROKE);

        arrowPaint = new Paint();
        arrowPaint.setStrokeWidth(10);
        arrowPaint.setColor(Color.WHITE);
        arrowPaint.setStyle(Paint.Style.FILL);

        userDotPaint = new Paint();
        userDotPaint.setColor(Color.YELLOW);
        userDotPaint.setStyle(Paint.Style.FILL);

        startDotPaint = new Paint();
        startDotPaint.setColor(Color.BLUE);
        startDotPaint.setStyle(Paint.Style.FILL);

        points = new ArrayList<>();
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        moveUser Runnable = new Runnable() {
            @Override
            public void run() {
                moveUserAlongPath(5); // Adjust step size as needed
                handler.postDelayed(this, 100);
            }
        };
    }

    public void updatePath(float dx, float dy) {
        if (canDrawPath) {
            if (points.isEmpty()) {
                points.add(new float[]{0, 0});
            }
            float[] lastPoint = points.get(points.size() - 1);
            points.add(new float[]{lastPoint[0] + dx, lastPoint[1] + dy});
            invalidate();
        }
    }

    public void updateUserPosition(float x, float y) {
        userX = x;
        userY = y;
        setShowUserDot(true);
        invalidate();
    }

    public void moveUserAlongPath(float stepSize) {
        if (isMapLoaded && currentSegmentIndex < points.size() - 1) {
            float[] startPoint = points.get(currentSegmentIndex);
            float[] endPoint = points.get(currentSegmentIndex + 1);

            // Calculate segment vector
            float deltaX = endPoint[0] - startPoint[0];
            float deltaY = endPoint[1] - startPoint[1];
            float segmentLength = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            // Update progress along the segment
            if (progressAlongSegment + (stepSize / segmentLength) >= 1) {
                // Move to the end of the segment
                userX = endPoint[0];
                userY = endPoint[1];
                progressAlongSegment = 0; // Reset progress for the next segment
                currentSegmentIndex++; // Move to the next segment
            } else {
                // Move along the segment
                progressAlongSegment += stepSize / segmentLength;
                userX = startPoint[0] + deltaX * progressAlongSegment;
                userY = startPoint[1] + deltaY * progressAlongSegment;
            }

            // Stop if the user reaches the end of the path
            if (currentSegmentIndex >= points.size() - 1) {
                stopUserMovement();
            }

            invalidate(); // Refresh the view
        }
    }

    public void setShowUserDot(boolean show) {
        showUser  Dot = show;
        invalidate();
    }

    public void clearPath() {
        points.clear();
        canDrawPath = true;
        showArrow = true; // Show arrow when clearing the path
        showStartDot = false;
        invalidate();
        currentSegmentIndex = 0;
    }

    public void undoLastPath() {
        if (!points.isEmpty()) {
            points.remove(points.size() - 1);
            invalidate();
        }
    }

    public String getPathAsJson() {
        JSONArray jsonArray = new JSONArray();
        try {
            for (float[] point : points) {
                JSONArray pointJson = new JSONArray();
                pointJson.put(point[0]);
                pointJson.put(point[1]);
                jsonArray.put(pointJson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray.toString();
    }

    public void loadPathFromJson(String jsonData) {
        points.clear();
        isMapLoaded = true;
        canDrawPath = false; // Disable path drawing
        showArrow = false; // Hide arrow when loading a saved map
        showStartDot = true;
        currentSegmentIndex = 0; // Reset the path index
        progressAlongSegment = 0; // Reset progress

        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONArray pointJson = jsonArray.getJSONArray(i);
                float x = (float) pointJson.getDouble(0);
                float y = (float) pointJson.getDouble(1);
                points.add(new float[]{x, y});

                if (i == 0) {
                    // Set the user position to the start of the path
                    userX = x;
                    userY = y;
                    startX = x;
                    startY = y;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Do not start movement automatically when loading a path
        invalidate();
    }

    public void setCoordinates(List<List<Double>> coordinates) {
        points.clear();
        isMapLoaded = true;
        canDrawPath = false; // Disable path drawing
        showArrow = false; // Hide arrow when setting new coordinates
        showStartDot = true;

        for (List<Double> coord : coordinates) {
            points.add(new float[]{coord.get(0).floatValue(), coord.get(1).floatValue()});
            if (coordinates.indexOf(coord) == 0) {
                userX = coord.get(0).floatValue();
                userY = coord.get(1).floatValue();
                startX = coord.get(0).floatValue();
                startY = coord.get(1).floatValue();
            }
        }
        // Do not start movement automatically when setting new coordinates
        invalidate();
    }

    public void startMovement() {
        if (isMapLoaded) {
            startUserMovement(); // Start moving the user along the path
        }
    }

    public void resetMapLoaded() {
        isMapLoaded = false;
        canDrawPath = true; // Allow drawing new path
        showArrow = true ; // Show arrow when resetting the map
        showStartDot = false;
        currentSegmentIndex = 0;
    }

    public void startTracking() {
        canDrawPath = true;
        isMapLoaded = false; // Map is not loaded
        showArrow = true; // Show arrow during tracking
        invalidate();
    }

    public void startUserMovement() {
        handler.post(moveUserRunnable);
    }

    public void stopUserMovement() {
        handler.removeCallbacks(moveUserRunnable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(scaleFactor, scaleFactor, getWidth() / 2, getHeight() / 2);

        if (!points.isEmpty()) {
            for (int i = 0; i < points.size() - 1; i++) {
                float[] start = points.get(i);
                float[] end = points.get(i + 1);

                float startX = start[0] * 50 + getWidth() /  2;
                float startY = start[1] * 50 + getHeight() / 2;
                float endX = end[0] * 50 + getWidth() / 2;
                float endY = end[1] * 50 + getHeight() / 2;

                canvas.drawLine(startX, startY, endX, endY, paint);
            }

            // Draw the directional arrow only if drawing a new path
            if (canDrawPath) {
                float[] lastPoint = points.get(points.size() - 1);
                float arrowX = lastPoint[0] * 50 + getWidth() / 2;
                float arrowY = lastPoint[1] * 50 + getHeight() / 2;

                // Draw directional arrow based on the last segment
                float angle = (float) Math.atan2(lastPoint[1] - points.get(points.size() - 2)[1], lastPoint[0] - points.get(points.size() - 2)[0]);
                drawDirectionalArrow(canvas, arrowX, arrowY, angle);
            }

            // Draw the starting dot
            if (showStartDot) {
                canvas.drawCircle(startX * 50 + getWidth() / 2, startY * 50 + getHeight() / 2, 20, startDotPaint);
            }
        }

        // Draw the user dot
        if (showUserDot) {
            float userDotSize = 20;
            canvas.drawCircle(userX * 50 + getWidth() / 2, userY * 50 + getHeight() / 2, userDotSize, userDotPaint);
        }

        canvas.restore();
    }

    private void drawDirectionalArrow(Canvas canvas, float x, float y, float angle) {
        float arrowSize = 30;

        float x1 = (float) (x - arrowSize * Math.cos(angle + Math.PI / 6));
        float y1 = (float) (y - arrowSize * Math.sin(angle + Math.PI / 6));
        float x2 = (float) (x - arrowSize * Math.cos(angle - Math.PI / 6));
        float y2 = (float) (y - arrowSize * Math.sin(angle - Math.PI / 6));

        canvas.drawLine(x, y, x1, y1, arrowPaint);
        canvas.drawLine(x, y, x2, y2, arrowPaint);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f)); // Limit scale
            invalidate();
            return true;
        }
    }
}

class MapManager {
    private static final String FILENAME = "map.json";

    public static boolean saveMapToJson(Context context, ArrayList<float[]> pathCoordinates) {
        try (FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
            Gson gson = new Gson();
            String json = gson.toJson(pathCoordinates);
            writer.write(json);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ArrayList<float[]> loadMapFromJson(Context context) {
        try (FileInputStream fis = context.openFileInput(FILENAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<float[]>>() {}.getType();
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean clearMap(Context context) {
        return context.deleteFile(FILENAME);
    }
}

class MapDisplayActivity extends AppCompatActivity {

    private PathView pathView;
    private TextView mapNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapping_function_track);

        pathView = findViewById(R.id.pathView);
        mapNameTextView = findViewById(R.id.mapNameTextView);

        // Get path coordinates and map name from Intent
        Intent intent = getIntent();
        ArrayList<float[]> pathCoordinates = (ArrayList<float[]>) intent.getSerializableExtra("pathCoordinates");
        String mapName = intent.getStringExtra("mapName");

        if (pathCoordinates != null) {
            loadPathFromCoordinates(pathCoordinates);
        } else {
            Log.e("MapDisplayActivity", "No path coordinates received.");
        }

        if (mapName != null) {
            mapNameTextView.setText(mapName);
        } else {
            Log.e("MapDisplayActivity", "No map name received.");
        }
    }

    private void loadPathFromCoordinates(ArrayList<float[]> pathCoordinates) {
        for (float[] point : pathCoordinates) {
            pathView.updatePath(point[ 0], point[1]); // Update path on PathView
        }
    }
}

 */