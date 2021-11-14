package com.facebook.react.viewmanagers;

import android.view.View;
import com.facebook.react.uimanager.BaseViewManagerDelegate;
import com.facebook.react.uimanager.BaseViewManagerInterface;
import com.facebook.react.viewmanagers.SafeAreaViewManagerInterface;

public class SafeAreaViewManagerDelegate<T extends View, U extends BaseViewManagerInterface<T> & SafeAreaViewManagerInterface<T>> extends BaseViewManagerDelegate<T, U> {
    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: U extends com.facebook.react.uimanager.BaseViewManagerInterface<T> & com.facebook.react.viewmanagers.SafeAreaViewManagerInterface<T> */
    /* JADX WARN: Multi-variable type inference failed */
    public SafeAreaViewManagerDelegate(U u) {
        super(u);
    }

    @Override // com.facebook.react.uimanager.ViewManagerDelegate, com.facebook.react.uimanager.BaseViewManagerDelegate
    public void setProperty(T t, String str, Object obj) {
        str.hashCode();
        if (!str.equals("emulateUnlessSupported")) {
            super.setProperty(t, str, obj);
        } else {
            ((SafeAreaViewManagerInterface) this.mViewManager).setEmulateUnlessSupported(t, obj == null ? false : ((Boolean) obj).booleanValue());
        }
    }
}
