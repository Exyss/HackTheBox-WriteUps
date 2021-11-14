package com.facebook.react.viewmanagers;

import android.view.View;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.BaseViewManagerDelegate;
import com.facebook.react.uimanager.BaseViewManagerInterface;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.viewmanagers.AndroidDialogPickerManagerInterface;

public class AndroidDialogPickerManagerDelegate<T extends View, U extends BaseViewManagerInterface<T> & AndroidDialogPickerManagerInterface<T>> extends BaseViewManagerDelegate<T, U> {
    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: U extends com.facebook.react.uimanager.BaseViewManagerInterface<T> & com.facebook.react.viewmanagers.AndroidDialogPickerManagerInterface<T> */
    /* JADX WARN: Multi-variable type inference failed */
    public AndroidDialogPickerManagerDelegate(U u) {
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
            case 1287124693:
                if (str.equals(ViewProps.BACKGROUND_COLOR)) {
                    c = 5;
                    break;
                }
                break;
        }
        Integer num = null;
        switch (c) {
            case 0:
                AndroidDialogPickerManagerInterface androidDialogPickerManagerInterface = (AndroidDialogPickerManagerInterface) this.mViewManager;
                if (obj != null) {
                    z = ((Boolean) obj).booleanValue();
                }
                androidDialogPickerManagerInterface.setEnabled(t, z);
                return;
            case 1:
                ((AndroidDialogPickerManagerInterface) this.mViewManager).setPrompt(t, obj == null ? "" : (String) obj);
                return;
            case 2:
                AndroidDialogPickerManagerInterface androidDialogPickerManagerInterface2 = (AndroidDialogPickerManagerInterface) this.mViewManager;
                if (obj != null) {
                    num = Integer.valueOf(((Double) obj).intValue());
                }
                androidDialogPickerManagerInterface2.setColor(t, num);
                return;
            case 3:
                ((AndroidDialogPickerManagerInterface) this.mViewManager).setItems(t, (ReadableArray) obj);
                return;
            case 4:
                AndroidDialogPickerManagerInterface androidDialogPickerManagerInterface3 = (AndroidDialogPickerManagerInterface) this.mViewManager;
                if (obj != null) {
                    i = ((Double) obj).intValue();
                }
                androidDialogPickerManagerInterface3.setSelected(t, i);
                return;
            case 5:
                AndroidDialogPickerManagerInterface androidDialogPickerManagerInterface4 = (AndroidDialogPickerManagerInterface) this.mViewManager;
                if (obj != null) {
                    num = Integer.valueOf(((Double) obj).intValue());
                }
                androidDialogPickerManagerInterface4.setBackgroundColor(t, num);
                return;
            default:
                super.setProperty(t, str, obj);
                return;
        }
    }
}
