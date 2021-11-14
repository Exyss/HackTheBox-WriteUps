package com.facebook.react.viewmanagers;

import android.view.View;

public interface DatePickerManagerInterface<T extends View> {
    void setDate(T t, float f);

    void setInitialDate(T t, float f);

    void setLocale(T t, String str);

    void setMaximumDate(T t, float f);

    void setMinimumDate(T t, float f);

    void setMinuteInterval(T t, Integer num);

    void setMode(T t, String str);

    void setNativeDate(T t, float f);

    void setTimeZoneOffsetInMinutes(T t, float f);
}
