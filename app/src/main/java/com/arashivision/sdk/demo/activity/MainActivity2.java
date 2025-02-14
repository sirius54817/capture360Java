package com.arashivision.sdk.demo.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.widget.ProgressBar;
import android.app.ProgressDialog;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.model.CaptureExposureData;
import com.arashivision.sdk.demo.osc.OscManager;
import com.arashivision.sdk.demo.osc.callback.IOscCallback;
import com.arashivision.sdk.demo.osc.delegate.OscRequestDelegate;
import com.arashivision.sdk.demo.util.CameraBindNetworkManager;
import com.arashivision.sdk.demo.util.NetworkUtils;
import com.arashivision.sdk.demo.util.TimeFormat;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdk.demo.activity.PathView;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.arashivision.sdk.demo.fragment.loadMapFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity2 extends AppCompatActivity implements DownloadActivity, IOscCallback {

    private Button mBtnPlayLocalFile;
    private Button mBtnPlayCameraFile;
    private TextView mTvCaptureCount;
    private TextView mTvCaptureStatus;
    private static final String TAG = "VideoUpload";
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int REQUEST_PERMISSION_CODE = 1001;
    private static final String MEDIA_PATH = "/Movies/";
    private static final String MEDIA_FOLDER_NAME = "Capture360";
    private final String API_ENDPOINT = "https://jsonplaceholder.typicode.com/posts/1";
    private AtomicBoolean mBtnCaptureFrontSensorClicked = new AtomicBoolean(false);
    private AtomicBoolean mBtnCaptureRearSensorClicked = new AtomicBoolean(false);
    private String mFrontSensorCapturePath = null;
    private String mRearSensorCapturePath = null;




    private EditText editTextDescription;
    private TextView tvProjectId;
    private TextView tvProject;
    private TextView tvPlanName;
    private TextView tvPlanId;
    private TextView tvfloorId;
    private TextView textResponse;
    private TextView tvStatus;
    private int projectId;
    private int project;
    private int buildingId;
    private int floorId;
    private String planName;
    private Integer planId; // Use Integer to allow null values
    private TextView mTvCaptureTime;
    private ProgressBar progressBar;


    private CaptureExposureData mCaptureExposureData = null;


    private boolean isCameraConnected() {
        return InstaCameraManager.getInstance().getCameraConnectedType() != InstaCameraManager.CONNECT_TYPE_NONE;
    }

    private void promptToConnectCamera() {
        Toast.makeText(this, R.string.osc_toast_connect_camera, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

        // Setting up a network request proxy
        OscManager.getInstance().setOscRequestDelegate(new OscRequestDelegate());
        Log.d(TAG, "Starting MainActivity2 with projectId: " + projectId);


        if (NetworkUtils.isNetworkConnected(this)) {
            Log.d(TAG, "onCreate: Internet connected");
            OkGo.<String>get(API_ENDPOINT)
                    .tag(this)
                    .execute(new StringCallback() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            Log.d(TAG, "onSuccess: " + response.body());
                        }

                        @Override
                        public void onError(Response<String> response) {
                            super.onError(response);
                            Log.e(TAG, "onError: " + response.getException().getMessage());
                        }
                    });
            /*new PostRequestAsyncTask().execute();*/
        } else {
            Log.d(TAG, "onCreate: Internet connected");
        }


        mTvCaptureTime = findViewById(R.id.tv_capture_time);
        tvProjectId = findViewById(R.id.tv_project_id);
        tvProject = findViewById(R.id.tv_project);
        tvPlanName = findViewById(R.id.tv_plan_name);
        tvPlanId = findViewById(R.id.tv_plan_id);
        tvfloorId = findViewById(R.id.tv_floor_id);
        editTextDescription = findViewById(R.id.et_description);
//        Button buttonCreateFolder = findViewById(R.id.btn_create_folder);
        Button buttonUpload = findViewById(R.id.btn_select_video);
        textResponse = findViewById(R.id.tv_selected_video);
        tvStatus = findViewById(R.id.tvStatus);

        progressBar = findViewById(R.id.progressBar);

        Intent intent = getIntent();
        if (intent != null) {
            projectId = intent.getIntExtra("PROJECT_ID", -1); // Use getIntExtra for int
            project = intent.getIntExtra("project", -1);
            planName = intent.getStringExtra("PLAN_NAME");
            planId = (Integer) intent.getSerializableExtra("MAP_ID");


            buildingId = intent.getIntExtra("buildingId", -1);
            floorId = intent.getIntExtra("FLOOR_ID", -1);

            Log.d(TAG, "Received Project ID: " + projectId);
            Log.d(TAG, "onCreate: Received projectId = " + projectId + " in MainActivity2");
            Log.d(TAG, "Received Building ID: " + buildingId);
            Log.d(TAG, "Received Floor ID: " + floorId);

            Log.d(TAG, "Received Plan Name: " + planName);
            Log.d(TAG, "Received Plan ID: " + planId);

            // Display the current project ID, plan name, and plan ID
            tvProjectId.setText("Project : " + (project != -1 ? String.valueOf(project) : "Not Available"));
            tvProject.setText("Building : " + (buildingId != -1 ? String.valueOf(buildingId) : "Not Available"));
            tvfloorId.setText("Floor : " + (floorId != -1 ? String.valueOf(floorId) : "Not Available"));
            tvPlanId.setText("Map : " + (planId != null ? planId.toString() : "Not Available"));
            tvPlanName.setText("Map Name : " + (planName != null ? planName : "Not Available"));
        } else {
            Log.e(TAG, "Intent is null");
        }



        findViewById(R.id.btn_connect_by_wifi).setOnClickListener(v -> {
            // Log and Toast when Wi-Fi button is clicked
            Log.d("WIFI-Button", "WIFI button clicked");

            // Show Toast message immediately
            Toast.makeText(MainActivity2.this, "Connecting to Wi-Fi...", Toast.LENGTH_SHORT).show();

            // Proceed with network binding and camera opening
            CameraBindNetworkManager.getInstance().bindNetwork(errorCode -> {
                // Log inside the callback to check if it gets triggered
                Log.d("WIFI-Button", "Network binding complete, opening camera");

                // Open camera
                InstaCameraManager.getInstance().openCamera(InstaCameraManager.CONNECT_TYPE_WIFI);
            });
        });

        findViewById(R.id.btn_connect_by_usb).setOnClickListener(v -> {
            // Log to check if USB button is clicked
            Log.d("USBButton", "USB button clicked");

            // Show Toast message immediately
            Toast.makeText(MainActivity2.this, "Connecting to USB...", Toast.LENGTH_SHORT).show();

            // Add USB connection functionality if needed
        });

        findViewById(R.id.btn_normal_record_start).setOnClickListener(v -> {
            Log.d(TAG, "Record Start");
            if (checkSdCardEnabled()) {
                InstaCameraManager.getInstance().startNormalRecord();
            }
        });

        findViewById(R.id.btn_normal_record_stop).setOnClickListener(v -> {
            Log.d(TAG, "Record stop");
            InstaCameraManager.getInstance().stopNormalRecord();
        });

        // 开始录像
        // Start Record
        findViewById(R.id.btn_start_record).setOnClickListener(v -> {
            if (isCameraConnected()) {
                String options = "\"captureMode\":\"video\"";
                OscManager.getInstance().startRecord(options, this);
            } else {
                promptToConnectCamera();
            }
        });

        // 停止录像
        // Stop Record
        findViewById(R.id.btn_stop_record).setOnClickListener(v -> {
            Log.d(TAG, "Stop Video Recording");
            if (isCameraConnected()) {
                OscManager.getInstance().stopRecord(this);
            } else {
                promptToConnectCamera();
            }
        });

        buttonUpload.setOnClickListener(v -> {
            // Check storage permission
            if (ContextCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(MainActivity2.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                // Permission is granted, proceed to upload videos
                uploadVideosForPresentDate();
            }
        });

/*
        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check storage permission
                if (ContextCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted, request it
                    ActivityCompat.requestPermissions(MainActivity2.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_STORAGE_PERMISSION);
                } else {
                    // Permission is granted, proceed to upload videos
                    uploadVideosForPresentDate();
                }
            }
        });

        Button buttonGoToMapping = findViewById(R.id.buttonLoadMap);
        buttonGoToMapping.setClickable(true);

        buttonGoToMapping.setOnClickListener(v -> {
            Log.d("MainActivity2", "Go to Map button clicked");

            // Log the values of projectId, planName, and planId
            Log.d("MainActivity2", "projectId: " + projectId);
            Log.d("MainActivity2", "planName: " + planName);
            Log.d("MainActivity2", "planId: " + planId);

            // Check if planName and planId are not null
            if (planName != null && planId != null) {
                // Create an Intent to start LoadTrackMapActivity
                Intent mappingIntent = new Intent(MainActivity2.this, LoadTrackMapActivity.class);

                // Pass the planName and planId as int
                mappingIntent.putExtra("PLAN_NAME", planName);
                mappingIntent.putExtra("MAP_ID", planId); // Pass as Integer (not String)

                Log.d("MainActivity2", "Starting LoadTrackMapActivity activity");
                startActivity(mappingIntent);  // Start the new activity
            } else {
                // Handle the case where planName or planId is null
                Log.e("MainActivity2", "One or more extras are null!");
                Toast.makeText(MainActivity2.this, "Invalid data, cannot proceed.", Toast.LENGTH_SHORT).show();
            }
        });
    }
*/
        if (savedInstanceState == null) {
            loadMapFragment fragment = new loadMapFragment();
            Bundle args = new Bundle();
            args.putInt("MAP_ID", planId != null ? planId : -1); // Pass the planId or -1 if null
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment) // Ensure you have a FrameLayout or similar in your activity_main3.xml
                    .commit();
        }
    }
    public void onSuccessful(Object object) {
//        mDialog.setTitle(R.string.osc_dialog_title_success);
        Log.d(TAG, "onSuccessful: " + object);
        if (object == null) {
            // setOptions()、startRecord() return null
//            mDialog.setContent(R.string.osc_dialog_msg_no_data);
            Log.d(TAG, "onSuccessful: null");
            mBtnCaptureFrontSensorClicked.set(false);
            mBtnCaptureRearSensorClicked.set(false);
        } else if (object instanceof CaptureExposureData) {
            Log.d(TAG, "onSuccessful: CaptureExposureData");
            mCaptureExposureData = (CaptureExposureData) object;
            mCaptureExposureData.exposureProgram = 1;
//            mDialog.setContent(object.toString());
//            updateUI();
        } else {
            try {
                // customRequest() will callback the original content returned by OSC
                JSONObject jsonObject = new JSONObject((String) object);
//                mDialog.setContent(jsonObject.toString(2));
                mBtnCaptureFrontSensorClicked.set(false);
                mBtnCaptureRearSensorClicked.set(false);
            } catch (Exception e) {
                // takePicture()、stopRecord() will return file address (String[] urls), could be downloaded to local
                Log.d(TAG, "onSuccessful: " + object);
                StringBuilder message = new StringBuilder();
                if (object.getClass().isArray()) {
                    for (Object obj : (Object[]) object) {
                        message.append(obj).append("\n");
                    }
                    downloadFiles((String[]) object, planId);
                } else {
                    message.append(object.toString());
                    mBtnCaptureFrontSensorClicked.set(false);
                    mBtnCaptureRearSensorClicked.set(false);
                }
//                mDialog.setContent(message.toString());
                Log.d(TAG, "Message: " + message.toString());
            }
        }
//        mDialog.getActionButton(DialogAction.POSITIVE).setVisibility(View.VISIBLE);
    }
    private void downloadFiles(String[] urls, int planId) {
        Log.d(TAG, "downloadFiles: " + urls.length);
        if (urls == null || urls.length == 0) {
            return;
        }

        String localFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/Capture360";
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String currentDateStr = sdf.format(new Date());

        String fileName = Integer.toString(planId) + "-" + currentDateStr+".mp4";


        String[] fileNames = new String[urls.length];
        String[] localPaths = new String[urls.length];

        for (int i = 0; i < localPaths.length; i++) {
            fileNames[i] = urls[i].substring(urls[i].lastIndexOf("/") + 1);
            localPaths[i] = localFolder + "/" + fileName;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.osc_dialog_title_downloading)
                .content(getString(R.string.osc_dialog_msg_downloading, urls.length, 0, 0))
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .show();

        AtomicInteger successfulCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        for (int i = 0; i < localPaths.length; i++) {
            String url = urls[i];
            OkGo.<File>get(url)
                    .execute(new FileCallback(localFolder, fileName) {

                        @Override

                        public void onError(Response<File> response) {
                            super.onError(response);
                            errorCount.incrementAndGet();
                            checkDownloadCount();
                        }

                        @Override
                        public void onSuccess(Response<File> response) {
                            successfulCount.incrementAndGet();
                            checkDownloadCount();
                        }

                        private void checkDownloadCount() {
                            dialog.setContent(getString(R.string.osc_dialog_msg_downloading, urls.length, successfulCount.intValue(), errorCount.intValue()));
                            if (successfulCount.intValue() + errorCount.intValue() >= urls.length) {
                                // Demo直接将filePaths传参打开播放页
                                // This demo directly transfers the file paths to the play page for playback
                                if (mBtnCaptureFrontSensorClicked.get()) {
                                    mFrontSensorCapturePath = localPaths[0];
                                } else if (mBtnCaptureRearSensorClicked.get()) {
                                    mRearSensorCapturePath = localPaths[0];
                                } else {
                                    PlayAndExportActivity.launchActivity(MainActivity2.this, localPaths);
                                }
                                dialog.dismiss();
                                mBtnCaptureFrontSensorClicked.set(false);
                                mBtnCaptureRearSensorClicked.set(false);
                            }
                        }
                    });
        }
    }

    @Override
    public void onCaptureFinish(String[] filePaths) {
        Log.i(TAG, "onCaptureFinish, filePaths = " + ((filePaths == null) ? "null" : Arrays.toString(filePaths)));
        mTvCaptureStatus.setText(R.string.capture_capture_finished);
        mTvCaptureTime.setVisibility(View.GONE);
        mTvCaptureCount.setVisibility(View.GONE);
        if (filePaths != null && filePaths.length > 0) {
            mBtnPlayCameraFile.setVisibility(View.VISIBLE);
            mBtnPlayCameraFile.setOnClickListener(v -> {
                PlayAndExportActivity.launchActivity(this, filePaths);
            });
            mBtnPlayLocalFile.setVisibility(View.VISIBLE);
            downloadFilesAndPlay(filePaths);
            mBtnPlayLocalFile.setOnClickListener(v -> {
                Log.d("video download","video download initiated: " + filePaths);
                downloadFilesAndPlay(filePaths);
            });

            String videoFilePath = filePaths[0];
            Log.d(TAG, "Video upload function called with videoFilePath: " + videoFilePath);

            String payload = "{\"user\":\"1\",\"description\":\"des\",\"file\":\"" + videoFilePath + "\",\"floor\":\"1\"}";
            Log.d(TAG, "Video upload function called with payload: " + payload);

            ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
            Call<User> call = apiInterface.getUserInformation("gokul", "FSD");
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, retrofit2.Response<User> response) {
                    Log.e(TAG, "onResponse: "+response.code() );
                    Log.e(TAG, "onResponse: name"+response.body().getName() );
                    Log.e(TAG, "onResponse: createdAt"+response.body().getCreatedAt() );
                    Log.e(TAG, "onResponse: job"+response.body().getJob() );
                    Log.e(TAG, "onResponse: id"+response.body().getId() );
                }

                @Override
                public void onFailure(Call<User> call, Throwable throwable) {
                    Log.e(TAG, "onFailure: "+ throwable.getMessage() );
                }
            });


            /*  new UploadVideoTask().execute(payload);*/

        } else {
            mBtnPlayCameraFile.setVisibility(View.GONE);
            mBtnPlayCameraFile.setOnClickListener(null);
            mBtnPlayLocalFile.setVisibility(View.GONE);
            mBtnPlayLocalFile.setOnClickListener(null);
        }
    }




    @Override
    public void onCaptureTimeChanged(long captureTime) {
        Log.i(TAG, "onCaptureTimeChanged, captureTime = " + captureTime);
        mTvCaptureTime.setVisibility(View.VISIBLE);
        mTvCaptureTime.setText(getString(R.string.capture_capture_time, TimeFormat.durationFormat(captureTime)));
    }

    @Override
    public void onCaptureCountChanged(int captureCount) {

    }


    private void downloadFilesAndPlay(String[] urls) {
        Log.d("video download", "Video download function called with urls: " + Arrays.toString(urls));
        if (urls == null || urls.length == 0) {
            return;
        }

        String localFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/GM360";
        String[] fileNames = new String[urls.length];
        String[] localPaths = new String[urls.length];
        boolean needDownload = false;
        for (int i = 0; i < localPaths.length; i++) {
            fileNames[i] = urls[i].substring(urls[i].lastIndexOf("/") + 1);
            localPaths[i] = localFolder + "/" + fileNames[i];
            Log.d("video download", "Video download function called with file: "+  localPaths[i]);
            if (!new File(localPaths[i]).exists()) {
                needDownload = true;
            }
        }

        if (!needDownload) {
            PlayAndExportActivity.launchActivity(MainActivity2.this, localPaths);
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.osc_dialog_title_downloading)
                .content(getString(R.string.osc_dialog_msg_downloading, urls.length, 0, 0))
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .show();

        AtomicInteger successfulCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        for (int i = 0; i < localPaths.length; i++) {
            String url = urls[i];
            OkGo.<File>get(url)
                    .execute(new FileCallback(localFolder, fileNames[i]) {

                        @Override
                        public void onError(Response<File> response) {
                            super.onError(response);
                            errorCount.incrementAndGet();
                            checkDownloadCount();
                        }

                        @Override
                        public void onSuccess(Response<File> response) {
                            successfulCount.incrementAndGet();
                            checkDownloadCount();
                        }

                        private void checkDownloadCount() {
                            dialog.setContent(getString(R.string.osc_dialog_msg_downloading, urls.length, successfulCount.intValue(), errorCount.intValue()));
                            if (successfulCount.intValue() + errorCount.intValue() >= urls.length) {
                                PlayAndExportActivity.launchActivity(MainActivity2.this, localPaths);
                                dialog.dismiss();
                            }
                        }
                    });
        }
    }


    private boolean checkSdCardEnabled() {
        if (!InstaCameraManager.getInstance().isSdCardEnabled()) {
            Toast.makeText(this, R.string.capture_toast_sd_card_error, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    public void onCreateFolderClick() {
        // Check for external storage write permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, request it
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION_CODE
                );
            } else {
                // Permission already granted, proceed to create or check folder
                createOrCheckMediaFolder();
            }
        } else {
            // Permissions granted automatically on versions lower than M
            createOrCheckMediaFolder();
        }
    }

    // Method to create or check a folder in Device storage/Android/media/Movies/
    private void createOrCheckMediaFolder() {
        // Check if external storage is available and writable
        if (isExternalStorageWritable()) {
            // Get the root directory of external storage
            File externalStorage = Environment.getExternalStorageDirectory();

            // Create a File object for the Movies directory
            File moviesDirectory = new File(externalStorage.getAbsolutePath() + MEDIA_PATH);

            // Create a File object for your folder
            File myFolder = new File(moviesDirectory, MEDIA_FOLDER_NAME);

            // Check if the folder exists, create it if it doesn't
            if (!myFolder.exists()) {
                boolean created = myFolder.mkdirs();
                if (created) {
                    updateStatus("Folder created successfully at: " + myFolder.getAbsolutePath());
                } else {
                    updateStatus("Failed to create folder");
                }
            } else {
                updateStatus("Folder already exists at: " + myFolder.getAbsolutePath());
            }
        } else {
            updateStatus("External storage not available or permission denied");
        }
    }

    private void updateStatus(String status) {
        String currentStatus = tvStatus.getText().toString();
        String newStatus = currentStatus + "\n" + status;
        tvStatus.setText(newStatus);
    }

    // Method to check if external storage is writable
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void uploadVideosForPresentDate() {
        // Directory path where videos are stored
        String mediaFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + MEDIA_PATH + MEDIA_FOLDER_NAME;

        // Check if the directory exists and list files
        File mediaFolder = new File(mediaFolderPath);
        if (mediaFolder.exists() && mediaFolder.isDirectory()) {
            File[] files = mediaFolder.listFiles();
            if (files != null && files.length > 0) {
                // Sort files by last modified timestamp to get the most recent one
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return Long.compare(f2.lastModified(), f1.lastModified());
                    }
                });

                // Get description and floor id from EditTexts
                String description = editTextDescription.getText().toString();
//                String floorId = editTextFloorId.getText().toString();

                // Flag to check if at least one file is uploaded
                boolean fileUploaded = false;

                // Iterate through files and upload those with the floor ID and present date in the filename
                for (File file : files) {
                    if (isFileForFloorAndPresentDate(file, Integer.toString(planId))) {
                        Uri videoUri = Uri.fromFile(file);
                        new UploadVideoTask(description,Integer.toString(planId) , videoUri).execute();
                        fileUploaded = true; // Set flag to true if at least one file is uploaded
                    }
                }

                // Display a message if no file was uploaded
                if (!fileUploaded) {
                    Toast.makeText(this, "No video files found matching floor ID " + planId + " and present date.", Toast.LENGTH_SHORT).show();
                    showDialog("No Videos Found", "No video files found in " + mediaFolderPath);
                }
            } else {
                Log.e(TAG, "No video files found in " + mediaFolderPath);
                Toast.makeText(this, "No video files found in " + mediaFolderPath, Toast.LENGTH_SHORT).show();
                showDialog("No Videos Found", "No video files found in " + mediaFolderPath);
            }
        } else {
            Log.e(TAG, "Media folder does not exist: " + mediaFolderPath);
            Toast.makeText(this, "Media folder does not exist: " + mediaFolderPath, Toast.LENGTH_SHORT).show();
            showDialog("No Videos Found", "Media Folder doesn't exist" + mediaFolderPath);
        }
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(MainActivity2.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private boolean isFileForFloorAndPresentDate(File file, String floorId) {
        // Get the file name
        String fileName = file.getName();

        // Get the current date in the format "dd-MM-yyyy"
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String currentDateStr = sdf.format(new Date());

        // Build the expected file name format

        String expectedFileName = floorId + "-" + currentDateStr + ".mp4";
        Log.d(TAG, "Video File Name:" + expectedFileName);

        // Check if the file name matches the expected format
        return fileName.equals(expectedFileName);
    }

    @Override
    public void onStartRequest() {
        IOscCallback.super.onStartRequest();
    }

    @Override
    public void onError(String message) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }


//    private class UploadVideoTask extends AsyncTask<Void, Void, String> {
//
//        private String description;
//        private String floorId;
//        private Uri videoUri;
//
//        public UploadVideoTask(String description, String floorId, Uri videoUri) {
//            this.description = description;
//            this.floorId = floorId;
//            this.videoUri = videoUri;
//        }
//
//        public UploadVideoTask() {
//
//        }
//
//        @Override
//        protected String doInBackground(Void... params) {
//            String response = uploadFile(description, floorId, videoUri);
//            return response;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            // Log the full response for debugging
//            Log.d(TAG, "Response from server after post: " + result);
//
//            // Display the result in textResponse TextView
//            textResponse.append("\nUpload Status:\n" + result);
//
//            // Check if the response starts with "HTTP error code:"
//            if (result.startsWith("HTTP error code:")) {
//                Log.e(TAG, "HTTP Error: " + result);
//                return;
//            }
//
//            // Handle the response as needed
//            try {
//                // Assume 'result' is the response string that includes both the response code and JSON
//                String jsonString = result.trim();
//
//                // Find the first occurrence of '{' to start extracting the JSON part
//                int jsonStartIndex = jsonString.indexOf("{");
//                if (jsonStartIndex != -1) {
//                    // Extract the JSON substring
//                    jsonString = jsonString.substring(jsonStartIndex);
//
//                    // Now parse the JSON
//                    JSONObject jsonObject = new JSONObject(jsonString);
//
//                    if (jsonObject.has("data")) {
//                        JSONObject dataObject = jsonObject.getJSONObject("data");
//
//                        if (dataObject.has("id")) {
//                            int videoId = dataObject.getInt("id");
//                            Log.d(TAG, "Video ID: " + videoId);
//                            showDialog("Upload Successful", "Video successfully uploaded!");
//                            startVideoProcessing(String.valueOf(videoId));
//                        } else {
//                            Log.e(TAG, "ID not found in the data object");
//                        }
//                    } else {
//                        Log.e(TAG, "Data object not found in the response");
//                    }
//                } else {
//                    Log.e(TAG, "JSON part not found in the response");
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//                Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
//            }
//
//
//            Log.d(TAG, "Upload successful: " + result);
//            showDialog("Upload Successful", "Video successfully uploaded!");
//        }
//
//        @Override
//        protected void onCancelled(String result) {
//            super.onCancelled(result);
//            // Handle cancellation if needed
//        }
//    }
private class UploadVideoTask extends AsyncTask<Void, Integer, String> {

    private String description;
    private String floorId;
    private Uri videoUri;
    private ProgressDialog progressDialog;

    public UploadVideoTask(String description, String floorId, Uri videoUri) {
        this.description = description;
        this.floorId = floorId;
        this.videoUri = videoUri;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // Initialize and show the progress dialog
        progressDialog = new ProgressDialog(MainActivity2.this);
        progressDialog.setMessage("Uploading video...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    protected String doInBackground(Void... params) {
        String response = uploadFile(description, floorId, videoUri);
        return response;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        // Update the progress dialog
        if (progressDialog != null) {
            progressDialog.setProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(String result) {
        // Dismiss the progress dialog
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        // Log the full response for debugging
        Log.d(TAG, "Response from server after post: " + result);

        // Display the result in textResponse TextView
        textResponse.append("\nUpload Status:\n" + result);

        // Check if the response starts with "HTTP error code:"
        if (result.startsWith("HTTP error code:")) {
            Log.e(TAG, "HTTP Error: " + result);
            return;
        }

        // Handle the response as needed
        try {
            // Assume 'result' is the response string that includes both the response code and JSON
            String jsonString = result.trim();

            // Find the first occurrence of '{' to start extracting the JSON part
            int jsonStartIndex = jsonString.indexOf("{");
            if (jsonStartIndex != -1) {
                // Extract the JSON substring
                jsonString = jsonString.substring(jsonStartIndex);

                // Now parse the JSON
                JSONObject jsonObject = new JSONObject(jsonString);

                if (jsonObject.has("data")) {
                    JSONObject dataObject = jsonObject.getJSONObject("data");

                    if (dataObject.has("id")) {
                        int videoId = dataObject.getInt("id");
                        Log.d(TAG, "Video ID: " + videoId);
                        showDialog("Upload Successful", "Video successfully uploaded!");
                        startVideoProcessing(String.valueOf(videoId));
                    } else {
                        Log.e(TAG, "ID not found in the data object");
                    }
                } else {
                    Log.e(TAG, "Data object not found in the response");
                }
            } else {
                Log.e(TAG, "JSON part not found in the response");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
        }

        Log.d(TAG, "Upload successful: " + result);
        showDialog("Upload Successful", "Video successfully uploaded!");
    }

    @Override
    protected void onCancelled(String result) {
        super.onCancelled(result);
        // Handle cancellation if needed
    }

    private String uploadFile(String description, String floorId, Uri videoUri) {
        Log.d("uploadFile:", "Function called line 888");
        String apiUrl = "https://fd84-59-97-51-97.ngrok-free.app/building/api/video/upload/";
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"; // Ensure this matches server expectations
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        try {
            File videoFile = new File(videoUri.getPath());
            if (!videoFile.exists()) {
                return "File does not exist: " + videoFile.getPath();
            }

            FileInputStream fileInputStream = new FileInputStream(videoFile);
            int totalBytes = (int) videoFile.length();
            int bytesRead = 0;

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"plan\"" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(planId + lineEnd);

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"json\"" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(floorId + lineEnd);

            // Add description parameter
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"description\"" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(description + lineEnd);

            // Add file
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + videoFile.getName() + "\"" + lineEnd);
            dos.writeBytes("Content-Type: video/mp4" + lineEnd); // Adjust MIME type if necessary
            dos.writeBytes(lineEnd);

            // Write file data
            byte[] buffer = new byte[1024];
            int read;
            while ((read = fileInputStream.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
                bytesRead += read;

                // Calculate progress percentage
                int progress = (int) ((bytesRead / (float) totalBytes) * 100);
                publishProgress(progress); // Update progress
            }
            fileInputStream.close();

            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            dos.flush();
            dos.close();

            int responseCode = conn.getResponseCode();
            InputStream inputStream = (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Return response message
            return "Response Code: " + responseCode + "\n" + response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception: " + e.getMessage();
        }
    }
}

    private void startVideoProcessing(String videoId) {
        // Replace apiUrl with your actual endpoint
        String apiUrl = "https://fd84-59-97-51-97.ngrok-free.app/building/video_processing/?video_id=" + videoId;
        Log.d("<<<<id>>>>",videoId + "-" + apiUrl);
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();
                        return response.toString();
                    } else {
                        return "HTTP error code: " + responseCode;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Exception: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                // Handle the result of the second API call as needed
                Log.d(TAG, "Second API call result: " + result);
                // Example: Displaying the result in textResponse TextView
                textResponse.append("\nSecond API call result:\n" + result);

                // Parse the result JSON and update UI or perform other actions
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    // Example: Update UI based on processed data
                    if (jsonObject.has("processed_data")) {
                        String processedData = jsonObject.getString("processed_data");
                        // Update UI or perform other actions based on processed data
                        // For example, display processed data in a TextView
//                        TextView textViewProcessedData = findViewById(R.id.textViewProcessedData);
//                        textViewProcessedData.setText(processedData);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                }
            }
        }.execute();
    }

    private String uploadFile(String description, String floorId, Uri videoUri) {
        Log.d("uploadFile:", "Function called line 888");
        Log.d("name",description + "-" + floorId + "-" + videoUri);
        String apiUrl = "https://fd84-59-97-51-97.ngrok-free.app/building/api/video/upload/";
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"; // Ensure this matches server expectations
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        try {
            File videoFile = new File(videoUri.getPath());
            if (!videoFile.exists()) {
                return "File does not exist: " + videoFile.getPath();
            }

            FileInputStream fileInputStream = new FileInputStream(videoFile);

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"plan\"" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(4 + lineEnd);

            // Add description parameter
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"description\"" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(description + lineEnd);

            // Add floor_id parameter
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"plan\"" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(floorId + lineEnd);

            // Add file
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + videoFile.getName() + "\"" + lineEnd);
            dos.writeBytes("Content-Type: video/mp4" + lineEnd); // Adjust MIME type if necessary
            dos.writeBytes(lineEnd);

            // Write file data
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            fileInputStream.close();

            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            dos.flush();
            dos.close();

            int responseCode = conn.getResponseCode();
            InputStream inputStream = (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Return response message
            return "Response Code: " + responseCode + "\n" + response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception: " + e.getMessage();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to upload videos
                uploadVideosForPresentDate();
            } else {
                // Permission denied, show a message or handle accordingly
                Toast.makeText(this, "Storage permission is required to upload videos.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}