package com.facebook.react.modules.core;

import com.facebook.common.logging.FLog;
import com.facebook.fbreact.specs.NativeExceptionsManagerSpec;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.JavascriptException;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.devsupport.interfaces.DevSupportManager;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.util.ExceptionDataHelper;
import com.facebook.react.util.JSStackTrace;

@ReactModule(name = ExceptionsManagerModule.NAME)
public class ExceptionsManagerModule extends NativeExceptionsManagerSpec {
    public static final String NAME = "ExceptionsManager";
    private final DevSupportManager mDevSupportManager;

    @Override // com.facebook.react.bridge.NativeModule
    public String getName() {
        return NAME;
    }

    public ExceptionsManagerModule(DevSupportManager devSupportManager) {
        super(null);
        this.mDevSupportManager = devSupportManager;
    }

    @Override // com.facebook.fbreact.specs.NativeExceptionsManagerSpec
    public void reportFatalException(String str, ReadableArray readableArray, double d) {
        JavaOnlyMap javaOnlyMap = new JavaOnlyMap();
        javaOnlyMap.putString("message", str);
        javaOnlyMap.putArray("stack", readableArray);
        javaOnlyMap.putInt("id", (int) d);
        javaOnlyMap.putBoolean("isFatal", true);
        reportException(javaOnlyMap);
    }

    @Override // com.facebook.fbreact.specs.NativeExceptionsManagerSpec
    public void reportSoftException(String str, ReadableArray readableArray, double d) {
        JavaOnlyMap javaOnlyMap = new JavaOnlyMap();
        javaOnlyMap.putString("message", str);
        javaOnlyMap.putArray("stack", readableArray);
        javaOnlyMap.putInt("id", (int) d);
        javaOnlyMap.putBoolean("isFatal", false);
        reportException(javaOnlyMap);
    }

    @Override // com.facebook.fbreact.specs.NativeExceptionsManagerSpec
    public void reportException(ReadableMap readableMap) {
        String string = readableMap.hasKey("message") ? readableMap.getString("message") : "";
        ReadableArray array = readableMap.hasKey("stack") ? readableMap.getArray("stack") : Arguments.createArray();
        int i = readableMap.hasKey("id") ? readableMap.getInt("id") : -1;
        boolean z = false;
        boolean z2 = readableMap.hasKey("isFatal") ? readableMap.getBoolean("isFatal") : false;
        if (this.mDevSupportManager.getDevSupportEnabled()) {
            if (readableMap.getMap("extraData") != null && readableMap.getMap("extraData").hasKey("suppressRedBox")) {
                z = readableMap.getMap("extraData").getBoolean("suppressRedBox");
            }
            if (!z) {
                this.mDevSupportManager.showNewJSError(string, array, i);
                return;
            }
            return;
        }
        String extraDataAsJson = ExceptionDataHelper.getExtraDataAsJson(readableMap);
        if (!z2) {
            FLog.e(ReactConstants.TAG, JSStackTrace.format(string, array));
            if (extraDataAsJson != null) {
                FLog.d(ReactConstants.TAG, "extraData: %s", extraDataAsJson);
                return;
            }
            return;
        }
        throw new JavascriptException(JSStackTrace.format(string, array)).setExtraDataAsJson(extraDataAsJson);
    }

    @Override // com.facebook.fbreact.specs.NativeExceptionsManagerSpec
    public void updateExceptionMessage(String str, ReadableArray readableArray, double d) {
        int i = (int) d;
        if (this.mDevSupportManager.getDevSupportEnabled()) {
            this.mDevSupportManager.updateJSError(str, readableArray, i);
        }
    }

    @Override // com.facebook.fbreact.specs.NativeExceptionsManagerSpec
    public void dismissRedbox() {
        if (this.mDevSupportManager.getDevSupportEnabled()) {
            this.mDevSupportManager.hideRedboxDialog();
        }
    }
}
