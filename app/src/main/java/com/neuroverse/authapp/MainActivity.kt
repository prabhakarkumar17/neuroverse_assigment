package com.neuroverse.authapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.neuroverse.authapp.ui.navigation.AuthNavGraph
import com.neuroverse.authapp.ui.theme.NeuroVerseTheme

/**
 * The app's ONLY Activity.
 *
 * SINGLE-ACTIVITY ARCHITECTURE: older Android apps used one Activity per screen,
 * which meant the back stack, transitions and argument passing were all handled
 * by the OS through Intents. The modern pattern is one Activity hosting a
 * Compose NavHost, with screens as composables. Navigation stays in your code
 * and in one file, and passing a typed object between screens no longer means
 * serialising it into an Intent.
 *
 * Note how little is here: Android lifecycle entry point, theme, nav graph. All
 * the logic lives in ViewModels and the repository, which is what keeps it
 * testable — anything written in an Activity needs a device to run.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draws behind the status and navigation bars. Required on Android 15+
        // (API 35), where edge-to-edge is enforced rather than opt-in. The
        // insets are then handled by Scaffold in each screen.
        enableEdgeToEdge()

        setContent {
            NeuroVerseTheme {
                // Surface paints the themed background colour behind everything;
                // without it the window background shows through in dark mode.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthNavGraph()
                }
            }
        }
    }
}
