package com.arashivision.sdk.demo.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.arashivision.insta360.basecamera.camera.CameraType;
import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.util.NetworkManager;
import com.arashivision.sdk.demo.util.PreviewParamsUtil;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.ILiveStatusListener;
import com.arashivision.sdkcamera.camera.callback.IPreviewStatusListener;
import com.arashivision.sdkcamera.camera.live.LiveParamsBuilder;
import com.arashivision.sdkcamera.camera.preview.PreviewParamsBuilder;
import com.arashivision.sdkcamera.camera.resolution.PreviewStreamResolution;
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder;
import com.arashivision.sdkmedia.player.capture.InstaCapturePlayerView;
import com.arashivision.sdkmedia.player.config.InstaStabType;
import com.arashivision.sdkmedia.player.listener.PlayerViewListener;

import java.util.List;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;

public class LiveActivity extends BaseObserveCameraActivity implements IPreviewStatusListener, ILiveStatusListener {

    private EditText mEtRtmp;
    private EditText mEtWidth;
    private EditText mEtHeight;
    private EditText mEtFps;
    private EditText mEtBitrate;
    private CheckBox mCbPanorama;
    private CheckBox mCbAudioEnabled;
    private ToggleButton mBtnLive;
    private ToggleButton mBtnBindNetwork;
    private TextView mTvLiveStatus;
    private Spinner mSpinnerResolution;
    private Spinner mSpinnerStabType;
    private InstaCapturePlayerView mCapturePlayerView;

    private PreviewStreamResolution mCurrentResolution;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
        setTitle(R.string.live_toolbar_title);
        bindViews();
        restoreLiveData();

        // Auto open preview after page gets focus
        List<PreviewStreamResolution> list = InstaCameraManager.getInstance().getSupportedPreviewStreamResolution(InstaCameraManager.PREVIEW_TYPE_LIVE);
        if (!list.isEmpty()) {
            mCurrentResolution = list.get(0);
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(this);
            // mSpinnerResolution的onItemSelected会自动触发，故此处注释掉
            // mSpinnerResolution -> onItemSelected() Will automatically trigger to open the preview, so comment it out here
//            restartPreview();
        }
    }

    private void bindViews() {
        mCapturePlayerView = findViewById(R.id.player_capture);
        mCapturePlayerView.setLifecycle(getLifecycle());

        mEtRtmp = findViewById(R.id.et_rtmp);
        mEtWidth = findViewById(R.id.et_width);
        mEtHeight = findViewById(R.id.et_height);
        mEtFps = findViewById(R.id.et_fps);
        mEtBitrate = findViewById(R.id.et_bitrate);
        mCbPanorama = findViewById(R.id.cb_panorama);
        mCbAudioEnabled = findViewById(R.id.cb_audio_enabled);
        mBtnLive = findViewById(R.id.btn_live);
        mBtnBindNetwork = findViewById(R.id.btn_bind_network);
        mTvLiveStatus = findViewById(R.id.tv_live_status);

        mCbAudioEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mCurrentResolution != null) {
                restartPreview();
            }
        });

        mBtnLive.setEnabled(false);
        mBtnLive.setOnClickListener(v -> {
            if (mBtnLive.isChecked()) {
                saveLiveData();
                mBtnLive.setChecked(checkToStartLive());
            } else {
                stopLive();
            }
        });
        mBtnLive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mSpinnerResolution.setEnabled(!isChecked);
            mCbPanorama.setEnabled(!isChecked);
            mCbAudioEnabled.setEnabled(!isChecked);
        });

        mBtnBindNetwork.setChecked(NetworkManager.getInstance().isBindingMobileNetwork());
        mBtnBindNetwork.setOnClickListener(v -> {
            if (mBtnBindNetwork.isChecked()) {
                NetworkManager.getInstance().exchangeNetToMobile();
            } else {
                NetworkManager.getInstance().clearBindProcess();
            }
        });

        mSpinnerResolution = findViewById(R.id.spinner_resolution);
        ArrayAdapter<PreviewStreamResolution> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(InstaCameraManager.getInstance().getSupportedPreviewStreamResolution(InstaCameraManager.PREVIEW_TYPE_LIVE));
        mSpinnerResolution.setAdapter(adapter);
        mSpinnerResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentResolution = adapter.getItem(position);
                restartPreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mSpinnerStabType = findViewById(R.id.spinner_stab_type);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        adapter2.add(getString(R.string.stab_type_auto));
        adapter2.add(getString(R.string.stab_type_panorama));
        adapter2.add(getString(R.string.stab_type_calibrate_horizon));
        adapter2.add(getString(R.string.stab_type_footage_motion_smooth));
        adapter2.add(getString(R.string.stab_type_off));
        mSpinnerStabType.setAdapter(adapter2);
        mSpinnerStabType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 4 && mCapturePlayerView.isStabEnabled()
                        || position != 4 && !mCapturePlayerView.isStabEnabled()) {
                    if (mCurrentResolution != null) {
                        restartPreview();
                    }
                } else {
                    mCapturePlayerView.setStabType(getStabType());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        boolean isNanoS = TextUtils.equals(InstaCameraManager.getInstance().getCameraType(), CameraType.NANOS.type);
        mSpinnerStabType.setVisibility(isNanoS ? View.GONE : View.VISIBLE);
    }

    private int getStabType() {
        switch (mSpinnerStabType.getSelectedItemPosition()) {
            case 0:
            default:
                return InstaStabType.STAB_TYPE_AUTO;
            case 1:
                return InstaStabType.STAB_TYPE_PANORAMA;
            case 2:
                return InstaStabType.STAB_TYPE_CALIBRATE_HORIZON;
            case 3:
                return InstaStabType.STAB_TYPE_FOOTAGE_MOTION_SMOOTH;
        }
    }

    private void restartPreview() {
        PreviewParamsBuilder builder = new PreviewParamsBuilder()
                .setStreamResolution(mCurrentResolution)
                .setPreviewType(InstaCameraManager.PREVIEW_TYPE_LIVE)
                .setAudioEnabled(mCbAudioEnabled.isChecked());
        InstaCameraManager.getInstance().closePreviewStream();
        InstaCameraManager.getInstance().startPreviewStream(builder);
    }

    private boolean checkToStartLive() {
        String rtmp = mEtRtmp.getText().toString();
        String width = mEtWidth.getText().toString();
        String height = mEtHeight.getText().toString();
        String fps = mEtFps.getText().toString();
        String bitrate = mEtBitrate.getText().toString();
        if (TextUtils.isEmpty(rtmp) || TextUtils.isEmpty(width) || TextUtils.isEmpty(height)
                || TextUtils.isEmpty(fps) || TextUtils.isEmpty(bitrate)) {
            Toast.makeText(this, R.string.live_toast_input_parameters, Toast.LENGTH_SHORT).show();
        } else if (!Pattern.matches("(rtmp|rtmps)://([\\w.]+/?)\\S*", rtmp)) {
            Toast.makeText(this, R.string.live_toast_invalid_rtmp, Toast.LENGTH_SHORT).show();
        } else {
            mCapturePlayerView.setLiveType(mCbPanorama.isChecked() ? InstaCapturePlayerView.LIVE_TYPE_PANORAMA : InstaCapturePlayerView.LIVE_TYPE_RECORDING);
            LiveParamsBuilder builder = new LiveParamsBuilder()
                    .setRtmp(rtmp)
                    .setWidth(Integer.parseInt(width))
                    .setHeight(Integer.parseInt(height))
                    .setFps(Integer.parseInt(fps))
                    .setBitrate(Integer.parseInt(bitrate) * 1024 * 1024)
                    .setPanorama(mCbPanorama.isChecked())
                    // 设置网络ID即可在使用WIFI连接相机时使用4G网络推流
                    // set NetId to use 4G to push live streaming when connecting camera by WIFI
                    .setNetId(NetworkManager.getInstance().getMobileNetId());
            InstaCameraManager.getInstance().startLive(builder, this);
            return true;
        }
        return false;
    }

    private void stopLive() {
        InstaCameraManager.getInstance().stopLive();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            // Auto close preview after page loses focus
            InstaCameraManager.getInstance().stopLive();
            InstaCameraManager.getInstance().closePreviewStream();
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(null);
            mCapturePlayerView.destroy();
            NetworkManager.getInstance().clearBindProcess();
            mBtnBindNetwork.setChecked(false);
        }
    }

    @Override
    public void onOpened() {
        // Preview stream is on and can be played
        InstaCameraManager.getInstance().setStreamEncode();
        mCapturePlayerView.setPlayerViewListener(new PlayerViewListener() {
            @Override
            public void onLoadingFinish() {
                mBtnLive.setEnabled(true);
                InstaCameraManager.getInstance().setPipeline(mCapturePlayerView.getPipeline());
            }

            @Override
            public void onReleaseCameraPipeline() {
                InstaCameraManager.getInstance().setPipeline(null);
            }
        });
        mCapturePlayerView.prepare(createParams());
        mCapturePlayerView.play();
        mCapturePlayerView.setKeepScreenOn(true);
    }

    private CaptureParamsBuilder createParams() {
        return PreviewParamsUtil.getCaptureParamsBuilder()
                .setStabType(getStabType())
                .setStabEnabled(mSpinnerStabType.getSelectedItemPosition() != 4)
                .setLive(true)
                .setResolutionParams(mCurrentResolution.width, mCurrentResolution.height, mCurrentResolution.fps);
    }

    @Override
    public void onIdle() {
        // Preview Stopped
        mBtnLive.setEnabled(false);
        mCapturePlayerView.destroy();
        mCapturePlayerView.setKeepScreenOn(false);
    }

    @Override
    public void onLivePushStarted() {
        mTvLiveStatus.setText(R.string.live_push_started);
    }

    @Override
    public void onLivePushFinished() {
        mBtnLive.setChecked(false);
        mTvLiveStatus.setText(R.string.live_push_finished);
    }

    @Override
    public void onLivePushError(int error, String desc) {
        mBtnLive.setChecked(false);
        mTvLiveStatus.setText(getString(R.string.live_push_error) + " (" + error + ")");
    }

    @Override
    public void onLiveFpsUpdate(int fps) {
        mTvLiveStatus.setText(getString(R.string.live_fps_update, fps));
    }

    @Override
    public void onCameraStatusChanged(boolean enabled) {
        super.onCameraStatusChanged(enabled);
        if (!enabled) {
            mBtnLive.setChecked(false);
            mBtnLive.setEnabled(false);
        }
    }

    private void saveLiveData() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putString("rtmp", mEtRtmp.getText().toString())
                .putString("width", mEtWidth.getText().toString())
                .putString("height", mEtHeight.getText().toString())
                .putString("fps", mEtFps.getText().toString())
                .putString("bitrate", mEtBitrate.getText().toString())
                .putBoolean("panorama", mCbPanorama.isChecked())
                .putBoolean("audio", mCbAudioEnabled.isChecked())
                .apply();
    }

    private void restoreLiveData() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mEtRtmp.setText(sp.getString("rtmp", ""));
        mEtWidth.setText(sp.getString("width", ""));
        mEtHeight.setText(sp.getString("height", ""));
        mEtFps.setText(sp.getString("fps", ""));
        mEtBitrate.setText(sp.getString("bitrate", ""));
        mCbPanorama.setChecked(sp.getBoolean("panorama", true));
        mCbAudioEnabled.setChecked(sp.getBoolean("audio", true));
    }

}
