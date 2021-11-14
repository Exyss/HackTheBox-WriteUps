package com.facebook.react.viewmanagers;

import android.view.View;
import com.facebook.react.bridge.ColorPropConverter;
import com.facebook.react.uimanager.BaseViewManagerDelegate;
import com.facebook.react.uimanager.BaseViewManagerInterface;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.viewmanagers.InputAccessoryViewManagerInterface;

public class InputAccessoryViewManagerDelegate<T extends View, U extends BaseViewManagerInterface<T> & InputAccessoryViewManagerInterface<T>> extends BaseViewManagerDelegate<T, U> {
    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: U extends com.facebook.react.uimanager.BaseViewManagerInterface<T> & com.facebook.react.viewmanagers.InputAccessoryViewManagerInterface<T> */
    /* JADX WARN: Multi-variable type inference failed */
    public InputAccessoryViewManagerDelegate(U u) {
        super(u);
    }

    @Override // com.facebook.react.uimanager.ViewManagerDelegate, com.facebook.react.uimanager.BaseViewManagerDelegate
    public void setProperty(T t, String str, Object obj) {
        str.hashCode();
        if (!str.equals(ViewProps.BACKGROUND_COLOR)) {
            super.setProperty(t, str, obj);
        } else {
            ((InputAccessoryViewManagerInterface) this.mViewManager).setBackgroundColor(t, ColorPropConverter.getColor(obj, t.getContext()));
        }
    }
}
