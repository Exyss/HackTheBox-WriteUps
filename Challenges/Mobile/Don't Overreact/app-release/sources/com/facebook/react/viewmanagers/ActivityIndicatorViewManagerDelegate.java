package com.facebook.react.viewmanagers;

import android.view.View;
import com.facebook.react.bridge.ColorPropConverter;
import com.facebook.react.uimanager.BaseViewManagerDelegate;
import com.facebook.react.uimanager.BaseViewManagerInterface;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.viewmanagers.ActivityIndicatorViewManagerInterface;

public class ActivityIndicatorViewManagerDelegate<T extends View, U extends BaseViewManagerInterface<T> & ActivityIndicatorViewManagerInterface<T>> extends BaseViewManagerDelegate<T, U> {
    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: U extends com.facebook.react.uimanager.BaseViewManagerInterface<T> & com.facebook.react.viewmanagers.ActivityIndicatorViewManagerInterface<T> */
    /* JADX WARN: Multi-variable type inference failed */
    public ActivityIndicatorViewManagerDelegate(U u) {
        super(u);
    }

    @Override // com.facebook.react.uimanager.ViewManagerDelegate, com.facebook.react.uimanager.BaseViewManagerDelegate
    public void setProperty(T t, String str, Object obj) {
        str.hashCode();
        boolean z = false;
        char c = 65535;
        switch (str.hashCode()) {
            case 3530753:
                if (str.equals("size")) {
                    c = 0;
                    break;
                }
                break;
            case 94842723:
                if (str.equals(ViewProps.COLOR)) {
                    c = 1;
                    break;
                }
                break;
            case 865748226:
                if (str.equals("hidesWhenStopped")) {
                    c = 2;
                    break;
                }
                break;
            case 1118509918:
                if (str.equals("animating")) {
                    c = 3;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                ((ActivityIndicatorViewManagerInterface) this.mViewManager).setSize(t, (String) obj);
                return;
            case 1:
                ((ActivityIndicatorViewManagerInterface) this.mViewManager).setColor(t, ColorPropConverter.getColor(obj, t.getContext()));
                return;
            case 2:
                ActivityIndicatorViewManagerInterface activityIndicatorViewManagerInterface = (ActivityIndicatorViewManagerInterface) this.mViewManager;
                if (obj != null) {
                    z = ((Boolean) obj).booleanValue();
                }
                activityIndicatorViewManagerInterface.setHidesWhenStopped(t, z);
                return;
            case 3:
                ActivityIndicatorViewManagerInterface activityIndicatorViewManagerInterface2 = (ActivityIndicatorViewManagerInterface) this.mViewManager;
                if (obj != null) {
                    z = ((Boolean) obj).booleanValue();
                }
                activityIndicatorViewManagerInterface2.setAnimating(t, z);
                return;
            default:
                super.setProperty(t, str, obj);
                return;
        }
    }
}
