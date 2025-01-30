package com.arashivision.sdk.demo.activity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.os.Handler;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class PathView extends View {

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
    private float translateX = 0;
    private float translateY = 0;
    private float lastTouchX;
    private float lastTouchY;


    public PathView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    // Initialize paints and setup scale gesture detector
    private void init(Context context) {
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
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());

        moveUserRunnable = new Runnable() {
            @Override
            public void run() {
                moveUserAlongPath(5); // Adjust step size as needed
                handler.postDelayed(this, 100);
            }
        };
    }

    // Update the path with new coordinates
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

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);

        switch (event.getAction()) {
            case android.view.MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                break;

            case android.view.MotionEvent.ACTION_MOVE:
                // Calculate the distance dragged
                float dx = event.getX() - lastTouchX;
                float dy = event.getY() - lastTouchY;

                // Update the translation values
                translateX += dx;
                translateY += dy;

                // Update last touch position for the next move
                lastTouchX = event.getX();
                lastTouchY = event.getY();

                // Redraw the view with updated translation
                invalidate();
                break;

            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
                break;
        }
        return true; // Return true to consume the touch event
    }


    // Update the user position on the map
    public void updateUserPosition(float x, float y) {
        userX = x;
        userY = y;
        setShowUserDot(true);
        invalidate();
    }

    // Move the user along the path, adjusting for each segment
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

    // Show or hide the user dot
    public void setShowUserDot(boolean show) {
        showUserDot = show;
        invalidate();
    }

    // Clear the path and reset relevant variables
    public void clearPath() {
        points.clear();
        canDrawPath = true;
        showArrow = true; // Show arrow when clearing the path
        showStartDot = false;
        invalidate();
        currentSegmentIndex = 0;
    }

    // Undo the last point added to the path
    public void undoLastPath() {
        if (!points.isEmpty()) {
            points.remove(points.size() - 1);
            invalidate();
        }
    }

    // Convert path to a JSON string
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

    // Load path from a JSON string
    public void loadPathFromJson(String jsonData) {
        points.clear();
        isMapLoaded = true;
        canDrawPath = false; // Disable path drawing
        showArrow = false; // Hide arrow when loading a saved map
        showStartDot = true; // Show starting dot
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
        invalidate();
    }


    // Set the coordinates for the path
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
        invalidate();
    }

    // Start user movement along the path
    public void startMovement() {
        if (isMapLoaded) {
            startUserMovement();
        }
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));  // Ensure scaling is within a reasonable range
        invalidate();  // Redraw the view with the new scale factor
    }
    // Reset map loading state and allow new path drawing
    public void resetMapLoaded() {
        isMapLoaded = false;
        canDrawPath = true; // Allow drawing new path
        showArrow = true; // Show arrow when resetting the map
        showStartDot = false;
        currentSegmentIndex = 0;
    }

    // Start tracking the path drawing
    public void startTracking() {
        canDrawPath = true;
        isMapLoaded = false; // Map is not loaded
        showArrow = true; // Show arrow during tracking
        invalidate();
    }


    // Start user movement along the path
    public void startUserMovement() {
        handler.post(moveUserRunnable);
    }

    // Stop user movement along the path
    public void stopUserMovement() {
        handler.removeCallbacks(moveUserRunnable);
    }

    // Get the list of path points
    public List<float[]> getPoints() {
        return points;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();

        // Apply the translation to move the content inside the PathView
        canvas.translate(translateX, translateY);

        // Apply the scale factor as before
        canvas.scale(scaleFactor, scaleFactor, getWidth() / 2, getHeight() / 2);

        // Draw path
        if (!points.isEmpty()) {
            for (int i = 0; i < points.size() - 1; i++) {
                float[] start = points.get(i);
                float[] end = points.get(i + 1);

                float startX = start[0] * 50 + getWidth() / 2;
                float startY = start[1] * 50 + getHeight() / 2;
                float endX = end[0] * 50 + getWidth() / 2;
                float endY = end[1] * 50 + getHeight() / 2;

                canvas.drawLine(startX, startY, endX, endY, paint);
            }

            // Draw the directional arrow
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

    // Draw the directional arrow
    private void drawDirectionalArrow(Canvas canvas, float x, float y, float angle) {
        float arrowSize = 30;

        float x1 = (float) (x - arrowSize * Math.cos(angle + Math.PI / 6));
        float y1 = (float) (y - arrowSize * Math.sin(angle + Math.PI / 6));
        float x2 = (float) (x - arrowSize * Math.cos(angle - Math.PI / 6));
        float y2 = (float) (y - arrowSize * Math.sin(angle - Math.PI / 6));

        canvas.drawLine(x, y, x1, y1, arrowPaint);
        canvas.drawLine(x, y, x2, y2, arrowPaint);
    }

    // Listener for zoom gestures
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f)); // Limit scale factor
            invalidate(); // Ensure the view is redrawn after scaling
            return true;
        }


        // Zoom in method
        public void zoomIn() {
            scaleFactor *= 1.1f;  // Zoom in by a factor of 1.1
            scaleFactor = Math.min(scaleFactor, 5.0f);  // Limit maximum zoom factor
            invalidate();  // Ensure the view is redrawn after zooming
        }

        // Zoom out method
        public void zoomOut() {
            scaleFactor *= 0.9f;  // Zoom out by a factor of 0.9
            scaleFactor = Math.max(scaleFactor, 0.1f);  // Limit minimum zoom factor
            invalidate();  // Ensure the view is redrawn after zooming
        }
    }
}