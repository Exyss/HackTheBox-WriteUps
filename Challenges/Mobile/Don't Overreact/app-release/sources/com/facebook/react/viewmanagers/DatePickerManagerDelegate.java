package com.facebook.react.viewmanagers;

import android.view.View;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.BaseViewManagerDelegate;
import com.facebook.react.uimanager.BaseViewManagerInterface;
import com.facebook.react.viewmanagers.DatePickerManagerInterface;

public class DatePickerManagerDelegate<T extends View, U extends BaseViewManagerInterface<T> & DatePickerManagerInterface<T>> extends BaseViewManagerDelegate<T, U> {
    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: U extends com.facebook.react.uimanager.BaseViewManagerInterface<T> & com.facebook.react.viewmanagers.DatePickerManagerInterface<T> */
    /* JADX WARN: Multi-variable type inference failed */
    public DatePickerManagerDelegate(U u) {
        super(u);
    }

    @Override // com.facebook.react.uimanager.ViewManagerDelegate, com.facebook.react.uimanager.BaseViewManagerDelegate
    public void setProperty(T t, String str, Object obj) {
        str.hashCode();
        int i = 1;
        char c = 65535;
        switch (str.hashCode()) {
            case -1339516167:
                if (str.equals("minuteInterval")) {
                    c = 0;
                    break;
                }
                break;
            case -1097462182:
                if (str.equals("locale")) {
                    c = 1;
                    break;
                }
                break;
            case -292758706:
                if (str.equals("timeZoneOffsetInMinutes")) {
                    c = 2;
                    break;
                }
                break;
            case 3076014:
                if (str.equals("date")) {
                    c = 3;
                    break;
                }
                break;
            case 3357091:
                if (str.equals("mode")) {
                    c = 4;
                    break;
                }
                break;
            case 1007762652:
                if (str.equals("minimumDate")) {
                    c = 5;
                    break;
                }
                break;
            case 1232894226:
                if (str.equals("initialDate")) {
                    c = 6;
                    break;
                }
                break;
            case 1685195246:
                if (str.equals("maximumDate")) {
                    c = 7;
                    break;
                }
                break;
        }
        float f = 0.0f;
        switch (c) {
            case 0:
                DatePickerManagerInterface datePickerManagerInterface = (DatePickerManagerInterface) this.mViewManager;
                if (obj != null) {
                    i = ((Double) obj).intValue();
                }
                datePickerManagerInterface.setMinuteInterval(t, Integer.valueOf(i));
                return;
            case 1:
                ((DatePickerManagerInterface) this.mViewManager).setLocale(t, obj == null ? null : (String) obj);
                return;
            case 2:
                DatePickerManagerInterface datePickerManagerInterface2 = (DatePickerManagerInterface) this.mViewManager;
                if (obj != null) {
                    f = ((Double) obj).floatValue();
                }
                datePickerManagerInterface2.setTimeZoneOffsetInMinutes(t, f);
                return;
            case 3:
                DatePickerManagerInterface datePickerManagerInterface3 = (DatePickerManagerInterface) this.mViewManager;
                if (obj != null) {
                    f = ((Double) obj).floatValue();
                }
                datePickerManagerInterface3.setDate(t, f);
                return;
            case 4:
                ((DatePickerManagerInterface) this.mViewManager).setMode(t, (String) obj);
                return;
            case 5:
                DatePickerManagerInterface datePickerManagerInterface4 = (DatePickerManagerInterface) this.mViewManager;
                if (obj != null) {
                    f = ((Double) obj).floatValue();
                }
                datePickerManagerInterface4.setMinimumDate(t, f);
                return;
            case 6:
                DatePickerManagerInterface datePickerManagerInterface5 = (DatePickerManagerInterface) this.mViewManager;
                if (obj != null) {
                    f = ((Double) obj).floatValue();
                }
                datePickerManagerInterface5.setInitialDate(t, f);
                return;
            case 7:
                DatePickerManagerInterface datePickerManagerInterface6 = (DatePickerManagerInterface) this.mViewManager;
                if (obj != null) {
                    f = ((Double) obj).floatValue();
                }
                datePickerManagerInterface6.setMaximumDate(t, f);
                return;
            default:
                super.setProperty(t, str, obj);
                return;
        }
    }

    public void receiveCommand(DatePickerManagerInterface<T> datePickerManagerInterface, T t, String str, ReadableArray readableArray) {
        str.hashCode();
        if (str.equals("setNativeDate")) {
            datePickerManagerInterface.setNativeDate(t, (float) readableArray.getDouble(0));
        }
    }
}
