package com.arashivision.sdk.demo.util;

import com.arashivision.insta360.basemedia.asset.WindowCropInfo;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder;

public class PreviewParamsUtil {

    public static CaptureParamsBuilder getCaptureParamsBuilder() {

        return new CaptureParamsBuilder()
                .setCameraType(InstaCameraManager.getInstance().getCameraType())
                .setMediaOffset(InstaCameraManager.getInstance().getMediaOffset())
                .setMediaOffsetV2(InstaCameraManager.getInstance().getMediaOffsetV2())
                .setMediaOffsetV3(InstaCameraManager.getInstance().getMediaOffsetV3())
                .setCameraSelfie(InstaCameraManager.getInstance().isCameraSelfie())
                .setGyroTimeStamp(InstaCameraManager.getInstance().getGyroTimeStamp())
                .setBatteryType(InstaCameraManager.getInstance().getBatteryType())
                .setWindowCropInfo(windowCropInfoConversion(InstaCameraManager.getInstance().getWindowCropInfo()));
    }

    public static WindowCropInfo windowCropInfoConversion(com.arashivision.onecamera.camerarequest.WindowCropInfo cameraWindowCropInfo) {
        if (cameraWindowCropInfo == null) return null;
        WindowCropInfo windowCropInfo = new WindowCropInfo();
        windowCropInfo.setDesHeight(cameraWindowCropInfo.getDstHeight());
        windowCropInfo.setDesWidth(cameraWindowCropInfo.getDstWidth());
        windowCropInfo.setSrcHeight(cameraWindowCropInfo.getSrcHeight());
        windowCropInfo.setSrcWidth(cameraWindowCropInfo.getSrcWidth());
        windowCropInfo.setOffsetX(cameraWindowCropInfo.getOffsetX());
        windowCropInfo.setOffsetY(cameraWindowCropInfo.getOffsetY());
        return windowCropInfo;
    }
}
