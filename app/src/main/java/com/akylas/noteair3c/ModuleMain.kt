package com.akylas.noteair3c

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.AndroidAppHelper
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Message
import android.os.PowerManager
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.akylas.noteair3c.utils.Preferences
import com.akylas.noteair3c.utils.registerReceiver

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedHelpers.findClass;
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.callStaticMethod
import java.lang.reflect.Method
import kotlin.text.split

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
        var mPowerManager: android.os.PowerManager? = null;
        var mInputManager: Any? = null;
        var mPhoneWindowManager: Any? = null;
        var mPhoneWindowManagerHandler: Handler? = null;
        var mVolumeWakeLock: android.os.PowerManager.WakeLock? = null;
        var mLastUpKeyEvent: android.view.KeyEvent? = null;
        var mLastDownKeyEvent: android.view.KeyEvent? = null;
        var mAlarmService: AlarmManager? = null
        var mPhoneWindowHelper: Any? = null
        var PhoneWindowManager: Class<Any>? = null
        var mCTMController: Any? = null
        var mCTMControllerBackLights: Any? = null
        var turnOnLight: Method? = null
        var isLightOn: Method? = null
        var enableKeyguard: Method? = null
        var goToSleep: Method? = null
        var disableWakeUpFrontLightEnabled = false
        var warmLightOnBeforeStandby = false
        var brightnessLightOnBeforeStandby = false
        var inStandBy = false
        var goingToSleep = false
        var usingKreader = false
        var screenOff = false
        var currentAppName: String? = null
        var readerMode: Boolean = false
    }


    fun lightEntryStateBeforeStandby(type: Int): Boolean {
        try {
           return XposedHelpers.getBooleanField(getLightEntry(type), "stateBeforeStandby")
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return false
    }
    fun setLightEntryStateBeforeStandby(type: Int, value: Boolean) {
        try {
            XposedHelpers.setBooleanField(getLightEntry(type), "stateBeforeStandby", value)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    fun getLightEntry(index: Int): Any {
        try {

            return callMethod(mCTMControllerBackLights, "get", index)
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }

    fun setKeyguardEnabled(value: Boolean) {
        enableKeyguard?.invoke(mPhoneWindowManager, value)
    }


    fun sendPastKeyDownEvent() {
        if (mLastDownKeyEvent != null) {
            val newKeyEvent = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                mLastDownKeyEvent!!.action, mLastDownKeyEvent!!.keyCode,  mLastDownKeyEvent!!.repeatCount, mLastDownKeyEvent!!.metaState)
            Log.i("sendPastKeyDownEvent " + mLastDownKeyEvent + " " + usingKreader + " " + newKeyEvent + " :${mVolumeWakeLock?.isHeld}")
            XposedHelpers.callMethod(mInputManager, "injectInputEvent", newKeyEvent, 0)
            mLastDownKeyEvent = null;

            if (mVolumeWakeLock?.isHeld == true) {
                mVolumeWakeLock?.release()
            }
            val prefs = Preferences()
            val delay = if (usingKreader) prefs.getInt("kreader_sleep_delay", 1299) else prefs.getInt("sleep_delay", 649)
            val cleanup_delay = prefs.getInt("volume_key_cleanup_delay", 1000)
            val key_up_delay = prefs.getInt(if (usingKreader) "kreader_volume_key_up_delay" else "volume_key_up_delay", 300)
            Log.i("delay $usingKreader delay:$delay cleanup_delay:$cleanup_delay key_up_delay:$key_up_delay")
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(595, delay.toLong());
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(596, (delay + cleanup_delay).toLong());
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(601, key_up_delay.toLong());
        }
    }

    fun sendPastKeyUpEvent() {
        if (mLastUpKeyEvent != null) {
            val newKeyEvent = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                mLastUpKeyEvent!!.action, mLastUpKeyEvent!!.keyCode,  mLastUpKeyEvent!!.repeatCount, mLastUpKeyEvent!!.metaState)
            Log.i("sendPastKeyUpEvent "  + mLastUpKeyEvent + " " + newKeyEvent)
            XposedHelpers.callMethod(mInputManager, "injectInputEvent",newKeyEvent, 0)
            mLastUpKeyEvent = null
        }
    }

    fun putDeviceToSleep() {
        goToSleep!!.invoke(mPowerManager, SystemClock.uptimeMillis())
        goingToSleep = true
//                        callMethod(mPowerManager, "goToSleep", SystemClock.uptimeMillis(), 6, 0)
        if (mVolumeWakeLock?.isHeld == true) {
            mVolumeWakeLock?.release()
        }
//                        setLightValue(savedBrightness)
//                        setLightEntryStateBeforeStandby(6, warmLightOnBeforeStandby)
//                        setLightEntryStateBeforeStandby(7, brightnessLightOnBeforeStandby)
//                        Log.i("setLightEntryStateBeforeStandby done")
        disableWakeUpFrontLightEnabled = false

    }
    fun cleanupAfterSleep(){
        disableWakeUpFrontLightEnabled = false
        Log.i("setLightEntryStateBeforeStandby warmLightOnBeforeStandby:$warmLightOnBeforeStandby brightnessLightOnBeforeStandby:$brightnessLightOnBeforeStandby ")
        setLightEntryStateBeforeStandby(6, warmLightOnBeforeStandby)
        setLightEntryStateBeforeStandby(7, brightnessLightOnBeforeStandby)
    }

    fun handleDeviceWokenUp() {
        val prefs = Preferences()
        if (mLastDownKeyEvent != null) {
            val key_down_delay = prefs.getInt(if (usingKreader) "kreader_volume_key_down_delay" else "volume_key_down_delay", 200)
            Log.i("handleDeviceWokenUp volume down with delay $key_down_delay" + " mVolumeWakeLock:WakeLock:$mVolumeWakeLock:WakeLock")
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(600, key_down_delay.toLong())
        } else {
            val delay = if (usingKreader) prefs.getInt("kreader_sleep_delay", 1299) else prefs.getInt("sleep_delay", 649)
            val cleanup_delay = prefs.getInt("volume_key_cleanup_delay", 1000)
            val key_up_delay = prefs.getInt(if (usingKreader) "kreader_volume_key_up_delay" else "volume_key_up_delay", 300)
            Log.i("handleDeviceWokenUp refresh $usingKreader delay:$delay cleanup_delay:$cleanup_delay key_up_delay:$key_up_delay")
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(595, delay.toLong());
            mPhoneWindowManagerHandler?.sendEmptyMessageDelayed(596, (delay + cleanup_delay).toLong());
        }
    }


    fun handleVolumeKeyEventDown(paramKeyEvent: KeyEvent): Boolean {
//        Log.i("handleVolumeKeyEventDown " + paramKeyEvent.keyCode + " " + paramKeyEvent.action + " " + mPowerManager!!.isInteractive)
        if (!mPowerManager!!.isInteractive && !goingToSleep && screenOff) {
            val wakeLock = mVolumeWakeLock!!
            if (!wakeLock.isHeld) {;
                wakeUpDevice()
                mLastDownKeyEvent = KeyEvent(paramKeyEvent);
                Log.i("handleVolumeKeyEventDown "  + paramKeyEvent + " " + mLastDownKeyEvent + " mVolumeWakeLock:WakeLock:$mVolumeWakeLock:WakeLock")
                val prefs = Preferences()
                val key_down_delay = prefs.getInt(if (usingKreader) "kreader_volume_key_down_delay" else "volume_key_down_delay", 200)
                val delay = if (usingKreader) prefs.getInt("kreader_sleep_delay", 1299) else prefs.getInt("sleep_delay", 649)
                val cleanup_delay = prefs.getInt("volume_key_cleanup_delay", 1000)
                val wakeLockTimeout = delay + cleanup_delay + key_down_delay + 3000L
                wakeLock.acquire(wakeLockTimeout);
            }
            return true;
        }
        return false;
    }
    fun handleVolumeKeyEventUp(paramKeyEvent: KeyEvent): Boolean {
        val wakeLock = mVolumeWakeLock!!;
        Log.i("handleVolumeKeyEventUp keycode:${paramKeyEvent.keyCode} eventTime:${paramKeyEvent.eventTime} action:${paramKeyEvent.action} isInteractive:${mPowerManager!!.isInteractive} wakeLockHeld:${wakeLock.isHeld} mLastUpKeyEvent:${mLastUpKeyEvent}")
        if (wakeLock.isHeld) {
            Log.i("handleVolumeKeyEventUp " + paramKeyEvent)
            if (mLastUpKeyEvent == null) {
                mLastUpKeyEvent = KeyEvent(paramKeyEvent);
            }
            return true
        }
        if (mLastUpKeyEvent != null && mLastUpKeyEvent!!.eventTime == paramKeyEvent.eventTime) {
            return true;
        }
        return false;
    }
    fun wakeUpDevice() {
        mPhoneWindowManagerHandler?.removeMessages(595);
        mPhoneWindowManagerHandler?.removeMessages(596);
        disableWakeUpFrontLightEnabled = true
        warmLightOnBeforeStandby = lightEntryStateBeforeStandby(6)
        brightnessLightOnBeforeStandby = lightEntryStateBeforeStandby(7)
        setKeyguardEnabled(false)
        Log.i("waking up device warmLightOn:$warmLightOnBeforeStandby brightnessLightOn:$brightnessLightOnBeforeStandby")
        callMethod(
            mPhoneWindowManager,
            "wakeUpFromPowerKey",
            SystemClock.uptimeMillis()
        )
        setKeyguardEnabled(true)
    }
    fun handleWakeUpOnVolume(paramKeyEvent: KeyEvent): Boolean {
        if (readerMode) {
            var keyCode = paramKeyEvent.keyCode;
            Log.i("handleWakeUpOnVolume keyCode:${keyCode}")
            if (keyCode == 24 || keyCode == 25) {
                if (paramKeyEvent.action == KeyEvent.ACTION_UP) {
                    return handleVolumeKeyEventUp(paramKeyEvent);
                } else if (paramKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    return handleVolumeKeyEventDown(paramKeyEvent);
                }
            } else if (keyCode == 79) {
                // only refresh screen without sending volume buttons
                val wakeLock = mVolumeWakeLock!!
                if (!wakeLock.isHeld) {
                    wakeUpDevice()
                    val prefs = Preferences()
                    val delay = if (usingKreader) prefs.getInt("kreader_sleep_delay", 1299) else prefs.getInt("sleep_delay", 649)
                    val cleanup_delay = prefs.getInt("volume_key_cleanup_delay", 1000)
                    val wakeLockTimeout = delay + cleanup_delay + 3000L
                    wakeLock.acquire(wakeLockTimeout);
                }

            }
        }

        return false;
    }
    @SuppressLint("NewApi")
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        Log.i("handleLoadPackage " + lpparam.packageName + " " + AndroidAppHelper.currentApplication())
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
        } else if (lpparam.packageName == "com.onyx.kreader") {
            val ReaderActivity = findClass(
                    "com.onyx.kreader.ui.ReaderActivity",
                    lpparam.classLoader
                )

            findMethod(
                ReaderActivity, true
            ) { name == "onCreate" }
                .hookBefore {
                    val currentActivity = it.thisObject as Activity
                    Log.i("onCreate " + currentActivity)
                }

            findMethod(
                ReaderActivity, true
            ) { name == "onResume" }
                .hookBefore {
                    val currentActivity = it.thisObject as Activity
                    appContext.sendBroadcast(Intent("KREADER_RESUME"))
                    Log.i("onResume " + currentActivity)
                }
            findMethod(
                ReaderActivity
            ) { name == "onDestroy" }
                .hookBefore {
                    val currentActivity = it.thisObject as Activity
                    Log.i("onDestroy " + currentActivity)
                }
            findMethod(
                ReaderActivity
            ) { name == "onStop" }
                .hookBefore {
                    val currentActivity = it.thisObject as Activity
                    appContext.sendBroadcast(Intent("KREADER_STOP"))
                    Log.i("onStop " + currentActivity + SystemClock.uptimeMillis() + " ")
                }
        } else if (lpparam.packageName == "android") {
            val ignoredPackages = listOf<String>("com.onyx.floatingbutton", "com.onyx", "com.android.systemui")
            findMethod( findClass(
                "android.accessibilityservice.IAccessibilityServiceClient\$Stub\$Proxy",
                lpparam.classLoader
            )) { name == "onAccessibilityEvent" }
                .hookBefore() {
                    val type = callMethod(it.args[0],"getEventType" )
                    if (type ==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                        val newAppName =callMethod(it.args[0],"getPackageName" ) as String?
                        if (!ignoredPackages.contains(newAppName)) {
                            currentAppName =newAppName
                            Log.i("setPackageName $currentAppName")
                            val prefs = Preferences()
                            var reader_apps = prefs.getString("reader_apps", "").split(",").toList()
                                .filter { it.length > 0 }
                            Log.i("reader_apps $reader_apps")
                            readerMode = reader_apps.contains(currentAppName)
                            Log.i("readerMode $readerMode")
                        }


                    }
                }
            val CTMController = findClass(
                "android.onyx.brightness.CTMController",
                lpparam.classLoader
            )
//            val LightEntry = findClass(
//                "android.onyx.brightness.CTMController\$LightEntry",
//                lpparam.classLoader
//            )

//            findMethod(LightEntry) { name == "standby" }
//                .hookAfter {
//                    Log.i("LightEntry.standby ${XposedHelpers.getBooleanField(it.thisObject, "stateBeforeStandby")}")
//                }
//            findMethod(LightEntry) { name == "wakeup" }
//                .hookBefore() {
//                    Log.i("LightEntry.wakeup ${XposedHelpers.getBooleanField(it.thisObject, "stateBeforeStandby")}")
//                }
            findMethod(CTMController) { name == "isRestoreLightOnWakeup" }
                .hookAfter {
//                    Log.i("isRestoreLightOnWakeup ${it.result} disableWakeUpFrontLightEnabled:$disableWakeUpFrontLightEnabled")
                    if (disableWakeUpFrontLightEnabled) {
                        it.result = false
                    }
                }
            turnOnLight = findMethod(CTMController) { name == "turnOnLight" }
            isLightOn = findMethod(CTMController) { name == "isLightOn" }
            findMethod(CTMController) { name == "init" }
                .hookBefore {
//                    Log.i("CTMController init " + it.thisObject)
                    mCTMController = it.thisObject
                    mCTMControllerBackLights = getObjectField(mCTMController!!, "backLights")
                }

            findMethod(CTMController) { name == "wakeup" }
                .hookBefore {
                    inStandBy = false
//                    Log.i("wakeup " + " " + lpparam.packageName + " " + disableWakeUpFrontLightEnabled + " " + inStandBy)

                }
            findMethod(CTMController) { name == "standby" }
                .hookBefore {
                    inStandBy = true
                }
            PhoneWindowManager = findClass(
                "com.android.server.policy.PhoneWindowManager",
                lpparam.classLoader
            ) as Class<Any>?
            val PowerManagerClazz = findClass(
                "android.os.PowerManager",
                lpparam.classLoader
            ) as Class<Any>?

            goToSleep = findMethod(PowerManagerClazz!!) { name == "goToSleep" }
            enableKeyguard = findMethod(PhoneWindowManager!!) { name == "enableKeyguard" }

            findMethod(
                PhoneWindowManager!!
            ) { name == "interceptKeyBeforeQueueing" }
                .hookBefore {
                    if (mPhoneWindowManager == null) {
                        mPhoneWindowManager = it.thisObject
                        mPhoneWindowManagerHandler = getObjectField(mPhoneWindowManager, "mHandler") as Handler?
                        mPowerManager =
                            appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                        val intentFilter = IntentFilter()
                        intentFilter.addAction("KREADER_RESUME")
                        intentFilter.addAction("KREADER_STOP")
                        intentFilter.addAction(Intent.ACTION_USER_PRESENT)
                        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
                        appContext.registerReceiver(intentFilter) { intent ->
                            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                                goingToSleep = false
                                Log.i("screen off!! screenOff:$screenOff mVolumeWakeLock:${mVolumeWakeLock}")
                                mPhoneWindowManagerHandler?.removeMessages(600);
                                mPhoneWindowManagerHandler?.removeMessages(601);
                                if (screenOff) {
                                    //we did not receive ACTION_USER_PRESENT
                                    cleanupAfterSleep()
                                }
                                screenOff = true
                            } else if (intent?.action == Intent.ACTION_USER_PRESENT) {
                                if (screenOff) {
                                    Log.i("waking up!!!  mVolumeWakeLock:${mVolumeWakeLock}  screenOff:${screenOff}")
                                    screenOff = false
                                    if (mVolumeWakeLock?.isHeld == true) {
                                        //we are waking up
                                        handleDeviceWokenUp()
                                    }
                                }
                            } else if (intent?.action == "KREADER_RESUME") {
                                usingKreader = true
                            } else if (intent?.action == "KREADER_STOP" && !inStandBy) {
                                usingKreader = false
                            }
                        }

                        mInputManager = callStaticMethod(findClass("android.hardware.input.InputManager",
                            lpparam.classLoader), "getInstance")

                        mPhoneWindowHelper =
                            XposedHelpers.getObjectField(mPhoneWindowManager, "mPhoneWindowHelper")
                        mAlarmService = appContext.getSystemService("alarm") as AlarmManager?
                        mVolumeWakeLock =
                            mPowerManager!!.newWakeLock(268435462, "Sys::VolumeWakeLock")

                    }
                    if (handleWakeUpOnVolume(it.args[0] as KeyEvent)) {
                        it.result = 0
                    }
                }
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
                        Log.i("handleMessage 595, going back to sleep mVolumeWakeLock:${mVolumeWakeLock} warmLightOnBeforeStandby:$warmLightOnBeforeStandby brightnessLightOnBeforeStandby:$brightnessLightOnBeforeStandby ")
                        putDeviceToSleep()

                        it.result = true
                    } else if (what == 596) {
                        Log.i("handleMessage 596 cleaning up")
                        cleanupAfterSleep()
                        it.result = true
                    } else if (what == 600) {
                        Log.i("handleMessage 600")
                        sendPastKeyDownEvent()
                        it.result = true
                    } else if (what == 601) {
                        Log.i("handleMessage 601")
                        sendPastKeyUpEvent()
                        it.result = true
                    }
                }
        }
    }
}
