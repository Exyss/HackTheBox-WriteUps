package com.facebook.react.views.text;

import android.os.Build;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import com.facebook.infer.annotation.Assertions;
import com.facebook.react.uimanager.NativeViewHierarchyOptimizer;
import com.facebook.react.uimanager.ReactShadowNode;
import com.facebook.react.uimanager.UIViewOperationQueue;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureMode;
import java.util.ArrayList;

public class ReactTextShadowNode extends ReactBaseTextShadowNode {
    private static final TextPaint sTextPaintInstance = new TextPaint(1);
    private Spannable mPreparedSpannableText;
    private boolean mShouldNotifyOnTextLayout;
    private final YogaMeasureFunction mTextMeasureFunction;

    @Override // com.facebook.react.uimanager.ReactShadowNodeImpl, com.facebook.react.uimanager.ReactShadowNode
    public boolean hoistNativeChildren() {
        return true;
    }

    @Override // com.facebook.react.uimanager.ReactShadowNodeImpl, com.facebook.react.uimanager.ReactShadowNode
    public boolean isVirtualAnchor() {
        return false;
    }

    public ReactTextShadowNode() {
        this(null);
    }

    public ReactTextShadowNode(ReactTextViewManagerCallback reactTextViewManagerCallback) {
        super(reactTextViewManagerCallback);
        this.mTextMeasureFunction = new YogaMeasureFunction() {
            /* class com.facebook.react.views.text.ReactTextShadowNode.AnonymousClass1 */

            /* JADX WARNING: Code restructure failed: missing block: B:47:0x0150, code lost:
                if (r2 > r21) goto L_0x0152;
             */
            @Override // com.facebook.yoga.YogaMeasureFunction
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public long measure(com.facebook.yoga.YogaNode r18, float r19, com.facebook.yoga.YogaMeasureMode r20, float r21, com.facebook.yoga.YogaMeasureMode r22) {
                /*
                // Method dump skipped, instructions count: 345
                */
                throw new UnsupportedOperationException("Method not decompiled: com.facebook.react.views.text.ReactTextShadowNode.AnonymousClass1.measure(com.facebook.yoga.YogaNode, float, com.facebook.yoga.YogaMeasureMode, float, com.facebook.yoga.YogaMeasureMode):long");
            }
        };
        initMeasureFunction();
    }

    private void initMeasureFunction() {
        if (!isVirtual()) {
            setMeasureFunction(this.mTextMeasureFunction);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private Layout measureSpannedText(Spannable spannable, float f, YogaMeasureMode yogaMeasureMode) {
        TextPaint textPaint = sTextPaintInstance;
        textPaint.setTextSize((float) this.mTextAttributes.getEffectiveFontSize());
        BoringLayout.Metrics isBoring = BoringLayout.isBoring(spannable, textPaint);
        float desiredWidth = isBoring == null ? Layout.getDesiredWidth(spannable, textPaint) : Float.NaN;
        boolean z = yogaMeasureMode == YogaMeasureMode.UNDEFINED || f < 0.0f;
        Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
        int textAlign = getTextAlign();
        if (textAlign == 1) {
            alignment = Layout.Alignment.ALIGN_CENTER;
        } else if (textAlign == 3) {
            alignment = Layout.Alignment.ALIGN_NORMAL;
        } else if (textAlign == 5) {
            alignment = Layout.Alignment.ALIGN_OPPOSITE;
        }
        if (isBoring == null && (z || (!YogaConstants.isUndefined(desiredWidth) && desiredWidth <= f))) {
            int ceil = (int) Math.ceil((double) desiredWidth);
            if (Build.VERSION.SDK_INT < 23) {
                return new StaticLayout(spannable, textPaint, ceil, alignment, 1.0f, 0.0f, this.mIncludeFontPadding);
            }
            StaticLayout.Builder hyphenationFrequency = StaticLayout.Builder.obtain(spannable, 0, spannable.length(), textPaint, ceil).setAlignment(alignment).setLineSpacing(0.0f, 1.0f).setIncludePad(this.mIncludeFontPadding).setBreakStrategy(this.mTextBreakStrategy).setHyphenationFrequency(this.mHyphenationFrequency);
            if (Build.VERSION.SDK_INT >= 26) {
                hyphenationFrequency.setJustificationMode(this.mJustificationMode);
            }
            if (Build.VERSION.SDK_INT >= 28) {
                hyphenationFrequency.setUseLineSpacingFromFallbacks(true);
            }
            return hyphenationFrequency.build();
        } else if (isBoring != null && (z || ((float) isBoring.width) <= f)) {
            return BoringLayout.make(spannable, textPaint, isBoring.width, alignment, 1.0f, 0.0f, isBoring, this.mIncludeFontPadding);
        } else {
            if (Build.VERSION.SDK_INT < 23) {
                return new StaticLayout(spannable, textPaint, (int) f, alignment, 1.0f, 0.0f, this.mIncludeFontPadding);
            }
            StaticLayout.Builder hyphenationFrequency2 = StaticLayout.Builder.obtain(spannable, 0, spannable.length(), textPaint, (int) f).setAlignment(alignment).setLineSpacing(0.0f, 1.0f).setIncludePad(this.mIncludeFontPadding).setBreakStrategy(this.mTextBreakStrategy).setHyphenationFrequency(this.mHyphenationFrequency);
            if (Build.VERSION.SDK_INT >= 28) {
                hyphenationFrequency2.setUseLineSpacingFromFallbacks(true);
            }
            return hyphenationFrequency2.build();
        }
    }

    private int getTextAlign() {
        int i = this.mTextAlign;
        if (getLayoutDirection() != YogaDirection.RTL) {
            return i;
        }
        if (i == 5) {
            return 3;
        }
        if (i == 3) {
            return 5;
        }
        return i;
    }

    @Override // com.facebook.react.uimanager.ReactShadowNodeImpl, com.facebook.react.uimanager.ReactShadowNode
    public void onBeforeLayout(NativeViewHierarchyOptimizer nativeViewHierarchyOptimizer) {
        this.mPreparedSpannableText = spannedFromShadowNode(this, null, true, nativeViewHierarchyOptimizer);
        markUpdated();
    }

    @Override // com.facebook.react.uimanager.ReactShadowNodeImpl, com.facebook.react.uimanager.ReactShadowNode
    public void markUpdated() {
        super.markUpdated();
        super.dirty();
    }

    @Override // com.facebook.react.uimanager.ReactShadowNodeImpl, com.facebook.react.uimanager.ReactShadowNode
    public void onCollectExtraUpdates(UIViewOperationQueue uIViewOperationQueue) {
        super.onCollectExtraUpdates(uIViewOperationQueue);
        Spannable spannable = this.mPreparedSpannableText;
        if (spannable != null) {
            uIViewOperationQueue.enqueueUpdateExtraData(getReactTag(), new ReactTextUpdate(spannable, -1, this.mContainsImages, getPadding(4), getPadding(1), getPadding(5), getPadding(3), getTextAlign(), this.mTextBreakStrategy, this.mJustificationMode));
        }
    }

    @ReactProp(name = "onTextLayout")
    public void setShouldNotifyOnTextLayout(boolean z) {
        this.mShouldNotifyOnTextLayout = z;
    }

    @Override // com.facebook.react.uimanager.ReactShadowNodeImpl, com.facebook.react.uimanager.ReactShadowNode
    public Iterable<? extends ReactShadowNode> calculateLayoutOnChildren() {
        if (this.mInlineViews == null || this.mInlineViews.isEmpty()) {
            return null;
        }
        Spanned spanned = (Spanned) Assertions.assertNotNull(this.mPreparedSpannableText, "Spannable element has not been prepared in onBeforeLayout");
        TextInlineViewPlaceholderSpan[] textInlineViewPlaceholderSpanArr = (TextInlineViewPlaceholderSpan[]) spanned.getSpans(0, spanned.length(), TextInlineViewPlaceholderSpan.class);
        ArrayList arrayList = new ArrayList(textInlineViewPlaceholderSpanArr.length);
        for (TextInlineViewPlaceholderSpan textInlineViewPlaceholderSpan : textInlineViewPlaceholderSpanArr) {
            ReactShadowNode reactShadowNode = (ReactShadowNode) this.mInlineViews.get(Integer.valueOf(textInlineViewPlaceholderSpan.getReactTag()));
            reactShadowNode.calculateLayout();
            arrayList.add(reactShadowNode);
        }
        return arrayList;
    }
}
