package com.arashivision.sdk.demo.activity;
import java.io.BufferedReader;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStreamReader;
import com.afollestad.materialdialogs.MaterialDialog;
import com.arashivision.insta360.basecamera.camera.CameraType;
import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.util.TimeFormat;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.ICameraOperateCallback;
import com.arashivision.sdkcamera.camera.callback.ICaptureStatusListener;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.callback.StringCallback;
import com.arashivision.sdk.demo.util.NetworkUtils;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.Nullable;

import android.os.AsyncTask;

import android.os.Handler;
import android.os.Looper;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import com.arashivision.sdk.demo.util.HttpUtils;

public class CaptureActivity extends BaseObserveCameraActivity implements ICaptureStatusListener {

    private final String TAG = "CaptureActivity";
    private final String API_ENDPOINT = "https://jsonplaceholder.typicode.com/posts/1";

    private TextView mTvCaptureStatus;
    private TextView mTvCaptureTime;
    private TextView mTvCaptureCount;
    private Button mBtnPlayCameraFile;
    private Button mBtnPlayLocalFile;
    private EditText etCaptureDelayTime;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

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
        setTitle(R.string.capture_toolbar_title);
        bindViews();

        if (InstaCameraManager.getInstance().getCameraConnectedType() == InstaCameraManager.CONNECT_TYPE_NONE) {
            finish();
            return;
        }

        SwitchSensorCallback switchSensorCallback = new SwitchSensorCallback(this);
        findViewById(R.id.layout_switch_sensor).setVisibility((isOneX2() || isOneX3()) ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_switch_dual_sensor).setOnClickListener(v -> {
            switchSensorCallback.onStart();
            InstaCameraManager.getInstance().switchCameraMode(InstaCameraManager.CAMERA_MODE_PANORAMA, InstaCameraManager.FOCUS_SENSOR_ALL, switchSensorCallback);
        });
        findViewById(R.id.btn_switch_front_sensor).setOnClickListener(v -> {
            switchSensorCallback.onStart();
            InstaCameraManager.getInstance().switchCameraMode(InstaCameraManager.CAMERA_MODE_SINGLE_FRONT, InstaCameraManager.FOCUS_SENSOR_FRONT, switchSensorCallback);
        });

        findViewById(R.id.btn_switch_rear_sensor).setOnClickListener(v -> {
            switchSensorCallback.onStart();
            InstaCameraManager.getInstance().switchCameraMode(InstaCameraManager.CAMERA_MODE_SINGLE_REAR, InstaCameraManager.FOCUS_SENSOR_REAR, switchSensorCallback);
        });

        findViewById(R.id.btn_normal_capture).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                int funcMode = InstaCameraManager.FUNCTION_MODE_CAPTURE_NORMAL;
                InstaCameraManager.getInstance().setRawToCamera(funcMode, false);
                InstaCameraManager.getInstance().startNormalCapture(false, getCaptureDelayTime());
            }
        });
        findViewById(R.id.btn_normal_capture_pure_shot).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                int funcMode = InstaCameraManager.FUNCTION_MODE_CAPTURE_NORMAL;
                InstaCameraManager.getInstance().setRawToCamera(funcMode, true);
                InstaCameraManager.getInstance().startNormalCapture(true, getCaptureDelayTime());
            }
        });

        findViewById(R.id.btn_normal_pano_capture).setVisibility(isOneX2() ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_normal_pano_capture).setEnabled(supportInstaPanoCapture());
        findViewById(R.id.btn_normal_pano_capture).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                InstaCameraManager.getInstance().startNormalPanoCapture(InstaCameraManager.FOCUS_SENSOR_REAR, false, getCaptureDelayTime());
            }
        });

        findViewById(R.id.btn_hdr_capture).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                if (!isNanoS()) {
                    int funcMode = InstaCameraManager.FUNCTION_MODE_HDR_CAPTURE;
                    InstaCameraManager.getInstance().setAEBCaptureNum(funcMode, 3);
                    InstaCameraManager.getInstance().setExposureEV(funcMode, 2f);
                }
                InstaCameraManager.getInstance().startHDRCapture(false, getCaptureDelayTime());
            }
        });

        findViewById(R.id.btn_hdr_pano_capture).setVisibility(isOneX2() ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_hdr_pano_capture).setEnabled(supportInstaPanoCapture());
        findViewById(R.id.btn_hdr_pano_capture).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                int funcMode = InstaCameraManager.FUNCTION_MODE_HDR_PANO_CAPTURE;
                InstaCameraManager.getInstance().setAEBCaptureNum(funcMode, 3);
                InstaCameraManager.getInstance().setExposureEV(funcMode, 2f);
                InstaCameraManager.getInstance().startHDRPanoCapture(InstaCameraManager.FOCUS_SENSOR_FRONT, false, getCaptureDelayTime());
            }
        });

        findViewById(R.id.btn_interval_shooting_start).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                InstaCameraManager.getInstance().setIntervalShootingTime(3000);
                InstaCameraManager.getInstance().startIntervalShooting();
            }
        });

        findViewById(R.id.btn_interval_shooting_stop).setOnClickListener(v -> {
            InstaCameraManager.getInstance().stopIntervalShooting();
        });

        findViewById(R.id.btn_normal_record_start).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                InstaCameraManager.getInstance().startNormalRecord();
            }
        });

        findViewById(R.id.btn_normal_record_stop).setOnClickListener(v -> {
            InstaCameraManager.getInstance().stopNormalRecord();
        });

        findViewById(R.id.btn_hdr_record_start).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                InstaCameraManager.getInstance().startHDRRecord();
            }
        });

        findViewById(R.id.btn_hdr_record_stop).setOnClickListener(v -> {
            InstaCameraManager.getInstance().stopHDRRecord();
        });

        findViewById(R.id.btn_timelapse_start).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                InstaCameraManager.getInstance().setTimeLapseInterval(500);
                InstaCameraManager.getInstance().startTimeLapse();
            }
        });

        findViewById(R.id.btn_timelapse_stop).setOnClickListener(v -> {
            InstaCameraManager.getInstance().stopTimeLapse();
        });

        // Capture Status Callback
        InstaCameraManager.getInstance().setCaptureStatusListener(this);
    }
    private class PostRequestAsyncTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String payload = "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(API_ENDPOINT);
                urlConnection = (HttpURLConnection) url.openConnection();

                // Set connection properties
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setDoOutput(true);

                // Write data to the connection
                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(payload);
                wr.flush();
                wr.close();

                // Get the response
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    return response.toString();
                } else {
                    // Handle unsuccessful response
                    Log.e(TAG, "POST request failed with code: " + responseCode);
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    in.close();
                    Log.e(TAG, "Error response: " + errorResponse.toString());
                }
            } catch (IOException e) {
                Log.e(TAG, "POST request failed: " + e.getMessage(), e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                // Handle successful response here
                Log.d(TAG, "POST request successful: " + result);
            } else {
                // Handle failure, if needed
                Log.e(TAG, "POST request failed: result is null");
            }
        }
    }
    private void bindViews() {
        mTvCaptureStatus = findViewById(R.id.tv_capture_status);
        mTvCaptureTime = findViewById(R.id.tv_capture_time);
        mTvCaptureCount = findViewById(R.id.tv_capture_count);
        mBtnPlayCameraFile = findViewById(R.id.btn_play_camera_file);
        mBtnPlayLocalFile = findViewById(R.id.btn_play_local_file);
        etCaptureDelayTime = findViewById(R.id.et_capture_delay_timer);
    }

    private int getCaptureDelayTime() {
        String delayTime = etCaptureDelayTime.getText().toString();
        if (TextUtils.isEmpty(delayTime)) return 0;
        try {
            return Integer.parseInt(delayTime);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isNanoS() {
        String cameraType = InstaCameraManager.getInstance().getCameraType();
        return TextUtils.isEmpty(cameraType) || CameraType.getForType(cameraType) == CameraType.NANOS;
    }

    private boolean isOneX2() {
        return CameraType.getForType(InstaCameraManager.getInstance().getCameraType()) == CameraType.ONEX2;
    }

    private boolean isOneX3() {
        return CameraType.getForType(InstaCameraManager.getInstance().getCameraType()) == CameraType.X3;
    }

    private boolean supportInstaPanoCapture() {
        return isOneX2() && InstaCameraManager.getInstance().getCurrentCameraMode() == InstaCameraManager.CAMERA_MODE_PANORAMA;
    }

    private boolean checkSdCardEnabled() {
        if (!InstaCameraManager.getInstance().isSdCardEnabled()) {
            Toast.makeText(this, R.string.capture_toast_sd_card_error, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onCameraStatusChanged(boolean enabled) {
        super.onCameraStatusChanged(enabled);
        if (!enabled) {
            finish();
        }
    }

    @Override
    public void onCameraSensorModeChanged(int cameraSensorMode) {
        super.onCameraSensorModeChanged(cameraSensorMode);
        findViewById(R.id.btn_normal_pano_capture).setEnabled(supportInstaPanoCapture());
        findViewById(R.id.btn_hdr_pano_capture).setEnabled(supportInstaPanoCapture());
    }

    @Override
    public void onCaptureStarting() {
        mTvCaptureStatus.setText(R.string.capture_capture_starting);
        mBtnPlayCameraFile.setVisibility(View.GONE);
        mBtnPlayLocalFile.setVisibility(View.GONE);
    }

    @Override
    public void onCaptureWorking() {
        mTvCaptureStatus.setText(R.string.capture_capture_working);
    }

    @Override
    public void onCaptureStopping() {
        mTvCaptureStatus.setText(R.string.capture_capture_stopping);
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

    /*private class UploadVideoTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                Log.d(TAG, "Video upload started");
                String payload = params[0];
                URL url = new URL("https://80ad-59-97-51-97.ngrok-free.app/building/api/video/upload/");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                    outputStream.writeBytes(payload);
                    outputStream.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Handle successful upload
                    Log.d(TAG, "Video upload successful");
                } else {
                    // Handle error
                    Log.e(TAG, "Video upload failed with response code: " + responseCode);
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        Log.e(TAG, "Response Video upload: " + response.toString());
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Exception during Video upload" + e);
            }
            return null;
        }
    }*/
    @Override
    public void onCaptureTimeChanged(long captureTime) {
        mTvCaptureTime.setVisibility(View.VISIBLE);
        mTvCaptureTime.setText(getString(R.string.capture_capture_time, TimeFormat.durationFormat(captureTime)));
    }

    @Override
    public void onCaptureCountChanged(int captureCount) {
        mTvCaptureCount.setVisibility(View.VISIBLE);
        mTvCaptureCount.setText(getString(R.string.capture_capture_count, captureCount));
    }

    private void downloadFilesAndPlay(String[] urls) {
        Log.d("video download","video download initiated: at main function");

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
            PlayAndExportActivity.launchActivity(CaptureActivity.this, localPaths);
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
                                PlayAndExportActivity.launchActivity(CaptureActivity.this, localPaths);
                                dialog.dismiss();
                            }
                        }
                    });
        }
    }
    private static class SwitchSensorCallback implements ICameraOperateCallback {

        private final Context context;
        private MaterialDialog dialog;

        private SwitchSensorCallback(Context context) {
            this.context = context;
        }

        public void onStart() {
            dialog = new MaterialDialog.Builder(context)
                    .content(R.string.capture_switch_sensor_ing)
                    .progress(true, 100)
                    .cancelable(false)
                    .canceledOnTouchOutside(false)
                    .show();
        }

        @Override
        public void onSuccessful() {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            Toast.makeText(context, R.string.capture_switch_sensor_success, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            Toast.makeText(context, R.string.capture_switch_sensor_failed, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCameraConnectError() {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            Toast.makeText(context, R.string.capture_switch_sensor_camera_connect_error, Toast.LENGTH_SHORT).show();
        }
    }

    ;
}
