package dr.ramm.bixbyfix

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityServiceInfo.DEFAULT
import android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.io.BufferedReader
import java.io.InputStreamReader


class BixbyInterceptService : AccessibilityService() {

    private val DEBUG = true
    private val TAG: String = "BX_FIXUP"
    private val checkLogsPeriod : Long = 250
    private var lastPress : String = "1.0"

    private lateinit var cameraManager: CameraManager
    private var torchEnabled = false
    private lateinit var torchCallback: CameraManager.TorchCallback
    private var cameraId: String? = null

    private var press: Boolean = false
    private var longPress: Boolean = false
    private var longPressIgnore: Boolean = false
    private var interactive: Boolean = false

    private fun debug(TAG2 : String, text : String) {
        if (DEBUG)
            Log.v(TAG2, text)
    }

    private fun getLog(): BufferedReader {
        // Get Bixby keypresses since last keypress.
        val command = arrayOf("logcat", "-s", "*:D",  "-e", "startBixbyService|AKEY_EVENT_FLAG_LONG_PRESS", "-d", "-t", lastPress)
        val proc = Runtime.getRuntime().exec(command)
        return BufferedReader(
            InputStreamReader(proc.inputStream)
        )
    }

    private fun handleBixbyPress() {
        debug(TAG, "handleBixbyPress")
    }

    private fun handleBixbyRelease() {
        debug(TAG, "handleBixbyRelease wasLong: $longPress")
        if (!longPress) {
            if (isDisplayEnabled()) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    private fun handleBixbyLongPress() {
        debug(TAG, "handleBixbyLongPress interactive: $interactive")
        if (cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId!!, !torchEnabled)
            } catch (e: CameraAccessException) {
                throw Exception(e)
            }
        }
    }

    private fun handleBixbyButton() {
        val log = getLog()
        var line: String? = ""
        var timestamp: String? = ""
        while (log.readLine().also { line = it } != null) {
            timestamp = line?.let { Regex(pattern = """^.*\d*\.\d*""").find(input = it)?.value }
            val isLongPressString: String? = line?.let { Regex(pattern = "AKEY_EVENT_FLAG_LONG_PRESS").find(input = it)?.value }
            // First start ignore all to get last event.
            if (lastPress == "1.0") {
                //debug(TAG, "Bootup sequence")
                continue
            }

            if (timestamp != null) {
                //line?.let { debug(TAG, it) }
                if (lastPress == timestamp) {
                    continue
                }
                if (isLongPressString != null) {
                    //debug(TAG, "LongPress event")
                    longPress = true
                    longPressIgnore = true
                    handleBixbyLongPress()
                } else {
                    interactive = line?.contains("interactive=true") ?: false
                    lastPress = timestamp;
                    if (longPressIgnore) {
                        // timestamp directly after longpress detect. ignore
                        longPressIgnore = false
                    } else {
                        //debug(TAG, "Press event")
                        press = !press;
                        if (press) {
                            handleBixbyPress()
                        } else {
                            handleBixbyRelease()
                        }
                        // Set to false after function call to check if press was long.
                        longPress = false
                    }
                }
            }

        }
        if (lastPress == "1.0") {
            if (timestamp != null) {
                debug(TAG, "$timestamp: Bootup sequence complete!")
                lastPress = timestamp
            } else {
                debug(TAG, "Bootup sequence complete (no entries)!")
                lastPress = "2.0"

            }
        }
        //debug(TAG, "Press: $press LongPress: $longPress")
    }

    private fun isDisplayEnabled(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isInteractive
    }

    private val checkEvent = Thread {
        while (true) {
            handleBixbyButton()
            Thread.sleep(checkLogsPeriod)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        debug(TAG, "onServiceConnected")
        val info = AccessibilityServiceInfo()

        info.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            // Set the type of feedback your service will provide.
            feedbackType = FEEDBACK_ALL_MASK
            flags = DEFAULT;
            notificationTimeout = 100
        }

        torchCallback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                torchEnabled = enabled
            }
        }
        cameraManager = getSystemService(CameraManager::class.java)!!
        cameraManager.registerTorchCallback(torchCallback, Handler())

        try {
            cameraId = cameraManager.cameraIdList[0]  // Usually back camera is at 0 position
        } catch (e: CameraAccessException) {
            throw Exception(e)
        }

        this.serviceInfo = info
        checkEvent.start()
    }

    /**
     * Called on an interrupt.
     */
    override fun onInterrupt() {
        debug(TAG, "***** onInterrupt")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //debug(TAG, "onAccessibilityEvent")
    }
}