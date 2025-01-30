package com.arashivision.sdk.demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.util.CameraBindNetworkManager;
import com.arashivision.sdk.demo.util.NetworkManager;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import android.util.Log;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
public class MainActivity extends BaseObserveCameraActivity {
    private static final String URL = "https://jsonplaceholder.typicode.com/posts/1";
    private static final String TAG = "MainActivity";
    private static final String FILE_NAME = "VID_20240623_115037_00_074.insv";
    private static final String DIRECTORY_NAME = "/storage/emulated/0/Pictures/GM360/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.main_toolbar_title);

        checkStoragePermission();

        File file = getFileFromInternalStorage(this, DIRECTORY_NAME, FILE_NAME);
        if (file != null && file.exists()) {
            Log.d(TAG, "File path: " + file.getAbsolutePath());
        } else {
            Log.e(TAG, "File not found!");
        }

        if (InstaCameraManager.getInstance().getCameraConnectedType() != InstaCameraManager.CONNECT_TYPE_NONE) {
            onCameraStatusChanged(true);
        }

        findViewById(R.id.show_Projects).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an intent to start AnotherActivity
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                // Start the activity
                startActivity(intent);
            }
        });


        findViewById(R.id.btn_full_demo).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FullDemoActivity.class));
        });

        findViewById(R.id.btn_connect_by_wifi).setOnClickListener(v -> {
            CameraBindNetworkManager.getInstance().bindNetwork(errorCode -> {
                InstaCameraManager.getInstance().openCamera(InstaCameraManager.CONNECT_TYPE_WIFI);
            });
        });

        findViewById(R.id.btn_connect_by_usb).setOnClickListener(v -> {
            InstaCameraManager.getInstance().openCamera(InstaCameraManager.CONNECT_TYPE_USB);
            makePostRequest();
        });

        findViewById(R.id.btn_connect_by_ble).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, BleActivity.class));
        });

        findViewById(R.id.btn_close_camera).setOnClickListener(v -> {
            CameraBindNetworkManager.getInstance().unbindNetwork();
            InstaCameraManager.getInstance().closeCamera();
        });

//        findViewById(R.id.btn_capture).setOnClickListener(v -> {
//            startActivity(new Intent(MainActivity.this, CaptureActivity.class));
//        });
//
//        findViewById(R.id.btn_preview).setOnClickListener(v -> {
//            startActivity(new Intent(MainActivity.this, PreviewActivity.class));
//        });
//
//        findViewById(R.id.btn_preview2).setOnClickListener(v -> {
//            startActivity(new Intent(MainActivity.this, Preview2Activity.class));
//        });
//
//        findViewById(R.id.btn_preview3).setOnClickListener(v -> {
//            startActivity(new Intent(MainActivity.this, Preview3Activity.class));
//        });
//
//        findViewById(R.id.btn_live).setOnClickListener(v -> {
//            startActivity(new Intent(MainActivity.this, LiveActivity.class));
//        });
//
//        findViewById(R.id.btn_osc).setOnClickListener(v -> {
//            startActivity(new Intent(MainActivity.this, OscActivity.class));
//        });
//
//        findViewById(R.id.btn_list_camera_file).setOnClickListener(v -> {
//            startActivity(new Intent(MainActivity.this, CameraFilesActivity.class));
//        });
//
//        findViewById(R.id.btn_settings).setOnClickListener(v -> {
//            startActivity(new Intent(MainActivity.this, MoreSettingActivity.class));
//        });
//
//        findViewById(R.id.btn_play).setOnClickListener(v -> {
//            // HDR
////            PlayAndExportActivity.launchActivity(this, StitchActivity.HDR_URLS);
//            // PureShot
//            PlayAndExportActivity.launchActivity(this, StitchActivity.PURE_SHOT_URLS);
//        });
//
//        findViewById(R.id.btn_stitch).setOnClickListener(v -> {
//            startActivity(new Intent(MainActivity.this, StitchActivity.class));
//        });
//
//        findViewById(R.id.btn_firmware_upgrade).setOnClickListener(v -> {
//            startActivity(new Intent(MainActivity.this, FwUpgradeActivity.class));
//        });
    }


    private File getFileFromInternalStorage(Context context, String directoryName, String fileName) {
        // Get the directory within the internal storage
        File directory = new File(context.getFilesDir(), directoryName);

        // Ensure the directory exists
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Return the file object
        return new File(directory, fileName);
    }

    private void makeGetRequest() {
        OkGo.<String>get(URL)
                .tag(this)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        Log.d("getrequest", "onSuccess: " + response.body());
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        Log.e("getrequest", "onError: " + response.getException().getMessage());
                    }
                });
    }

    private void makePostRequest() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user", "1");
            jsonObject.put("description", "this is the des");
            jsonObject.put("floor", "1");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        OkGo.<String>post("https://api.capture360.ai/building/api/video/upload/")
                .tag(this)
                .upJson(jsonObject.toString())
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
    }

    private void checkStoragePermission() {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.STORAGE, Permission.Group.LOCATION)
                .onDenied(permissions -> {
                })
                .start();
    }

    @Override
    public void onCameraStatusChanged(boolean enabled) {
        super.onCameraStatusChanged(enabled);
//        findViewById(R.id.btn_capture).setEnabled(enabled);
//        findViewById(R.id.btn_preview).setEnabled(enabled);
//        findViewById(R.id.btn_preview2).setEnabled(enabled);
//        findViewById(R.id.btn_preview3).setEnabled(enabled);
//        findViewById(R.id.btn_live).setEnabled(enabled);
//        findViewById(R.id.btn_osc).setEnabled(enabled);
//        findViewById(R.id.btn_list_camera_file).setEnabled(enabled);
//        findViewById(R.id.btn_settings).setEnabled(enabled);
//        findViewById(R.id.btn_firmware_upgrade).setEnabled(enabled);
        if (enabled) {
            Toast.makeText(this, R.string.main_toast_camera_connected, Toast.LENGTH_SHORT).show();
        } else {
            CameraBindNetworkManager.getInstance().unbindNetwork();
            NetworkManager.getInstance().clearBindProcess();
            Toast.makeText(this, R.string.main_toast_camera_disconnected, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCameraConnectError(int errorCode) {
        super.onCameraConnectError(errorCode);
        CameraBindNetworkManager.getInstance().unbindNetwork();
        Toast.makeText(this, getResources().getString(R.string.main_toast_camera_connect_error, errorCode), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraSDCardStateChanged(boolean enabled) {
        super.onCameraSDCardStateChanged(enabled);
        if (enabled) {
            Toast.makeText(this, R.string.main_toast_sd_enabled, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.main_toast_sd_disabled, Toast.LENGTH_SHORT).show();
        }
    }

}
