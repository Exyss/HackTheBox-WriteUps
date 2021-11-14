package com.facebook.react.animated;

import com.facebook.fbreact.specs.NativeAnimatedModuleSpec;
import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactSoftException;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UIManager;
import com.facebook.react.bridge.UIManagerListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.core.ReactChoreographer;
import com.facebook.react.uimanager.GuardedFrameCallback;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerHelper;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.common.ViewUtil;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

@ReactModule(name = NativeAnimatedModule.NAME)
public class NativeAnimatedModule extends NativeAnimatedModuleSpec implements LifecycleEventListener, UIManagerListener {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final boolean ANIMATED_MODULE_DEBUG = false;
    public static final String NAME = "NativeAnimatedModule";
    private final GuardedFrameCallback mAnimatedFrameCallback;
    private boolean mBatchingControlledByJS = false;
    private volatile long mCurrentBatchNumber;
    private volatile long mCurrentFrameNumber;
    private boolean mInitializedForFabric = false;
    private boolean mInitializedForNonFabric = false;
    private final AtomicReference<NativeAnimatedNodesManager> mNodesManager = new AtomicReference<>();
    private int mNumFabricAnimations = 0;
    private int mNumNonFabricAnimations = 0;
    private final ConcurrentLinkedQueue<UIThreadOperation> mOperations = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<UIThreadOperation> mPreOperations = new ConcurrentLinkedQueue<>();
    private final ReactChoreographer mReactChoreographer = ReactChoreographer.getInstance();
    private int mUIManagerType = 1;

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void addListener(String str) {
    }

    @Override // com.facebook.react.bridge.NativeModule
    public String getName() {
        return NAME;
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void removeListeners(double d) {
    }

    /* access modifiers changed from: private */
    public abstract class UIThreadOperation {
        long mBatchNumber;

        /* access modifiers changed from: package-private */
        public abstract void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager);

        private UIThreadOperation() {
            this.mBatchNumber = -1;
        }

        public void setBatchNumber(long j) {
            this.mBatchNumber = j;
        }

        public long getBatchNumber() {
            return this.mBatchNumber;
        }
    }

    public NativeAnimatedModule(ReactApplicationContext reactApplicationContext) {
        super(reactApplicationContext);
        this.mAnimatedFrameCallback = new GuardedFrameCallback(reactApplicationContext) {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass1 */

            /* access modifiers changed from: protected */
            @Override // com.facebook.react.uimanager.GuardedFrameCallback
            public void doFrameGuarded(long j) {
                try {
                    NativeAnimatedNodesManager nodesManager = NativeAnimatedModule.this.getNodesManager();
                    if (nodesManager != null && nodesManager.hasActiveAnimations()) {
                        nodesManager.runUpdates(j);
                    }
                    if (nodesManager != null || NativeAnimatedModule.this.mReactChoreographer != null) {
                        ((ReactChoreographer) Assertions.assertNotNull(NativeAnimatedModule.this.mReactChoreographer)).postFrameCallback(ReactChoreographer.CallbackType.NATIVE_ANIMATED_MODULE, NativeAnimatedModule.this.mAnimatedFrameCallback);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override // com.facebook.react.bridge.BaseJavaModule, com.facebook.react.bridge.NativeModule
    public void initialize() {
        ReactApplicationContext reactApplicationContextIfActiveOrWarn = getReactApplicationContextIfActiveOrWarn();
        if (reactApplicationContextIfActiveOrWarn != null) {
            reactApplicationContextIfActiveOrWarn.addLifecycleEventListener(this);
        }
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostResume() {
        enqueueFrameCallback();
    }

    private void addOperation(UIThreadOperation uIThreadOperation) {
        uIThreadOperation.setBatchNumber(this.mCurrentBatchNumber);
        this.mOperations.add(uIThreadOperation);
    }

    private void addUnbatchedOperation(UIThreadOperation uIThreadOperation) {
        uIThreadOperation.setBatchNumber(-1);
        this.mOperations.add(uIThreadOperation);
    }

    private void addPreOperation(UIThreadOperation uIThreadOperation) {
        uIThreadOperation.setBatchNumber(this.mCurrentBatchNumber);
        this.mPreOperations.add(uIThreadOperation);
    }

    @Override // com.facebook.react.bridge.UIManagerListener
    public void didScheduleMountItems(UIManager uIManager) {
        this.mCurrentFrameNumber++;
    }

    @Override // com.facebook.react.bridge.UIManagerListener
    public void didDispatchMountItems(UIManager uIManager) {
        if (this.mUIManagerType == 2) {
            long j = this.mCurrentBatchNumber - 1;
            if (!this.mBatchingControlledByJS) {
                this.mCurrentFrameNumber++;
                if (this.mCurrentFrameNumber - this.mCurrentBatchNumber > 2) {
                    this.mCurrentBatchNumber = this.mCurrentFrameNumber;
                    j = this.mCurrentBatchNumber;
                }
            }
            executeAllOperations(this.mPreOperations, j);
            executeAllOperations(this.mOperations, j);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void executeAllOperations(Queue<UIThreadOperation> queue, long j) {
        UIThreadOperation poll;
        NativeAnimatedNodesManager nodesManager = getNodesManager();
        while (true) {
            UIThreadOperation peek = queue.peek();
            if (peek != null && peek.getBatchNumber() <= j && (poll = queue.poll()) != null) {
                poll.execute(nodesManager);
            } else {
                return;
            }
        }
    }

    @Override // com.facebook.react.bridge.UIManagerListener
    public void willDispatchViewUpdates(UIManager uIManager) {
        if ((!this.mOperations.isEmpty() || !this.mPreOperations.isEmpty()) && this.mUIManagerType != 2) {
            final long j = this.mCurrentBatchNumber;
            this.mCurrentBatchNumber = 1 + j;
            AnonymousClass2 r2 = new UIBlock() {
                /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass2 */

                @Override // com.facebook.react.uimanager.UIBlock
                public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                    NativeAnimatedModule nativeAnimatedModule = NativeAnimatedModule.this;
                    nativeAnimatedModule.executeAllOperations(nativeAnimatedModule.mPreOperations, j);
                }
            };
            AnonymousClass3 r3 = new UIBlock() {
                /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass3 */

                @Override // com.facebook.react.uimanager.UIBlock
                public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                    NativeAnimatedModule nativeAnimatedModule = NativeAnimatedModule.this;
                    nativeAnimatedModule.executeAllOperations(nativeAnimatedModule.mOperations, j);
                }
            };
            UIManagerModule uIManagerModule = (UIManagerModule) uIManager;
            uIManagerModule.prependUIBlock(r2);
            uIManagerModule.addUIBlock(r3);
        }
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostPause() {
        clearFrameCallback();
    }

    @Override // com.facebook.react.bridge.LifecycleEventListener
    public void onHostDestroy() {
        clearFrameCallback();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private NativeAnimatedNodesManager getNodesManager() {
        ReactApplicationContext reactApplicationContextIfActiveOrWarn;
        if (this.mNodesManager.get() == null && (reactApplicationContextIfActiveOrWarn = getReactApplicationContextIfActiveOrWarn()) != null) {
            this.mNodesManager.compareAndSet(null, new NativeAnimatedNodesManager(reactApplicationContextIfActiveOrWarn));
        }
        return this.mNodesManager.get();
    }

    private void clearFrameCallback() {
        ((ReactChoreographer) Assertions.assertNotNull(this.mReactChoreographer)).removeFrameCallback(ReactChoreographer.CallbackType.NATIVE_ANIMATED_MODULE, this.mAnimatedFrameCallback);
    }

    private void enqueueFrameCallback() {
        ((ReactChoreographer) Assertions.assertNotNull(this.mReactChoreographer)).postFrameCallback(ReactChoreographer.CallbackType.NATIVE_ANIMATED_MODULE, this.mAnimatedFrameCallback);
    }

    public void setNodesManager(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
        this.mNodesManager.set(nativeAnimatedNodesManager);
    }

    private void initializeLifecycleEventListenersForViewTag(int i) {
        ReactApplicationContext reactApplicationContext;
        UIManager uIManager;
        int uIManagerType = ViewUtil.getUIManagerType(i);
        this.mUIManagerType = uIManagerType;
        if (uIManagerType == 2) {
            this.mNumFabricAnimations++;
        } else {
            this.mNumNonFabricAnimations++;
        }
        NativeAnimatedNodesManager nodesManager = getNodesManager();
        if (nodesManager != null) {
            nodesManager.initializeEventListenerForUIManagerType(this.mUIManagerType);
        } else {
            ReactSoftException.logSoftException(NAME, new RuntimeException("initializeLifecycleEventListenersForViewTag could not get NativeAnimatedNodesManager"));
        }
        if (this.mInitializedForFabric && this.mUIManagerType == 2) {
            return;
        }
        if ((!this.mInitializedForNonFabric || this.mUIManagerType != 1) && (reactApplicationContext = getReactApplicationContext()) != null && (uIManager = UIManagerHelper.getUIManager(reactApplicationContext, this.mUIManagerType)) != null) {
            uIManager.addUIManagerEventListener(this);
            if (this.mUIManagerType == 2) {
                this.mInitializedForFabric = true;
            } else {
                this.mInitializedForNonFabric = true;
            }
        }
    }

    private void decrementInFlightAnimationsForViewTag(int i) {
        if (ViewUtil.getUIManagerType(i) == 2) {
            this.mNumFabricAnimations--;
        } else {
            this.mNumNonFabricAnimations--;
        }
        int i2 = this.mNumNonFabricAnimations;
        if (i2 == 0 && this.mNumFabricAnimations > 0 && this.mUIManagerType != 2) {
            this.mUIManagerType = 2;
        } else if (this.mNumFabricAnimations == 0 && i2 > 0 && this.mUIManagerType != 1) {
            this.mUIManagerType = 1;
        }
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void startOperationBatch() {
        this.mBatchingControlledByJS = true;
        this.mCurrentBatchNumber++;
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void finishOperationBatch() {
        this.mBatchingControlledByJS = true;
        this.mCurrentBatchNumber++;
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void createAnimatedNode(double d, final ReadableMap readableMap) {
        final int i = (int) d;
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass4 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.createAnimatedNode(i, readableMap);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void startListeningToAnimatedNodeValue(double d) {
        final int i = (int) d;
        final AnonymousClass5 r3 = new AnimatedNodeValueListener() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass5 */

            @Override // com.facebook.react.animated.AnimatedNodeValueListener
            public void onValueUpdate(double d) {
                WritableMap createMap = Arguments.createMap();
                createMap.putInt("tag", i);
                createMap.putDouble("value", d);
                ReactApplicationContext reactApplicationContextIfActiveOrWarn = NativeAnimatedModule.this.getReactApplicationContextIfActiveOrWarn();
                if (reactApplicationContextIfActiveOrWarn != null) {
                    ((DeviceEventManagerModule.RCTDeviceEventEmitter) reactApplicationContextIfActiveOrWarn.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)).emit("onAnimatedValueUpdate", createMap);
                }
            }
        };
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass6 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.startListeningToAnimatedNodeValue(i, r3);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void stopListeningToAnimatedNodeValue(double d) {
        final int i = (int) d;
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass7 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.stopListeningToAnimatedNodeValue(i);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void dropAnimatedNode(double d) {
        final int i = (int) d;
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass8 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.dropAnimatedNode(i);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void setAnimatedNodeValue(double d, final double d2) {
        final int i = (int) d;
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass9 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.setAnimatedNodeValue(i, d2);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void setAnimatedNodeOffset(double d, final double d2) {
        final int i = (int) d;
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass10 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.setAnimatedNodeOffset(i, d2);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void flattenAnimatedNodeOffset(double d) {
        final int i = (int) d;
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass11 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.flattenAnimatedNodeOffset(i);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void extractAnimatedNodeOffset(double d) {
        final int i = (int) d;
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass12 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.extractAnimatedNodeOffset(i);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void startAnimatingNode(double d, double d2, final ReadableMap readableMap, final Callback callback) {
        final int i = (int) d;
        final int i2 = (int) d2;
        addUnbatchedOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass13 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.startAnimatingNode(i, i2, readableMap, callback);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void stopAnimation(double d) {
        final int i = (int) d;
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass14 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.stopAnimation(i);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void connectAnimatedNodes(double d, double d2) {
        final int i = (int) d;
        final int i2 = (int) d2;
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass15 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.connectAnimatedNodes(i, i2);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void disconnectAnimatedNodes(double d, double d2) {
        final int i = (int) d;
        final int i2 = (int) d2;
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass16 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.disconnectAnimatedNodes(i, i2);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void connectAnimatedNodeToView(double d, double d2) {
        final int i = (int) d;
        final int i2 = (int) d2;
        initializeLifecycleEventListenersForViewTag(i2);
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass17 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.connectAnimatedNodeToView(i, i2);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void disconnectAnimatedNodeFromView(double d, double d2) {
        final int i = (int) d;
        final int i2 = (int) d2;
        decrementInFlightAnimationsForViewTag(i2);
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass18 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.disconnectAnimatedNodeFromView(i, i2);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void restoreDefaultValues(double d) {
        final int i = (int) d;
        addPreOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass19 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.restoreDefaultValues(i);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void addAnimatedEventToView(double d, final String str, final ReadableMap readableMap) {
        final int i = (int) d;
        initializeLifecycleEventListenersForViewTag(i);
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass20 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.addAnimatedEventToView(i, str, readableMap);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void removeAnimatedEventFromView(double d, final String str, double d2) {
        final int i = (int) d;
        final int i2 = (int) d2;
        decrementInFlightAnimationsForViewTag(i);
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass21 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.removeAnimatedEventFromView(i, str, i2);
            }
        });
    }

    @Override // com.facebook.fbreact.specs.NativeAnimatedModuleSpec
    public void getValue(double d, final Callback callback) {
        final int i = (int) d;
        addOperation(new UIThreadOperation() {
            /* class com.facebook.react.animated.NativeAnimatedModule.AnonymousClass22 */

            @Override // com.facebook.react.animated.NativeAnimatedModule.UIThreadOperation
            public void execute(NativeAnimatedNodesManager nativeAnimatedNodesManager) {
                nativeAnimatedNodesManager.getValue(i, callback);
            }
        });
    }
}
