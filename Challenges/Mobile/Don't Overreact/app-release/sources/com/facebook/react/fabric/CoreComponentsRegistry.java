package com.facebook.react.fabric;

import com.facebook.jni.HybridData;
import com.facebook.soloader.SoLoader;

public class CoreComponentsRegistry {
    private final HybridData mHybridData;

    private native HybridData initHybrid(ComponentFactory componentFactory);

    static {
        SoLoader.loadLibrary("fabricjni");
    }

    private CoreComponentsRegistry(ComponentFactory componentFactory) {
        this.mHybridData = initHybrid(componentFactory);
    }

    public static CoreComponentsRegistry register(ComponentFactory componentFactory) {
        return new CoreComponentsRegistry(componentFactory);
    }
}
