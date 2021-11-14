package com.facebook.react.viewmanagers;

import android.view.View;
import com.facebook.react.bridge.ColorPropConverter;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.BaseViewManagerDelegate;
import com.facebook.react.uimanager.BaseViewManagerInterface;
import com.facebook.react.viewmanagers.SwitchManagerInterface;

public class SwitchManagerDelegate<T extends View, U extends BaseViewManagerInterface<T> & SwitchManagerInterface<T>> extends BaseViewManagerDelegate<T, U> {
    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: U extends com.facebook.react.uimanager.BaseViewManagerInterface<T> & com.facebook.react.viewmanagers.SwitchManagerInterface<T> */
    /* JADX WARN: Multi-variable type inference failed */
    public SwitchManagerDelegate(U u) {
        super(u);
    }

    @Override // com.facebook.react.uimanager.ViewManagerDelegate, com.facebook.react.uimanager.BaseViewManagerDelegate
    public void setProperty(T t, String str, Object obj) {
        str.hashCode();
        boolean z = false;
        char c = 65535;
        switch (str.hashCode()) {
            case -1742453971:
                if (str.equals("thumbColor")) {
                    c = 0;
                    break;
                }
                break;
            case 111972721:
                if (str.equals("value")) {
                    c = 1;
                    break;
                }
                break;
            case 270940796:
                if (str.equals("disabled")) {
                    c = 2;
                    break;
                }
                break;
            case 1084662482:
                if (str.equals("trackColorForFalse")) {
                    c = 3;
                    break;
                }
                break;
            case 1296813577:
                if (str.equals("onTintColor")) {
                    c = 4;
                    break;
                }
                break;
            case 1327599912:
                if (str.equals("tintColor")) {
                    c = 5;
                    break;
                }
                break;
            case 1912319986:
                if (str.equals("thumbTintColor")) {
                    c = 6;
                    break;
                }
                break;
            case 2113632767:
                if (str.equals("trackColorForTrue")) {
                    c = 7;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                ((SwitchManagerInterface) this.mViewManager).setThumbColor(t, ColorPropConverter.getColor(obj, t.getContext()));
                return;
            case 1:
                SwitchManagerInterface switchManagerInterface = (SwitchManagerInterface) this.mViewManager;
                if (obj != null) {
                    z = ((Boolean) obj).booleanValue();
                }
                switchManagerInterface.setValue(t, z);
                return;
            case 2:
                SwitchManagerInterface switchManagerInterface2 = (SwitchManagerInterface) this.mViewManager;
                if (obj != null) {
                    z = ((Boolean) obj).booleanValue();
                }
                switchManagerInterface2.setDisabled(t, z);
                return;
            case 3:
                ((SwitchManagerInterface) this.mViewManager).setTrackColorForFalse(t, ColorPropConverter.getColor(obj, t.getContext()));
                return;
            case 4:
                ((SwitchManagerInterface) this.mViewManager).setOnTintColor(t, ColorPropConverter.getColor(obj, t.getContext()));
                return;
            case 5:
                ((SwitchManagerInterface) this.mViewManager).setTintColor(t, ColorPropConverter.getColor(obj, t.getContext()));
                return;
            case 6:
                ((SwitchManagerInterface) this.mViewManager).setThumbTintColor(t, ColorPropConverter.getColor(obj, t.getContext()));
                return;
            case 7:
                ((SwitchManagerInterface) this.mViewManager).setTrackColorForTrue(t, ColorPropConverter.getColor(obj, t.getContext()));
                return;
            default:
                super.setProperty(t, str, obj);
                return;
        }
    }

    @Override // com.facebook.react.uimanager.ViewManagerDelegate, com.facebook.react.uimanager.BaseViewManagerDelegate
    public void receiveCommand(T t, String str, ReadableArray readableArray) {
        str.hashCode();
        if (str.equals("setValue")) {
            ((SwitchManagerInterface) this.mViewManager).setValue(t, readableArray.getBoolean(0));
        }
    }
}
