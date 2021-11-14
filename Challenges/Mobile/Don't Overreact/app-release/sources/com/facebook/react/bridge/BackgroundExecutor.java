package com.facebook.react.bridge;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundExecutor {
    private static final String TAG = "FabricBackgroundExecutor";
    private final ExecutorService mExecutorService = Executors.newFixedThreadPool(1);

    private BackgroundExecutor() {
    }

    private void queueRunnable(Runnable runnable) {
        if (runnable == null) {
            ReactSoftException.logSoftException(TAG, new ReactNoCrashSoftException("runnable is null"));
            return;
        }
        ExecutorService executorService = this.mExecutorService;
        if (executorService == null) {
            ReactSoftException.logSoftException(TAG, new ReactNoCrashSoftException("executorService is null"));
        } else {
            executorService.execute(runnable);
        }
    }
}
