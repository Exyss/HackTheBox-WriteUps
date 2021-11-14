package com.facebook.react.devsupport;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.fbreact.specs.NativeLogBoxSpec;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.devsupport.interfaces.DevSupportManager;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.util.RNLog;

@ReactModule(name = LogBoxModule.NAME)
public class LogBoxModule extends NativeLogBoxSpec {
    public static final String NAME = "LogBox";
    private final DevSupportManager mDevSupportManager;
    private LogBoxDialog mLogBoxDialog;
    private View mReactRootView;

    @Override // com.facebook.react.bridge.NativeModule
    public String getName() {
        return NAME;
    }

    public LogBoxModule(ReactApplicationContext reactApplicationContext, DevSupportManager devSupportManager) {
        super(reactApplicationContext);
        this.mDevSupportManager = devSupportManager;
        UiThreadUtil.runOnUiThread(new Runnable() {
            /* class com.facebook.react.devsupport.LogBoxModule.AnonymousClass1 */

            public void run() {
                if (LogBoxModule.this.mReactRootView == null && LogBoxModule.this.mDevSupportManager != null) {
                    LogBoxModule logBoxModule = LogBoxModule.this;
                    logBoxModule.mReactRootView = logBoxModule.mDevSupportManager.createRootView(LogBoxModule.NAME);
                    if (LogBoxModule.this.mReactRootView == null) {
                        RNLog.e("Unable to launch logbox because react was unable to create the root view");
                    }
                }
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeLogBoxSpec
    public void show() {
        if (this.mReactRootView != null) {
            UiThreadUtil.runOnUiThread(new Runnable() {
                /* class com.facebook.react.devsupport.LogBoxModule.AnonymousClass2 */

                public void run() {
                    if (LogBoxModule.this.mLogBoxDialog == null && LogBoxModule.this.mReactRootView != null) {
                        Activity currentActivity = LogBoxModule.this.getCurrentActivity();
                        if (currentActivity == null || currentActivity.isFinishing()) {
                            RNLog.e("Unable to launch logbox because react activity is not available, here is the error that logbox would've displayed: ");
                            return;
                        }
                        LogBoxModule.this.mLogBoxDialog = new LogBoxDialog(currentActivity, LogBoxModule.this.mReactRootView);
                        LogBoxModule.this.mLogBoxDialog.setCancelable(false);
                        LogBoxModule.this.mLogBoxDialog.show();
                    }
                }
            });
        }
    }

    @Override // com.facebook.fbreact.specs.NativeLogBoxSpec
    public void hide() {
        UiThreadUtil.runOnUiThread(new Runnable() {
            /* class com.facebook.react.devsupport.LogBoxModule.AnonymousClass3 */

            public void run() {
                if (LogBoxModule.this.mLogBoxDialog != null) {
                    if (!(LogBoxModule.this.mReactRootView == null || LogBoxModule.this.mReactRootView.getParent() == null)) {
                        ((ViewGroup) LogBoxModule.this.mReactRootView.getParent()).removeView(LogBoxModule.this.mReactRootView);
                    }
                    LogBoxModule.this.mLogBoxDialog.dismiss();
                    LogBoxModule.this.mLogBoxDialog = null;
                }
            }
        });
    }

    @Override // com.facebook.react.bridge.BaseJavaModule, com.facebook.react.bridge.NativeModule
    public void onCatalystInstanceDestroy() {
        UiThreadUtil.runOnUiThread(new Runnable() {
            /* class com.facebook.react.devsupport.LogBoxModule.AnonymousClass4 */

            public void run() {
                if (LogBoxModule.this.mReactRootView != null) {
                    LogBoxModule.this.mDevSupportManager.destroyRootView(LogBoxModule.this.mReactRootView);
                    LogBoxModule.this.mReactRootView = null;
                }
            }
        });
    }
}
