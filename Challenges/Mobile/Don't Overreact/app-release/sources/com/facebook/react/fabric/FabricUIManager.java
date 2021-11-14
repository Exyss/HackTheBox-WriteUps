package com.facebook.react.fabric;

import android.content.Context;
import android.graphics.Point;
import android.os.SystemClock;
import android.view.View;
import com.facebook.common.logging.FLog;
import com.facebook.debug.holder.PrinterHolder;
import com.facebook.debug.tags.ReactDebugOverlayTags;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.NativeArray;
import com.facebook.react.bridge.NativeMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactIgnorableMountingException;
import com.facebook.react.bridge.ReactMarker;
import com.facebook.react.bridge.ReactMarkerConstants;
import com.facebook.react.bridge.ReactNoCrashSoftException;
import com.facebook.react.bridge.ReactSoftException;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.RetryableMountingLayerException;
import com.facebook.react.bridge.UIManager;
import com.facebook.react.bridge.UIManagerListener;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.config.ReactFeatureFlags;
import com.facebook.react.fabric.events.EventBeatManager;
import com.facebook.react.fabric.events.EventEmitterWrapper;
import com.facebook.react.fabric.events.FabricEventEmitter;
import com.facebook.react.fabric.mounting.LayoutMetricsConversions;
import com.facebook.react.fabric.mounting.MountingManager;
import com.facebook.react.fabric.mounting.mountitems.BatchMountItem;
import com.facebook.react.fabric.mounting.mountitems.CreateMountItem;
import com.facebook.react.fabric.mounting.mountitems.DispatchCommandMountItem;
import com.facebook.react.fabric.mounting.mountitems.DispatchIntCommandMountItem;
import com.facebook.react.fabric.mounting.mountitems.DispatchStringCommandMountItem;
import com.facebook.react.fabric.mounting.mountitems.InsertMountItem;
import com.facebook.react.fabric.mounting.mountitems.IntBufferBatchMountItem;
import com.facebook.react.fabric.mounting.mountitems.MountItem;
import com.facebook.react.fabric.mounting.mountitems.PreAllocateViewMountItem;
import com.facebook.react.fabric.mounting.mountitems.RemoveDeleteMultiMountItem;
import com.facebook.react.fabric.mounting.mountitems.SendAccessibilityEvent;
import com.facebook.react.fabric.mounting.mountitems.UpdateEventEmitterMountItem;
import com.facebook.react.fabric.mounting.mountitems.UpdateLayoutMountItem;
import com.facebook.react.fabric.mounting.mountitems.UpdatePaddingMountItem;
import com.facebook.react.fabric.mounting.mountitems.UpdatePropsMountItem;
import com.facebook.react.fabric.mounting.mountitems.UpdateStateMountItem;
import com.facebook.react.modules.core.ReactChoreographer;
import com.facebook.react.modules.i18nmanager.I18nUtil;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.ReactRoot;
import com.facebook.react.uimanager.ReactRootViewTagGenerator;
import com.facebook.react.uimanager.StateWrapper;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerHelper;
import com.facebook.react.uimanager.ViewManagerPropertyUpdater;
import com.facebook.react.uimanager.ViewManagerRegistry;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.views.text.TextLayoutManager;
import com.facebook.react.views.textinput.ReactEditTextInputConnectionWrapper;
import com.facebook.systrace.Systrace;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FabricUIManager implements UIManager, LifecycleEventListener {
    public static final boolean ENABLE_FABRIC_LOGS = (ReactFeatureFlags.enableFabricLogs || PrinterHolder.getPrinter().shouldDisplayLogMessage(ReactDebugOverlayTags.FABRIC_UI_MANAGER));
    private static final int FRAME_TIME_MS = 16;
    public static final boolean IS_DEVELOPMENT_ENVIRONMENT = false;
    private static final int MAX_TIME_IN_FRAME_FOR_NON_BATCHED_OPERATIONS_MS = 8;
    private static final int PRE_MOUNT_ITEMS_INITIAL_SIZE_ARRAY = 250;
    public static final String TAG = "FabricUIManager";
    private long mBatchedExecutionTime = 0;
    private Binding mBinding;
    private long mCommitStartTime = 0;
    private int mCurrentSynchronousCommitNumber = 10000;
    private volatile boolean mDestroyed = false;
    private final DispatchUIFrameCallback mDispatchUIFrameCallback;
    private long mDispatchViewUpdatesTime = 0;
    private boolean mDriveCxxAnimations = false;
    private final EventBeatManager mEventBeatManager;
    private final EventDispatcher mEventDispatcher;
    private long mFinishTransactionCPPTime = 0;
    private long mFinishTransactionTime = 0;
    private boolean mInDispatch = false;
    private int mLastExecutedMountItemSurfaceId = -1;
    private boolean mLastExecutedMountItemSurfaceIdActive = false;
    private long mLayoutTime = 0;
    private final CopyOnWriteArrayList<UIManagerListener> mListeners = new CopyOnWriteArrayList<>();
    private List<MountItem> mMountItems = new ArrayList();
    private final Object mMountItemsLock = new Object();
    private final MountingManager mMountingManager;
    private ArrayDeque<PreAllocateViewMountItem> mPreMountItems = new ArrayDeque<>((int) PRE_MOUNT_ITEMS_INITIAL_SIZE_ARRAY);
    private final Object mPreMountItemsLock = new Object();
    private int mReDispatchCounter = 0;
    private final ReactApplicationContext mReactApplicationContext;
    private final ConcurrentHashMap<Integer, ThemedReactContext> mReactContextForRootTag = new ConcurrentHashMap<>();
    private long mRunStartTime = 0;
    private List<DispatchCommandMountItem> mViewCommandMountItems = new ArrayList();
    private final Object mViewCommandMountItemsLock = new Object();

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostDestroy() {
    }

    @Override // com.facebook.react.bridge.PerformanceCounter
    public void profileNextBatch() {
    }

    static {
        FabricSoLoader.staticInit();
    }

    public FabricUIManager(ReactApplicationContext reactApplicationContext, ViewManagerRegistry viewManagerRegistry, EventDispatcher eventDispatcher, EventBeatManager eventBeatManager) {
        this.mDispatchUIFrameCallback = new DispatchUIFrameCallback(reactApplicationContext);
        this.mReactApplicationContext = reactApplicationContext;
        this.mMountingManager = new MountingManager(viewManagerRegistry);
        this.mEventDispatcher = eventDispatcher;
        this.mEventBeatManager = eventBeatManager;
        reactApplicationContext.addLifecycleEventListener(this);
    }

    @Override // com.facebook.react.bridge.UIManager
    public <T extends View> int addRootView(T t, WritableMap writableMap, String str) {
        int nextRootViewTag = ReactRootViewTagGenerator.getNextRootViewTag();
        ReactRoot reactRoot = (ReactRoot) t;
        ThemedReactContext themedReactContext = new ThemedReactContext(this.mReactApplicationContext, t.getContext(), reactRoot.getSurfaceID());
        this.mMountingManager.addRootView(nextRootViewTag, t);
        String jSModuleName = reactRoot.getJSModuleName();
        this.mReactContextForRootTag.put(Integer.valueOf(nextRootViewTag), themedReactContext);
        if (ENABLE_FABRIC_LOGS) {
            FLog.d(TAG, "Starting surface for module: %s and reactTag: %d", jSModuleName, Integer.valueOf(nextRootViewTag));
        }
        this.mBinding.startSurface(nextRootViewTag, jSModuleName, (NativeMap) writableMap);
        if (str != null) {
            this.mBinding.renderTemplateToSurface(nextRootViewTag, str);
        }
        return nextRootViewTag;
    }

    @Override // com.facebook.react.bridge.UIManager
    public <T extends View> int startSurface(T t, String str, WritableMap writableMap, int i, int i2) {
        int nextRootViewTag = ReactRootViewTagGenerator.getNextRootViewTag();
        Context context = t.getContext();
        ThemedReactContext themedReactContext = new ThemedReactContext(this.mReactApplicationContext, context, str);
        if (ENABLE_FABRIC_LOGS) {
            FLog.d(TAG, "Starting surface for module: %s and reactTag: %d", str, Integer.valueOf(nextRootViewTag));
        }
        this.mMountingManager.addRootView(nextRootViewTag, t);
        this.mReactContextForRootTag.put(Integer.valueOf(nextRootViewTag), themedReactContext);
        Point viewportOffset = ReactRootView.getViewportOffset(t);
        this.mBinding.startSurfaceWithConstraints(nextRootViewTag, str, (NativeMap) writableMap, LayoutMetricsConversions.getMinSize(i), LayoutMetricsConversions.getMaxSize(i), LayoutMetricsConversions.getMinSize(i2), LayoutMetricsConversions.getMaxSize(i2), (float) viewportOffset.x, (float) viewportOffset.y, I18nUtil.getInstance().isRTL(context), I18nUtil.getInstance().doLeftAndRightSwapInRTL(context));
        return nextRootViewTag;
    }

    public void onRequestEventBeat() {
        this.mEventDispatcher.dispatchAllEvents();
    }

    @Override // com.facebook.react.bridge.UIManager
    public void stopSurface(final int i) {
        this.mReactContextForRootTag.remove(Integer.valueOf(i));
        this.mBinding.stopSurface(i);
        UiThreadUtil.runOnUiThread(new Runnable() {
            /* class com.facebook.react.fabric.FabricUIManager.AnonymousClass1 */

            public void run() {
                FabricUIManager.this.mMountingManager.deleteRootView(i);
            }
        });
    }

    @Override // com.facebook.react.bridge.JSIModule
    public void initialize() {
        this.mEventDispatcher.registerEventEmitter(2, new FabricEventEmitter(this));
        this.mEventDispatcher.addBatchEventDispatchedListener(this.mEventBeatManager);
    }

    @Override // com.facebook.react.bridge.JSIModule
    public void onCatalystInstanceDestroy() {
        FLog.i(TAG, "FabricUIManager.onCatalystInstanceDestroy");
        if (this.mDestroyed) {
            ReactSoftException.logSoftException(TAG, new IllegalStateException("Cannot double-destroy FabricUIManager"));
            return;
        }
        this.mDestroyed = true;
        this.mDispatchUIFrameCallback.stop();
        this.mEventDispatcher.removeBatchEventDispatchedListener(this.mEventBeatManager);
        this.mEventDispatcher.unregisterEventEmitter(2);
        this.mReactApplicationContext.removeLifecycleEventListener(this);
        onHostPause();
        this.mDispatchUIFrameCallback.stop();
        this.mBinding.unregister();
        this.mBinding = null;
        ViewManagerPropertyUpdater.clear();
    }

    private void preallocateView(int i, int i2, String str, ReadableMap readableMap, Object obj, boolean z) {
        ThemedReactContext themedReactContext = this.mReactContextForRootTag.get(Integer.valueOf(i));
        String fabricComponentName = FabricComponents.getFabricComponentName(str);
        synchronized (this.mPreMountItemsLock) {
            this.mPreMountItems.add(new PreAllocateViewMountItem(themedReactContext, i, i2, fabricComponentName, readableMap, (StateWrapper) obj, z));
        }
    }

    private MountItem createMountItem(String str, ReadableMap readableMap, Object obj, int i, int i2, boolean z) {
        return new CreateMountItem(this.mReactContextForRootTag.get(Integer.valueOf(i)), i, i2, FabricComponents.getFabricComponentName(str), readableMap, (StateWrapper) obj, z);
    }

    private MountItem insertMountItem(int i, int i2, int i3) {
        return new InsertMountItem(i, i2, i3);
    }

    private MountItem removeDeleteMultiMountItem(int[] iArr) {
        return new RemoveDeleteMultiMountItem(iArr);
    }

    private MountItem updateLayoutMountItem(int i, int i2, int i3, int i4, int i5, int i6) {
        return new UpdateLayoutMountItem(i, i2, i3, i4, i5, i6);
    }

    private MountItem updatePaddingMountItem(int i, int i2, int i3, int i4, int i5) {
        return new UpdatePaddingMountItem(i, i2, i3, i4, i5);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private MountItem updatePropsMountItem(int i, ReadableMap readableMap) {
        return new UpdatePropsMountItem(i, readableMap);
    }

    private MountItem updateStateMountItem(int i, Object obj) {
        return new UpdateStateMountItem(i, (StateWrapper) obj);
    }

    private MountItem updateEventEmitterMountItem(int i, Object obj) {
        return new UpdateEventEmitterMountItem(i, (EventEmitterWrapper) obj);
    }

    private MountItem createBatchMountItem(int i, MountItem[] mountItemArr, int i2, int i3) {
        return new BatchMountItem(i, mountItemArr, i2, i3);
    }

    private MountItem createIntBufferBatchMountItem(int i, int[] iArr, Object[] objArr, int i2) {
        return new IntBufferBatchMountItem(i, this.mReactContextForRootTag.get(Integer.valueOf(i)), iArr, objArr, i2);
    }

    private NativeArray measureLines(ReadableMap readableMap, ReadableMap readableMap2, float f, float f2) {
        return (NativeArray) TextLayoutManager.measureLines(this.mReactApplicationContext, readableMap, readableMap2, PixelUtil.toPixelFromDIP(f));
    }

    private long measure(int i, String str, ReadableMap readableMap, ReadableMap readableMap2, ReadableMap readableMap3, float f, float f2, float f3, float f4) {
        return measure(i, str, readableMap, readableMap2, readableMap3, f, f2, f3, f4, null);
    }

    private long measure(int i, String str, ReadableMap readableMap, ReadableMap readableMap2, ReadableMap readableMap3, float f, float f2, float f3, float f4, float[] fArr) {
        ThemedReactContext themedReactContext;
        if (i < 0) {
            themedReactContext = this.mReactApplicationContext;
        } else {
            themedReactContext = this.mReactContextForRootTag.get(Integer.valueOf(i));
        }
        if (themedReactContext == null) {
            return 0;
        }
        return this.mMountingManager.measure(themedReactContext, str, readableMap, readableMap2, readableMap3, LayoutMetricsConversions.getYogaSize(f, f2), LayoutMetricsConversions.getYogaMeasureMode(f, f2), LayoutMetricsConversions.getYogaSize(f3, f4), LayoutMetricsConversions.getYogaMeasureMode(f3, f4), fArr);
    }

    public boolean getThemeData(int i, float[] fArr) {
        ThemedReactContext themedReactContext = this.mReactContextForRootTag.get(Integer.valueOf(i));
        if (themedReactContext == null) {
            ReactSoftException.logSoftException(TAG, new ReactNoCrashSoftException("Unable to find ThemedReactContext associated to surfaceID: " + i));
            return false;
        }
        float[] defaultTextInputPadding = UIManagerHelper.getDefaultTextInputPadding(themedReactContext);
        fArr[0] = defaultTextInputPadding[0];
        fArr[1] = defaultTextInputPadding[1];
        fArr[2] = defaultTextInputPadding[2];
        fArr[3] = defaultTextInputPadding[3];
        return true;
    }

    @Override // com.facebook.react.bridge.UIManager
    public void synchronouslyUpdateViewOnUIThread(final int i, final ReadableMap readableMap) {
        UiThreadUtil.assertOnUiThread();
        int i2 = this.mCurrentSynchronousCommitNumber;
        this.mCurrentSynchronousCommitNumber = i2 + 1;
        if (!ReactFeatureFlags.enableDrawMutationFix) {
            tryDispatchMountItems();
        }
        AnonymousClass2 r1 = new MountItem() {
            /* class com.facebook.react.fabric.FabricUIManager.AnonymousClass2 */

            @Override // com.facebook.react.fabric.mounting.mountitems.MountItem
            public void execute(MountingManager mountingManager) {
                try {
                    FabricUIManager.this.updatePropsMountItem(i, readableMap).execute(mountingManager);
                } catch (Exception e) {
                    ReactSoftException.logSoftException(FabricUIManager.TAG, new ReactNoCrashSoftException("Caught exception in synchronouslyUpdateViewOnUIThread", e));
                }
            }
        };
        if (!this.mMountingManager.getViewExists(i)) {
            synchronized (this.mMountItemsLock) {
                this.mMountItems.add(r1);
            }
            return;
        }
        ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_UPDATE_UI_MAIN_THREAD_START, null, i2);
        if (ENABLE_FABRIC_LOGS) {
            FLog.d(TAG, "SynchronouslyUpdateViewOnUIThread for tag %d: %s", Integer.valueOf(i), "<hidden>");
        }
        r1.execute(this.mMountingManager);
        ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_UPDATE_UI_MAIN_THREAD_END, null, i2);
    }

    @Override // com.facebook.react.bridge.UIManager
    public void addUIManagerEventListener(UIManagerListener uIManagerListener) {
        this.mListeners.add(uIManagerListener);
    }

    @Override // com.facebook.react.bridge.UIManager
    public void removeUIManagerEventListener(UIManagerListener uIManagerListener) {
        this.mListeners.remove(uIManagerListener);
    }

    private void scheduleMountItem(MountItem mountItem, int i, long j, long j2, long j3, long j4, long j5, long j6, long j7) {
        boolean z = mountItem instanceof BatchMountItem;
        boolean z2 = mountItem instanceof IntBufferBatchMountItem;
        boolean z3 = false;
        boolean z4 = z || z2;
        if ((z && ((BatchMountItem) mountItem).shouldSchedule()) || ((z2 && ((IntBufferBatchMountItem) mountItem).shouldSchedule()) || (!z4 && mountItem != null))) {
            z3 = true;
        }
        Iterator<UIManagerListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().didScheduleMountItems(this);
        }
        if (z4) {
            this.mCommitStartTime = j;
            this.mLayoutTime = j5 - j4;
            this.mFinishTransactionCPPTime = j7 - j6;
            this.mFinishTransactionTime = SystemClock.uptimeMillis() - j6;
            this.mDispatchViewUpdatesTime = SystemClock.uptimeMillis();
        }
        if (z3 && mountItem != null) {
            synchronized (this.mMountItemsLock) {
                this.mMountItems.add(mountItem);
            }
            if (UiThreadUtil.isOnUiThread()) {
                tryDispatchMountItems();
            }
        }
        if (z4) {
            ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_COMMIT_START, null, i, j);
            ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_FINISH_TRANSACTION_START, null, i, j6);
            ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_FINISH_TRANSACTION_END, null, i, j7);
            ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_DIFF_START, null, i, j2);
            ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_DIFF_END, null, i, j3);
            ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_LAYOUT_START, null, i, j4);
            ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_LAYOUT_END, null, i, j5);
            ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_COMMIT_END, null, i);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void tryDispatchMountItems() {
        if (!this.mInDispatch) {
            try {
                boolean dispatchMountItems = dispatchMountItems();
                this.mInDispatch = false;
                Iterator<UIManagerListener> it = this.mListeners.iterator();
                while (it.hasNext()) {
                    it.next().didDispatchMountItems(this);
                }
                int i = this.mReDispatchCounter;
                if (i < 10 && dispatchMountItems) {
                    if (i > 2) {
                        ReactSoftException.logSoftException(TAG, new ReactNoCrashSoftException("Re-dispatched " + this.mReDispatchCounter + " times. This indicates setState (?) is likely being called too many times during mounting."));
                    }
                    this.mReDispatchCounter++;
                    tryDispatchMountItems();
                }
                this.mReDispatchCounter = 0;
                this.mLastExecutedMountItemSurfaceId = -1;
            } catch (Throwable th) {
                this.mInDispatch = false;
                throw th;
            }
        }
    }

    private List<DispatchCommandMountItem> getAndResetViewCommandMountItems() {
        synchronized (this.mViewCommandMountItemsLock) {
            List<DispatchCommandMountItem> list = this.mViewCommandMountItems;
            if (list.isEmpty()) {
                return null;
            }
            this.mViewCommandMountItems = new ArrayList();
            return list;
        }
    }

    private List<MountItem> getAndResetMountItems() {
        synchronized (this.mMountItemsLock) {
            List<MountItem> list = this.mMountItems;
            if (list.isEmpty()) {
                return null;
            }
            this.mMountItems = new ArrayList();
            return list;
        }
    }

    private ArrayDeque<PreAllocateViewMountItem> getAndResetPreMountItems() {
        synchronized (this.mPreMountItemsLock) {
            ArrayDeque<PreAllocateViewMountItem> arrayDeque = this.mPreMountItems;
            if (arrayDeque.isEmpty()) {
                return null;
            }
            this.mPreMountItems = new ArrayDeque<>((int) PRE_MOUNT_ITEMS_INITIAL_SIZE_ARRAY);
            return arrayDeque;
        }
    }

    private boolean surfaceActiveForExecution(int i, String str) {
        if (this.mLastExecutedMountItemSurfaceId != i) {
            this.mLastExecutedMountItemSurfaceId = i;
            boolean z = this.mReactContextForRootTag.get(Integer.valueOf(i)) != null;
            this.mLastExecutedMountItemSurfaceIdActive = z;
            if (!z) {
                ReactSoftException.logSoftException(TAG, new ReactNoCrashSoftException("dispatchMountItems: skipping " + str + ", because surface not available: " + i));
            }
        }
        return this.mLastExecutedMountItemSurfaceIdActive;
    }

    private static void printMountItem(MountItem mountItem, String str) {
        String[] split = mountItem.toString().split(ReactEditTextInputConnectionWrapper.NEWLINE_RAW_VALUE);
        for (String str2 : split) {
            FLog.e(TAG, str + ": " + str2);
        }
    }

    private boolean dispatchMountItems() {
        if (this.mReDispatchCounter == 0) {
            this.mBatchedExecutionTime = 0;
        }
        this.mRunStartTime = SystemClock.uptimeMillis();
        List<DispatchCommandMountItem> andResetViewCommandMountItems = getAndResetViewCommandMountItems();
        List<MountItem> andResetMountItems = getAndResetMountItems();
        if (andResetMountItems == null && andResetViewCommandMountItems == null) {
            return false;
        }
        if (andResetViewCommandMountItems != null) {
            Systrace.beginSection(0, "FabricUIManager::mountViews viewCommandMountItems to execute: " + andResetViewCommandMountItems.size());
            for (DispatchCommandMountItem dispatchCommandMountItem : andResetViewCommandMountItems) {
                if (ENABLE_FABRIC_LOGS) {
                    printMountItem(dispatchCommandMountItem, "dispatchMountItems: Executing viewCommandMountItem");
                }
                try {
                    dispatchCommandMountItem.execute(this.mMountingManager);
                } catch (RetryableMountingLayerException e) {
                    if (dispatchCommandMountItem.getRetries() == 0) {
                        dispatchCommandMountItem.incrementRetries();
                        dispatchCommandMountItem(dispatchCommandMountItem);
                    } else {
                        ReactSoftException.logSoftException(TAG, new ReactNoCrashSoftException("Caught exception executing ViewCommand: " + dispatchCommandMountItem.toString(), e));
                    }
                } catch (Throwable th) {
                    ReactSoftException.logSoftException(TAG, new RuntimeException("Caught exception executing ViewCommand: " + dispatchCommandMountItem.toString(), th));
                }
            }
            Systrace.endSection(0);
        }
        ArrayDeque<PreAllocateViewMountItem> andResetPreMountItems = getAndResetPreMountItems();
        if (andResetPreMountItems != null) {
            Systrace.beginSection(0, "FabricUIManager::mountViews preMountItems to execute: " + andResetPreMountItems.size());
            while (!andResetPreMountItems.isEmpty()) {
                PreAllocateViewMountItem pollFirst = andResetPreMountItems.pollFirst();
                if (surfaceActiveForExecution(pollFirst.getRootTag(), "dispatchMountItems PreAllocateViewMountItem")) {
                    pollFirst.execute(this.mMountingManager);
                }
            }
            Systrace.endSection(0);
        }
        if (andResetMountItems != null) {
            Systrace.beginSection(0, "FabricUIManager::mountViews mountItems to execute: " + andResetMountItems.size());
            long uptimeMillis = SystemClock.uptimeMillis();
            for (MountItem mountItem : andResetMountItems) {
                if (ENABLE_FABRIC_LOGS) {
                    printMountItem(mountItem, "dispatchMountItems: Executing mountItem");
                }
                try {
                    if (!(mountItem instanceof BatchMountItem) || surfaceActiveForExecution(((BatchMountItem) mountItem).getRootTag(), "dispatchMountItems BatchMountItem")) {
                        mountItem.execute(this.mMountingManager);
                    }
                } catch (Throwable th2) {
                    FLog.e(TAG, "dispatchMountItems: caught exception, displaying all MountItems", th2);
                    for (MountItem mountItem2 : andResetMountItems) {
                        printMountItem(mountItem2, "dispatchMountItems: mountItem");
                    }
                    if (ReactIgnorableMountingException.isIgnorable(th2)) {
                        ReactSoftException.logSoftException(TAG, th2);
                    } else {
                        throw th2;
                    }
                }
            }
            this.mBatchedExecutionTime += SystemClock.uptimeMillis() - uptimeMillis;
        }
        Systrace.endSection(0);
        return true;
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0046, code lost:
        if (surfaceActiveForExecution(r4.getRootTag(), "dispatchPreMountItems") == false) goto L_0x000a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0048, code lost:
        r4.execute(r10.mMountingManager);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void dispatchPreMountItems(long r11) {
        /*
            r10 = this;
            java.lang.String r0 = "FabricUIManager::premountViews"
            r1 = 0
            com.facebook.systrace.Systrace.beginSection(r1, r0)
            r0 = 1
            r10.mInDispatch = r0
        L_0x000a:
            r3 = 16
            r0 = -1
            r5 = 0
            long r6 = java.lang.System.nanoTime()     // Catch:{ all -> 0x0051 }
            long r6 = r6 - r11
            r8 = 1000000(0xf4240, double:4.940656E-318)
            long r6 = r6 / r8
            long r3 = r3 - r6
            r6 = 8
            int r8 = (r3 > r6 ? 1 : (r3 == r6 ? 0 : -1))
            if (r8 >= 0) goto L_0x001f
            goto L_0x002b
        L_0x001f:
            java.lang.Object r3 = r10.mPreMountItemsLock     // Catch:{ all -> 0x0051 }
            monitor-enter(r3)     // Catch:{ all -> 0x0051 }
            java.util.ArrayDeque<com.facebook.react.fabric.mounting.mountitems.PreAllocateViewMountItem> r4 = r10.mPreMountItems     // Catch:{ all -> 0x004e }
            boolean r4 = r4.isEmpty()     // Catch:{ all -> 0x004e }
            if (r4 == 0) goto L_0x0033
            monitor-exit(r3)     // Catch:{ all -> 0x004e }
        L_0x002b:
            r10.mInDispatch = r5
            r10.mLastExecutedMountItemSurfaceId = r0
            com.facebook.systrace.Systrace.endSection(r1)
            return
        L_0x0033:
            java.util.ArrayDeque<com.facebook.react.fabric.mounting.mountitems.PreAllocateViewMountItem> r4 = r10.mPreMountItems
            java.lang.Object r4 = r4.pollFirst()
            com.facebook.react.fabric.mounting.mountitems.PreAllocateViewMountItem r4 = (com.facebook.react.fabric.mounting.mountitems.PreAllocateViewMountItem) r4
            monitor-exit(r3)
            int r3 = r4.getRootTag()
            java.lang.String r6 = "dispatchPreMountItems"
            boolean r3 = r10.surfaceActiveForExecution(r3, r6)
            if (r3 == 0) goto L_0x000a
            com.facebook.react.fabric.mounting.MountingManager r3 = r10.mMountingManager
            r4.execute(r3)
            goto L_0x000a
        L_0x004e:
            r11 = move-exception
            monitor-exit(r3)
            throw r11
        L_0x0051:
            r11 = move-exception
            r10.mInDispatch = r5
            r10.mLastExecutedMountItemSurfaceId = r0
            throw r11
        */
        throw new UnsupportedOperationException("Method not decompiled: com.facebook.react.fabric.FabricUIManager.dispatchPreMountItems(long):void");
    }

    public void setBinding(Binding binding) {
        this.mBinding = binding;
    }

    @Override // com.facebook.react.bridge.UIManager
    public void updateRootLayoutSpecs(int i, int i2, int i3, int i4, int i5) {
        boolean z;
        boolean z2;
        if (ENABLE_FABRIC_LOGS) {
            FLog.d(TAG, "Updating Root Layout Specs");
        }
        ThemedReactContext themedReactContext = this.mReactContextForRootTag.get(Integer.valueOf(i));
        if (themedReactContext != null) {
            boolean isRTL = I18nUtil.getInstance().isRTL(themedReactContext);
            z = I18nUtil.getInstance().doLeftAndRightSwapInRTL(themedReactContext);
            z2 = isRTL;
        } else {
            z2 = false;
            z = false;
        }
        this.mBinding.setConstraints(i, LayoutMetricsConversions.getMinSize(i2), LayoutMetricsConversions.getMaxSize(i2), LayoutMetricsConversions.getMinSize(i3), LayoutMetricsConversions.getMaxSize(i3), (float) i4, (float) i5, z2, z);
    }

    @Override // com.facebook.react.bridge.UIManager
    public void receiveEvent(int i, String str, WritableMap writableMap) {
        EventEmitterWrapper eventEmitter = this.mMountingManager.getEventEmitter(i);
        if (eventEmitter == null) {
            FLog.d(TAG, "Unable to invoke event: " + str + " for reactTag: " + i);
            return;
        }
        eventEmitter.invoke(str, writableMap);
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostResume() {
        ReactChoreographer.getInstance().postFrameCallback(ReactChoreographer.CallbackType.DISPATCH_UI, this.mDispatchUIFrameCallback);
    }

    @Override // com.facebook.react.bridge.UIManager
    public EventDispatcher getEventDispatcher() {
        return this.mEventDispatcher;
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostPause() {
        ReactChoreographer.getInstance().removeFrameCallback(ReactChoreographer.CallbackType.DISPATCH_UI, this.mDispatchUIFrameCallback);
    }

    @Override // com.facebook.react.bridge.UIManager
    @Deprecated
    public void dispatchCommand(int i, int i2, ReadableArray readableArray) {
        dispatchCommandMountItem(new DispatchIntCommandMountItem(i, i2, readableArray));
    }

    @Override // com.facebook.react.bridge.UIManager
    public void dispatchCommand(int i, String str, ReadableArray readableArray) {
        dispatchCommandMountItem(new DispatchStringCommandMountItem(i, str, readableArray));
    }

    private void dispatchCommandMountItem(DispatchCommandMountItem dispatchCommandMountItem) {
        synchronized (this.mViewCommandMountItemsLock) {
            this.mViewCommandMountItems.add(dispatchCommandMountItem);
        }
    }

    @Override // com.facebook.react.bridge.UIManager
    public void sendAccessibilityEvent(int i, int i2) {
        synchronized (this.mMountItemsLock) {
            this.mMountItems.add(new SendAccessibilityEvent(i, i2));
        }
    }

    public void setJSResponder(final int i, final int i2, final boolean z) {
        synchronized (this.mMountItemsLock) {
            this.mMountItems.add(new MountItem() {
                /* class com.facebook.react.fabric.FabricUIManager.AnonymousClass3 */

                @Override // com.facebook.react.fabric.mounting.mountitems.MountItem
                public void execute(MountingManager mountingManager) {
                    mountingManager.setJSResponder(i, i2, z);
                }
            });
        }
    }

    public void clearJSResponder() {
        synchronized (this.mMountItemsLock) {
            this.mMountItems.add(new MountItem() {
                /* class com.facebook.react.fabric.FabricUIManager.AnonymousClass4 */

                @Override // com.facebook.react.fabric.mounting.mountitems.MountItem
                public void execute(MountingManager mountingManager) {
                    mountingManager.clearJSResponder();
                }
            });
        }
    }

    @Override // com.facebook.react.bridge.UIManager
    @Deprecated
    public String resolveCustomDirectEventName(String str) {
        if (str == null) {
            return null;
        }
        if (!str.substring(0, 3).equals(ViewProps.TOP)) {
            return str;
        }
        return ViewProps.ON + str.substring(3);
    }

    public void onAnimationStarted() {
        this.mDriveCxxAnimations = true;
    }

    public void onAllAnimationsComplete() {
        this.mDriveCxxAnimations = false;
    }

    @Override // com.facebook.react.bridge.PerformanceCounter
    public Map<String, Long> getPerformanceCounters() {
        HashMap hashMap = new HashMap();
        hashMap.put("CommitStartTime", Long.valueOf(this.mCommitStartTime));
        hashMap.put("LayoutTime", Long.valueOf(this.mLayoutTime));
        hashMap.put("DispatchViewUpdatesTime", Long.valueOf(this.mDispatchViewUpdatesTime));
        hashMap.put("RunStartTime", Long.valueOf(this.mRunStartTime));
        hashMap.put("BatchedExecutionTime", Long.valueOf(this.mBatchedExecutionTime));
        hashMap.put("FinishFabricTransactionTime", Long.valueOf(this.mFinishTransactionTime));
        hashMap.put("FinishFabricTransactionCPPTime", Long.valueOf(this.mFinishTransactionCPPTime));
        return hashMap;
    }

    /* access modifiers changed from: private */
    public class DispatchUIFrameCallback extends GuardedFrameCallback {
        private volatile boolean mIsMountingEnabled;

        private DispatchUIFrameCallback(ReactContext reactContext) {
            super(reactContext);
            this.mIsMountingEnabled = true;
        }

        /* access modifiers changed from: package-private */
        public void stop() {
            this.mIsMountingEnabled = false;
        }

        @Override // com.facebook.react.fabric.GuardedFrameCallback
        public void doFrameGuarded(long j) {
            if (!this.mIsMountingEnabled || FabricUIManager.this.mDestroyed) {
                FLog.w(FabricUIManager.TAG, "Not flushing pending UI operations because of previously thrown Exception");
                return;
            }
            if (FabricUIManager.this.mDriveCxxAnimations && FabricUIManager.this.mBinding != null) {
                FabricUIManager.this.mBinding.driveCxxAnimations();
            }
            try {
                FabricUIManager.this.dispatchPreMountItems(j);
                FabricUIManager.this.tryDispatchMountItems();
                ReactChoreographer.getInstance().postFrameCallback(ReactChoreographer.CallbackType.DISPATCH_UI, FabricUIManager.this.mDispatchUIFrameCallback);
            } catch (Exception e) {
                FLog.e(FabricUIManager.TAG, "Exception thrown when executing UIFrameGuarded", e);
                stop();
                throw e;
            } catch (Throwable th) {
                ReactChoreographer.getInstance().postFrameCallback(ReactChoreographer.CallbackType.DISPATCH_UI, FabricUIManager.this.mDispatchUIFrameCallback);
                throw th;
            }
        }
    }
}
