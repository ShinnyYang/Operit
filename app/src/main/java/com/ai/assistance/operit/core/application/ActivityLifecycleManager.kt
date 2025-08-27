package com.ai.assistance.operit.core.application

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.ai.assistance.operit.data.preferences.ApiPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * A robust manager to track the current foreground activity using Android's standard
 * ActivityLifecycleCallbacks. This avoids reflection and provides a stable way to get
 * the current activity context when needed.
 */
object ActivityLifecycleManager : Application.ActivityLifecycleCallbacks {

    private const val TAG = "ActivityLifecycleManager"
    private var currentActivity: WeakReference<Activity>? = null
    private lateinit var apiPreferences: ApiPreferences
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Initializes the manager and registers it with the application.
     * This should be called once from the Application's `onCreate` method.
     * @param application The application instance.
     */
    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
                    apiPreferences = ApiPreferences.getInstance(application.applicationContext)
    }

    /**
     * Retrieves the current foreground activity, if available.
     * @return The current Activity, or null if no activity is in the foreground or tracked.
     */
    fun getCurrentActivity(): Activity? {
        return currentActivity?.get()
    }

    /**
     * Checks the user preference and applies the keep screen on flag to the current activity's window.
     * This operation is performed on the main thread.
     *
     * @param enable True to add the `FLAG_KEEP_SCREEN_ON`, false to clear it.
     */
    fun checkAndApplyKeepScreenOn(enable: Boolean) {
        scope.launch {
            try {
                val keepScreenOnEnabled = apiPreferences.keepScreenOnFlow.first()
                if (!keepScreenOnEnabled) {
                    // The feature is disabled by the user, so we do nothing.
                    return@launch
                }

                val activity = getCurrentActivity()
                if (activity == null) {
                    Log.w(TAG, "Cannot apply screen on flag: current activity is null.")
                    return@launch
                }

                // Window operations must be done on the UI thread.
                activity.runOnUiThread {
                    val window = activity.window
                    if (enable) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        Log.d(TAG, "FLAG_KEEP_SCREEN_ON added.")
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        Log.d(TAG, "FLAG_KEEP_SCREEN_ON cleared.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply screen on flag", e)
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Not used, but required by the interface.
    }

    override fun onActivityStarted(activity: Activity) {
        // Not used, but required by the interface.
    }

    override fun onActivityResumed(activity: Activity) {
        // When an activity is resumed, it becomes the current foreground activity.
        currentActivity = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        // If the paused activity is the one we are currently tracking, clear it.
        if (currentActivity?.get() == activity) {
            currentActivity?.clear()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        // Not used, but required by the interface.
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Not used, but required by the interface.
    }

    override fun onActivityDestroyed(activity: Activity) {
        // If the destroyed activity is the one we are tracking, ensure it is cleared.
        if (currentActivity?.get() == activity) {
            currentActivity?.clear()
        }
    }
} 