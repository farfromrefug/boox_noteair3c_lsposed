package com.akylas.noteair3c

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.AndroidAppHelper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.content.IntentCompat
import com.akylas.noteair3c.lsposed.BuildConfig
import com.akylas.noteair3c.utils.Preferences
import com.akylas.noteair3c.utils.registerReceiver

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedHelpers.findClass;
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.callStaticMethod

val rootPackage = "com.onyx";
val TAG = "com.akylas.noteair3c";


val supportedPackages =
    arrayOf(
        "android",
        "com.onyx",
        "com.onyx.kreader"
    )

class ModuleMain : IXposedHookLoadPackage {


    var _appContext: Context? = null
    val appContext: Context
        get() {
            if (_appContext == null) {
                _appContext = AndroidAppHelper.currentApplication()

            }

            return _appContext!!
        }

    companion object {
        var AKYLAS_DISABLE_LIGHT = "akylas_disable_light"
        var AKYLAS_ENABLE_LIGHT = "akylas_enable_light"
        var AKYLAS_DISABLE_LIGHT_DONE = "akylas_disable_light_done"
        var mPowerManager: android.os.PowerManager? = null;
        var mInputManager: Any? = null;
        var mPhoneWindowManager: Any? = null;
        var mPhoneWindowManagerHandler: Handler? = null;
        var mVolumeWakeLock: android.os.PowerManager.WakeLock? = null;
        var mLastUpKeyEvent: android.view.KeyEvent? = null;
        var mLastDownKeyEvent: android.view.KeyEvent? = null;
        var mAlarmService: AlarmManager? = null
        var mPhoneWindowHelper: Any? = null
        var ViewUpdateHelper: Class<Any>? = null
        var TransparentDream: Class<Any>? = null
        var disableWakeUpFrontLightEnabled = false
        var inStandBy = false
        var usingKreader = false
    }

    var mPagesTurned = 0


    fun forceHideKeyguard() {
        val keyguardServiceDelegate = getObjectField(mPhoneWindowManager, "mKeyguardDelegate")
        Log.i("forceHideKeyguard " + keyguardServiceDelegate)
        if (keyguardServiceDelegate != null) {
            callMethod(
                keyguardServiceDelegate,
                "startKeyguardExitAnimation",
                SystemClock.uptimeMillis(),
                0
            )
        }
    }

    fun disableKeyguard() {
        val keyguardServiceDelegate = getObjectField(mPhoneWindowManager, "mKeyguardDelegate")
        if (keyguardServiceDelegate != null) {
            Log.i("disableKeyguard " + keyguardServiceDelegate)
            try {
                callMethod(
                    keyguardServiceDelegate,
                    "setKeyguardEnabled",
                    false
                )
                forceHideKeyguard()
            } catch (thr: Throwable) {
                Log.ex(thr)
            }
        }
    }

    fun enableKeyguard() {
        val keyguardServiceDelegate = getObjectField(mPhoneWindowManager, "mKeyguardDelegate")
        if (keyguardServiceDelegate != null) {
            Log.i("disableKeyguard " + keyguardServiceDelegate)
            try {
                callMethod(
                    keyguardServiceDelegate,
                    "setKeyguardEnabled",
                    true
                )
            } catch (thr: Throwable) {
                Log.ex(thr)
            }
        }
    }

//    fun getCurrentLightValue(): Int {
//        try {
//            return callMethod(getLightEntry(7), "getCurrentValue") as Int
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return -1
//        }
//    }
//    fun getLightEntry(index: Int): Any {
//        try {
//
//            return callMethod( getObjectField(mCTMController!!, "backLights"), "get", index)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return -1
//        }
//    }
//    fun setCurrentLightValue(value: Int) {
//        try {
//              callMethod(getLightEntry(7), "setLightValue", value)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//    fun turnOnLight(value: Boolean) {
//        try {
//              callMethod(getLightEntry(7), "turnOnLight", value)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }

    fun disableScreenUpdate() {
        try {
            callStaticMethod(ViewUpdateHelper, "enableScreenUpdate", false)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun enableScreenUpdate() {
        try {
            callStaticMethod(ViewUpdateHelper, "enableScreenUpdate", true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendPastKeyDownEvent() {
        if (mLastDownKeyEvent != null) {
            Log.i("sendPastKeyDownEvent " + mLastDownKeyEvent + " " + usingKreader)
            XposedHelpers.callMethod(mInputManager, "injectInputEvent", mLastDownKeyEvent, 0)
            mLastDownKeyEvent = null;
//            XposedHelpers.callMethod(
//                mPhoneWindowHelper, "sendKeyEvent",
//                25
//            )

            if (mVolumeWakeLock?.isHeld == true) {
                mVolumeWakeLock?.release()
            }

            val prefs = Preferences()
            val delay = if (usingKreader) prefs.getInt("kreader_sleep_delay", 1299) else prefs.getInt("sleep_delay", 649)
            val cleanup_delay = prefs.getInt("volume_key_cleanup_delay", 1000)
            val key_up_delay = prefs.getInt(if (usingKreader) "kreader_volume_key_up_delay" else "volume_key_up_delay", 300)
            Log.i("delay $usingKreader delay:$delay cleanup_delay:$cleanup_delay key_up_delay:$key_up_delay")
            mPagesTurned += 1
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(595, delay.toLong());
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(596, (delay + cleanup_delay).toLong());
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(601, key_up_delay.toLong());
        }
    }

    fun sendPastKeyUpEvent() {
        if (mLastUpKeyEvent != null) {
            Log.i("sendPastKeyUpEvent "  + mLastUpKeyEvent)
            XposedHelpers.callMethod(mInputManager, "injectInputEvent",mLastUpKeyEvent, 0)
//            XposedHelpers.callMethod(
//                mPhoneWindowHelper, "sendKeyEvent",
//                24
//            )
            mLastUpKeyEvent = null
        }
    }

    fun handleVolumeKeyEventDown(paramKeyEvent: KeyEvent): Boolean {
//        Log.i("handleVolumeKeyEventDown " + paramKeyEvent.keyCode + " " + paramKeyEvent.action + " " + mRefreshItent + " " + mAlarmService + " " + mCurrentDevice + " " + mPowerUtil)
        if (!mPowerManager!!.isInteractive) {
            val wakeLock = mVolumeWakeLock!!;
            if (!wakeLock.isHeld) {;
                mPhoneWindowManagerHandler?.removeMessages(595);
                mPhoneWindowManagerHandler?.removeMessages(596);
                disableWakeUpFrontLightEnabled = true
                forceHideKeyguard()
//                disableKeyguard()
                callMethod(
                    mPhoneWindowManager,
                    "wakeUpFromPowerKey",
                    SystemClock.uptimeMillis()
                )
//                val intent = Intent("AKYLAS_VOLUME_DOWN")
//                intent.putExtra("event", mLastDownKeyEvent)
//                if (mLastUpKeyEvent != null) {
//                    intent.putExtra("eventUp", mLastUpKeyEvent)
//                    mLastUpKeyEvent = null
//                }
//                mLastDownKeyEvent = null
//                appContext.sendBroadcast(intent)
//                sendPastKeyDownEvent()
//                sendPastKeyUpEvent()
                wakeLock.acquire(2300L);
                mLastDownKeyEvent = KeyEvent(paramKeyEvent);
                mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(600, 200L)
                return true;
            }
        }
        return false;
    }

    fun handleVolumeKeyEventUp(paramKeyEvent: KeyEvent): Boolean {
        val wakeLock = mVolumeWakeLock!!;
        if (wakeLock.isHeld && mLastUpKeyEvent == null) {
            Log.i("handleVolumeKeyEventUp " + paramKeyEvent)
            mLastUpKeyEvent = KeyEvent(paramKeyEvent);
            return true
        }
        return false;
    }

    fun handleWakeUpOnVolume(paramKeyEvent: KeyEvent): Boolean {
        var i = paramKeyEvent.keyCode;
        if (i == 24 || i == 25) {
            i = paramKeyEvent.action;
            if (paramKeyEvent.action == KeyEvent.ACTION_UP) {
                return handleVolumeKeyEventUp(paramKeyEvent);
            } else if (paramKeyEvent.action == KeyEvent.ACTION_DOWN) {
                return handleVolumeKeyEventDown(paramKeyEvent);
            }
        }
        return false;
    }

    // kreader
//    var mVolumeDownRegister: BroadcastReceiver? = null
//    var mVolumeUpRegister: BroadcastReceiver? = null
//    var mDisableLightRegister: BroadcastReceiver? = null
//    var mScreenOffRegister: BroadcastReceiver? = null
//    var mHandler: Handler? = null
//    var mUnregisterRunnable: Runnable? = null

//    fun unregisterReaderVolumeReceivers(currentActivity: Activity) {
//        if (mVolumeDownRegister != null) {
//            currentActivity.unregisterReceiver(mVolumeDownRegister!!)
//            mVolumeDownRegister = null
//        }
//        if (mVolumeUpRegister != null) {
//            currentActivity.unregisterReceiver(mVolumeUpRegister!!)
//            mVolumeUpRegister = null
//        }
//    }
//
//    fun registerReaderVolumeReceivers(currentActivity: Activity) {
//
//        if (mVolumeDownRegister == null) {
//            mVolumeDownRegister =
//                currentActivity.registerReceiver(IntentFilter("AKYLAS_VOLUME_DOWN")) { intent ->
////                        Log.i("AKYLAS_VOLUME_DOWN " + lpparam.packageName + " " + it.thisObject + " " + mHandleManager)
////                    val wakeLock = mVolumeWakeLock!!;
////                    if (!wakeLock.isHeld) {
////                        wakeLock.acquire(2300L);
////                    }
//                    val event = IntentCompat.getParcelableExtra(
//                        intent!!, "event",
//                        KeyEvent::class.java
//                    )
//                    Log.i("AKYLAS_VOLUME_DOWN1 " + event)
//                    callMethod(mHandleManager, "onKeyDown", event!!.keyCode, event)
//                    val eventUp = IntentCompat.getParcelableExtra(
//                        intent, "eventUp",
//                        KeyEvent::class.java
//                    )
//                    if (eventUp != null) {
//                        callMethod(mHandleManager, "onKeyUp", eventUp.keyCode, eventUp)
//                    }
//                }
//        }
//        if (mVolumeUpRegister == null) {
//            mVolumeUpRegister =
//                currentActivity.registerReceiver(IntentFilter("AKYLAS_VOLUME_UP")) { intent ->
//                    //                        Log.i("AKYLAS_VOLUME_UP " + lpparam.packageName + " " + it.thisObject + " " + mHandleManager)
//                    val event = IntentCompat.getParcelableExtra(
//                        intent!!, "event",
//                        KeyEvent::class.java
//                    )
//                    Log.i("AKYLAS_VOLUME_UP1 " + event)
//                    callMethod(mHandleManager, "onKeyUp", event!!.keyCode, event)
//                }
//        }
//    }

    @SuppressLint("NewApi")
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        Log.i("handleLoadPackage " + lpparam.packageName + " " + AndroidAppHelper.currentApplication())
        ViewUpdateHelper = findClass(
            "android.onyx.ViewUpdateHelper",
            lpparam.classLoader
        ) as Class<Any>?
        try {
            TransparentDream = XposedHelpers.findClassIfExists(
                "com.onyx.common.dream.ui.TransparentDream",
                lpparam.classLoader
            ) as Class<Any>?
        } catch (e: Exception) {
        }
//            for (method in ViewUpdateHelper.declaredMethods) {
//                method.isAccessible =true
//                Log.i("patching ViewUpdateHelper." + method.name)
//                method.hookBefore() {
//                    Log.i("ViewUpdateHelper." + method.name)
//
//                }
        var ignoreGC = false
        if (ViewUpdateHelper != null) {
            findMethod(ViewUpdateHelper!!) { name == "fillWhiteOnWakeup" }
                .hookBefore {
                    Log.i("com.onyx.fillWhiteOnWakeup " + it.args[0] + " " + it.args[1] + " " + disableWakeUpFrontLightEnabled)

                }
            findMethod(ViewUpdateHelper!!) { name == "applyGCOnce" }
                .hookBefore {
//                    Log.i(
//                        "com.onyx.applyGCOnce " + " " + lpparam.packageName + " " + disableWakeUpFrontLightEnabled + " " + inStandBy + " " + ignoreGC + " " + Thread.currentThread().stackTrace.joinToString(
//                            "\n "
//                        )
//                    )
                    if (inStandBy || disableWakeUpFrontLightEnabled || ignoreGC) {
                        it.result = 1
                    }
                }
            findMethod(ViewUpdateHelper!!) { name == "repaintEverything" && parameterCount == 0 }
                .hookBefore {
                    Log.i("com.onyx.repaintEverything " + " " + lpparam.packageName + " " + disableWakeUpFrontLightEnabled + " " + inStandBy + " " + ignoreGC)
                    if (inStandBy || disableWakeUpFrontLightEnabled || ignoreGC) {
                        it.result = 0
                    }
                }
            findMethod(ViewUpdateHelper!!) { name == "repaintEverything" && parameterCount == 1 }
                .hookBefore {
                    Log.i("com.onyx.repaintEverything1 " + " " + lpparam.packageName + " " + disableWakeUpFrontLightEnabled + " " + inStandBy + " " + ignoreGC)
                    if (inStandBy || disableWakeUpFrontLightEnabled || ignoreGC) {
                        it.result = 0
                    }
                }
        }
        if (TransparentDream != null) {
            findMethod(TransparentDream!!) { name == "onAttachedToWindow" }.hookBefore {
                Log.i("TransparentDream.onAttachedToWindow" + " " + lpparam.packageName + " " + disableWakeUpFrontLightEnabled + " " + inStandBy)
                inStandBy = true
            }
            val method = findMethod(TransparentDream!!) { name == "initShowContent" }
            method.hookBefore {
                ignoreGC = true
                Log.i("TransparentDream.initShowContent" + " " + lpparam.packageName + " " + disableWakeUpFrontLightEnabled + " " + inStandBy)
            }
            method.hookAfter {
                ignoreGC = false
                Log.i("TransparentDream.initShowContent after" + " " + lpparam.packageName + " " + disableWakeUpFrontLightEnabled + " " + inStandBy)

            }
        }

        if (lpparam.packageName == "com.android.systemui") {

            // we disable fingerprint to unlock while screen is off
            val keyguardUpdateMonitorClass =
                findClass("com.android.keyguard.KeyguardUpdateMonitor", lpparam.classLoader)
            findMethod(keyguardUpdateMonitorClass) { name == "shouldListenForFingerprint" }
                .hookBefore {
                    val ctx = AndroidAppHelper.currentApplication()
                    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager;
                    if (!pm.isInteractive) {
                        it.result = false
                    }
                }
        } else if (lpparam.packageName == "com.onyx") {
//            Log.i("patching  com.onyx" + lpparam.packageName + " " + disableLightDoneIntent)

//            mPowerUtil = findClass(
//                "com.onyx.android.libsetting.util.PowerUtil",
//                lpparam.classLoader
//            ) as Class<Any>?
//            Log.i("mPowerUtil " + mPowerUtil)
//            mCurrentDevice = callStaticMethod(
//                findClass(
//                    "com.onyx.android.sdk.device.Device",
//                    lpparam.classLoader
//                ), "currentDevice"
//            )
//            Log.i("currentDevice " + mCurrentDevice)
//            var receiverRegistered = false
            findMethod(
                findClass(
                    "com.onyx.common.dream.OnyxDaydreamService",
                    lpparam.classLoader
                )
            ) { name == "onCreate" }
                .hookBefore {
//                    Log.i("OnyxDaydreamService onCreate " + lpparam.packageName + " ")
//                    if (!receiverRegistered) {
//                        receiverRegistered = true
//////                        Log.i("OnyxDaydreamService receiverRegistered ")
//                        val intentFilter = IntentFilter()
//                        intentFilter.addAction("akylas.SCREEN_OFF")
//                        intentFilter.addAction("akylas.SCREEN_ON")
//                        intentFilter.addAction("android.intent.action.SCREEN_OFF")
//                        intentFilter.addAction("android.intent.action.SCREEN_ON")
//                        appContext.registerReceiver(intentFilter) { intent ->
//                            Log.i("com.onyx received " + intent?.action + " " + inStandBy)
//                            if (intent?.action == "akylas.SCREEN_ON" || intent?.action == "android.intent.action.SCREEN_ON") {
//                                inStandBy = false
//                            }
//                        }
//                    }
                }

//            val EpdController = findClass(
//                "com.onyx.android.sdk.api.device.epd.EpdController",
//                lpparam.classLoader
//            )
//
//            findMethod(EpdController) { name == "fillWhiteOnWakeup" }
//                .hookBefore {
//                    Log.i("EpdController.fillWhiteOnWakeup " + it.args[0] + it.args[1] + " " + disableWakeUpFrontLightEnabled)
////                    if (disableWakeUpFrontLightEnabled) {
////                        Log.i("ignoring setLightValue")
////                        it.result = 0
////                    }
//                }

//            val EpdDeviceManager = findClass(
//                "com.onyx.android.sdk.api.device.EpdDeviceManager",
//                lpparam.classLoader
//            )
//            val EInkHelper = findClass(
//                "android.onyx.optimization.EInkHelper",
//                lpparam.classLoader
//            )
//            for (method in EInkHelper.declaredMethods) {
//                method.isAccessible = true
//                Log.i("patching EInkHelper." + method.name)
//                method.hookBefore() {
//                    Log.i("EInkHelper." + method.name)
//
//                }
//            }
//            for (method in EpdDeviceManager.declaredMethods) {
//                method.isAccessible =true
//                Log.i("patching EpdDeviceManager." + method.name)
//                method.hookBefore() {
//                    Log.i("EpdDeviceManager1." + method.name)
//
//                }
//            }

        } else if (lpparam.packageName == "com.onyx.kreader") {
//            val EpdDeviceManager = findClass(
//                "com.onyx.android.sdk.api.device.EpdDeviceManager",
//                lpparam.classLoader
//            )
//            val EpdDevice = findClass(
//                "com.onyx.android.sdk.api.device.EpdDevice",
//                lpparam.classLoader
//            )
//            for (method in EpdDevice.declaredMethods) {
//                method.isAccessible =true
//                Log.i("patching EpdDevice." + method.name)
//                method.hookBefore() {
//                    Log.i("EpdDevice." + method.name)
//
//                }
//            }
//            for (method in EpdDeviceManager.declaredMethods) {
//                method.isAccessible =true
//                Log.i("patching EpdDeviceManager." + method.name)
//                method.hookBefore() {
//                    Log.i("EpdDeviceManager1." + method.name)
//
//                }
//            }
            findMethod(
                findClass(
                    "com.onyx.kreader.ui.ReaderActivity",
                    lpparam.classLoader
                ), true
            ) { name == "onCreate" }
                .hookBefore {
                    val currentActivity = it.thisObject as Activity
                    Log.i("onCreate " + currentActivity)
//                    if (mScreenOffRegister == null) {
//                        mScreenOffRegister =
//                            currentActivity.registerReceiver(IntentFilter(Intent.ACTION_SCREEN_OFF)) { intent ->
////                            Log.i("SCREEN__OFF " + SystemClock.uptimeMillis() + " " + mUnregisterRunnable)
//                                if (mUnregisterRunnable != null) {
//                                    mHandler?.removeCallbacks(mUnregisterRunnable!!)
//                                    mUnregisterRunnable = null
//                                }
//                            }
//                    }
//                    Log.i("onCreate " + lpparam.packageName + " " + it.thisObject + " " + mPowerManager + " " + mHandleManager)
//                    if (mHandler == null) {
//                        mHandler = Handler(Looper.getMainLooper());
//                    }
//                    if (mPowerManager == null) {
//                        mTurnPageUtils = findClass(
//                            "com.onyx.android.sdk.utils.TurnPageUtils",
//                            lpparam.classLoader
//                        ) as Class<Any>?
//                        mPowerManager =
//                            appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
//                        mVolumeWakeLock =
//                            mPowerManager!!.newWakeLock(268435462, "Sys::VolumeWakeLock")
//                        Log.i("onCreate init done " + lpparam.packageName + " " + it.thisObject + " " + mPowerManager)
//                    }
//                    mDisableLightRegister = appContext.registerReceiver(IntentFilter(AKYLAS_DISABLE_LIGHT)) { intent ->
//                        Log.i("reader received  AKYLAS_DISABLE_LIGHT " + currentActivity)
//                        mCurrentReaderActivity = currentActivity
//                    }
//                    Log.i("mDisableLightRegister " + mDisableLightRegister)
//                    Log.i("onCreate done " + lpparam.packageName + " " + it.thisObject + " " + mVolumeDownRegister + " " + mVolumeUpRegister)
                }

            findMethod(
                findClass(
                    "com.onyx.kreader.ui.ReaderActivity",
                    lpparam.classLoader
                ), true
            ) { name == "onResume" }
                .hookBefore {
                    val currentActivity = it.thisObject as Activity
                    appContext.sendBroadcast(Intent("KREADER_RESUME"))
                    Log.i("onResume " + currentActivity)
                    //we need to wait for this as getReaderBundle is returning null in onCreate
//                    if (mHandleManager == null) {
//                        mHandleManager = callMethod(
//                            callMethod(currentActivity, "getReaderBundle"),
//                            "getHandlerManager"
//                        )
//                    }
//                    registerReaderVolumeReceivers(currentActivity)
                }
            findMethod(
                findClass(
                    "com.onyx.kreader.ui.ReaderActivity",
                    lpparam.classLoader
                )
            ) { name == "onDestroy" }
                .hookBefore {
                    val currentActivity = it.thisObject as Activity
                    Log.i("onDestroy " + currentActivity)
//                    unregisterReaderVolumeReceivers(currentActivity)

//                    if (mDisableLightRegister != null) {
//                        currentActivity.unregisterReceiver(mDisableLightRegister!!)
//                        mDisableLightRegister = null
//                    }
                }
            findMethod(
                findClass(
                    "com.onyx.kreader.ui.ReaderActivity",
                    lpparam.classLoader
                )
            ) { name == "onStop" }
                .hookBefore {
                    val currentActivity = it.thisObject as Activity
                    appContext.sendBroadcast(Intent("KREADER_STOP"))
                    Log.i("onStop " + currentActivity + SystemClock.uptimeMillis() + " ")
                }
        } else if (lpparam.packageName == "android") {
            val CTMController = findClass(
                "android.onyx.brightness.CTMController",
                lpparam.classLoader
            )
            val OnyxPowerManager = findClass(
                "android.onyx.pm.OnyxPowerManager",
                lpparam.classLoader
            )

//            findMethod(OnyxPowerManager) { name == "fillWhiteOnWakeup" }
//                .hookBefore {
//                    Log.i("OnyxPowerManager.fillWhiteOnWakeup " + it.args[0] + it.args[1] + " " + disableWakeUpFrontLightEnabled)
//                }

//            findMethod(CTMController) { name == "init" }
//                .hookBefore {
//                    Log.i("CTMController init " + it.thisObject)
////                    mCTMController = it.thisObject
////                    if (disableWakeUpFrontLightEnabled) {
////                        Log.i("ignoring setLightValue")
////                        it.result = 0
////                    }
//                }
//            val LightEntry  =findClass(
//                "android.onyx.brightness.CTMController.LightEntry",
//                lpparam.classLoader
//            )

//            findMethod(CTMController) { name == "setLightValue" }
//                .hookBefore {
//                    Log.i("setLightValue " + it.args[0] + it.args[1]   + " " + disableWakeUpFrontLightEnabled)
////                    if (disableWakeUpFrontLightEnabled) {
////                        Log.i("ignoring setLightValue")
////                        it.result = 0
////                    }
//                }
            findMethod(CTMController) { name == "wakeup" }
                .hookBefore {
                    inStandBy = false
                    appContext.sendBroadcast(Intent("akylas.SCREEN_ON"))
                    enableScreenUpdate()
                    Log.i("wakeup " + " " + lpparam.packageName + " " + disableWakeUpFrontLightEnabled + " " + inStandBy)
                    if (disableWakeUpFrontLightEnabled) {
//                        Log.i("ignoring setLightValue")
                        it.result = 0
                    }
                }
            findMethod(CTMController) { name == "standby" }
                .hookBefore {
                    inStandBy = true
                    disableScreenUpdate()

                    appContext.sendBroadcast(Intent("akylas.SCREEN_OFF"))
                    Log.i("standby " + " " + lpparam.packageName + " " + disableWakeUpFrontLightEnabled + " " + inStandBy)
                    if (disableWakeUpFrontLightEnabled) {
//                        Log.i("ignoring setLightValue")
                        it.result = 0
                    }
                }

            findMethod(
                findClass(
                    "com.android.server.policy.PhoneWindowManager",
                    lpparam.classLoader
                )
            ) { name == "interceptKeyBeforeQueueing" }
                .hookBefore {
                    if (mPhoneWindowManager == null) {
                        mPhoneWindowManager = it.thisObject
                        mPhoneWindowManagerHandler =
                            getObjectField(mPhoneWindowManager, "mHandler") as Handler?
                        mPowerManager =
                            appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

                        Log.i("mPowerManager " + " " + mPowerManager)
//                        mRefreshItent = PendingIntent.getBroadcast(
//                            appContext,
//                            0,
//                            Intent("onyx_standby_test"),
//                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//                        );
                        val intentFilter = IntentFilter()
                        intentFilter.addAction("KREADER_RESUME")
                        intentFilter.addAction("KREADER_STOP")
                        appContext.registerReceiver(intentFilter) { intent ->

                            if (intent?.action == "KREADER_RESUME") {
                                usingKreader = true
                            } else if (intent?.action == "KREADER_STOP" && !inStandBy) {
                                usingKreader = false
                            }
                            Log.i("received KREADER action " + intent?.action + " " + inStandBy + " " + usingKreader)
                        }

                        mInputManager = callStaticMethod(findClass("android.hardware.input.InputManager",
                            lpparam.classLoader), "getInstance")
                        Log.i("mInputManager " + " " + mInputManager)

                        mPhoneWindowHelper =
                            XposedHelpers.getObjectField(mPhoneWindowManager, "mPhoneWindowHelper")
                        mAlarmService = appContext.getSystemService("alarm") as AlarmManager?
                        mVolumeWakeLock =
                            mPowerManager!!.newWakeLock(268435462, "Sys::VolumeWakeLock")

                    }
                    Log.i("interceptKeyBeforeQueueing " + lpparam.packageName + " " + mPowerManager?.isInteractive + " " + it.args[0])
//                    if (!mPowerManager!!.isInteractive) {
                    if (handleWakeUpOnVolume(it.args[0] as KeyEvent)) {
                        it.result = 0
                    }
//                    }
                }
//
//            findMethod(
//                findClass(
//                    "com.android.server.policy.keyguard.KeyguardServiceDelegate",
//                    lpparam.classLoader
//                )
//            ) { name == "hasKeyguard" }
//                .hookBefore {
//                    Log.i("hasKeyguard ")
//                    it.result = 0
//                }
//            Log.i("patching  PhoneWindowManager2" + lpparam.packageName + " " + mPowerManager)
            findMethod(
                findClass(
                    "com.android.server.policy.PhoneWindowManager.PolicyHandler",
                    lpparam.classLoader
                )
            ) { name == "handleMessage" }
                .hookBefore {
                    val message = it.args[0] as Message
                    val what = message.what

                    if (what == 595) {
                        Log.i("handleMessage 595, going back to sleep")
                        mPagesTurned = 0
                        callMethod(mPowerManager, "goToSleep", SystemClock.uptimeMillis(), 6, 0)
                        forceHideKeyguard()
                        if (mVolumeWakeLock?.isHeld == true) {
                            mVolumeWakeLock?.release()
                        }
                        it.result = true
                    } else if (what == 596) {
                        Log.i("handleMessage 596 cleaning up")
                        disableWakeUpFrontLightEnabled = false
                        it.result = true
                    } else if (what == 600) {
                        Log.i("handleMessage 600")
                        sendPastKeyDownEvent()
                        it.result = true
                    } else if (what == 601) {
                        Log.i("handleMessage 601")
//                        enableKeyguard()
                        sendPastKeyUpEvent()
                        it.result = true
                    }
                }
        }
    }
}
