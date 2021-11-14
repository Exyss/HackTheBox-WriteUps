package com.facebook.react.modules.core;

import com.facebook.fbreact.specs.NativeTimingSpec;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.devsupport.interfaces.DevSupportManager;
import com.facebook.react.jstasks.HeadlessJsTaskContext;
import com.facebook.react.jstasks.HeadlessJsTaskEventListener;
import com.facebook.react.module.annotations.ReactModule;

@ReactModule(name = TimingModule.NAME)
public final class TimingModule extends NativeTimingSpec implements LifecycleEventListener, HeadlessJsTaskEventListener {
    public static final String NAME = "Timing";
    private final JavaTimerManager mJavaTimerManager;

    @Override // com.facebook.react.bridge.NativeModule
    public String getName() {
        return NAME;
    }

    public class BridgeTimerManager implements JavaScriptTimerManager {
        public BridgeTimerManager() {
        }

        @Override // com.facebook.react.modules.core.JavaScriptTimerManager
        public void callTimers(WritableArray writableArray) {
            ReactApplicationContext reactApplicationContextIfActiveOrWarn = TimingModule.this.getReactApplicationContextIfActiveOrWarn();
            if (reactApplicationContextIfActiveOrWarn != null) {
                ((JSTimers) reactApplicationContextIfActiveOrWarn.getJSModule(JSTimers.class)).callTimers(writableArray);
            }
        }

        @Override // com.facebook.react.modules.core.JavaScriptTimerManager
        public void callIdleCallbacks(double d) {
            ReactApplicationContext reactApplicationContextIfActiveOrWarn = TimingModule.this.getReactApplicationContextIfActiveOrWarn();
            if (reactApplicationContextIfActiveOrWarn != null) {
                ((JSTimers) reactApplicationContextIfActiveOrWarn.getJSModule(JSTimers.class)).callIdleCallbacks(d);
            }
        }

        @Override // com.facebook.react.modules.core.JavaScriptTimerManager
        public void emitTimeDriftWarning(String str) {
            ReactApplicationContext reactApplicationContextIfActiveOrWarn = TimingModule.this.getReactApplicationContextIfActiveOrWarn();
            if (reactApplicationContextIfActiveOrWarn != null) {
                ((JSTimers) reactApplicationContextIfActiveOrWarn.getJSModule(JSTimers.class)).emitTimeDriftWarning(str);
            }
        }
    }

    public TimingModule(ReactApplicationContext reactApplicationContext, DevSupportManager devSupportManager) {
        super(reactApplicationContext);
        this.mJavaTimerManager = new JavaTimerManager(reactApplicationContext, new BridgeTimerManager(), ReactChoreographer.getInstance(), devSupportManager);
    }

    @Override // com.facebook.react.bridge.BaseJavaModule, com.facebook.react.bridge.NativeModule
    public void initialize() {
        getReactApplicationContext().addLifecycleEventListener(this);
        HeadlessJsTaskContext.getInstance(getReactApplicationContext()).addTaskEventListener(this);
    }

    @Override // com.facebook.fbreact.specs.NativeTimingSpec
    public void createTimer(double d, double d2, double d3, boolean z) {
        this.mJavaTimerManager.createAndMaybeCallTimer((int) d, (int) d2, d3, z);
    }

    @Override // com.facebook.fbreact.specs.NativeTimingSpec
    public void deleteTimer(double d) {
        this.mJavaTimerManager.deleteTimer((int) d);
    }

    @Override // com.facebook.fbreact.specs.NativeTimingSpec
    public void setSendIdleEvents(boolean z) {
        this.mJavaTimerManager.setSendIdleEvents(z);
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostResume() {
        this.mJavaTimerManager.onHostResume();
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostPause() {
        this.mJavaTimerManager.onHostPause();
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostDestroy() {
        this.mJavaTimerManager.onHostDestroy();
    }

    @Override // com.facebook.react.jstasks.HeadlessJsTaskEventListener
    public void onHeadlessJsTaskStart(int i) {
        this.mJavaTimerManager.onHeadlessJsTaskStart(i);
    }

    @Override // com.facebook.react.jstasks.HeadlessJsTaskEventListener
    public void onHeadlessJsTaskFinish(int i) {
        this.mJavaTimerManager.onHeadlessJsTaskFinish(i);
    }

    @Override // com.facebook.react.bridge.BaseJavaModule, com.facebook.react.bridge.NativeModule
    public void onCatalystInstanceDestroy() {
        HeadlessJsTaskContext.getInstance(getReactApplicationContext()).removeTaskEventListener(this);
        this.mJavaTimerManager.onInstanceDestroy();
    }

    public boolean hasActiveTimersInRange(long j) {
        return this.mJavaTimerManager.hasActiveTimersInRange(j);
    }
}
