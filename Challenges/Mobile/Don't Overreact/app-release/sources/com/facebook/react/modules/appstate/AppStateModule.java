package com.facebook.react.modules.appstate;

import com.facebook.fbreact.specs.NativeAppStateSpec;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WindowFocusChangeListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.LifecycleState;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

@ReactModule(name = AppStateModule.NAME)
public class AppStateModule extends NativeAppStateSpec implements LifecycleEventListener, WindowFocusChangeListener {
    public static final String APP_STATE_ACTIVE = "active";
    public static final String APP_STATE_BACKGROUND = "background";
    private static final String INITIAL_STATE = "initialAppState";
    public static final String NAME = "AppState";
    public static final String TAG = "AppStateModule";
    private String mAppState;

    @Override // com.facebook.fbreact.specs.NativeAppStateSpec
    public void addListener(String str) {
    }

    @Override // com.facebook.react.bridge.NativeModule
    public String getName() {
        return NAME;
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostDestroy() {
    }

    @Override // com.facebook.fbreact.specs.NativeAppStateSpec
    public void removeListeners(double d) {
    }

    public AppStateModule(ReactApplicationContext reactApplicationContext) {
        super(reactApplicationContext);
        reactApplicationContext.addLifecycleEventListener(this);
        reactApplicationContext.addWindowFocusChangeListener(this);
        this.mAppState = reactApplicationContext.getLifecycleState() == LifecycleState.RESUMED ? APP_STATE_ACTIVE : APP_STATE_BACKGROUND;
    }

    @Override // com.facebook.fbreact.specs.NativeAppStateSpec
    public Map<String, Object> getTypedExportedConstants() {
        HashMap hashMap = new HashMap();
        hashMap.put(INITIAL_STATE, this.mAppState);
        return hashMap;
    }

    @Override // com.facebook.fbreact.specs.NativeAppStateSpec
    public void getCurrentAppState(Callback callback, Callback callback2) {
        callback.invoke(createAppStateEventMap());
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostResume() {
        this.mAppState = APP_STATE_ACTIVE;
        sendAppStateChangeEvent();
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostPause() {
        this.mAppState = APP_STATE_BACKGROUND;
        sendAppStateChangeEvent();
    }

    @Override // com.facebook.react.bridge.WindowFocusChangeListener
    public void onWindowFocusChange(boolean z) {
        sendEvent("appStateFocusChange", Boolean.valueOf(z));
    }

    private WritableMap createAppStateEventMap() {
        WritableMap createMap = Arguments.createMap();
        createMap.putString("app_state", this.mAppState);
        return createMap;
    }

    private void sendEvent(String str, @Nullable Object obj) {
        ReactApplicationContext reactApplicationContext = getReactApplicationContext();
        if (reactApplicationContext != null && reactApplicationContext.hasActiveCatalystInstance()) {
            ((DeviceEventManagerModule.RCTDeviceEventEmitter) reactApplicationContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)).emit(str, obj);
        }
    }

    private void sendAppStateChangeEvent() {
        sendEvent("appStateDidChange", createAppStateEventMap());
    }
}
