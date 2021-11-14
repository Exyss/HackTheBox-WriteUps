package com.facebook.react.fabric;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.JSIModuleProvider;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.UIManager;
import com.facebook.react.fabric.events.EventBeatManager;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.systrace.Systrace;

public class FabricJSIModuleProvider implements JSIModuleProvider<UIManager> {
    private final ComponentFactory mComponentFactory;
    private final ReactNativeConfig mConfig;
    private final ReactApplicationContext mReactApplicationContext;

    public FabricJSIModuleProvider(ReactApplicationContext reactApplicationContext, ComponentFactory componentFactory, ReactNativeConfig reactNativeConfig) {
        this.mReactApplicationContext = reactApplicationContext;
        this.mComponentFactory = componentFactory;
        this.mConfig = reactNativeConfig;
    }

    @Override // com.facebook.react.bridge.JSIModuleProvider
    public UIManager get() {
        EventBeatManager eventBeatManager = new EventBeatManager(this.mReactApplicationContext);
        FabricUIManager createUIManager = createUIManager(eventBeatManager);
        Systrace.beginSection(0, "FabricJSIModuleProvider.registerBinding");
        Binding binding = new Binding();
        loadClasses();
        binding.register(this.mReactApplicationContext.getCatalystInstance().getRuntimeExecutor(), createUIManager, eventBeatManager, this.mReactApplicationContext.getCatalystInstance().getReactQueueConfiguration().getJSQueueThread(), this.mComponentFactory, this.mConfig);
        Systrace.endSection(0);
        return createUIManager;
    }

    private FabricUIManager createUIManager(EventBeatManager eventBeatManager) {
        Systrace.beginSection(0, "FabricJSIModuleProvider.createUIManager");
        UIManagerModule uIManagerModule = (UIManagerModule) Assertions.assertNotNull(this.mReactApplicationContext.getNativeModule(UIManagerModule.class));
        FabricUIManager fabricUIManager = new FabricUIManager(this.mReactApplicationContext, uIManagerModule.getViewManagerRegistry_DO_NOT_USE(), uIManagerModule.getEventDispatcher(), eventBeatManager);
        Systrace.endSection(0);
        return fabricUIManager;
    }

    private static void loadClasses() {
    }
}
