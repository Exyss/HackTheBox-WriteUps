package com.facebook.react.modules.debug;

import com.facebook.fbreact.specs.NativeDevSettingsSpec;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.devsupport.interfaces.DevOptionHandler;
import com.facebook.react.devsupport.interfaces.DevSupportManager;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

@ReactModule(name = DevSettingsModule.NAME)
public class DevSettingsModule extends NativeDevSettingsSpec {
    public static final String NAME = "DevSettings";
    private final DevSupportManager mDevSupportManager;

    @Override // com.facebook.fbreact.specs.NativeDevSettingsSpec
    public void addListener(String str) {
    }

    @Override // com.facebook.react.bridge.NativeModule
    public String getName() {
        return NAME;
    }

    @Override // com.facebook.fbreact.specs.NativeDevSettingsSpec
    public void onFastRefresh() {
    }

    @Override // com.facebook.fbreact.specs.NativeDevSettingsSpec
    public void removeListeners(double d) {
    }

    @Override // com.facebook.fbreact.specs.NativeDevSettingsSpec
    public void setIsShakeToShowDevMenuEnabled(boolean z) {
    }

    public DevSettingsModule(ReactApplicationContext reactApplicationContext, DevSupportManager devSupportManager) {
        super(reactApplicationContext);
        this.mDevSupportManager = devSupportManager;
    }

    @Override // com.facebook.fbreact.specs.NativeDevSettingsSpec
    public void reload() {
        if (this.mDevSupportManager.getDevSupportEnabled()) {
            UiThreadUtil.runOnUiThread(new Runnable() {
                /* class com.facebook.react.modules.debug.DevSettingsModule.AnonymousClass1 */

                public void run() {
                    DevSettingsModule.this.mDevSupportManager.handleReloadJS();
                }
            });
        }
    }

    @Override // com.facebook.fbreact.specs.NativeDevSettingsSpec
    public void reloadWithReason(String str) {
        reload();
    }

    @Override // com.facebook.fbreact.specs.NativeDevSettingsSpec
    public void setHotLoadingEnabled(boolean z) {
        this.mDevSupportManager.setHotModuleReplacementEnabled(z);
    }

    @Override // com.facebook.fbreact.specs.NativeDevSettingsSpec
    public void setIsDebuggingRemotely(boolean z) {
        this.mDevSupportManager.setRemoteJSDebugEnabled(z);
    }

    @Override // com.facebook.fbreact.specs.NativeDevSettingsSpec
    public void setProfilingEnabled(boolean z) {
        this.mDevSupportManager.setFpsDebugEnabled(z);
    }

    @Override // com.facebook.fbreact.specs.NativeDevSettingsSpec
    public void toggleElementInspector() {
        this.mDevSupportManager.toggleElementInspector();
    }

    @Override // com.facebook.fbreact.specs.NativeDevSettingsSpec
    public void addMenuItem(final String str) {
        this.mDevSupportManager.addCustomDevOption(str, new DevOptionHandler() {
            /* class com.facebook.react.modules.debug.DevSettingsModule.AnonymousClass2 */

            @Override // com.facebook.react.devsupport.interfaces.DevOptionHandler
            public void onOptionSelected() {
                WritableMap createMap = Arguments.createMap();
                createMap.putString("title", str);
                ReactApplicationContext reactApplicationContextIfActiveOrWarn = DevSettingsModule.this.getReactApplicationContextIfActiveOrWarn();
                if (reactApplicationContextIfActiveOrWarn != null) {
                    ((DeviceEventManagerModule.RCTDeviceEventEmitter) reactApplicationContextIfActiveOrWarn.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)).emit("didPressMenuItem", createMap);
                }
            }
        });
    }
}
