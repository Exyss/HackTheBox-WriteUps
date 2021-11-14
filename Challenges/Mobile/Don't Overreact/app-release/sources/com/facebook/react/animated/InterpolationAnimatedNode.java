package com.facebook.react.animated;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* access modifiers changed from: package-private */
public class InterpolationAnimatedNode extends ValueAnimatedNode {
    public static final String EXTRAPOLATE_TYPE_CLAMP = "clamp";
    public static final String EXTRAPOLATE_TYPE_EXTEND = "extend";
    public static final String EXTRAPOLATE_TYPE_IDENTITY = "identity";
    private static final Pattern fpPattern = Pattern.compile(fpRegex);
    private static final String fpRegex = "[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?";
    private final String mExtrapolateLeft;
    private final String mExtrapolateRight;
    private final boolean mHasStringOutput;
    private final double[] mInputRange;
    private int mNumVals;
    private final double[] mOutputRange;
    private double[][] mOutputs;
    private ValueAnimatedNode mParent;
    private String mPattern;
    private final Matcher mSOutputMatcher;
    private boolean mShouldRound;

    private static double[] fromDoubleArray(ReadableArray readableArray) {
        int size = readableArray.size();
        double[] dArr = new double[size];
        for (int i = 0; i < size; i++) {
            dArr[i] = readableArray.getDouble(i);
        }
        return dArr;
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x006f, code lost:
        if (r24.equals(com.facebook.react.animated.InterpolationAnimatedNode.EXTRAPOLATE_TYPE_CLAMP) == false) goto L_0x0069;
     */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x005f  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00a8 A[RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x00a9  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static double interpolate(double r13, double r15, double r17, double r19, double r21, java.lang.String r23, java.lang.String r24) {
        /*
        // Method dump skipped, instructions count: 236
        */
        throw new UnsupportedOperationException("Method not decompiled: com.facebook.react.animated.InterpolationAnimatedNode.interpolate(double, double, double, double, double, java.lang.String, java.lang.String):double");
    }

    static double interpolate(double d, double[] dArr, double[] dArr2, String str, String str2) {
        int findRangeIndex = findRangeIndex(d, dArr);
        int i = findRangeIndex + 1;
        return interpolate(d, dArr[findRangeIndex], dArr[i], dArr2[findRangeIndex], dArr2[i], str, str2);
    }

    private static int findRangeIndex(double d, double[] dArr) {
        int i = 1;
        while (i < dArr.length - 1 && dArr[i] < d) {
            i++;
        }
        return i - 1;
    }

    public InterpolationAnimatedNode(ReadableMap readableMap) {
        this.mInputRange = fromDoubleArray(readableMap.getArray("inputRange"));
        ReadableArray array = readableMap.getArray("outputRange");
        boolean z = array.getType(0) == ReadableType.String;
        this.mHasStringOutput = z;
        if (z) {
            int size = array.size();
            this.mOutputRange = new double[size];
            String string = array.getString(0);
            this.mPattern = string;
            this.mShouldRound = string.startsWith("rgb");
            this.mSOutputMatcher = fpPattern.matcher(this.mPattern);
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < size; i++) {
                Matcher matcher = fpPattern.matcher(array.getString(i));
                ArrayList arrayList2 = new ArrayList();
                arrayList.add(arrayList2);
                while (matcher.find()) {
                    arrayList2.add(Double.valueOf(Double.parseDouble(matcher.group())));
                }
                this.mOutputRange[i] = ((Double) arrayList2.get(0)).doubleValue();
            }
            int size2 = ((ArrayList) arrayList.get(0)).size();
            this.mNumVals = size2;
            this.mOutputs = new double[size2][];
            for (int i2 = 0; i2 < this.mNumVals; i2++) {
                double[] dArr = new double[size];
                this.mOutputs[i2] = dArr;
                for (int i3 = 0; i3 < size; i3++) {
                    dArr[i3] = ((Double) ((ArrayList) arrayList.get(i3)).get(i2)).doubleValue();
                }
            }
        } else {
            this.mOutputRange = fromDoubleArray(array);
            this.mSOutputMatcher = null;
        }
        this.mExtrapolateLeft = readableMap.getString("extrapolateLeft");
        this.mExtrapolateRight = readableMap.getString("extrapolateRight");
    }

    @Override // com.facebook.react.animated.AnimatedNode
    public void onAttachedToNode(AnimatedNode animatedNode) {
        if (this.mParent != null) {
            throw new IllegalStateException("Parent already attached");
        } else if (animatedNode instanceof ValueAnimatedNode) {
            this.mParent = (ValueAnimatedNode) animatedNode;
        } else {
            throw new IllegalArgumentException("Parent is of an invalid type");
        }
    }

    @Override // com.facebook.react.animated.AnimatedNode
    public void onDetachedFromNode(AnimatedNode animatedNode) {
        if (animatedNode == this.mParent) {
            this.mParent = null;
            return;
        }
        throw new IllegalArgumentException("Invalid parent node provided");
    }

    @Override // com.facebook.react.animated.AnimatedNode
    public void update() {
        ValueAnimatedNode valueAnimatedNode = this.mParent;
        if (valueAnimatedNode != null) {
            double value = valueAnimatedNode.getValue();
            this.mValue = interpolate(value, this.mInputRange, this.mOutputRange, this.mExtrapolateLeft, this.mExtrapolateRight);
            if (!this.mHasStringOutput) {
                return;
            }
            if (this.mNumVals > 1) {
                StringBuffer stringBuffer = new StringBuffer(this.mPattern.length());
                this.mSOutputMatcher.reset();
                int i = 0;
                while (this.mSOutputMatcher.find()) {
                    int i2 = i + 1;
                    double interpolate = interpolate(value, this.mInputRange, this.mOutputs[i], this.mExtrapolateLeft, this.mExtrapolateRight);
                    if (this.mShouldRound) {
                        boolean z = i2 == 4;
                        if (z) {
                            interpolate *= 1000.0d;
                        }
                        int round = (int) Math.round(interpolate);
                        this.mSOutputMatcher.appendReplacement(stringBuffer, z ? Double.toString(((double) round) / 1000.0d) : Integer.toString(round));
                    } else {
                        int i3 = (int) interpolate;
                        this.mSOutputMatcher.appendReplacement(stringBuffer, ((double) i3) != interpolate ? Double.toString(interpolate) : Integer.toString(i3));
                    }
                    i = i2;
                }
                this.mSOutputMatcher.appendTail(stringBuffer);
                this.mAnimatedObject = stringBuffer.toString();
                return;
            }
            this.mAnimatedObject = this.mSOutputMatcher.replaceFirst(String.valueOf(this.mValue));
        }
    }

    @Override // com.facebook.react.animated.ValueAnimatedNode, com.facebook.react.animated.AnimatedNode
    public String prettyPrint() {
        return "InterpolationAnimatedNode[" + this.mTag + "] super: " + super.prettyPrint();
    }
}
