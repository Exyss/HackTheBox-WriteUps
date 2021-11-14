package com.facebook.react.views.scroll;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.OverScroller;
import android.widget.ScrollView;
import androidx.core.view.ViewCompat;
import com.facebook.common.logging.FLog;
import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.config.ReactFeatureFlags;
import com.facebook.react.uimanager.FabricViewStateManager;
import com.facebook.react.uimanager.MeasureSpecAssertions;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.ReactClippingViewGroup;
import com.facebook.react.uimanager.ReactClippingViewGroupHelper;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.events.NativeGestureUtil;
import com.facebook.react.views.view.ReactViewBackgroundManager;
import java.lang.reflect.Field;
import java.util.List;

public class ReactScrollView extends ScrollView implements ReactClippingViewGroup, ViewGroup.OnHierarchyChangeListener, View.OnLayoutChangeListener, FabricViewStateManager.HasFabricViewStateManager {
    private static final String CONTENT_OFFSET_LEFT = "contentOffsetLeft";
    private static final String CONTENT_OFFSET_TOP = "contentOffsetTop";
    private static final int UNSET_CONTENT_OFFSET = -1;
    private static Field sScrollerField = null;
    private static boolean sTriedToGetScrollerField = false;
    private boolean mActivelyScrolling;
    private Rect mClippingRect;
    private View mContentView;
    private float mDecelerationRate;
    private boolean mDisableIntervalMomentum;
    private boolean mDragging;
    private Drawable mEndBackground;
    private int mEndFillColor;
    private final FabricViewStateManager mFabricViewStateManager;
    private int mFinalAnimatedPositionScrollX;
    private int mFinalAnimatedPositionScrollY;
    private FpsListener mFpsListener;
    private int mLastStateUpdateScrollX;
    private int mLastStateUpdateScrollY;
    private final OnScrollDispatchHelper mOnScrollDispatchHelper;
    private String mOverflow;
    private boolean mPagingEnabled;
    private Runnable mPostTouchRunnable;
    private ReactViewBackgroundManager mReactBackgroundManager;
    private final Rect mRect;
    private boolean mRemoveClippedSubviews;
    private ValueAnimator mScrollAnimator;
    private boolean mScrollEnabled;
    private String mScrollPerfTag;
    private final OverScroller mScroller;
    private boolean mSendMomentumEvents;
    private int mSnapInterval;
    private List<Integer> mSnapOffsets;
    private boolean mSnapToEnd;
    private boolean mSnapToStart;
    private final VelocityHelper mVelocityHelper;
    private int pendingContentOffsetX;
    private int pendingContentOffsetY;

    public ReactScrollView(ReactContext reactContext) {
        this(reactContext, null);
    }

    public ReactScrollView(ReactContext reactContext, FpsListener fpsListener) {
        super(reactContext);
        this.mOnScrollDispatchHelper = new OnScrollDispatchHelper();
        this.mVelocityHelper = new VelocityHelper();
        this.mRect = new Rect();
        this.mOverflow = ViewProps.HIDDEN;
        this.mPagingEnabled = false;
        this.mScrollEnabled = true;
        this.mFpsListener = null;
        this.mEndFillColor = 0;
        this.mDisableIntervalMomentum = false;
        this.mSnapInterval = 0;
        this.mDecelerationRate = 0.985f;
        this.mSnapToStart = true;
        this.mSnapToEnd = true;
        this.pendingContentOffsetX = -1;
        this.pendingContentOffsetY = -1;
        this.mFabricViewStateManager = new FabricViewStateManager();
        this.mLastStateUpdateScrollX = -1;
        this.mLastStateUpdateScrollY = -1;
        this.mFpsListener = fpsListener;
        this.mReactBackgroundManager = new ReactViewBackgroundManager(this);
        this.mScroller = getOverScrollerFromParent();
        setOnHierarchyChangeListener(this);
        setScrollBarStyle(33554432);
    }

    private OverScroller getOverScrollerFromParent() {
        if (!sTriedToGetScrollerField) {
            sTriedToGetScrollerField = true;
            try {
                Field declaredField = ScrollView.class.getDeclaredField("mScroller");
                sScrollerField = declaredField;
                declaredField.setAccessible(true);
            } catch (NoSuchFieldException unused) {
                FLog.w(ReactConstants.TAG, "Failed to get mScroller field for ScrollView! This app will exhibit the bounce-back scrolling bug :(");
            }
        }
        Field field = sScrollerField;
        if (field == null) {
            return null;
        }
        try {
            Object obj = field.get(this);
            if (obj instanceof OverScroller) {
                return (OverScroller) obj;
            }
            FLog.w(ReactConstants.TAG, "Failed to cast mScroller field in ScrollView (probably due to OEM changes to AOSP)! This app will exhibit the bounce-back scrolling bug :(");
            return null;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get mScroller from ScrollView!", e);
        }
    }

    public void setDisableIntervalMomentum(boolean z) {
        this.mDisableIntervalMomentum = z;
    }

    public void setSendMomentumEvents(boolean z) {
        this.mSendMomentumEvents = z;
    }

    public void setScrollPerfTag(String str) {
        this.mScrollPerfTag = str;
    }

    public void setScrollEnabled(boolean z) {
        this.mScrollEnabled = z;
    }

    public void setPagingEnabled(boolean z) {
        this.mPagingEnabled = z;
    }

    public void setDecelerationRate(float f) {
        this.mDecelerationRate = f;
        OverScroller overScroller = this.mScroller;
        if (overScroller != null) {
            overScroller.setFriction(1.0f - f);
        }
    }

    public void setSnapInterval(int i) {
        this.mSnapInterval = i;
    }

    public void setSnapOffsets(List<Integer> list) {
        this.mSnapOffsets = list;
    }

    public void setSnapToStart(boolean z) {
        this.mSnapToStart = z;
    }

    public void setSnapToEnd(boolean z) {
        this.mSnapToEnd = z;
    }

    public void flashScrollIndicators() {
        awakenScrollBars();
    }

    public void setOverflow(String str) {
        this.mOverflow = str;
        invalidate();
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        MeasureSpecAssertions.assertExplicitMeasureSpec(i, i2);
        setMeasuredDimension(View.MeasureSpec.getSize(i), View.MeasureSpec.getSize(i2));
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5 = this.pendingContentOffsetX;
        if (i5 == -1) {
            i5 = getScrollX();
        }
        int i6 = this.pendingContentOffsetY;
        if (i6 == -1) {
            i6 = getScrollY();
        }
        reactScrollTo(i5, i6);
        ReactScrollViewHelper.emitLayoutEvent(this);
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (this.mRemoveClippedSubviews) {
            updateClippingRect();
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mRemoveClippedSubviews) {
            updateClippingRect();
        }
    }

    public void requestChildFocus(View view, View view2) {
        if (view2 != null) {
            scrollToChild(view2);
        }
        super.requestChildFocus(view, view2);
    }

    private void scrollToChild(View view) {
        Rect rect = new Rect();
        view.getDrawingRect(rect);
        offsetDescendantRectToMyCoords(view, rect);
        int computeScrollDeltaToGetChildRectOnScreen = computeScrollDeltaToGetChildRectOnScreen(rect);
        if (computeScrollDeltaToGetChildRectOnScreen != 0) {
            scrollBy(0, computeScrollDeltaToGetChildRectOnScreen);
        }
    }

    /* access modifiers changed from: protected */
    public void onScrollChanged(int i, int i2, int i3, int i4) {
        super.onScrollChanged(i, i2, i3, i4);
        this.mActivelyScrolling = true;
        if (this.mOnScrollDispatchHelper.onScrollChanged(i, i2)) {
            if (this.mRemoveClippedSubviews) {
                updateClippingRect();
            }
            ReactScrollViewHelper.emitScrollEvent(this, this.mOnScrollDispatchHelper.getXFlingVelocity(), this.mOnScrollDispatchHelper.getYFlingVelocity());
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (!this.mScrollEnabled) {
            return false;
        }
        try {
            if (super.onInterceptTouchEvent(motionEvent)) {
                NativeGestureUtil.notifyNativeGestureStarted(this, motionEvent);
                ReactScrollViewHelper.emitScrollBeginDragEvent(this);
                this.mDragging = true;
                enableFpsListener();
                return true;
            }
        } catch (IllegalArgumentException e) {
            FLog.w(ReactConstants.TAG, "Error intercepting touch event.", e);
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.mScrollEnabled) {
            return false;
        }
        this.mVelocityHelper.calculateVelocity(motionEvent);
        if ((motionEvent.getAction() & 255) == 1 && this.mDragging) {
            updateStateOnScroll();
            float xVelocity = this.mVelocityHelper.getXVelocity();
            float yVelocity = this.mVelocityHelper.getYVelocity();
            ReactScrollViewHelper.emitScrollEndDragEvent(this, xVelocity, yVelocity);
            this.mDragging = false;
            handlePostTouchScrolling(Math.round(xVelocity), Math.round(yVelocity));
        }
        return super.onTouchEvent(motionEvent);
    }

    public boolean executeKeyEvent(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        if (this.mScrollEnabled || (keyCode != 19 && keyCode != 20)) {
            return super.executeKeyEvent(keyEvent);
        }
        return false;
    }

    @Override // com.facebook.react.uimanager.ReactClippingViewGroup
    public void setRemoveClippedSubviews(boolean z) {
        if (z && this.mClippingRect == null) {
            this.mClippingRect = new Rect();
        }
        this.mRemoveClippedSubviews = z;
        updateClippingRect();
    }

    @Override // com.facebook.react.uimanager.ReactClippingViewGroup
    public boolean getRemoveClippedSubviews() {
        return this.mRemoveClippedSubviews;
    }

    @Override // com.facebook.react.uimanager.ReactClippingViewGroup
    public void updateClippingRect() {
        if (this.mRemoveClippedSubviews) {
            Assertions.assertNotNull(this.mClippingRect);
            ReactClippingViewGroupHelper.calculateClippingRect(this, this.mClippingRect);
            View childAt = getChildAt(0);
            if (childAt instanceof ReactClippingViewGroup) {
                ((ReactClippingViewGroup) childAt).updateClippingRect();
            }
        }
    }

    @Override // com.facebook.react.uimanager.ReactClippingViewGroup
    public void getClippingRect(Rect rect) {
        rect.set((Rect) Assertions.assertNotNull(this.mClippingRect));
    }

    public boolean getChildVisibleRect(View view, Rect rect, Point point) {
        if (ReactFeatureFlags.clipChildRectsIfOverflowIsHidden) {
            return ReactClippingViewGroupHelper.getChildVisibleRectHelper(view, rect, point, this, this.mOverflow);
        }
        return super.getChildVisibleRect(view, rect, point);
    }

    public void fling(int i) {
        float signum = Math.signum(this.mOnScrollDispatchHelper.getYFlingVelocity());
        if (signum == 0.0f) {
            signum = Math.signum((float) i);
        }
        int abs = (int) (((float) Math.abs(i)) * signum);
        if (this.mPagingEnabled) {
            flingAndSnap(abs);
        } else if (this.mScroller != null) {
            this.mScroller.fling(getScrollX(), getScrollY(), 0, abs, 0, 0, 0, Integer.MAX_VALUE, 0, ((getHeight() - getPaddingBottom()) - getPaddingTop()) / 2);
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            super.fling(abs);
        }
        handlePostTouchScrolling(0, abs);
    }

    private void enableFpsListener() {
        if (isScrollPerfLoggingEnabled()) {
            Assertions.assertNotNull(this.mFpsListener);
            Assertions.assertNotNull(this.mScrollPerfTag);
            this.mFpsListener.enable(this.mScrollPerfTag);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void disableFpsListener() {
        if (isScrollPerfLoggingEnabled()) {
            Assertions.assertNotNull(this.mFpsListener);
            Assertions.assertNotNull(this.mScrollPerfTag);
            this.mFpsListener.disable(this.mScrollPerfTag);
        }
    }

    private boolean isScrollPerfLoggingEnabled() {
        String str;
        return (this.mFpsListener == null || (str = this.mScrollPerfTag) == null || str.isEmpty()) ? false : true;
    }

    private int getMaxScrollY() {
        return Math.max(0, this.mContentView.getHeight() - ((getHeight() - getPaddingBottom()) - getPaddingTop()));
    }

    public void draw(Canvas canvas) {
        if (this.mEndFillColor != 0) {
            View childAt = getChildAt(0);
            if (!(this.mEndBackground == null || childAt == null || childAt.getBottom() >= getHeight())) {
                this.mEndBackground.setBounds(0, childAt.getBottom(), getWidth(), getHeight());
                this.mEndBackground.draw(canvas);
            }
        }
        getDrawingRect(this.mRect);
        String str = this.mOverflow;
        str.hashCode();
        if (!str.equals(ViewProps.VISIBLE)) {
            canvas.clipRect(this.mRect);
        }
        super.draw(canvas);
    }

    private void handlePostTouchScrolling(int i, int i2) {
        if (this.mPostTouchRunnable == null) {
            if (this.mSendMomentumEvents) {
                enableFpsListener();
                ReactScrollViewHelper.emitScrollMomentumBeginEvent(this, i, i2);
            }
            this.mActivelyScrolling = false;
            AnonymousClass1 r3 = new Runnable() {
                /* class com.facebook.react.views.scroll.ReactScrollView.AnonymousClass1 */
                private boolean mRunning = true;
                private boolean mSnappingToPage = false;
                private int mStableFrames = 0;

                public void run() {
                    if (ReactScrollView.this.mActivelyScrolling) {
                        ReactScrollView.this.mActivelyScrolling = false;
                        this.mStableFrames = 0;
                        this.mRunning = true;
                    } else {
                        ReactScrollView.this.updateStateOnScroll();
                        int i = this.mStableFrames + 1;
                        this.mStableFrames = i;
                        this.mRunning = i < 3;
                        if (!ReactScrollView.this.mPagingEnabled || this.mSnappingToPage) {
                            if (ReactScrollView.this.mSendMomentumEvents) {
                                ReactScrollViewHelper.emitScrollMomentumEndEvent(ReactScrollView.this);
                            }
                            ReactScrollView.this.disableFpsListener();
                        } else {
                            this.mSnappingToPage = true;
                            ReactScrollView.this.flingAndSnap(0);
                            ViewCompat.postOnAnimationDelayed(ReactScrollView.this, this, 20);
                        }
                    }
                    if (this.mRunning) {
                        ViewCompat.postOnAnimationDelayed(ReactScrollView.this, this, 20);
                    } else {
                        ReactScrollView.this.mPostTouchRunnable = null;
                    }
                }
            };
            this.mPostTouchRunnable = r3;
            ViewCompat.postOnAnimationDelayed(this, r3, 20);
        }
    }

    private int getPostAnimationScrollX() {
        ValueAnimator valueAnimator = this.mScrollAnimator;
        if (valueAnimator == null || !valueAnimator.isRunning()) {
            return getScrollX();
        }
        return this.mFinalAnimatedPositionScrollX;
    }

    private int getPostAnimationScrollY() {
        ValueAnimator valueAnimator = this.mScrollAnimator;
        if (valueAnimator == null || !valueAnimator.isRunning()) {
            return getScrollY();
        }
        return this.mFinalAnimatedPositionScrollY;
    }

    private int predictFinalScrollPosition(int i) {
        OverScroller overScroller = new OverScroller(getContext());
        overScroller.setFriction(1.0f - this.mDecelerationRate);
        overScroller.fling(getPostAnimationScrollX(), getPostAnimationScrollY(), 0, i, 0, 0, 0, getMaxScrollY(), 0, ((getHeight() - getPaddingBottom()) - getPaddingTop()) / 2);
        return overScroller.getFinalY();
    }

    private void smoothScrollAndSnap(int i) {
        double snapInterval = (double) getSnapInterval();
        double postAnimationScrollY = (double) getPostAnimationScrollY();
        double d = postAnimationScrollY / snapInterval;
        int floor = (int) Math.floor(d);
        int ceil = (int) Math.ceil(d);
        int round = (int) Math.round(d);
        int round2 = (int) Math.round(((double) predictFinalScrollPosition(i)) / snapInterval);
        if (i > 0 && ceil == floor) {
            ceil++;
        } else if (i < 0 && floor == ceil) {
            floor--;
        }
        if (i > 0 && round < ceil && round2 > floor) {
            round = ceil;
        } else if (i < 0 && round > floor && round2 < ceil) {
            round = floor;
        }
        double d2 = ((double) round) * snapInterval;
        if (d2 != postAnimationScrollY) {
            this.mActivelyScrolling = true;
            reactSmoothScrollTo(getScrollX(), (int) d2);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00c0, code lost:
        if (getScrollY() <= r4) goto L_0x00c2;
     */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00eb  */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0118  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void flingAndSnap(int r19) {
        /*
        // Method dump skipped, instructions count: 288
        */
        throw new UnsupportedOperationException("Method not decompiled: com.facebook.react.views.scroll.ReactScrollView.flingAndSnap(int):void");
    }

    private int getSnapInterval() {
        int i = this.mSnapInterval;
        if (i != 0) {
            return i;
        }
        return getHeight();
    }

    public void setEndFillColor(int i) {
        if (i != this.mEndFillColor) {
            this.mEndFillColor = i;
            this.mEndBackground = new ColorDrawable(this.mEndFillColor);
        }
    }

    /* access modifiers changed from: protected */
    public void onOverScrolled(int i, int i2, boolean z, boolean z2) {
        int maxScrollY;
        OverScroller overScroller = this.mScroller;
        if (!(overScroller == null || this.mContentView == null || overScroller.isFinished() || this.mScroller.getCurrY() == this.mScroller.getFinalY() || i2 < (maxScrollY = getMaxScrollY()))) {
            this.mScroller.abortAnimation();
            i2 = maxScrollY;
        }
        super.onOverScrolled(i, i2, z, z2);
    }

    public void onChildViewAdded(View view, View view2) {
        this.mContentView = view2;
        view2.addOnLayoutChangeListener(this);
    }

    public void onChildViewRemoved(View view, View view2) {
        this.mContentView.removeOnLayoutChangeListener(this);
        this.mContentView = null;
    }

    public void reactSmoothScrollTo(int i, int i2) {
        ValueAnimator valueAnimator = this.mScrollAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        this.mFinalAnimatedPositionScrollX = i;
        this.mFinalAnimatedPositionScrollY = i2;
        ValueAnimator ofPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofInt("scrollX", getScrollX(), i), PropertyValuesHolder.ofInt("scrollY", getScrollY(), i2));
        this.mScrollAnimator = ofPropertyValuesHolder;
        ofPropertyValuesHolder.setDuration((long) ReactScrollViewHelper.getDefaultScrollAnimationDuration(getContext()));
        this.mScrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            /* class com.facebook.react.views.scroll.ReactScrollView.AnonymousClass2 */

            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ReactScrollView.this.scrollTo(((Integer) valueAnimator.getAnimatedValue("scrollX")).intValue(), ((Integer) valueAnimator.getAnimatedValue("scrollY")).intValue());
            }
        });
        this.mScrollAnimator.addListener(new Animator.AnimatorListener() {
            /* class com.facebook.react.views.scroll.ReactScrollView.AnonymousClass3 */

            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
                ReactScrollView.this.mFinalAnimatedPositionScrollX = -1;
                ReactScrollView.this.mFinalAnimatedPositionScrollY = -1;
                ReactScrollView.this.mScrollAnimator = null;
                ReactScrollView.this.updateStateOnScroll();
            }
        });
        this.mScrollAnimator.start();
        updateStateOnScroll(i, i2);
        setPendingContentOffsets(i, i2);
    }

    public void reactScrollTo(int i, int i2) {
        scrollTo(i, i2);
        updateStateOnScroll(i, i2);
        setPendingContentOffsets(i, i2);
    }

    private void setPendingContentOffsets(int i, int i2) {
        View childAt = getChildAt(0);
        if (childAt == null || childAt.getWidth() == 0 || childAt.getHeight() == 0) {
            this.pendingContentOffsetX = i;
            this.pendingContentOffsetY = i2;
            return;
        }
        this.pendingContentOffsetX = -1;
        this.pendingContentOffsetY = -1;
    }

    public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        int maxScrollY;
        if (this.mContentView != null && getScrollY() > (maxScrollY = getMaxScrollY())) {
            reactScrollTo(getScrollX(), maxScrollY);
        }
    }

    public void setBackgroundColor(int i) {
        this.mReactBackgroundManager.setBackgroundColor(i);
    }

    public void setBorderWidth(int i, float f) {
        this.mReactBackgroundManager.setBorderWidth(i, f);
    }

    public void setBorderColor(int i, float f, float f2) {
        this.mReactBackgroundManager.setBorderColor(i, f, f2);
    }

    public void setBorderRadius(float f) {
        this.mReactBackgroundManager.setBorderRadius(f);
    }

    public void setBorderRadius(float f, int i) {
        this.mReactBackgroundManager.setBorderRadius(f, i);
    }

    public void setBorderStyle(String str) {
        this.mReactBackgroundManager.setBorderStyle(str);
    }

    private void updateStateOnScroll(final int i, final int i2) {
        if (i != this.mLastStateUpdateScrollX || i2 != this.mLastStateUpdateScrollY) {
            this.mLastStateUpdateScrollX = i;
            this.mLastStateUpdateScrollY = i2;
            this.mFabricViewStateManager.setState(new FabricViewStateManager.StateUpdateCallback() {
                /* class com.facebook.react.views.scroll.ReactScrollView.AnonymousClass4 */

                @Override // com.facebook.react.uimanager.FabricViewStateManager.StateUpdateCallback
                public WritableMap getStateUpdate() {
                    WritableNativeMap writableNativeMap = new WritableNativeMap();
                    writableNativeMap.putDouble(ReactScrollView.CONTENT_OFFSET_LEFT, (double) PixelUtil.toDIPFromPixel((float) i));
                    writableNativeMap.putDouble(ReactScrollView.CONTENT_OFFSET_TOP, (double) PixelUtil.toDIPFromPixel((float) i2));
                    return writableNativeMap;
                }
            });
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateStateOnScroll() {
        updateStateOnScroll(getScrollX(), getScrollY());
    }

    @Override // com.facebook.react.uimanager.FabricViewStateManager.HasFabricViewStateManager
    public FabricViewStateManager getFabricViewStateManager() {
        return this.mFabricViewStateManager;
    }
}
