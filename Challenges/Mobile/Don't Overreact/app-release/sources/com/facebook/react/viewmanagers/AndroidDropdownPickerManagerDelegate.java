package com.facebook.react.viewmanagers;

import android.view.View;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.BaseViewManagerDelegate;
import com.facebook.react.uimanager.BaseViewManagerInterface;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.viewmanagers.AndroidDropdownPickerManagerInterface;

public class AndroidDropdownPickerManagerDelegate<T extends View, U extends BaseViewManagerInterface<T> & AndroidDropdownPickerManagerInterface<T>> extends BaseViewManagerDelegate<T, U> {
    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: U extends com.facebook.react.uimanager.BaseViewManagerInterface<T> & com.facebook.react.viewmanagers.AndroidDropdownPickerManagerInterface<T> */
    /* JADX WARN: Multi-variable type inference failed */
    public AndroidDropdownPickerManagerDelegate(U u) {
        super(u);
    }

    @Override // com.facebook.react.uimanager.ViewManagerDelegate, com.facebook.react.uimanager.BaseViewManagerDelegate
    public void setProperty(T t, String str, Object obj) {
        str.hashCode();
        boolean z = true;
        int i = 0;
        char c = 65535;
        switch (str.hashCode()) {
            case -1609594047:
                if (str.equals(ViewProps.ENABLED)) {
                    c = 0;
                    break;
                }
                break;
            case -979805852:
                if (str.equals("prompt")) {
                    c = 1;
                    break;
                }
                break;
            case 94842723:
                if (str.equals(ViewProps.COLOR)) {
                    c = 2;
                    break;
                }
                break;
            case 100526016:
                if (str.equals("items")) {
                    c = 3;
                    break;
                }
                break;
            case 1191572123:
                if (str.equals("selected")) {
                    c = 4;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                AndroidDropdownPickerManagerInterface androidDropdownPickerManagerInterface = (AndroidDropdownPickerManagerInterface) this.mViewManager;
                if (obj != null) {
                    z = ((Boolean) obj).booleanValue();
                }
                androidDropdownPickerManagerInterface.setEnabled(t, z);
                return;
            case 1:
                ((AndroidDropdownPickerManagerInterface) this.mViewManager).setPrompt(t, obj == null ? "" : (String) obj);
                return;
            case 2:
                ((AndroidDropdownPickerManagerInterface) this.mViewManager).setColor(t, obj == null ? null : Integer.valueOf(((Double) obj).intValue()));
                return;
            case 3:
                ((AndroidDropdownPickerManagerInterface) this.mViewManager).setItems(t, (ReadableArray) obj);
                return;
            case 4:
                AndroidDropdownPickerManagerInterface androidDropdownPickerManagerInterface2 = (AndroidDropdownPickerManagerInterface) this.mViewManager;
                if (obj != null) {
                    i = ((Double) obj).intValue();
                }
                androidDropdownPickerManagerInterface2.setSelected(t, i);
                return;
            default:
                super.setProperty(t, str, obj);
                return;
        }
    }
}
