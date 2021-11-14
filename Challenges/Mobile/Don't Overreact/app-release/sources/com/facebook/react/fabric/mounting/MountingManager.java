package com.facebook.react.fabric.mounting;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import com.facebook.common.logging.FLog;
import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.ReactSoftException;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableNativeMap;
import com.facebook.react.bridge.RetryableMountingLayerException;
import com.facebook.react.bridge.SoftAssertions;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.fabric.events.EventEmitterWrapper;
import com.facebook.react.touch.JSResponderHandler;
import com.facebook.react.uimanager.IllegalViewOperationException;
import com.facebook.react.uimanager.ReactStylesDiffMap;
import com.facebook.react.uimanager.RootView;
import com.facebook.react.uimanager.RootViewManager;
import com.facebook.react.uimanager.StateWrapper;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.uimanager.ViewManagerRegistry;
import com.facebook.yoga.YogaMeasureMode;
import java.util.concurrent.ConcurrentHashMap;

public class MountingManager {
    private static final boolean SHOW_CHANGED_VIEW_HIERARCHIES = false;
    public static final String TAG = "MountingManager";
    private final JSResponderHandler mJSResponderHandler = new JSResponderHandler();
    private final RootViewManager mRootViewManager = new RootViewManager();
    private final ConcurrentHashMap<Integer, ViewState> mTagToViewState = new ConcurrentHashMap<>();
    private final ViewManagerRegistry mViewManagerRegistry;

    public MountingManager(ViewManagerRegistry viewManagerRegistry) {
        this.mViewManagerRegistry = viewManagerRegistry;
    }

    private static void logViewHierarchy(ViewGroup viewGroup, boolean z) {
        int i;
        int id = viewGroup.getId();
        String str = TAG;
        FLog.e(str, "  <ViewGroup tag=" + id + " class=" + viewGroup.getClass().toString() + ">");
        for (int i2 = 0; i2 < viewGroup.getChildCount(); i2++) {
            String str2 = TAG;
            FLog.e(str2, "     <View idx=" + i2 + " tag=" + viewGroup.getChildAt(i2).getId() + " class=" + viewGroup.getChildAt(i2).getClass().toString() + ">");
        }
        String str3 = TAG;
        FLog.e(str3, "  </ViewGroup tag=" + id + ">");
        if (z) {
            FLog.e(str3, "Displaying Ancestors:");
            for (ViewParent parent = viewGroup.getParent(); parent != null; parent = parent.getParent()) {
                ViewGroup viewGroup2 = parent instanceof ViewGroup ? (ViewGroup) parent : null;
                if (viewGroup2 == null) {
                    i = -1;
                } else {
                    i = viewGroup2.getId();
                }
                String str4 = TAG;
                FLog.e(str4, "<ViewParent tag=" + i + " class=" + parent.getClass().toString() + ">");
            }
        }
    }

    public void addRootView(int i, View view) {
        if (view.getId() == -1) {
            this.mTagToViewState.put(Integer.valueOf(i), new ViewState(i, view, this.mRootViewManager, true));
            view.setId(i);
            return;
        }
        throw new IllegalViewOperationException("Trying to add a root view with an explicit id already set. React Native uses the id field to track react tags and will overwrite this field. If that is fine, explicitly overwrite the id field to View.NO_ID before calling addRootView.");
    }

    public void deleteRootView(int i) {
        if (this.mTagToViewState.containsKey(Integer.valueOf(i))) {
            dropView(this.mTagToViewState.get(Integer.valueOf(i)).mView, true);
        }
    }

    private void dropView(View view, boolean z) {
        UiThreadUtil.assertOnUiThread();
        final int id = view.getId();
        ViewState viewState = getViewState(id);
        ViewManager viewManager = viewState.mViewManager;
        if (!viewState.mIsRoot && viewManager != null) {
            viewManager.onDropViewInstance(view);
        }
        if ((view instanceof ViewGroup) && (viewManager instanceof ViewGroupManager)) {
            final ViewGroup viewGroup = (ViewGroup) view;
            final ViewGroupManager<ViewGroup> viewGroupManager = getViewGroupManager(viewState);
            if (z) {
                dropChildren(id, viewGroup, viewGroupManager);
            } else {
                UiThreadUtil.runOnUiThread(new Runnable() {
                    /* class com.facebook.react.fabric.mounting.MountingManager.AnonymousClass1 */

                    public void run() {
                        MountingManager.this.dropChildren(id, viewGroup, viewGroupManager);
                    }
                });
            }
        }
        this.mTagToViewState.remove(Integer.valueOf(id));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void dropChildren(int i, ViewGroup viewGroup, ViewGroupManager<ViewGroup> viewGroupManager) {
        for (int childCount = viewGroupManager.getChildCount(viewGroup) - 1; childCount >= 0; childCount--) {
            View childAt = viewGroupManager.getChildAt(viewGroup, childCount);
            if (getNullableViewState(childAt.getId()) != null) {
                ViewParent parent = childAt.getParent();
                if (parent == null || !parent.equals(viewGroup)) {
                    int i2 = -1;
                    if (parent != null && (parent instanceof ViewGroup)) {
                        i2 = ((ViewGroup) parent).getId();
                    }
                    FLog.e(TAG, "Recursively deleting children of [" + i + "] but parent of child [" + childAt.getId() + "] is [" + i2 + "]");
                } else {
                    dropView(childAt, true);
                }
            }
            viewGroupManager.removeViewAt(viewGroup, childCount);
        }
    }

    public void addViewAt(int i, int i2, int i3) {
        UiThreadUtil.assertOnUiThread();
        ViewState viewState = getViewState(i);
        if (viewState.mView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) viewState.mView;
            ViewState viewState2 = getViewState(i2);
            View view = viewState2.mView;
            if (view != null) {
                try {
                    getViewGroupManager(viewState).addView(viewGroup, view, i3);
                } catch (IllegalStateException e) {
                    throw new IllegalStateException("addViewAt: failed to insert view [" + i2 + "] into parent [" + i + "] at index " + i3, e);
                }
            } else {
                throw new IllegalStateException("Unable to find view for viewState " + viewState2 + " and tag " + i2);
            }
        } else {
            String str = "Unable to add a view into a view that is not a ViewGroup. ParentTag: " + i + " - Tag: " + i2 + " - Index: " + i3;
            FLog.e(TAG, str);
            throw new IllegalStateException(str);
        }
    }

    private ViewState getViewState(int i) {
        ViewState viewState = this.mTagToViewState.get(Integer.valueOf(i));
        if (viewState != null) {
            return viewState;
        }
        throw new RetryableMountingLayerException("Unable to find viewState view for tag " + i);
    }

    public boolean getViewExists(int i) {
        return this.mTagToViewState.get(Integer.valueOf(i)) != null;
    }

    private ViewState getNullableViewState(int i) {
        return this.mTagToViewState.get(Integer.valueOf(i));
    }

    @Deprecated
    public void receiveCommand(int i, int i2, ReadableArray readableArray) {
        ViewState nullableViewState = getNullableViewState(i);
        if (nullableViewState == null) {
            throw new RetryableMountingLayerException("Unable to find viewState for tag: " + i + " for commandId: " + i2);
        } else if (nullableViewState.mViewManager == null) {
            throw new RetryableMountingLayerException("Unable to find viewManager for tag " + i);
        } else if (nullableViewState.mView != null) {
            nullableViewState.mViewManager.receiveCommand(nullableViewState.mView, i2, readableArray);
        } else {
            throw new RetryableMountingLayerException("Unable to find viewState view for tag " + i);
        }
    }

    public void receiveCommand(int i, String str, ReadableArray readableArray) {
        ViewState nullableViewState = getNullableViewState(i);
        if (nullableViewState == null) {
            throw new RetryableMountingLayerException("Unable to find viewState for tag: " + i + " for commandId: " + str);
        } else if (nullableViewState.mViewManager == null) {
            throw new RetryableMountingLayerException("Unable to find viewState manager for tag " + i);
        } else if (nullableViewState.mView != null) {
            nullableViewState.mViewManager.receiveCommand(nullableViewState.mView, str, readableArray);
        } else {
            throw new RetryableMountingLayerException("Unable to find viewState view for tag " + i);
        }
    }

    public void sendAccessibilityEvent(int i, int i2) {
        ViewState viewState = getViewState(i);
        if (viewState.mViewManager == null) {
            throw new RetryableMountingLayerException("Unable to find viewState manager for tag " + i);
        } else if (viewState.mView != null) {
            viewState.mView.sendAccessibilityEvent(i2);
        } else {
            throw new RetryableMountingLayerException("Unable to find viewState view for tag " + i);
        }
    }

    private static ViewGroupManager<ViewGroup> getViewGroupManager(ViewState viewState) {
        if (viewState.mViewManager != null) {
            return (ViewGroupManager) viewState.mViewManager;
        }
        throw new IllegalStateException("Unable to find ViewManager for view: " + viewState);
    }

    public void removeViewAt(int i, int i2, int i3) {
        UiThreadUtil.assertOnUiThread();
        ViewState nullableViewState = getNullableViewState(i2);
        if (nullableViewState == null) {
            String str = TAG;
            ReactSoftException.logSoftException(str, new IllegalStateException("Unable to find viewState for tag: " + i2 + " for removeViewAt"));
            return;
        }
        ViewGroup viewGroup = (ViewGroup) nullableViewState.mView;
        if (viewGroup != null) {
            ViewGroupManager<ViewGroup> viewGroupManager = getViewGroupManager(nullableViewState);
            View childAt = viewGroupManager.getChildAt(viewGroup, i3);
            int id = childAt != null ? childAt.getId() : -1;
            if (id != i) {
                int childCount = viewGroup.getChildCount();
                int i4 = 0;
                while (true) {
                    if (i4 >= childCount) {
                        i4 = -1;
                        break;
                    } else if (viewGroup.getChildAt(i4).getId() == i) {
                        break;
                    } else {
                        i4++;
                    }
                }
                if (i4 == -1) {
                    String str2 = TAG;
                    FLog.e(str2, "removeViewAt: [" + i + "] -> [" + i2 + "] @" + i3 + ": view already removed from parent! Children in parent: " + childCount);
                    return;
                }
                logViewHierarchy(viewGroup, true);
                String str3 = TAG;
                ReactSoftException.logSoftException(str3, new IllegalStateException("Tried to remove view [" + i + "] of parent [" + i2 + "] at index " + i3 + ", but got view tag " + id + " - actual index of view: " + i4));
                i3 = i4;
            }
            try {
                viewGroupManager.removeViewAt(viewGroup, i3);
            } catch (RuntimeException e) {
                int childCount2 = viewGroupManager.getChildCount(viewGroup);
                logViewHierarchy(viewGroup, true);
                throw new IllegalStateException("Cannot remove child at index " + i3 + " from parent ViewGroup [" + viewGroup.getId() + "], only " + childCount2 + " children in parent. Warning: childCount may be incorrect!", e);
            }
        } else {
            throw new IllegalStateException("Unable to find view for tag " + i2);
        }
    }

    public void createView(ThemedReactContext themedReactContext, String str, int i, ReadableMap readableMap, StateWrapper stateWrapper, boolean z) {
        ViewManager viewManager;
        View view;
        if (getNullableViewState(i) == null) {
            ReadableNativeMap readableNativeMap = null;
            ReactStylesDiffMap reactStylesDiffMap = readableMap != null ? new ReactStylesDiffMap(readableMap) : null;
            if (z) {
                viewManager = this.mViewManagerRegistry.get(str);
                view = viewManager.createView(themedReactContext, reactStylesDiffMap, stateWrapper, this.mJSResponderHandler);
                view.setId(i);
            } else {
                view = null;
                viewManager = null;
            }
            ViewState viewState = new ViewState(i, view, viewManager);
            viewState.mCurrentProps = reactStylesDiffMap;
            if (stateWrapper != null) {
                readableNativeMap = stateWrapper.getState();
            }
            viewState.mCurrentState = readableNativeMap;
            this.mTagToViewState.put(Integer.valueOf(i), viewState);
        }
    }

    public void updateProps(int i, ReadableMap readableMap) {
        if (readableMap != null) {
            UiThreadUtil.assertOnUiThread();
            ViewState viewState = getViewState(i);
            viewState.mCurrentProps = new ReactStylesDiffMap(readableMap);
            View view = viewState.mView;
            if (view != null) {
                ((ViewManager) Assertions.assertNotNull(viewState.mViewManager)).updateProperties(view, viewState.mCurrentProps);
                return;
            }
            throw new IllegalStateException("Unable to find view for tag " + i);
        }
    }

    public void updateLayout(int i, int i2, int i3, int i4, int i5) {
        UiThreadUtil.assertOnUiThread();
        ViewState viewState = getViewState(i);
        if (!viewState.mIsRoot) {
            View view = viewState.mView;
            if (view != null) {
                view.measure(View.MeasureSpec.makeMeasureSpec(i4, 1073741824), View.MeasureSpec.makeMeasureSpec(i5, 1073741824));
                ViewParent parent = view.getParent();
                if (parent instanceof RootView) {
                    parent.requestLayout();
                }
                view.layout(i2, i3, i4 + i2, i5 + i3);
                return;
            }
            throw new IllegalStateException("Unable to find View for tag: " + i);
        }
    }

    public void updatePadding(int i, int i2, int i3, int i4, int i5) {
        UiThreadUtil.assertOnUiThread();
        ViewState viewState = getViewState(i);
        if (!viewState.mIsRoot) {
            View view = viewState.mView;
            if (view != null) {
                ViewManager viewManager = viewState.mViewManager;
                if (viewManager != null) {
                    viewManager.setPadding(view, i2, i3, i4, i5);
                    return;
                }
                throw new IllegalStateException("Unable to find ViewManager for view: " + viewState);
            }
            throw new IllegalStateException("Unable to find View for tag: " + i);
        }
    }

    public void deleteView(int i) {
        UiThreadUtil.assertOnUiThread();
        ViewState nullableViewState = getNullableViewState(i);
        if (nullableViewState == null) {
            String str = TAG;
            ReactSoftException.logSoftException(str, new IllegalStateException("Unable to find viewState for tag: " + i + " for deleteView"));
            return;
        }
        this.mTagToViewState.remove(Integer.valueOf(i));
        ViewManager viewManager = nullableViewState.mViewManager;
        if (!nullableViewState.mIsRoot && viewManager != null) {
            viewManager.onDropViewInstance(nullableViewState.mView);
        }
    }

    public void updateState(int i, StateWrapper stateWrapper) {
        ReadableNativeMap readableNativeMap;
        UiThreadUtil.assertOnUiThread();
        ViewState viewState = getViewState(i);
        if (stateWrapper == null) {
            readableNativeMap = null;
        } else {
            readableNativeMap = stateWrapper.getState();
        }
        viewState.mCurrentState = readableNativeMap;
        ViewManager viewManager = viewState.mViewManager;
        if (viewManager != null) {
            Object updateState = viewManager.updateState(viewState.mView, viewState.mCurrentProps, stateWrapper);
            if (updateState != null) {
                viewManager.updateExtraData(viewState.mView, updateState);
                return;
            }
            return;
        }
        throw new IllegalStateException("Unable to find ViewManager for tag: " + i);
    }

    public void preallocateView(ThemedReactContext themedReactContext, String str, int i, ReadableMap readableMap, StateWrapper stateWrapper, boolean z) {
        if (getNullableViewState(i) == null) {
            createView(themedReactContext, str, i, readableMap, stateWrapper, z);
            return;
        }
        throw new IllegalStateException("View for component " + str + " with tag " + i + " already exists.");
    }

    public void updateEventEmitter(int i, EventEmitterWrapper eventEmitterWrapper) {
        UiThreadUtil.assertOnUiThread();
        ViewState viewState = this.mTagToViewState.get(Integer.valueOf(i));
        if (viewState == null) {
            viewState = new ViewState(i, (View) null, (ViewManager) null);
            this.mTagToViewState.put(Integer.valueOf(i), viewState);
        }
        viewState.mEventEmitter = eventEmitterWrapper;
    }

    public synchronized void setJSResponder(int i, int i2, boolean z) {
        if (!z) {
            this.mJSResponderHandler.setJSResponder(i2, null);
            return;
        }
        ViewState viewState = getViewState(i);
        View view = viewState.mView;
        if (i2 != i && (view instanceof ViewParent)) {
            this.mJSResponderHandler.setJSResponder(i2, (ViewParent) view);
        } else if (view == null) {
            SoftAssertions.assertUnreachable("Cannot find view for tag " + i + ".");
        } else {
            if (viewState.mIsRoot) {
                SoftAssertions.assertUnreachable("Cannot block native responder on " + i + " that is a root view");
            }
            this.mJSResponderHandler.setJSResponder(i2, view.getParent());
        }
    }

    public void clearJSResponder() {
        this.mJSResponderHandler.clearJSResponder();
    }

    public long measure(Context context, String str, ReadableMap readableMap, ReadableMap readableMap2, ReadableMap readableMap3, float f, YogaMeasureMode yogaMeasureMode, float f2, YogaMeasureMode yogaMeasureMode2, float[] fArr) {
        return this.mViewManagerRegistry.get(str).measure(context, readableMap, readableMap2, readableMap3, f, yogaMeasureMode, f2, yogaMeasureMode2, fArr);
    }

    public EventEmitterWrapper getEventEmitter(int i) {
        ViewState nullableViewState = getNullableViewState(i);
        if (nullableViewState == null) {
            return null;
        }
        return nullableViewState.mEventEmitter;
    }

    /* access modifiers changed from: private */
    public static class ViewState {
        public ReadableMap mCurrentLocalData;
        public ReactStylesDiffMap mCurrentProps;
        public ReadableMap mCurrentState;
        public EventEmitterWrapper mEventEmitter;
        final boolean mIsRoot;
        final int mReactTag;
        final View mView;
        final ViewManager mViewManager;

        private ViewState(int i, View view, ViewManager viewManager) {
            this(i, view, viewManager, false);
        }

        private ViewState(int i, View view, ViewManager viewManager, boolean z) {
            this.mCurrentProps = null;
            this.mCurrentLocalData = null;
            this.mCurrentState = null;
            this.mEventEmitter = null;
            this.mReactTag = i;
            this.mView = view;
            this.mIsRoot = z;
            this.mViewManager = viewManager;
        }

        public String toString() {
            boolean z = this.mViewManager == null;
            return "ViewState [" + this.mReactTag + "] - isRoot: " + this.mIsRoot + " - props: " + this.mCurrentProps + " - localData: " + this.mCurrentLocalData + " - viewManager: " + this.mViewManager + " - isLayoutOnly: " + z;
        }
    }
}
