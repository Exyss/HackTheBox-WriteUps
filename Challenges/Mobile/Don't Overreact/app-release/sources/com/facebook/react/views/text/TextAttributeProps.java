package com.facebook.react.views.text;

import android.os.Build;
import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.ReactAccessibilityDelegate;
import com.facebook.react.uimanager.ReactStylesDiffMap;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.yoga.YogaDirection;

public class TextAttributeProps {
    private static final int DEFAULT_BREAK_STRATEGY;
    private static final int DEFAULT_JUSTIFICATION_MODE = 0;
    private static final int DEFAULT_TEXT_SHADOW_COLOR = 1426063360;
    private static final String INLINE_IMAGE_PLACEHOLDER = "I";
    private static final String PROP_SHADOW_COLOR = "textShadowColor";
    private static final String PROP_SHADOW_OFFSET = "textShadowOffset";
    private static final String PROP_SHADOW_OFFSET_HEIGHT = "height";
    private static final String PROP_SHADOW_OFFSET_WIDTH = "width";
    private static final String PROP_SHADOW_RADIUS = "textShadowRadius";
    private static final String PROP_TEXT_TRANSFORM = "textTransform";
    public static final int UNSET = -1;
    protected ReactAccessibilityDelegate.AccessibilityRole mAccessibilityRole;
    protected boolean mAllowFontScaling = true;
    protected int mBackgroundColor;
    protected int mColor;
    protected boolean mContainsImages;
    protected String mFontFamily;
    protected String mFontFeatureSettings;
    protected int mFontSize = -1;
    protected float mFontSizeInput = -1.0f;
    protected int mFontStyle;
    protected int mFontWeight;
    protected float mHeightOfTallestInlineImage;
    protected boolean mIncludeFontPadding = true;
    protected boolean mIsAccessibilityRoleSet;
    protected boolean mIsBackgroundColorSet = false;
    protected boolean mIsColorSet = false;
    protected boolean mIsLineThroughTextDecorationSet = false;
    protected boolean mIsUnderlineTextDecorationSet = false;
    protected int mLayoutDirection = -1;
    protected float mLetterSpacingInput = Float.NaN;
    protected float mLineHeight = Float.NaN;
    protected float mLineHeightInput = -1.0f;
    protected int mNumberOfLines = -1;
    private final ReactStylesDiffMap mProps;
    protected int mTextAlign = 0;
    protected int mTextShadowColor = 1426063360;
    protected float mTextShadowOffsetDx = 0.0f;
    protected float mTextShadowOffsetDy = 0.0f;
    protected float mTextShadowRadius = 1.0f;
    protected TextTransform mTextTransform = TextTransform.UNSET;

    static {
        int i = Build.VERSION.SDK_INT;
        int i2 = 0;
        if (Build.VERSION.SDK_INT >= 23) {
            i2 = 1;
        }
        DEFAULT_BREAK_STRATEGY = i2;
    }

    public TextAttributeProps(ReactStylesDiffMap reactStylesDiffMap) {
        ReadableMap readableMap = null;
        this.mAccessibilityRole = null;
        this.mIsAccessibilityRoleSet = false;
        this.mFontStyle = -1;
        this.mFontWeight = -1;
        this.mFontFamily = null;
        this.mFontFeatureSettings = null;
        this.mContainsImages = false;
        this.mHeightOfTallestInlineImage = Float.NaN;
        this.mProps = reactStylesDiffMap;
        setNumberOfLines(getIntProp(ViewProps.NUMBER_OF_LINES, -1));
        setLineHeight(getFloatProp(ViewProps.LINE_HEIGHT, -1.0f));
        setLetterSpacing(getFloatProp(ViewProps.LETTER_SPACING, Float.NaN));
        setAllowFontScaling(getBooleanProp(ViewProps.ALLOW_FONT_SCALING, true));
        setFontSize(getFloatProp(ViewProps.FONT_SIZE, -1.0f));
        setColor(reactStylesDiffMap.hasKey(ViewProps.COLOR) ? Integer.valueOf(reactStylesDiffMap.getInt(ViewProps.COLOR, 0)) : null);
        setColor(reactStylesDiffMap.hasKey("foregroundColor") ? Integer.valueOf(reactStylesDiffMap.getInt("foregroundColor", 0)) : null);
        setBackgroundColor(reactStylesDiffMap.hasKey(ViewProps.BACKGROUND_COLOR) ? Integer.valueOf(reactStylesDiffMap.getInt(ViewProps.BACKGROUND_COLOR, 0)) : null);
        setFontFamily(getStringProp(ViewProps.FONT_FAMILY));
        setFontWeight(getStringProp(ViewProps.FONT_WEIGHT));
        setFontStyle(getStringProp(ViewProps.FONT_STYLE));
        setFontVariant(getArrayProp(ViewProps.FONT_VARIANT));
        setIncludeFontPadding(getBooleanProp(ViewProps.INCLUDE_FONT_PADDING, true));
        setTextDecorationLine(getStringProp(ViewProps.TEXT_DECORATION_LINE));
        setTextShadowOffset(reactStylesDiffMap.hasKey("textShadowOffset") ? reactStylesDiffMap.getMap("textShadowOffset") : readableMap);
        setTextShadowRadius((float) getIntProp("textShadowRadius", 1));
        setTextShadowColor(getIntProp("textShadowColor", 1426063360));
        setTextTransform(getStringProp("textTransform"));
        setLayoutDirection(getStringProp(ViewProps.LAYOUT_DIRECTION));
        setAccessibilityRole(getStringProp(ViewProps.ACCESSIBILITY_ROLE));
    }

    public static int getTextAlignment(ReactStylesDiffMap reactStylesDiffMap, boolean z) {
        String string = reactStylesDiffMap.hasKey(ViewProps.TEXT_ALIGN) ? reactStylesDiffMap.getString(ViewProps.TEXT_ALIGN) : null;
        if ("justify".equals(string)) {
            return 3;
        }
        if (string == null || "auto".equals(string)) {
            return 0;
        }
        if (ViewProps.LEFT.equals(string)) {
            if (z) {
                return 5;
            }
            return 3;
        } else if (ViewProps.RIGHT.equals(string)) {
            if (z) {
                return 3;
            }
        } else if ("center".equals(string)) {
            return 1;
        } else {
            throw new JSApplicationIllegalArgumentException("Invalid textAlign: " + string);
        }
        return 5;
    }

    public static int getJustificationMode(ReactStylesDiffMap reactStylesDiffMap) {
        if (!"justify".equals(reactStylesDiffMap.hasKey(ViewProps.TEXT_ALIGN) ? reactStylesDiffMap.getString(ViewProps.TEXT_ALIGN) : null) || Build.VERSION.SDK_INT < 26) {
            return DEFAULT_JUSTIFICATION_MODE;
        }
        return 1;
    }

    private boolean getBooleanProp(String str, boolean z) {
        return this.mProps.hasKey(str) ? this.mProps.getBoolean(str, z) : z;
    }

    private String getStringProp(String str) {
        if (this.mProps.hasKey(str)) {
            return this.mProps.getString(str);
        }
        return null;
    }

    private int getIntProp(String str, int i) {
        return this.mProps.hasKey(str) ? this.mProps.getInt(str, i) : i;
    }

    private float getFloatProp(String str, float f) {
        return this.mProps.hasKey(str) ? this.mProps.getFloat(str, f) : f;
    }

    private ReadableArray getArrayProp(String str) {
        if (this.mProps.hasKey(str)) {
            return this.mProps.getArray(str);
        }
        return null;
    }

    public float getEffectiveLineHeight() {
        return !Float.isNaN(this.mLineHeight) && !Float.isNaN(this.mHeightOfTallestInlineImage) && (this.mHeightOfTallestInlineImage > this.mLineHeight ? 1 : (this.mHeightOfTallestInlineImage == this.mLineHeight ? 0 : -1)) > 0 ? this.mHeightOfTallestInlineImage : this.mLineHeight;
    }

    public void setNumberOfLines(int i) {
        if (i == 0) {
            i = -1;
        }
        this.mNumberOfLines = i;
    }

    public void setLineHeight(float f) {
        float f2;
        this.mLineHeightInput = f;
        if (f == -1.0f) {
            this.mLineHeight = Float.NaN;
            return;
        }
        if (this.mAllowFontScaling) {
            f2 = PixelUtil.toPixelFromSP(f);
        } else {
            f2 = PixelUtil.toPixelFromDIP(f);
        }
        this.mLineHeight = f2;
    }

    public void setLetterSpacing(float f) {
        this.mLetterSpacingInput = f;
    }

    public float getLetterSpacing() {
        float f;
        if (this.mAllowFontScaling) {
            f = PixelUtil.toPixelFromSP(this.mLetterSpacingInput);
        } else {
            f = PixelUtil.toPixelFromDIP(this.mLetterSpacingInput);
        }
        int i = this.mFontSize;
        if (i > 0) {
            return f / ((float) i);
        }
        throw new IllegalArgumentException("FontSize should be a positive value. Current value: " + this.mFontSize);
    }

    public void setAllowFontScaling(boolean z) {
        if (z != this.mAllowFontScaling) {
            this.mAllowFontScaling = z;
            setFontSize(this.mFontSizeInput);
            setLineHeight(this.mLineHeightInput);
            setLetterSpacing(this.mLetterSpacingInput);
        }
    }

    public void setFontSize(float f) {
        double d;
        this.mFontSizeInput = f;
        if (f != -1.0f) {
            if (this.mAllowFontScaling) {
                d = Math.ceil((double) PixelUtil.toPixelFromSP(f));
            } else {
                d = Math.ceil((double) PixelUtil.toPixelFromDIP(f));
            }
            f = (float) d;
        }
        this.mFontSize = (int) f;
    }

    public void setColor(Integer num) {
        boolean z = num != null;
        this.mIsColorSet = z;
        if (z) {
            this.mColor = num.intValue();
        }
    }

    public void setBackgroundColor(Integer num) {
        boolean z = num != null;
        this.mIsBackgroundColorSet = z;
        if (z) {
            this.mBackgroundColor = num.intValue();
        }
    }

    public void setFontFamily(String str) {
        this.mFontFamily = str;
    }

    public void setFontVariant(ReadableArray readableArray) {
        this.mFontFeatureSettings = ReactTypefaceUtils.parseFontVariant(readableArray);
    }

    public void setFontWeight(String str) {
        int i = -1;
        int parseNumericFontWeight = str != null ? parseNumericFontWeight(str) : -1;
        if (parseNumericFontWeight >= 500 || "bold".equals(str)) {
            i = 1;
        } else if ("normal".equals(str) || (parseNumericFontWeight != -1 && parseNumericFontWeight < 500)) {
            i = 0;
        }
        if (i != this.mFontWeight) {
            this.mFontWeight = i;
        }
    }

    public void setFontStyle(String str) {
        int i;
        if ("italic".equals(str)) {
            i = 2;
        } else {
            i = "normal".equals(str) ? 0 : -1;
        }
        if (i != this.mFontStyle) {
            this.mFontStyle = i;
        }
    }

    public void setIncludeFontPadding(boolean z) {
        this.mIncludeFontPadding = z;
    }

    public void setTextDecorationLine(String str) {
        this.mIsUnderlineTextDecorationSet = false;
        this.mIsLineThroughTextDecorationSet = false;
        if (str != null) {
            String[] split = str.split("-");
            for (String str2 : split) {
                if ("underline".equals(str2)) {
                    this.mIsUnderlineTextDecorationSet = true;
                } else if ("strikethrough".equals(str2)) {
                    this.mIsLineThroughTextDecorationSet = true;
                }
            }
        }
    }

    public void setTextShadowOffset(ReadableMap readableMap) {
        this.mTextShadowOffsetDx = 0.0f;
        this.mTextShadowOffsetDy = 0.0f;
        if (readableMap != null) {
            if (readableMap.hasKey("width") && !readableMap.isNull("width")) {
                this.mTextShadowOffsetDx = PixelUtil.toPixelFromDIP(readableMap.getDouble("width"));
            }
            if (readableMap.hasKey("height") && !readableMap.isNull("height")) {
                this.mTextShadowOffsetDy = PixelUtil.toPixelFromDIP(readableMap.getDouble("height"));
            }
        }
    }

    public void setLayoutDirection(String str) {
        if (str == null || "undefined".equals(str)) {
            this.mLayoutDirection = -1;
        } else if ("rtl".equals(str)) {
            this.mLayoutDirection = 1;
        } else if ("ltr".equals(str)) {
            this.mLayoutDirection = 0;
        } else {
            throw new JSApplicationIllegalArgumentException("Invalid layoutDirection: " + str);
        }
    }

    public void setTextShadowRadius(float f) {
        if (f != this.mTextShadowRadius) {
            this.mTextShadowRadius = f;
        }
    }

    public void setTextShadowColor(int i) {
        if (i != this.mTextShadowColor) {
            this.mTextShadowColor = i;
        }
    }

    public void setTextTransform(String str) {
        if (str == null || ViewProps.NONE.equals(str)) {
            this.mTextTransform = TextTransform.NONE;
        } else if ("uppercase".equals(str)) {
            this.mTextTransform = TextTransform.UPPERCASE;
        } else if ("lowercase".equals(str)) {
            this.mTextTransform = TextTransform.LOWERCASE;
        } else if ("capitalize".equals(str)) {
            this.mTextTransform = TextTransform.CAPITALIZE;
        } else {
            throw new JSApplicationIllegalArgumentException("Invalid textTransform: " + str);
        }
    }

    public void setAccessibilityRole(String str) {
        if (str != null) {
            this.mIsAccessibilityRoleSet = str != null;
            this.mAccessibilityRole = ReactAccessibilityDelegate.AccessibilityRole.fromValue(str);
        }
    }

    public static int getTextBreakStrategy(String str) {
        int i = DEFAULT_BREAK_STRATEGY;
        if (str == null) {
            return i;
        }
        str.hashCode();
        if (!str.equals("balanced")) {
            return !str.equals("simple") ? 1 : 0;
        }
        return 2;
    }

    private static int parseNumericFontWeight(String str) {
        if (str.length() != 3 || !str.endsWith("00") || str.charAt(0) > '9' || str.charAt(0) < '1') {
            return -1;
        }
        return (str.charAt(0) - '0') * 100;
    }

    private YogaDirection getLayoutDirection() {
        return YogaDirection.LTR;
    }

    public float getBottomPadding() {
        return getPaddingProp(ViewProps.PADDING_BOTTOM);
    }

    public float getLeftPadding() {
        return getPaddingProp(ViewProps.PADDING_LEFT);
    }

    public float getStartPadding() {
        return getPaddingProp(ViewProps.PADDING_START);
    }

    public float getEndPadding() {
        return getPaddingProp(ViewProps.PADDING_END);
    }

    public float getTopPadding() {
        return getPaddingProp(ViewProps.PADDING_TOP);
    }

    public float getRightPadding() {
        return getPaddingProp(ViewProps.PADDING_RIGHT);
    }

    private float getPaddingProp(String str) {
        if (this.mProps.hasKey(ViewProps.PADDING)) {
            return PixelUtil.toPixelFromDIP(getFloatProp(ViewProps.PADDING, 0.0f));
        }
        return PixelUtil.toPixelFromDIP(getFloatProp(str, 0.0f));
    }
}
