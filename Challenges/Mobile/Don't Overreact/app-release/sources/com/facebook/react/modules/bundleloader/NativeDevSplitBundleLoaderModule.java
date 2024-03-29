package com.facebook.react.modules.bundleloader;

import com.facebook.fbreact.specs.NativeDevSplitBundleLoaderSpec;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.common.DebugServerException;
import com.facebook.react.devsupport.interfaces.DevSplitBundleCallback;
import com.facebook.react.devsupport.interfaces.DevSupportManager;
import com.facebook.react.module.annotations.ReactModule;

@ReactModule(name = NativeDevSplitBundleLoaderModule.NAME)
public class NativeDevSplitBundleLoaderModule extends NativeDevSplitBundleLoaderSpec {
    public static final String NAME = "DevSplitBundleLoader";
    private static final String REJECTION_CODE = "E_BUNDLE_LOAD_ERROR";
    private final DevSupportManager mDevSupportManager;

    @Override // com.facebook.react.bridge.NativeModule
    public String getName() {
        return NAME;
    }

    public NativeDevSplitBundleLoaderModule(ReactApplicationContext reactApplicationContext, DevSupportManager devSupportManager) {
        super(reactApplicationContext);
        this.mDevSupportManager = devSupportManager;
    }

    @Override // com.facebook.fbreact.specs.NativeDevSplitBundleLoaderSpec
    public void loadBundle(String str, final Promise promise) {
        this.mDevSupportManager.loadSplitBundleFromServer(str, new DevSplitBundleCallback() {
            /* class com.facebook.react.modules.bundleloader.NativeDevSplitBundleLoaderModule.AnonymousClass1 */

            @Override // com.facebook.react.devsupport.interfaces.DevSplitBundleCallback
            public void onSuccess() {
                promise.resolve(true);
            }

            @Override // com.facebook.react.devsupport.interfaces.DevSplitBundleCallback
            public void onError(String str, Throwable th) {
                String str2;
                if (th instanceof DebugServerException) {
                    str2 = ((DebugServerException) th).getOriginalMessage();
                } else {
                    str2 = "Unknown error fetching '" + str + "'.";
                }
                promise.reject(NativeDevSplitBundleLoaderModule.REJECTION_CODE, str2, th);
            }
        });
    }
}
