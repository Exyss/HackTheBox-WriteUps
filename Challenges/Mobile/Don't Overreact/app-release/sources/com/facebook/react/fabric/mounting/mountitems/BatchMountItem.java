package com.facebook.react.fabric.mounting.mountitems;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.ReactMarker;
import com.facebook.react.bridge.ReactMarkerConstants;
import com.facebook.react.fabric.mounting.MountingManager;
import com.facebook.react.views.textinput.ReactEditTextInputConnectionWrapper;
import com.facebook.systrace.Systrace;

public class BatchMountItem implements MountItem {
    static final String TAG = "FabricBatchMountItem";
    private final int mCommitNumber;
    private final MountItem[] mMountItems;
    private final int mRootTag;
    private final int mSize;

    public BatchMountItem(int i, MountItem[] mountItemArr, int i2, int i3) {
        int i4;
        if (mountItemArr == null) {
            i4 = 0;
        } else {
            i4 = mountItemArr.length;
        }
        if (i2 < 0 || i2 > i4) {
            throw new IllegalArgumentException("Invalid size received by parameter size: " + i2 + " items.size = " + i4);
        }
        this.mRootTag = i;
        this.mMountItems = mountItemArr;
        this.mSize = i2;
        this.mCommitNumber = i3;
    }

    private void beginMarkers(String str) {
        Systrace.beginSection(0, "FabricUIManager::" + str + " - " + this.mSize + " items");
        if (this.mCommitNumber > 0) {
            ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_BATCH_EXECUTION_START, null, this.mCommitNumber);
        }
    }

    private void endMarkers() {
        if (this.mCommitNumber > 0) {
            ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_BATCH_EXECUTION_END, null, this.mCommitNumber);
        }
        Systrace.endSection(0);
    }

    @Override // com.facebook.react.fabric.mounting.mountitems.MountItem
    public void execute(MountingManager mountingManager) {
        beginMarkers("mountViews");
        for (int i = 0; i < this.mSize; i++) {
            try {
                this.mMountItems[i].execute(mountingManager);
            } catch (RuntimeException e) {
                FLog.e(TAG, "Caught exception executing mountItem @" + i + ": " + this.mMountItems[i].toString(), e);
                throw e;
            }
        }
        endMarkers();
    }

    public int getRootTag() {
        return this.mRootTag;
    }

    public boolean shouldSchedule() {
        return this.mSize != 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < this.mSize) {
            if (sb.length() > 0) {
                sb.append(ReactEditTextInputConnectionWrapper.NEWLINE_RAW_VALUE);
            }
            sb.append("BatchMountItem [S:" + this.mRootTag + "] (");
            int i2 = i + 1;
            sb.append(i2);
            sb.append("/");
            sb.append(this.mSize);
            sb.append("): ");
            sb.append(this.mMountItems[i]);
            i = i2;
        }
        return sb.toString();
    }
}
