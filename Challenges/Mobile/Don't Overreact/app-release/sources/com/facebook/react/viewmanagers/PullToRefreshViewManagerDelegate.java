package com.facebook.react.viewmanagers;

import android.view.View;
import com.facebook.react.bridge.ColorPropConverter;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.BaseViewManagerDelegate;
import com.facebook.react.uimanager.BaseViewManagerInterface;
import com.facebook.react.viewmanagers.PullToRefreshViewManagerInterface;

public class PullToRefreshViewManagerDelegate<T extends View, U extends BaseViewManagerInterface<T> & PullToRefreshViewManagerInterface<T>> extends BaseViewManagerDelegate<T, U> {
    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: U extends com.facebook.react.uimanager.BaseViewManagerInterface<T> & com.facebook.react.viewmanagers.PullToRefreshViewManagerInterface<T> */
    /* JADX WARN: Multi-variable type inference failed */
    public PullToRefreshViewManagerDelegate(U u) {
        super(u);
    }

    @Override // com.facebook.react.uimanager.ViewManagerDelegate, com.facebook.react.uimanager.BaseViewManagerDelegate
    public void setProperty(T t, String str, Object obj) {
        str.hashCode();
        boolean z = false;
        char c = 65535;
        switch (str.hashCode()) {
            case -1799367701:
                if (str.equals("titleColor")) {
                    c = 0;
                    break;
                }
                break;
            case -321826009:
                if (str.equals("refreshing")) {
                    c = 1;
                    break;
                }
                break;
            case 110371416:
                if (str.equals("title")) {
                    c = 2;
                    break;
                }
                break;
            case 1327599912:
                if (str.equals("tintColor")) {
                    c = 3;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                ((PullToRefreshViewManagerInterface) this.mViewManager).setTitleColor(t, ColorPropConverter.getColor(obj, t.getContext()));
                return;
            case 1:
                PullToRefreshViewManagerInterface pullToRefreshViewManagerInterface = (PullToRefreshViewManagerInterface) this.mViewManager;
                if (obj != null) {
                    z = ((Boolean) obj).booleanValue();
                }
                pullToRefreshViewManagerInterface.setRefreshing(t, z);
                return;
            case 2:
                ((PullToRefreshViewManagerInterface) this.mViewManager).setTitle(t, obj == null ? null : (String) obj);
                return;
            case 3:
                ((PullToRefreshViewManagerInterface) this.mViewManager).setTintColor(t, ColorPropConverter.getColor(obj, t.getContext()));
                return;
            default:
                super.setProperty(t, str, obj);
                return;
        }
    }

    public void receiveCommand(PullToRefreshViewManagerInterface<T> pullToRefreshViewManagerInterface, T t, String str, ReadableArray readableArray) {
        str.hashCode();
        if (str.equals("setNativeRefreshing")) {
            pullToRefreshViewManagerInterface.setNativeRefreshing(t, readableArray.getBoolean(0));
        }
    }
}
