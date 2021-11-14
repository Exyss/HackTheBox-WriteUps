package com.facebook.react.modules.core;

import com.facebook.common.logging.FLog;
import com.facebook.fbreact.specs.NativeHeadlessJsTaskSupportSpec;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.jstasks.HeadlessJsTaskContext;
import com.facebook.react.module.annotations.ReactModule;

@ReactModule(name = HeadlessJsTaskSupportModule.NAME)
public class HeadlessJsTaskSupportModule extends NativeHeadlessJsTaskSupportSpec {
    public static final String NAME = "HeadlessJsTaskSupport";

    @Override // com.facebook.react.bridge.NativeModule
    public String getName() {
        return NAME;
    }

    public HeadlessJsTaskSupportModule(ReactApplicationContext reactApplicationContext) {
        super(reactApplicationContext);
    }

    @Override // com.facebook.fbreact.specs.NativeHeadlessJsTaskSupportSpec
    public void notifyTaskRetry(double d, Promise promise) {
        int i = (int) d;
        HeadlessJsTaskContext instance = HeadlessJsTaskContext.getInstance(getReactApplicationContext());
        if (instance.isTaskRunning(i)) {
            promise.resolve(Boolean.valueOf(instance.retryTask(i)));
            return;
        }
        FLog.w(HeadlessJsTaskSupportModule.class, "Tried to retry non-active task with id %d. Did it time out?", Integer.valueOf(i));
        promise.resolve(false);
    }

    @Override // com.facebook.fbreact.specs.NativeHeadlessJsTaskSupportSpec
    public void notifyTaskFinished(double d) {
        int i = (int) d;
        HeadlessJsTaskContext instance = HeadlessJsTaskContext.getInstance(getReactApplicationContext());
        if (instance.isTaskRunning(i)) {
            instance.finishTask(i);
            return;
        }
        FLog.w(HeadlessJsTaskSupportModule.class, "Tried to finish non-active task with id %d. Did it time out?", Integer.valueOf(i));
    }
}
