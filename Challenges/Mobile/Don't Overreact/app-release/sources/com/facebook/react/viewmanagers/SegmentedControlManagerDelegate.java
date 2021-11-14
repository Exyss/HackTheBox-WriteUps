package com.facebook.react.viewmanagers;

import android.view.View;
import com.facebook.react.bridge.ColorPropConverter;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.BaseViewManagerDelegate;
import com.facebook.react.uimanager.BaseViewManagerInterface;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.viewmanagers.SegmentedControlManagerInterface;

public class SegmentedControlManagerDelegate<T extends View, U extends BaseViewManagerInterface<T> & SegmentedControlManagerInterface<T>> extends BaseViewManagerDelegate<T, U> {
    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: U extends com.facebook.react.uimanager.BaseViewManagerInterface<T> & com.facebook.react.viewmanagers.SegmentedControlManagerInterface<T> */
    /* JADX WARN: Multi-variable type inference failed */
    public SegmentedControlManagerDelegate(U u) {
        super(u);
    }

    @Override // com.facebook.react.uimanager.ViewManagerDelegate, com.facebook.react.uimanager.BaseViewManagerDelegate
    public void setProperty(T t, String str, Object obj) {
        str.hashCode();
        boolean z = true;
        int i = 0;
        boolean z2 = false;
        char c = 65535;
        switch (str.hashCode()) {
            case -1609594047:
                if (str.equals(ViewProps.ENABLED)) {
                    c = 0;
                    break;
                }
                break;
            case -1063571914:
                if (str.equals("textColor")) {
                    c = 1;
                    break;
                }
                break;
            case -823812830:
                if (str.equals("values")) {
                    c = 2;
                    break;
                }
                break;
            case 1287124693:
                if (str.equals(ViewProps.BACKGROUND_COLOR)) {
                    c = 3;
                    break;
                }
                break;
            case 1327599912:
                if (str.equals("tintColor")) {
                    c = 4;
                    break;
                }
                break;
            case 1436069623:
                if (str.equals("selectedIndex")) {
                    c = 5;
                    break;
                }
                break;
            case 1684715624:
                if (str.equals("momentary")) {
                    c = 6;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                SegmentedControlManagerInterface segmentedControlManagerInterface = (SegmentedControlManagerInterface) this.mViewManager;
                if (obj != null) {
                    z = ((Boolean) obj).booleanValue();
                }
                segmentedControlManagerInterface.setEnabled(t, z);
                return;
            case 1:
                ((SegmentedControlManagerInterface) this.mViewManager).setTextColor(t, ColorPropConverter.getColor(obj, t.getContext()));
                return;
            case 2:
                ((SegmentedControlManagerInterface) this.mViewManager).setValues(t, (ReadableArray) obj);
                return;
            case 3:
                ((SegmentedControlManagerInterface) this.mViewManager).setBackgroundColor(t, ColorPropConverter.getColor(obj, t.getContext()));
                return;
            case 4:
                ((SegmentedControlManagerInterface) this.mViewManager).setTintColor(t, ColorPropConverter.getColor(obj, t.getContext()));
                return;
            case 5:
                SegmentedControlManagerInterface segmentedControlManagerInterface2 = (SegmentedControlManagerInterface) this.mViewManager;
                if (obj != null) {
                    i = ((Double) obj).intValue();
                }
                segmentedControlManagerInterface2.setSelectedIndex(t, i);
                return;
            case 6:
                SegmentedControlManagerInterface segmentedControlManagerInterface3 = (SegmentedControlManagerInterface) this.mViewManager;
                if (obj != null) {
                    z2 = ((Boolean) obj).booleanValue();
                }
                segmentedControlManagerInterface3.setMomentary(t, z2);
                return;
            default:
                super.setProperty(t, str, obj);
                return;
        }
    }
}
