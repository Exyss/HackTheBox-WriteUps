package com.facebook.soloader;

import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import com.facebook.soloader.nativeloader.NativeLoader;
import dalvik.system.BaseDexClassLoader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;

public class SoLoader {
    static final boolean DEBUG = false;
    public static final int SOLOADER_ALLOW_ASYNC_INIT = 2;
    public static final int SOLOADER_DISABLE_BACKUP_SOSOURCE = 8;
    public static final int SOLOADER_DONT_TREAT_AS_SYSTEMAPP = 32;
    public static final int SOLOADER_ENABLE_EXOPACKAGE = 1;
    public static final int SOLOADER_LOOK_IN_ZIP = 4;
    public static final int SOLOADER_SKIP_MERGED_JNI_ONLOAD = 16;
    private static final String SO_STORE_NAME_MAIN = "lib-main";
    private static final String SO_STORE_NAME_SPLIT = "lib-";
    static final boolean SYSTRACE_LIBRARY_LOADING;
    static final String TAG = "SoLoader";
    private static boolean isSystemApp;
    @Nullable
    private static ApplicationSoSource sApplicationSoSource;
    @Nullable
    private static UnpackingSoSource[] sBackupSoSources;
    private static int sFlags;
    private static final Set<String> sLoadedAndMergedLibraries = Collections.newSetFromMap(new ConcurrentHashMap());
    private static final HashSet<String> sLoadedLibraries = new HashSet<>();
    private static final Map<String, Object> sLoadingLibraries = new HashMap();
    @Nullable
    static SoFileLoader sSoFileLoader;
    @Nullable
    private static SoSource[] sSoSources = null;
    private static final ReentrantReadWriteLock sSoSourcesLock = new ReentrantReadWriteLock();
    private static volatile int sSoSourcesVersion = 0;
    @Nullable
    private static SystemLoadLibraryWrapper sSystemLoadLibraryWrapper = null;

    static /* synthetic */ int access$208() {
        int i = sSoSourcesVersion;
        sSoSourcesVersion = i + 1;
        return i;
    }

    static {
        boolean z = false;
        try {
            if (Build.VERSION.SDK_INT >= 18) {
                z = true;
            }
        } catch (NoClassDefFoundError | UnsatisfiedLinkError unused) {
        }
        SYSTRACE_LIBRARY_LOADING = z;
    }

    public static void init(Context context, int i) throws IOException {
        init(context, i, null);
    }

    public static void init(Context context, int i, @Nullable SoFileLoader soFileLoader) throws IOException {
        StrictMode.ThreadPolicy allowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
        try {
            isSystemApp = checkIfSystemApp(context, i);
            initSoLoader(soFileLoader);
            initSoSources(context, i, soFileLoader);
            if (!NativeLoader.isInitialized()) {
                NativeLoader.init(new NativeLoaderToSoLoaderDelegate());
            }
        } finally {
            StrictMode.setThreadPolicy(allowThreadDiskWrites);
        }
    }

    public static void init(Context context, boolean z) {
        try {
            init(context, z ? 1 : 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initSoSources(Context context, int i, @Nullable SoFileLoader soFileLoader) throws IOException {
        int i2;
        sSoSourcesLock.writeLock().lock();
        try {
            if (sSoSources == null) {
                Log.d(TAG, "init start");
                sFlags = i;
                ArrayList arrayList = new ArrayList();
                String str = System.getenv("LD_LIBRARY_PATH");
                if (str == null) {
                    str = SysUtil.is64Bit() ? "/vendor/lib64:/system/lib64" : "/vendor/lib:/system/lib";
                }
                String[] split = str.split(":");
                for (String str2 : split) {
                    Log.d(TAG, "adding system library source: " + str2);
                    arrayList.add(new DirectorySoSource(new File(str2), 2));
                }
                if (context != null) {
                    if ((i & 1) != 0) {
                        sBackupSoSources = null;
                        Log.d(TAG, "adding exo package source: lib-main");
                        arrayList.add(0, new ExoSoSource(context, SO_STORE_NAME_MAIN));
                    } else {
                        if (isSystemApp) {
                            i2 = 0;
                        } else {
                            sApplicationSoSource = new ApplicationSoSource(context, Build.VERSION.SDK_INT <= 17 ? 1 : 0);
                            Log.d(TAG, "adding application source: " + sApplicationSoSource.toString());
                            arrayList.add(0, sApplicationSoSource);
                            i2 = 1;
                        }
                        if ((sFlags & 8) != 0) {
                            sBackupSoSources = null;
                        } else {
                            File file = new File(context.getApplicationInfo().sourceDir);
                            ArrayList arrayList2 = new ArrayList();
                            ApkSoSource apkSoSource = new ApkSoSource(context, file, SO_STORE_NAME_MAIN, i2);
                            arrayList2.add(apkSoSource);
                            Log.d(TAG, "adding backup source from : " + apkSoSource.toString());
                            if (Build.VERSION.SDK_INT >= 21 && context.getApplicationInfo().splitSourceDirs != null) {
                                Log.d(TAG, "adding backup sources from split apks");
                                String[] strArr = context.getApplicationInfo().splitSourceDirs;
                                int length = strArr.length;
                                int i3 = 0;
                                int i4 = 0;
                                while (i3 < length) {
                                    File file2 = new File(strArr[i3]);
                                    StringBuilder sb = new StringBuilder();
                                    sb.append(SO_STORE_NAME_SPLIT);
                                    sb.append(i4);
                                    ApkSoSource apkSoSource2 = new ApkSoSource(context, file2, sb.toString(), i2);
                                    Log.d(TAG, "adding backup source: " + apkSoSource2.toString());
                                    arrayList2.add(apkSoSource2);
                                    i3++;
                                    i4++;
                                }
                            }
                            sBackupSoSources = (UnpackingSoSource[]) arrayList2.toArray(new UnpackingSoSource[arrayList2.size()]);
                            arrayList.addAll(0, arrayList2);
                        }
                    }
                }
                SoSource[] soSourceArr = (SoSource[]) arrayList.toArray(new SoSource[arrayList.size()]);
                int makePrepareFlags = makePrepareFlags();
                int length2 = soSourceArr.length;
                while (true) {
                    int i5 = length2 - 1;
                    if (length2 <= 0) {
                        break;
                    }
                    Log.d(TAG, "Preparing SO source: " + soSourceArr[i5]);
                    soSourceArr[i5].prepare(makePrepareFlags);
                    length2 = i5;
                }
                sSoSources = soSourceArr;
                sSoSourcesVersion++;
                Log.d(TAG, "init finish: " + sSoSources.length + " SO sources prepared");
            }
        } finally {
            Log.d(TAG, "init exiting");
            sSoSourcesLock.writeLock().unlock();
        }
    }

    private static int makePrepareFlags() {
        ReentrantReadWriteLock reentrantReadWriteLock = sSoSourcesLock;
        reentrantReadWriteLock.writeLock().lock();
        try {
            int i = (sFlags & 2) != 0 ? 1 : 0;
            reentrantReadWriteLock.writeLock().unlock();
            return i;
        } catch (Throwable th) {
            sSoSourcesLock.writeLock().unlock();
            throw th;
        }
    }

    private static synchronized void initSoLoader(@Nullable SoFileLoader soFileLoader) {
        synchronized (SoLoader.class) {
            if (soFileLoader != null) {
                sSoFileLoader = soFileLoader;
                return;
            }
            final Runtime runtime = Runtime.getRuntime();
            final Method nativeLoadRuntimeMethod = getNativeLoadRuntimeMethod();
            final boolean z = nativeLoadRuntimeMethod != null;
            final String classLoaderLdLoadLibrary = z ? Api14Utils.getClassLoaderLdLoadLibrary() : null;
            final String makeNonZipPath = makeNonZipPath(classLoaderLdLoadLibrary);
            sSoFileLoader = new SoFileLoader() {
                /* class com.facebook.soloader.SoLoader.AnonymousClass1 */

                /* JADX WARNING: Code restructure failed: missing block: B:18:0x0035, code lost:
                    if (r1 == null) goto L_?;
                 */
                /* JADX WARNING: Code restructure failed: missing block: B:19:0x0037, code lost:
                    android.util.Log.e(com.facebook.soloader.SoLoader.TAG, "Error when loading lib: " + r1 + " lib hash: " + getLibHash(r9) + " search path is " + r10);
                 */
                /* JADX WARNING: Code restructure failed: missing block: B:42:?, code lost:
                    return;
                 */
                /* JADX WARNING: Code restructure failed: missing block: B:43:?, code lost:
                    return;
                 */
                /* JADX WARNING: Removed duplicated region for block: B:38:0x009e  */
                @Override // com.facebook.soloader.SoFileLoader
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public void load(java.lang.String r9, int r10) {
                    /*
                    // Method dump skipped, instructions count: 205
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.facebook.soloader.SoLoader.AnonymousClass1.load(java.lang.String, int):void");
                }

                /* JADX WARNING: Code restructure failed: missing block: B:13:0x0039, code lost:
                    r0 = move-exception;
                 */
                /* JADX WARNING: Code restructure failed: missing block: B:15:?, code lost:
                    r1.close();
                 */
                /* JADX WARNING: Code restructure failed: missing block: B:16:0x003e, code lost:
                    r1 = move-exception;
                 */
                /* JADX WARNING: Code restructure failed: missing block: B:17:0x003f, code lost:
                    r7.addSuppressed(r1);
                 */
                /* JADX WARNING: Code restructure failed: missing block: B:18:0x0042, code lost:
                    throw r0;
                 */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                private java.lang.String getLibHash(java.lang.String r7) {
                    /*
                        r6 = this;
                        java.io.File r0 = new java.io.File     // Catch:{ IOException -> 0x004f, SecurityException -> 0x0049, NoSuchAlgorithmException -> 0x0043 }
                        r0.<init>(r7)     // Catch:{ IOException -> 0x004f, SecurityException -> 0x0049, NoSuchAlgorithmException -> 0x0043 }
                        java.lang.String r7 = "MD5"
                        java.security.MessageDigest r7 = java.security.MessageDigest.getInstance(r7)     // Catch:{ IOException -> 0x004f, SecurityException -> 0x0049, NoSuchAlgorithmException -> 0x0043 }
                        java.io.FileInputStream r1 = new java.io.FileInputStream     // Catch:{ IOException -> 0x004f, SecurityException -> 0x0049, NoSuchAlgorithmException -> 0x0043 }
                        r1.<init>(r0)     // Catch:{ IOException -> 0x004f, SecurityException -> 0x0049, NoSuchAlgorithmException -> 0x0043 }
                        r0 = 4096(0x1000, float:5.74E-42)
                        byte[] r0 = new byte[r0]     // Catch:{ all -> 0x0037 }
                    L_0x0014:
                        int r2 = r1.read(r0)     // Catch:{ all -> 0x0037 }
                        r3 = 0
                        if (r2 <= 0) goto L_0x001f
                        r7.update(r0, r3, r2)     // Catch:{ all -> 0x0037 }
                        goto L_0x0014
                    L_0x001f:
                        java.lang.String r0 = "%32x"
                        r2 = 1
                        java.lang.Object[] r4 = new java.lang.Object[r2]     // Catch:{ all -> 0x0037 }
                        java.math.BigInteger r5 = new java.math.BigInteger     // Catch:{ all -> 0x0037 }
                        byte[] r7 = r7.digest()     // Catch:{ all -> 0x0037 }
                        r5.<init>(r2, r7)     // Catch:{ all -> 0x0037 }
                        r4[r3] = r5     // Catch:{ all -> 0x0037 }
                        java.lang.String r7 = java.lang.String.format(r0, r4)     // Catch:{ all -> 0x0037 }
                        r1.close()
                        goto L_0x0054
                    L_0x0037:
                        r7 = move-exception
                        throw r7     // Catch:{ all -> 0x0039 }
                    L_0x0039:
                        r0 = move-exception
                        r1.close()     // Catch:{ all -> 0x003e }
                        goto L_0x0042
                    L_0x003e:
                        r1 = move-exception
                        r7.addSuppressed(r1)
                    L_0x0042:
                        throw r0
                    L_0x0043:
                        r7 = move-exception
                        java.lang.String r7 = r7.toString()
                        goto L_0x0054
                    L_0x0049:
                        r7 = move-exception
                        java.lang.String r7 = r7.toString()
                        goto L_0x0054
                    L_0x004f:
                        r7 = move-exception
                        java.lang.String r7 = r7.toString()
                    L_0x0054:
                        return r7
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.facebook.soloader.SoLoader.AnonymousClass1.getLibHash(java.lang.String):java.lang.String");
                }
            };
        }
    }

    @Nullable
    private static Method getNativeLoadRuntimeMethod() {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 27) {
            try {
                Method declaredMethod = Runtime.class.getDeclaredMethod("nativeLoad", String.class, ClassLoader.class, String.class);
                declaredMethod.setAccessible(true);
                return declaredMethod;
            } catch (NoSuchMethodException | SecurityException e) {
                Log.w(TAG, "Cannot get nativeLoad method", e);
            }
        }
        return null;
    }

    private static boolean checkIfSystemApp(Context context, int i) {
        return ((i & 32) != 0 || context == null || (context.getApplicationInfo().flags & 129) == 0) ? false : true;
    }

    public static void setInTestMode() {
        TestOnlyUtils.setSoSources(new SoSource[]{new NoopSoSource()});
    }

    public static void deinitForTest() {
        TestOnlyUtils.setSoSources(null);
    }

    static class TestOnlyUtils {
        TestOnlyUtils() {
        }

        static void setSoSources(SoSource[] soSourceArr) {
            SoLoader.sSoSourcesLock.writeLock().lock();
            try {
                SoSource[] unused = SoLoader.sSoSources = soSourceArr;
                SoLoader.access$208();
            } finally {
                SoLoader.sSoSourcesLock.writeLock().unlock();
            }
        }

        static void setSoFileLoader(SoFileLoader soFileLoader) {
            SoLoader.sSoFileLoader = soFileLoader;
        }

        static void resetStatus() {
            synchronized (SoLoader.class) {
                SoLoader.sLoadedLibraries.clear();
                SoLoader.sLoadingLibraries.clear();
                SoLoader.sSoFileLoader = null;
            }
            setSoSources(null);
        }
    }

    public static void setSystemLoadLibraryWrapper(SystemLoadLibraryWrapper systemLoadLibraryWrapper) {
        sSystemLoadLibraryWrapper = systemLoadLibraryWrapper;
    }

    public static final class WrongAbiError extends UnsatisfiedLinkError {
        WrongAbiError(Throwable th, String str) {
            super("APK was built for a different platform. Supported ABIs: " + Arrays.toString(SysUtil.getSupportedAbis()) + " error: " + str);
            initCause(th);
        }
    }

    @Nullable
    public static String getLibraryPath(String str) throws IOException {
        sSoSourcesLock.readLock().lock();
        try {
            String str2 = null;
            if (sSoSources != null) {
                int i = 0;
                while (str2 == null) {
                    SoSource[] soSourceArr = sSoSources;
                    if (i >= soSourceArr.length) {
                        break;
                    }
                    str2 = soSourceArr[i].getLibraryPath(str);
                    i++;
                }
            }
            return str2;
        } finally {
            sSoSourcesLock.readLock().unlock();
        }
    }

    @Nullable
    public static String[] getLibraryDependencies(String str) throws IOException {
        sSoSourcesLock.readLock().lock();
        try {
            String[] strArr = null;
            if (sSoSources != null) {
                int i = 0;
                while (strArr == null) {
                    SoSource[] soSourceArr = sSoSources;
                    if (i >= soSourceArr.length) {
                        break;
                    }
                    strArr = soSourceArr[i].getLibraryDependencies(str);
                    i++;
                }
            }
            return strArr;
        } finally {
            sSoSourcesLock.readLock().unlock();
        }
    }

    public static boolean loadLibrary(String str) {
        return loadLibrary(str, 0);
    }

    public static boolean loadLibrary(String str, int i) throws UnsatisfiedLinkError {
        SystemLoadLibraryWrapper systemLoadLibraryWrapper;
        boolean z;
        ReentrantReadWriteLock reentrantReadWriteLock = sSoSourcesLock;
        reentrantReadWriteLock.readLock().lock();
        try {
            if (sSoSources == null) {
                if ("http://www.android.com/".equals(System.getProperty("java.vendor.url"))) {
                    assertInitialized();
                } else {
                    synchronized (SoLoader.class) {
                        z = !sLoadedLibraries.contains(str);
                        if (z) {
                            SystemLoadLibraryWrapper systemLoadLibraryWrapper2 = sSystemLoadLibraryWrapper;
                            if (systemLoadLibraryWrapper2 != null) {
                                systemLoadLibraryWrapper2.loadLibrary(str);
                            } else {
                                System.loadLibrary(str);
                            }
                        }
                    }
                    reentrantReadWriteLock.readLock().unlock();
                    return z;
                }
            }
            reentrantReadWriteLock.readLock().unlock();
            if (!isSystemApp || (systemLoadLibraryWrapper = sSystemLoadLibraryWrapper) == null) {
                String mapLibName = MergedSoMapping.mapLibName(str);
                return loadLibraryBySoName(System.mapLibraryName(mapLibName != null ? mapLibName : str), str, mapLibName, i, null);
            }
            systemLoadLibraryWrapper.loadLibrary(str);
            return true;
        } catch (Throwable th) {
            sSoSourcesLock.readLock().unlock();
            throw th;
        }
    }

    static void loadLibraryBySoName(String str, int i, StrictMode.ThreadPolicy threadPolicy) {
        loadLibraryBySoNameImpl(str, null, null, i, threadPolicy);
    }

    private static boolean loadLibraryBySoName(String str, @Nullable String str2, @Nullable String str3, int i, @Nullable StrictMode.ThreadPolicy threadPolicy) {
        boolean z;
        boolean z2 = false;
        do {
            try {
                z2 = loadLibraryBySoNameImpl(str, str2, str3, i, threadPolicy);
                z = false;
                continue;
            } catch (UnsatisfiedLinkError e) {
                int i2 = sSoSourcesVersion;
                sSoSourcesLock.writeLock().lock();
                try {
                    z = true;
                    if (sApplicationSoSource == null || !sApplicationSoSource.checkAndMaybeUpdate()) {
                        z = false;
                    } else {
                        Log.w(TAG, "sApplicationSoSource updated during load: " + str + ", attempting load again.");
                        sSoSourcesVersion = sSoSourcesVersion + 1;
                    }
                    sSoSourcesLock.writeLock().unlock();
                    if (sSoSourcesVersion == i2) {
                        throw e;
                    }
                } catch (IOException e2) {
                    throw new RuntimeException(e2);
                } catch (Throwable th) {
                    sSoSourcesLock.writeLock().unlock();
                    throw th;
                }
            }
        } while (z);
        return z2;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:19:0x003a, code lost:
        monitor-enter(r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x003b, code lost:
        if (r3 != false) goto L_0x00a8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:?, code lost:
        monitor-enter(com.facebook.soloader.SoLoader.class);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0042, code lost:
        if (r1.contains(r9) == false) goto L_0x004a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0044, code lost:
        if (r11 != null) goto L_0x0049;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0046, code lost:
        monitor-exit(com.facebook.soloader.SoLoader.class);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0047, code lost:
        monitor-exit(r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0048, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0049, code lost:
        r3 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x004a, code lost:
        monitor-exit(com.facebook.soloader.SoLoader.class);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x004b, code lost:
        if (r3 != false) goto L_0x00a8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:?, code lost:
        android.util.Log.d(com.facebook.soloader.SoLoader.TAG, "About to load: " + r9);
        doLoadLibraryBySoName(r9, r12, r13);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x0066, code lost:
        monitor-enter(com.facebook.soloader.SoLoader.class);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:?, code lost:
        android.util.Log.d(com.facebook.soloader.SoLoader.TAG, "Loaded: " + r9);
        r1.add(r9);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x0080, code lost:
        monitor-exit(com.facebook.soloader.SoLoader.class);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x0085, code lost:
        r9 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x0086, code lost:
        r10 = r9.getMessage();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x008a, code lost:
        if (r10 == null) goto L_0x00a4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x00a3, code lost:
        throw new com.facebook.soloader.SoLoader.WrongAbiError(r9, r10.substring(r10.lastIndexOf("unexpected e_machine:")));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x00a4, code lost:
        throw r9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:57:0x00aa, code lost:
        if ((r12 & 16) != 0) goto L_0x0125;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:0x00b0, code lost:
        if (android.text.TextUtils.isEmpty(r10) != false) goto L_0x00bb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:61:0x00b8, code lost:
        if (com.facebook.soloader.SoLoader.sLoadedAndMergedLibraries.contains(r10) == false) goto L_0x00bb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:62:0x00ba, code lost:
        r2 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:63:0x00bb, code lost:
        if (r11 == null) goto L_0x0125;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:64:0x00bd, code lost:
        if (r2 != false) goto L_0x0125;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x00bf, code lost:
        r11 = com.facebook.soloader.SoLoader.SYSTRACE_LIBRARY_LOADING;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:66:0x00c1, code lost:
        if (r11 == false) goto L_0x00ca;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:67:0x00c3, code lost:
        com.facebook.soloader.Api18TraceUtils.beginTraceSection("MergedSoMapping.invokeJniOnload[", r10, "]");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:?, code lost:
        android.util.Log.d(com.facebook.soloader.SoLoader.TAG, "About to merge: " + r10 + " / " + r9);
        com.facebook.soloader.MergedSoMapping.invokeJniOnload(r10);
        com.facebook.soloader.SoLoader.sLoadedAndMergedLibraries.add(r10);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x00f0, code lost:
        if (r11 == false) goto L_0x0125;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x00f2, code lost:
        com.facebook.soloader.Api18TraceUtils.endSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x00f6, code lost:
        r9 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x00f8, code lost:
        r11 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x011c, code lost:
        throw new java.lang.RuntimeException("Failed to call JNI_OnLoad from '" + r10 + "', which has been merged into '" + r9 + "'.  See comment for details.", r11);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x011f, code lost:
        if (com.facebook.soloader.SoLoader.SYSTRACE_LIBRARY_LOADING != false) goto L_0x0121;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x0121, code lost:
        com.facebook.soloader.Api18TraceUtils.endSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x0124, code lost:
        throw r9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x0125, code lost:
        monitor-exit(r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x0128, code lost:
        return !r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean loadLibraryBySoNameImpl(java.lang.String r9, @javax.annotation.Nullable java.lang.String r10, @javax.annotation.Nullable java.lang.String r11, int r12, @javax.annotation.Nullable android.os.StrictMode.ThreadPolicy r13) {
        /*
        // Method dump skipped, instructions count: 303
        */
        throw new UnsupportedOperationException("Method not decompiled: com.facebook.soloader.SoLoader.loadLibraryBySoNameImpl(java.lang.String, java.lang.String, java.lang.String, int, android.os.StrictMode$ThreadPolicy):boolean");
    }

    public static File unpackLibraryAndDependencies(String str) throws UnsatisfiedLinkError {
        assertInitialized();
        try {
            return unpackLibraryBySoName(System.mapLibraryName(str));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:57:0x0112  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x0117  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x0131  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void doLoadLibraryBySoName(java.lang.String r12, int r13, @javax.annotation.Nullable android.os.StrictMode.ThreadPolicy r14) throws java.lang.UnsatisfiedLinkError {
        /*
        // Method dump skipped, instructions count: 396
        */
        throw new UnsupportedOperationException("Method not decompiled: com.facebook.soloader.SoLoader.doLoadLibraryBySoName(java.lang.String, int, android.os.StrictMode$ThreadPolicy):void");
    }

    @Nullable
    public static String makeNonZipPath(String str) {
        if (str == null) {
            return null;
        }
        String[] split = str.split(":");
        ArrayList arrayList = new ArrayList(split.length);
        for (String str2 : split) {
            if (!str2.contains("!")) {
                arrayList.add(str2);
            }
        }
        return TextUtils.join(":", arrayList);
    }

    static File unpackLibraryBySoName(String str) throws IOException {
        sSoSourcesLock.readLock().lock();
        try {
            for (SoSource soSource : sSoSources) {
                File unpackLibrary = soSource.unpackLibrary(str);
                if (unpackLibrary != null) {
                    return unpackLibrary;
                }
            }
            sSoSourcesLock.readLock().unlock();
            throw new FileNotFoundException(str);
        } finally {
            sSoSourcesLock.readLock().unlock();
        }
    }

    private static void assertInitialized() {
        if (!isInitialized()) {
            throw new RuntimeException("SoLoader.init() not yet called");
        }
    }

    public static boolean isInitialized() {
        ReentrantReadWriteLock reentrantReadWriteLock = sSoSourcesLock;
        reentrantReadWriteLock.readLock().lock();
        try {
            boolean z = sSoSources != null;
            reentrantReadWriteLock.readLock().unlock();
            return z;
        } catch (Throwable th) {
            sSoSourcesLock.readLock().unlock();
            throw th;
        }
    }

    public static int getSoSourcesVersion() {
        return sSoSourcesVersion;
    }

    public static void prependSoSource(SoSource soSource) throws IOException {
        ReentrantReadWriteLock reentrantReadWriteLock = sSoSourcesLock;
        reentrantReadWriteLock.writeLock().lock();
        try {
            Log.d(TAG, "Prepending to SO sources: " + soSource);
            assertInitialized();
            soSource.prepare(makePrepareFlags());
            SoSource[] soSourceArr = sSoSources;
            SoSource[] soSourceArr2 = new SoSource[(soSourceArr.length + 1)];
            soSourceArr2[0] = soSource;
            System.arraycopy(soSourceArr, 0, soSourceArr2, 1, soSourceArr.length);
            sSoSources = soSourceArr2;
            sSoSourcesVersion++;
            Log.d(TAG, "Prepended to SO sources: " + soSource);
            reentrantReadWriteLock.writeLock().unlock();
        } catch (Throwable th) {
            sSoSourcesLock.writeLock().unlock();
            throw th;
        }
    }

    public static String makeLdLibraryPath() {
        sSoSourcesLock.readLock().lock();
        try {
            assertInitialized();
            Log.d(TAG, "makeLdLibraryPath");
            ArrayList arrayList = new ArrayList();
            SoSource[] soSourceArr = sSoSources;
            if (soSourceArr != null) {
                for (SoSource soSource : soSourceArr) {
                    soSource.addToLdLibraryPath(arrayList);
                }
            }
            String join = TextUtils.join(":", arrayList);
            Log.d(TAG, "makeLdLibraryPath final path: " + join);
            return join;
        } finally {
            sSoSourcesLock.readLock().unlock();
        }
    }

    /* JADX INFO: finally extract failed */
    public static boolean areSoSourcesAbisSupported() {
        ReentrantReadWriteLock reentrantReadWriteLock = sSoSourcesLock;
        reentrantReadWriteLock.readLock().lock();
        try {
            if (sSoSources != null) {
                String[] supportedAbis = SysUtil.getSupportedAbis();
                for (SoSource soSource : sSoSources) {
                    String[] soSourceAbis = soSource.getSoSourceAbis();
                    for (String str : soSourceAbis) {
                        boolean z = false;
                        for (int i = 0; i < supportedAbis.length && !z; i++) {
                            z = str.equals(supportedAbis[i]);
                        }
                        if (!z) {
                            Log.e(TAG, "abi not supported: " + str);
                            reentrantReadWriteLock = sSoSourcesLock;
                        }
                    }
                }
                sSoSourcesLock.readLock().unlock();
                return true;
            }
            reentrantReadWriteLock.readLock().unlock();
            return false;
        } catch (Throwable th) {
            sSoSourcesLock.readLock().unlock();
            throw th;
        }
    }

    /* access modifiers changed from: private */
    public static class Api14Utils {
        private Api14Utils() {
        }

        public static String getClassLoaderLdLoadLibrary() {
            ClassLoader classLoader = SoLoader.class.getClassLoader();
            if (classLoader == null || (classLoader instanceof BaseDexClassLoader)) {
                try {
                    return (String) BaseDexClassLoader.class.getMethod("getLdLibraryPath", new Class[0]).invoke((BaseDexClassLoader) classLoader, new Object[0]);
                } catch (Exception e) {
                    throw new RuntimeException("Cannot call getLdLibraryPath", e);
                }
            } else {
                throw new IllegalStateException("ClassLoader " + classLoader.getClass().getName() + " should be of type BaseDexClassLoader");
            }
        }
    }
}
