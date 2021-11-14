package com.facebook.react.uimanager.events;

import com.facebook.common.logging.FLog;

public class BlackHoleEventDispatcher implements EventDispatcher {
    private static final EventDispatcher sEventDispatcher = new BlackHoleEventDispatcher();

    @Override // com.facebook.react.uimanager.events.EventDispatcher
    public void addBatchEventDispatchedListener(BatchEventDispatchedListener batchEventDispatchedListener) {
    }

    @Override // com.facebook.react.uimanager.events.EventDispatcher
    public void addListener(EventDispatcherListener eventDispatcherListener) {
    }

    @Override // com.facebook.react.uimanager.events.EventDispatcher
    public void dispatchAllEvents() {
    }

    @Override // com.facebook.react.uimanager.events.EventDispatcher
    public void onCatalystInstanceDestroyed() {
    }

    @Override // com.facebook.react.uimanager.events.EventDispatcher
    public void registerEventEmitter(int i, RCTEventEmitter rCTEventEmitter) {
    }

    @Override // com.facebook.react.uimanager.events.EventDispatcher
    public void removeBatchEventDispatchedListener(BatchEventDispatchedListener batchEventDispatchedListener) {
    }

    @Override // com.facebook.react.uimanager.events.EventDispatcher
    public void removeListener(EventDispatcherListener eventDispatcherListener) {
    }

    @Override // com.facebook.react.uimanager.events.EventDispatcher
    public void unregisterEventEmitter(int i) {
    }

    public static EventDispatcher get() {
        return sEventDispatcher;
    }

    private BlackHoleEventDispatcher() {
    }

    @Override // com.facebook.react.uimanager.events.EventDispatcher
    public void dispatchEvent(Event event) {
        String simpleName = getClass().getSimpleName();
        FLog.d(simpleName, "Trying to emit event to JS, but the React instance isn't ready. Event: " + event.getEventName());
    }
}
