package io.mryusuf.kabarkabar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import io.mryusuf.kabarkabar.ui.navigation.KabarKabarNavGraph
import io.mryusuf.kabarkabar.ui.theme.KabarKabarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            KabarKabarTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    KabarKabarNavGraph()
                }
            }
        }
    }
}
