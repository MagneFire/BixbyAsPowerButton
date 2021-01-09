package dr.ramm.bixbyfix

import android.app.Service
import android.content.Intent

import android.os.IBinder
import android.util.Log


class CatchIntentService : Service() {
    private val TAG_LOG = "BX SHIT"
    private var STOP = false
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG_LOG, "TimeService.onCreate")
    }

    override fun onStartCommand(
        intent: Intent?, flags: Int,
        startId: Int
    ): Int {
        Log.d(TAG_LOG, "TimeService.onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        STOP = true
        Log.d(TAG_LOG, "TimeService.onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG_LOG, "TimeService.onBind")
        return null
    }
}