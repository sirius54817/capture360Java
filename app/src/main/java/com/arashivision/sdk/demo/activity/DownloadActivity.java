package com.arashivision.sdk.demo.activity;

public interface DownloadActivity {
    void onCaptureFinish(String[] filePaths);

    void onCaptureTimeChanged(long captureTime);

    void onCaptureCountChanged(int captureCount);
}
