package com.facebook.react.fabric.mounting.mountitems;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.ReactMarker;
import com.facebook.react.bridge.ReactMarkerConstants;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.fabric.FabricComponents;
import com.facebook.react.fabric.events.EventEmitterWrapper;
import com.facebook.react.fabric.mounting.MountingManager;
import com.facebook.react.uimanager.StateWrapper;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.systrace.Systrace;

public class IntBufferBatchMountItem implements MountItem {
    static final int INSTRUCTION_CREATE = 2;
    static final int INSTRUCTION_DELETE = 4;
    static final int INSTRUCTION_FLAG_MULTIPLE = 1;
    static final int INSTRUCTION_INSERT = 8;
    static final int INSTRUCTION_REMOVE = 16;
    static final int INSTRUCTION_UPDATE_EVENT_EMITTER = 256;
    static final int INSTRUCTION_UPDATE_LAYOUT = 128;
    static final int INSTRUCTION_UPDATE_PADDING = 512;
    static final int INSTRUCTION_UPDATE_PROPS = 32;
    static final int INSTRUCTION_UPDATE_STATE = 64;
    static final String TAG = "IntBufferBatchMountItem";
    private final int mCommitNumber;
    private final ThemedReactContext mContext;
    private final int[] mIntBuffer;
    private final int mIntBufferLen;
    private final Object[] mObjBuffer;
    private final int mObjBufferLen;
    private final int mRootTag;

    public IntBufferBatchMountItem(int i, ThemedReactContext themedReactContext, int[] iArr, Object[] objArr, int i2) {
        this.mRootTag = i;
        this.mCommitNumber = i2;
        this.mContext = themedReactContext;
        this.mIntBuffer = iArr;
        this.mObjBuffer = objArr;
        int i3 = 0;
        this.mIntBufferLen = iArr != null ? iArr.length : 0;
        this.mObjBufferLen = objArr != null ? objArr.length : i3;
    }

    private void beginMarkers(String str) {
        Systrace.beginSection(0, "FabricUIManager::" + str + " - " + this.mIntBufferLen + " intBufSize  - " + this.mObjBufferLen + " objBufSize");
        if (this.mCommitNumber > 0) {
            ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_BATCH_EXECUTION_START, null, this.mCommitNumber);
        }
    }

    private void endMarkers() {
        if (this.mCommitNumber > 0) {
            ReactMarker.logFabricMarker(ReactMarkerConstants.FABRIC_BATCH_EXECUTION_END, null, this.mCommitNumber);
        }
        Systrace.endSection(0);
    }

    private static StateWrapper castToState(Object obj) {
        if (obj != null) {
            return (StateWrapper) obj;
        }
        return null;
    }

    private static ReadableMap castToProps(Object obj) {
        if (obj != null) {
            return (ReadableMap) obj;
        }
        return null;
    }

    private static EventEmitterWrapper castToEventEmitter(Object obj) {
        if (obj != null) {
            return (EventEmitterWrapper) obj;
        }
        return null;
    }

    @Override // com.facebook.react.fabric.mounting.mountitems.MountItem
    public void execute(MountingManager mountingManager) {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        if (this.mContext == null) {
            String str = TAG;
            FLog.e(str, "Cannot execute batch of %s MountItems; no context. Hopefully this is because StopSurface was called.", str);
            return;
        }
        beginMarkers("mountViews");
        int i6 = 0;
        int i7 = 0;
        while (i6 < this.mIntBufferLen) {
            int[] iArr = this.mIntBuffer;
            int i8 = i6 + 1;
            int i9 = iArr[i6];
            int i10 = i9 & -2;
            if ((i9 & 1) != 0) {
                int i11 = iArr[i8];
                i8++;
                i = i11;
            } else {
                i = 1;
            }
            int i12 = i7;
            i6 = i8;
            for (int i13 = 0; i13 < i; i13++) {
                if (i10 == 2) {
                    int i14 = i12 + 1;
                    String fabricComponentName = FabricComponents.getFabricComponentName((String) this.mObjBuffer[i12]);
                    int i15 = i6 + 1;
                    int i16 = i14 + 1;
                    int i17 = i16 + 1;
                    int i18 = i15 + 1;
                    mountingManager.createView(this.mContext, fabricComponentName, this.mIntBuffer[i6], castToProps(this.mObjBuffer[i14]), castToState(this.mObjBuffer[i16]), this.mIntBuffer[i15] == 1);
                    i12 = i17;
                    i6 = i18;
                } else if (i10 == 4) {
                    mountingManager.deleteView(this.mIntBuffer[i6]);
                    i6++;
                } else {
                    if (i10 == 8) {
                        int[] iArr2 = this.mIntBuffer;
                        int i19 = i6 + 1;
                        int i20 = i19 + 1;
                        i5 = i20 + 1;
                        mountingManager.addViewAt(iArr2[i19], iArr2[i6], iArr2[i20]);
                    } else if (i10 == 16) {
                        int[] iArr3 = this.mIntBuffer;
                        int i21 = i6 + 1;
                        int i22 = i21 + 1;
                        i5 = i22 + 1;
                        mountingManager.removeViewAt(iArr3[i6], iArr3[i21], iArr3[i22]);
                    } else {
                        if (i10 == 32) {
                            i2 = i6 + 1;
                            i3 = i12 + 1;
                            mountingManager.updateProps(this.mIntBuffer[i6], castToProps(this.mObjBuffer[i12]));
                        } else if (i10 == 64) {
                            i2 = i6 + 1;
                            i3 = i12 + 1;
                            mountingManager.updateState(this.mIntBuffer[i6], castToState(this.mObjBuffer[i12]));
                        } else {
                            if (i10 == 128) {
                                int[] iArr4 = this.mIntBuffer;
                                int i23 = i6 + 1;
                                int i24 = iArr4[i6];
                                int i25 = i23 + 1;
                                int i26 = iArr4[i23];
                                int i27 = i25 + 1;
                                int i28 = iArr4[i25];
                                int i29 = i27 + 1;
                                mountingManager.updateLayout(i24, i26, i28, iArr4[i27], iArr4[i29]);
                                i4 = i29 + 1 + 1;
                            } else if (i10 == 512) {
                                int[] iArr5 = this.mIntBuffer;
                                int i30 = i6 + 1;
                                int i31 = iArr5[i6];
                                int i32 = i30 + 1;
                                int i33 = iArr5[i30];
                                int i34 = i32 + 1;
                                int i35 = iArr5[i32];
                                int i36 = i34 + 1;
                                i4 = i36 + 1;
                                mountingManager.updatePadding(i31, i33, i35, iArr5[i34], iArr5[i36]);
                            } else if (i10 == 256) {
                                i2 = i6 + 1;
                                i3 = i12 + 1;
                                mountingManager.updateEventEmitter(this.mIntBuffer[i6], castToEventEmitter(this.mObjBuffer[i12]));
                            } else {
                                throw new IllegalArgumentException("Invalid type argument to IntBufferBatchMountItem: " + i10 + " at index: " + i6);
                            }
                            i6 = i4;
                        }
                        i6 = i2;
                        i12 = i3;
                    }
                    i6 = i5;
                }
            }
            i7 = i12;
        }
        endMarkers();
    }

    public int getRootTag() {
        return this.mRootTag;
    }

    public boolean shouldSchedule() {
        return this.mIntBufferLen != 0;
    }

    public String toString() {
        int i;
        int i2;
        int i3;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("IntBufferBatchMountItem:");
            int i4 = 0;
            int i5 = 0;
            while (i4 < this.mIntBufferLen) {
                int[] iArr = this.mIntBuffer;
                int i6 = i4 + 1;
                int i7 = iArr[i4];
                int i8 = i7 & -2;
                if ((i7 & 1) != 0) {
                    i = iArr[i6];
                    i6++;
                } else {
                    i = 1;
                }
                i4 = i6;
                int i9 = 0;
                while (true) {
                    if (i9 < i) {
                        if (i8 == 2) {
                            int i10 = i4 + 1;
                            i2 = i10 + 1;
                            sb.append(String.format("CREATE [%d] - layoutable:%d - %s\n", Integer.valueOf(this.mIntBuffer[i4]), Integer.valueOf(this.mIntBuffer[i10]), FabricComponents.getFabricComponentName((String) this.mObjBuffer[i5])));
                            i5 = i5 + 1 + 2;
                        } else {
                            if (i8 == 4) {
                                i3 = i4 + 1;
                                sb.append(String.format("DELETE [%d]\n", Integer.valueOf(this.mIntBuffer[i4])));
                            } else if (i8 == 8) {
                                int i11 = i4 + 1;
                                int i12 = i11 + 1;
                                i2 = i12 + 1;
                                sb.append(String.format("INSERT [%d]->[%d] @%d\n", Integer.valueOf(this.mIntBuffer[i4]), Integer.valueOf(this.mIntBuffer[i11]), Integer.valueOf(this.mIntBuffer[i12])));
                            } else if (i8 == 16) {
                                int i13 = i4 + 1;
                                int i14 = i13 + 1;
                                i2 = i14 + 1;
                                sb.append(String.format("REMOVE [%d]->[%d] @%d\n", Integer.valueOf(this.mIntBuffer[i4]), Integer.valueOf(this.mIntBuffer[i13]), Integer.valueOf(this.mIntBuffer[i14])));
                            } else if (i8 == 32) {
                                castToProps(this.mObjBuffer[i5]);
                                i2 = i4 + 1;
                                sb.append(String.format("UPDATE PROPS [%d]: %s\n", Integer.valueOf(this.mIntBuffer[i4]), "<hidden>"));
                                i5++;
                            } else if (i8 == 64) {
                                i5++;
                                i3 = i4 + 1;
                                sb.append(String.format("UPDATE STATE [%d]\n", Integer.valueOf(this.mIntBuffer[i4])));
                            } else {
                                if (i8 == 128) {
                                    int i15 = i4 + 1;
                                    int i16 = i15 + 1;
                                    int i17 = i16 + 1;
                                    int i18 = i17 + 1;
                                    int i19 = i18 + 1;
                                    sb.append(String.format("UPDATE LAYOUT [%d]: x:%d y:%d w:%d h:%d layoutDirection:%d\n", Integer.valueOf(this.mIntBuffer[i4]), Integer.valueOf(this.mIntBuffer[i15]), Integer.valueOf(this.mIntBuffer[i16]), Integer.valueOf(this.mIntBuffer[i17]), Integer.valueOf(this.mIntBuffer[i18]), Integer.valueOf(this.mIntBuffer[i19])));
                                    i4 = i19 + 1;
                                } else if (i8 == 512) {
                                    int i20 = i4 + 1;
                                    int i21 = i20 + 1;
                                    int i22 = i21 + 1;
                                    int i23 = i22 + 1;
                                    sb.append(String.format("UPDATE PADDING [%d]: top:%d right:%d bottom:%d left:%d\n", Integer.valueOf(this.mIntBuffer[i4]), Integer.valueOf(this.mIntBuffer[i20]), Integer.valueOf(this.mIntBuffer[i21]), Integer.valueOf(this.mIntBuffer[i22]), Integer.valueOf(this.mIntBuffer[i23])));
                                    i4 = i23 + 1;
                                } else if (i8 == 256) {
                                    i5++;
                                    i3 = i4 + 1;
                                    sb.append(String.format("UPDATE EVENTEMITTER [%d]\n", Integer.valueOf(this.mIntBuffer[i4])));
                                } else {
                                    String str = TAG;
                                    FLog.e(str, "String so far: " + sb.toString());
                                    throw new IllegalArgumentException("Invalid type argument to IntBufferBatchMountItem: " + i8 + " at index: " + i4);
                                }
                                i9++;
                            }
                            i4 = i3;
                            i9++;
                        }
                        i4 = i2;
                        i9++;
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            FLog.e(TAG, "Caught exception trying to print", e);
            StringBuilder sb2 = new StringBuilder();
            for (int i24 = 0; i24 < this.mIntBufferLen; i24++) {
                sb2.append(this.mIntBuffer[i24]);
                sb2.append(", ");
            }
            FLog.e(TAG, sb2.toString());
            for (int i25 = 0; i25 < this.mObjBufferLen; i25++) {
                String str2 = TAG;
                Object[] objArr = this.mObjBuffer;
                FLog.e(str2, objArr[i25] != null ? objArr[i25].toString() : "null");
            }
            return "";
        }
    }
}
