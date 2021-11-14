package com.facebook.react.views.view;

import android.graphics.Rect;
import android.view.View;
import androidx.core.view.ViewCompat;
import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.PointerEvents;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerHelper;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.annotations.ReactPropGroup;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.yoga.YogaConstants;
import java.util.Locale;
import java.util.Map;

@ReactModule(name = "RCTView")
public class ReactViewManager extends ReactClippingViewManager<ReactViewGroup> {
    private static final int CMD_HOTSPOT_UPDATE = 1;
    private static final int CMD_SET_PRESSED = 2;
    private static final String HOTSPOT_UPDATE_KEY = "hotspotUpdate";
    public static final String REACT_CLASS = "RCTView";
    private static final int[] SPACING_TYPES = {8, 0, 2, 1, 3, 4, 5};

    @Override // com.facebook.react.bridge.NativeModule, com.facebook.react.uimanager.ViewManager
    public String getName() {
        return "RCTView";
    }

    @ReactProp(name = ViewProps.COLLAPSABLE)
    public void setCollapsable(ReactViewGroup reactViewGroup, boolean z) {
    }

    @ReactProp(name = "accessible")
    public void setAccessible(ReactViewGroup reactViewGroup, boolean z) {
        reactViewGroup.setFocusable(z);
    }

    @ReactProp(name = "hasTVPreferredFocus")
    public void setTVPreferredFocus(ReactViewGroup reactViewGroup, boolean z) {
        if (z) {
            reactViewGroup.setFocusable(true);
            reactViewGroup.setFocusableInTouchMode(true);
            reactViewGroup.requestFocus();
        }
    }

    @ReactProp(defaultInt = -1, name = "nextFocusDown")
    public void nextFocusDown(ReactViewGroup reactViewGroup, int i) {
        reactViewGroup.setNextFocusDownId(i);
    }

    @ReactProp(defaultInt = -1, name = "nextFocusForward")
    public void nextFocusForward(ReactViewGroup reactViewGroup, int i) {
        reactViewGroup.setNextFocusForwardId(i);
    }

    @ReactProp(defaultInt = -1, name = "nextFocusLeft")
    public void nextFocusLeft(ReactViewGroup reactViewGroup, int i) {
        reactViewGroup.setNextFocusLeftId(i);
    }

    @ReactProp(defaultInt = -1, name = "nextFocusRight")
    public void nextFocusRight(ReactViewGroup reactViewGroup, int i) {
        reactViewGroup.setNextFocusRightId(i);
    }

    @ReactProp(defaultInt = -1, name = "nextFocusUp")
    public void nextFocusUp(ReactViewGroup reactViewGroup, int i) {
        reactViewGroup.setNextFocusUpId(i);
    }

    @ReactPropGroup(defaultFloat = YogaConstants.UNDEFINED, names = {ViewProps.BORDER_RADIUS, ViewProps.BORDER_TOP_LEFT_RADIUS, ViewProps.BORDER_TOP_RIGHT_RADIUS, ViewProps.BORDER_BOTTOM_RIGHT_RADIUS, ViewProps.BORDER_BOTTOM_LEFT_RADIUS, ViewProps.BORDER_TOP_START_RADIUS, ViewProps.BORDER_TOP_END_RADIUS, ViewProps.BORDER_BOTTOM_START_RADIUS, ViewProps.BORDER_BOTTOM_END_RADIUS})
    public void setBorderRadius(ReactViewGroup reactViewGroup, int i, float f) {
        if (!YogaConstants.isUndefined(f) && f < 0.0f) {
            f = Float.NaN;
        }
        if (!YogaConstants.isUndefined(f)) {
            f = PixelUtil.toPixelFromDIP(f);
        }
        if (i == 0) {
            reactViewGroup.setBorderRadius(f);
        } else {
            reactViewGroup.setBorderRadius(f, i - 1);
        }
    }

    @ReactProp(name = "borderStyle")
    public void setBorderStyle(ReactViewGroup reactViewGroup, String str) {
        reactViewGroup.setBorderStyle(str);
    }

    @ReactProp(name = "hitSlop")
    public void setHitSlop(ReactViewGroup reactViewGroup, ReadableMap readableMap) {
        if (readableMap == null) {
            reactViewGroup.setHitSlopRect(null);
            return;
        }
        int i = 0;
        int pixelFromDIP = readableMap.hasKey(ViewProps.LEFT) ? (int) PixelUtil.toPixelFromDIP(readableMap.getDouble(ViewProps.LEFT)) : 0;
        int pixelFromDIP2 = readableMap.hasKey(ViewProps.TOP) ? (int) PixelUtil.toPixelFromDIP(readableMap.getDouble(ViewProps.TOP)) : 0;
        int pixelFromDIP3 = readableMap.hasKey(ViewProps.RIGHT) ? (int) PixelUtil.toPixelFromDIP(readableMap.getDouble(ViewProps.RIGHT)) : 0;
        if (readableMap.hasKey(ViewProps.BOTTOM)) {
            i = (int) PixelUtil.toPixelFromDIP(readableMap.getDouble(ViewProps.BOTTOM));
        }
        reactViewGroup.setHitSlopRect(new Rect(pixelFromDIP, pixelFromDIP2, pixelFromDIP3, i));
    }

    @ReactProp(name = ViewProps.POINTER_EVENTS)
    public void setPointerEvents(ReactViewGroup reactViewGroup, String str) {
        if (str == null) {
            reactViewGroup.setPointerEvents(PointerEvents.AUTO);
        } else {
            reactViewGroup.setPointerEvents(PointerEvents.valueOf(str.toUpperCase(Locale.US).replace("-", "_")));
        }
    }

    @ReactProp(name = "nativeBackgroundAndroid")
    public void setNativeBackground(ReactViewGroup reactViewGroup, ReadableMap readableMap) {
        reactViewGroup.setTranslucentBackgroundDrawable(readableMap == null ? null : ReactDrawableHelper.createDrawableFromJSDescription(reactViewGroup.getContext(), readableMap));
    }

    @ReactProp(name = "nativeForegroundAndroid")
    public void setNativeForeground(ReactViewGroup reactViewGroup, ReadableMap readableMap) {
        reactViewGroup.setForeground(readableMap == null ? null : ReactDrawableHelper.createDrawableFromJSDescription(reactViewGroup.getContext(), readableMap));
    }

    @ReactProp(name = ViewProps.NEEDS_OFFSCREEN_ALPHA_COMPOSITING)
    public void setNeedsOffscreenAlphaCompositing(ReactViewGroup reactViewGroup, boolean z) {
        reactViewGroup.setNeedsOffscreenAlphaCompositing(z);
    }

    @ReactPropGroup(defaultFloat = YogaConstants.UNDEFINED, names = {ViewProps.BORDER_WIDTH, ViewProps.BORDER_LEFT_WIDTH, ViewProps.BORDER_RIGHT_WIDTH, ViewProps.BORDER_TOP_WIDTH, ViewProps.BORDER_BOTTOM_WIDTH, ViewProps.BORDER_START_WIDTH, ViewProps.BORDER_END_WIDTH})
    public void setBorderWidth(ReactViewGroup reactViewGroup, int i, float f) {
        if (!YogaConstants.isUndefined(f) && f < 0.0f) {
            f = Float.NaN;
        }
        if (!YogaConstants.isUndefined(f)) {
            f = PixelUtil.toPixelFromDIP(f);
        }
        reactViewGroup.setBorderWidth(SPACING_TYPES[i], f);
    }

    @ReactPropGroup(customType = "Color", names = {ViewProps.BORDER_COLOR, ViewProps.BORDER_LEFT_COLOR, ViewProps.BORDER_RIGHT_COLOR, ViewProps.BORDER_TOP_COLOR, ViewProps.BORDER_BOTTOM_COLOR, ViewProps.BORDER_START_COLOR, ViewProps.BORDER_END_COLOR})
    public void setBorderColor(ReactViewGroup reactViewGroup, int i, Integer num) {
        float f = Float.NaN;
        float intValue = num == null ? Float.NaN : (float) (num.intValue() & ViewCompat.MEASURED_SIZE_MASK);
        if (num != null) {
            f = (float) (num.intValue() >>> 24);
        }
        reactViewGroup.setBorderColor(SPACING_TYPES[i], intValue, f);
    }

    @ReactProp(name = "focusable")
    public void setFocusable(final ReactViewGroup reactViewGroup, boolean z) {
        if (z) {
            reactViewGroup.setOnClickListener(new View.OnClickListener() {
                /* class com.facebook.react.views.view.ReactViewManager.AnonymousClass1 */

                public void onClick(View view) {
                    EventDispatcher eventDispatcherForReactTag = UIManagerHelper.getEventDispatcherForReactTag((ReactContext) reactViewGroup.getContext(), reactViewGroup.getId());
                    if (eventDispatcherForReactTag != null) {
                        eventDispatcherForReactTag.dispatchEvent(new ViewGroupClickEvent(reactViewGroup.getId()));
                    }
                }
            });
            reactViewGroup.setFocusable(true);
            return;
        }
        reactViewGroup.setOnClickListener(null);
        reactViewGroup.setClickable(false);
    }

    @ReactProp(name = ViewProps.OVERFLOW)
    public void setOverflow(ReactViewGroup reactViewGroup, String str) {
        reactViewGroup.setOverflow(str);
    }

    @ReactProp(name = "backfaceVisibility")
    public void setBackfaceVisibility(ReactViewGroup reactViewGroup, String str) {
        reactViewGroup.setBackfaceVisibility(str);
    }

    public void setOpacity(ReactViewGroup reactViewGroup, float f) {
        reactViewGroup.setOpacityIfPossible(f);
    }

    public void setTransform(ReactViewGroup reactViewGroup, ReadableArray readableArray) {
        super.setTransform((View) reactViewGroup, readableArray);
        reactViewGroup.setBackfaceVisibilityDependantOpacity();
    }

    @Override // com.facebook.react.uimanager.ViewManager
    public ReactViewGroup createViewInstance(ThemedReactContext themedReactContext) {
        return new ReactViewGroup(themedReactContext);
    }

    @Override // com.facebook.react.uimanager.ViewManager
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(HOTSPOT_UPDATE_KEY, 1, "setPressed", 2);
    }

    public void receiveCommand(ReactViewGroup reactViewGroup, int i, ReadableArray readableArray) {
        if (i == 1) {
            handleHotspotUpdate(reactViewGroup, readableArray);
        } else if (i == 2) {
            handleSetPressed(reactViewGroup, readableArray);
        }
    }

    public void receiveCommand(ReactViewGroup reactViewGroup, String str, ReadableArray readableArray) {
        str.hashCode();
        if (str.equals("setPressed")) {
            handleSetPressed(reactViewGroup, readableArray);
        } else if (str.equals(HOTSPOT_UPDATE_KEY)) {
            handleHotspotUpdate(reactViewGroup, readableArray);
        }
    }

    private void handleSetPressed(ReactViewGroup reactViewGroup, ReadableArray readableArray) {
        if (readableArray == null || readableArray.size() != 1) {
            throw new JSApplicationIllegalArgumentException("Illegal number of arguments for 'setPressed' command");
        }
        reactViewGroup.setPressed(readableArray.getBoolean(0));
    }

    private void handleHotspotUpdate(ReactViewGroup reactViewGroup, ReadableArray readableArray) {
        if (readableArray == null || readableArray.size() != 2) {
            throw new JSApplicationIllegalArgumentException("Illegal number of arguments for 'updateHotspot' command");
        }
        reactViewGroup.drawableHotspotChanged(PixelUtil.toPixelFromDIP(readableArray.getDouble(0)), PixelUtil.toPixelFromDIP(readableArray.getDouble(1)));
    }
}
