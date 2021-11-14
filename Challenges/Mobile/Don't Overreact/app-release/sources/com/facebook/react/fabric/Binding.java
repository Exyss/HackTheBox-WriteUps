package com.facebook.react.fabric;

import com.facebook.jni.HybridData;
import com.facebook.react.bridge.NativeMap;
import com.facebook.react.bridge.RuntimeExecutor;
import com.facebook.react.bridge.queue.MessageQueueThread;
import com.facebook.react.fabric.events.EventBeatManager;
import com.facebook.react.uimanager.PixelUtil;

public class Binding {
    private final HybridData mHybridData = initHybrid();

    private static native HybridData initHybrid();

    private native void installFabricUIManager(RuntimeExecutor runtimeExecutor, Object obj, EventBeatManager eventBeatManager, MessageQueueThread messageQueueThread, ComponentFactory componentFactory, Object obj2);

    private native void uninstallFabricUIManager();

    public native void driveCxxAnimations();

    public native void renderTemplateToSurface(int i, String str);

    public native void setConstraints(int i, float f, float f2, float f3, float f4, float f5, float f6, boolean z, boolean z2);

    public native void setPixelDensity(float f);

    public native void startSurface(int i, String str, NativeMap nativeMap);

    public native void startSurfaceWithConstraints(int i, String str, NativeMap nativeMap, float f, float f2, float f3, float f4, float f5, float f6, boolean z, boolean z2);

    public native void stopSurface(int i);

    static {
        FabricSoLoader.staticInit();
    }

    public void register(RuntimeExecutor runtimeExecutor, FabricUIManager fabricUIManager, EventBeatManager eventBeatManager, MessageQueueThread messageQueueThread, ComponentFactory componentFactory, ReactNativeConfig reactNativeConfig) {
        fabricUIManager.setBinding(this);
        installFabricUIManager(runtimeExecutor, fabricUIManager, eventBeatManager, messageQueueThread, componentFactory, reactNativeConfig);
        setPixelDensity(PixelUtil.getDisplayMetricDensity());
    }

    public void unregister() {
        uninstallFabricUIManager();
    }
}
