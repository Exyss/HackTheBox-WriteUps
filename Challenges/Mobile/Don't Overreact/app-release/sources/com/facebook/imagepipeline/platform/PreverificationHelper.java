package com.facebook.imagepipeline.platform;

import android.graphics.Bitmap;

/* access modifiers changed from: package-private */
public class PreverificationHelper {
    PreverificationHelper() {
    }

    /* access modifiers changed from: package-private */
    public boolean shouldUseHardwareBitmapConfig(Bitmap.Config config) {
        return config == Bitmap.Config.HARDWARE;
    }
}
