package com.facebook.react.modules.deviceinfo;

import android.content.Context;
import com.facebook.fbreact.specs.NativeDeviceInfoSpec;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactNoCrashSoftException;
import com.facebook.react.bridge.ReactSoftException;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.DisplayMetricsHolder;
import java.util.HashMap;
import java.util.Map;

@ReactModule(name = DeviceInfoModule.NAME)
public class DeviceInfoModule extends NativeDeviceInfoSpec implements LifecycleEventListener {
    public static final String NAME = "DeviceInfo";
    private float mFontScale;
    private ReadableMap mPreviousDisplayMetrics;
    private ReactApplicationContext mReactApplicationContext;

    @Override // com.facebook.react.bridge.NativeModule
    public String getName() {
        return NAME;
    }

    @Override // com.facebook.react.bridge.BaseJavaModule, com.facebook.react.turbomodule.core.interfaces.TurboModule
    public void invalidate() {
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostDestroy() {
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostPause() {
    }

    public DeviceInfoModule(ReactApplicationContext reactApplicationContext) {
        super(reactApplicationContext);
        DisplayMetricsHolder.initDisplayMetricsIfNotInitialized(reactApplicationContext);
        this.mFontScale = reactApplicationContext.getResources().getConfiguration().fontScale;
        this.mReactApplicationContext = reactApplicationContext;
        reactApplicationContext.addLifecycleEventListener(this);
    }

    public DeviceInfoModule(Context context) {
        super(null);
        this.mReactApplicationContext = null;
        DisplayMetricsHolder.initDisplayMetricsIfNotInitialized(context);
        this.mFontScale = context.getResources().getConfiguration().fontScale;
    }

    @Override // com.facebook.fbreact.specs.NativeDeviceInfoSpec
    public Map<String, Object> getTypedExportedConstants() {
        HashMap hashMap = new HashMap();
        hashMap.put("Dimensions", DisplayMetricsHolder.getDisplayMetricsMap((double) this.mFontScale));
        return hashMap;
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostResume() {
        ReactApplicationContext reactApplicationContext = this.mReactApplicationContext;
        if (reactApplicationContext != null) {
            float f = reactApplicationContext.getResources().getConfiguration().fontScale;
            if (this.mFontScale != f) {
                this.mFontScale = f;
                emitUpdateDimensionsEvent();
            }
        }
    }

    public void emitUpdateDimensionsEvent() {
        ReactApplicationContext reactApplicationContext = this.mReactApplicationContext;
        if (reactApplicationContext != null) {
            if (reactApplicationContext.hasActiveCatalystInstance()) {
                WritableNativeMap displayMetricsNativeMap = DisplayMetricsHolder.getDisplayMetricsNativeMap((double) this.mFontScale);
                ReadableMap readableMap = this.mPreviousDisplayMetrics;
                if (readableMap == null) {
                    this.mPreviousDisplayMetrics = displayMetricsNativeMap.copy();
                } else if (!displayMetricsNativeMap.equals(readableMap)) {
                    this.mPreviousDisplayMetrics = displayMetricsNativeMap.copy();
                    ((DeviceEventManagerModule.RCTDeviceEventEmitter) this.mReactApplicationContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)).emit("didUpdateDimensions", displayMetricsNativeMap);
                }
            } else {
                ReactSoftException.logSoftException(NAME, new ReactNoCrashSoftException("No active CatalystInstance, cannot emitUpdateDimensionsEvent"));
            }
        }
    }
}
