package com.arashivision.sdk.demo;

import android.app.Application;

import com.arashivision.sdk.demo.activity.StitchActivity;
import com.arashivision.sdk.demo.util.AssetsUtil;
import com.arashivision.sdkcamera.InstaCameraSDK;
import com.arashivision.sdkmedia.InstaMediaSDK;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import java.util.logging.Level;

import java.io.File;

public class MyApp extends Application {

    private static MyApp sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        // Init SDK
        InstaCameraSDK.init(this);
        InstaMediaSDK.init(this);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
        loggingInterceptor.setColorLevel(Level.INFO);
        builder.addInterceptor(loggingInterceptor);

        builder.readTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.writeTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.connectTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);

        OkGo.getInstance().init(this)
                .setOkHttpClient(builder.build())
                .setRetryCount(3);

        // Copy sample pictures from assets to local
        copyHdrSourceFromAssets();
    }


    private void copyHdrSourceFromAssets() {
        File dirHdr = new File(StitchActivity.HDR_COPY_DIR);
        if (!dirHdr.exists()) {
            AssetsUtil.copyFilesFromAssets(this, "hdr_source", dirHdr.getAbsolutePath());
        }

        File dirPureShot = new File(StitchActivity.PURE_SHOT_COPY_DIR);
        if (!dirPureShot.exists()) {
            AssetsUtil.copyFilesFromAssets(this, "pure_shot_source", dirPureShot.getAbsolutePath());
        }
    }

    public static MyApp getInstance() {
        return sInstance;
    }

}
