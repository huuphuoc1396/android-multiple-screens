package com.android.multiplescreens

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lge.display.DisplayManagerHelper

/*
* The first activity is displayed.
* You can display the second screen by tapping the button
* */
class MainActivity : AppCompatActivity() {

    // DisplayManagerHelper is a SDK provided by LG
    // which support to track events related to dual screens on LG V50.
    // See more at http://mobile.developer.lge.com/develop/sdks/lg-dual-screen-sdk/
    private var displayManagerHelper: DisplayManagerHelper? = null

    // This callbacks where receive events from the cover
    private var coverDisplayCallback: MainCoverDisplayCallback? = null
    private var smartCoverCallback: MainSmartCoverCallback? = null

    // Save previous state of dual screens
    private var prevDualScreenState = DisplayManagerHelper.STATE_UNMOUNT

    private var isLGDualScreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // Try to construct the DisplayMangerHelper.
            // If it isn't successful, this device isn't LG dual screens
            displayManagerHelper = DisplayManagerHelper(applicationContext)
            coverDisplayCallback = MainCoverDisplayCallback()
            smartCoverCallback = MainSmartCoverCallback()

            // Register the callbacks for covers
            displayManagerHelper?.registerCoverDisplayEnabledCallback(
                applicationContext.packageName,
                coverDisplayCallback
            )
            displayManagerHelper?.registerSmartCoverCallback(smartCoverCallback)
            isLGDualScreen = true
        } catch (e: Exception) {
            isLGDualScreen = false
            Log.e(TAG, "This device isn't LG dual screens", e)
        }

        findViewById<Button>(R.id.buttonDisplaySecondScreen).setOnClickListener {
            toSecondScreen()
        }
    }

    override fun onDestroy() {
        // Remove all callbacks when this activity is destroyed
        displayManagerHelper?.unregisterCoverDisplayEnabledCallback(applicationContext.packageName)
        displayManagerHelper?.unregisterSmartCoverCallback(smartCoverCallback)
        super.onDestroy()
    }

    /*
    * Convert cover display states to string to serve for logging
    *
    * @param state is the value integer of state
    * @return a string for this state
    * */
    private fun coverDisplayStateToString(state: Int): String {
        return when (state) {
            DisplayManagerHelper.STATE_UNMOUNT -> "STATE_UNMOUNT"
            DisplayManagerHelper.STATE_DISABLED -> "STATE_DISABLED"
            DisplayManagerHelper.STATE_ENABLED -> "STATE_ENABLED"
            else -> "UNKNOWN_STATE"
        }
    }

    /*
    * Convert smart cover display states to string to serve for logging
    *
    * @param state is the value integer of state
    * @return a string for this state
    * */
    private fun smartCoverStateToString(state: Int): String {
        return when (state) {
            DisplayManagerHelper.STATE_COVER_OPENED -> "STATE_COVER_OPENED"
            DisplayManagerHelper.STATE_COVER_CLOSED -> "STATE_COVER_CLOSED"
            DisplayManagerHelper.STATE_COVER_FLIPPED_OVER -> "STATE_COVER_FLIPPED_OVER"
            else -> "UNKNOWN_STATE"
        }
    }

    /*
    * Navigate to the second screen.
    * See more at https://developer.android.com/guide/topics/ui/foldables?#using_secondary_screens
    * */
    private fun toSecondScreen() {
        // DisplayManager manages the properties of attached displays.
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

        // List displays was attached
        val displays = displayManager.displays

        if (displays.size > 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Activity options are used to select the display screen.
                val options = ActivityOptions.makeBasic()
                // Select the display screen that you want to show the second activity
                options.launchDisplayId = displays[1].displayId
                // To display on the second screen that your intent must be set flag to make
                // single task (combine FLAG_ACTIVITY_CLEAR_TOP and FLAG_ACTIVITY_NEW_TASK)
                // or you also set it in the manifest (see more at the manifest file)
                startActivity(
                    Intent(this@MainActivity, SecondActivity::class.java),
                    options.toBundle()
                )
            }
        } else {
            Toast.makeText(this, "Not found the second screen", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class MainCoverDisplayCallback : DisplayManagerHelper.CoverDisplayCallback() {
        override fun onCoverDisplayEnabledChangedCallback(state: Int) {
            displayManagerHelper?.coverDisplayState?.let {
                Log.i(TAG, "Current DualScreen Callback state: ${coverDisplayStateToString(it)}")
            }
            if (prevDualScreenState != state) {
                when (state) {
                    DisplayManagerHelper.STATE_UNMOUNT -> {
                        Log.i(TAG, "Changed DualScreen State to STATE_UNMOUNT")
                    }
                    DisplayManagerHelper.STATE_DISABLED -> {
                        Log.i(TAG, "Changed DualScreen State to  STATE_DISABLED")
                    }
                    DisplayManagerHelper.STATE_ENABLED -> {
                        toSecondScreen()
                        Log.i(TAG, "Changed DualScreen State to  STATE_ENABLED")
                    }
                }
                prevDualScreenState = state
            }
        }
    }

    private inner class MainSmartCoverCallback : DisplayManagerHelper.SmartCoverCallback() {
        override fun onTypeChanged(type: Int) {
            Log.i(TAG, "SmartCoverCallback type: ${displayManagerHelper?.coverType}")
        }

        override fun onStateChanged(state: Int) {
            displayManagerHelper?.coverState?.let {
                Log.i(TAG, "Current SmartCoverCallback state: ${smartCoverStateToString(it)}")
            }
            when (state) {
                DisplayManagerHelper.STATE_COVER_OPENED -> {
                    Log.i(TAG, "Received SmartCoverCallback is STATE_COVER_OPENED")
                }
                DisplayManagerHelper.STATE_COVER_CLOSED -> {
                    Log.i(TAG, "Received SmartCoverCallback is STATE_COVER_CLOSED")
                }
                DisplayManagerHelper.STATE_COVER_FLIPPED_OVER -> {
                    Log.i(TAG, "Received SmartCoverCallback is STATE_COVER_FLIPPED_OVER")
                }
            }
        }
    }

    companion object {
        private const val TAG = "DualScreenStatus"
    }
}
