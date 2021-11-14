package com.facebook.react.uimanager;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.config.ReactFeatureFlags;

public class FabricViewStateManager {
    private static final String TAG = "FabricViewStateManager";
    private StateWrapper mStateWrapper = null;

    public interface HasFabricViewStateManager {
        FabricViewStateManager getFabricViewStateManager();
    }

    public interface StateUpdateCallback {
        WritableMap getStateUpdate();
    }

    public void setStateWrapper(StateWrapper stateWrapper) {
        this.mStateWrapper = stateWrapper;
    }

    public boolean hasStateWrapper() {
        return this.mStateWrapper != null;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setState(final StateWrapper stateWrapper, final StateUpdateCallback stateUpdateCallback, final int i) {
        if (stateWrapper == null) {
            FLog.e(TAG, "setState called without a StateWrapper");
        } else if (stateWrapper == this.mStateWrapper && i <= 60) {
            AnonymousClass1 r0 = null;
            if (ReactFeatureFlags.enableExperimentalStateUpdateRetry) {
                r0 = new Runnable() {
                    /* class com.facebook.react.uimanager.FabricViewStateManager.AnonymousClass1 */

                    public void run() {
                        FLog.e(FabricViewStateManager.TAG, "UpdateState failed - retrying! " + i);
                        FabricViewStateManager.this.setState(stateWrapper, stateUpdateCallback, i + 1);
                    }
                };
            }
            WritableMap stateUpdate = stateUpdateCallback.getStateUpdate();
            if (stateUpdate != null) {
                stateWrapper.updateState(stateUpdate, r0);
            }
        }
    }

    public void setState(StateUpdateCallback stateUpdateCallback) {
        setState(this.mStateWrapper, stateUpdateCallback, 0);
    }

    public ReadableMap getState() {
        StateWrapper stateWrapper = this.mStateWrapper;
        if (stateWrapper != null) {
            return stateWrapper.getState();
        }
        return null;
    }
}
