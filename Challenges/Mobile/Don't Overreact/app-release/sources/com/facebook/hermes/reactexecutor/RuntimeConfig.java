package com.facebook.hermes.reactexecutor;

public final class RuntimeConfig {
    public long heapSizeMB;

    RuntimeConfig() {
    }

    RuntimeConfig(long j) {
        this.heapSizeMB = j;
    }
}
