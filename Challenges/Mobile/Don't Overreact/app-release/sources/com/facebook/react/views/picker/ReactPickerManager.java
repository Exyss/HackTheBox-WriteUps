package com.facebook.react.views.picker;

import android.view.View;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerHelper;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.views.picker.ReactPicker;
import com.facebook.react.views.picker.events.PickerItemSelectEvent;

public abstract class ReactPickerManager extends SimpleViewManager<ReactPicker> {
    @ReactProp(name = "items")
    public void setItems(ReactPicker reactPicker, ReadableArray readableArray) {
        reactPicker.setStagedItems(ReactPickerItem.createFromJsArrayMap(readableArray));
    }

    @ReactProp(customType = "Color", name = ViewProps.COLOR)
    public void setColor(ReactPicker reactPicker, Integer num) {
        reactPicker.setStagedPrimaryTextColor(num);
    }

    @ReactProp(name = "prompt")
    public void setPrompt(ReactPicker reactPicker, String str) {
        reactPicker.setPrompt(str);
    }

    @ReactProp(defaultBoolean = true, name = ViewProps.ENABLED)
    public void setEnabled(ReactPicker reactPicker, boolean z) {
        reactPicker.setEnabled(z);
    }

    @ReactProp(name = "selected")
    public void setSelected(ReactPicker reactPicker, int i) {
        reactPicker.setStagedSelection(i);
    }

    /* access modifiers changed from: protected */
    public void onAfterUpdateTransaction(ReactPicker reactPicker) {
        super.onAfterUpdateTransaction((View) reactPicker);
        reactPicker.commitStagedData();
    }

    /* access modifiers changed from: protected */
    public void addEventEmitters(ThemedReactContext themedReactContext, ReactPicker reactPicker) {
        reactPicker.setOnSelectListener(new PickerEventEmitter(reactPicker, UIManagerHelper.getEventDispatcherForReactTag(themedReactContext, reactPicker.getId())));
    }

    public void receiveCommand(ReactPicker reactPicker, String str, ReadableArray readableArray) {
        str.hashCode();
        if (str.equals("setNativeSelectedPosition") && readableArray != null) {
            reactPicker.setImmediateSelection(readableArray.getInt(0));
        }
    }

    /* access modifiers changed from: private */
    public static class PickerEventEmitter implements ReactPicker.OnSelectListener {
        private final EventDispatcher mEventDispatcher;
        private final ReactPicker mReactPicker;

        public PickerEventEmitter(ReactPicker reactPicker, EventDispatcher eventDispatcher) {
            this.mReactPicker = reactPicker;
            this.mEventDispatcher = eventDispatcher;
        }

        @Override // com.facebook.react.views.picker.ReactPicker.OnSelectListener
        public void onItemSelected(int i) {
            this.mEventDispatcher.dispatchEvent(new PickerItemSelectEvent(this.mReactPicker.getId(), i));
        }
    }
}
