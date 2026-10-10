package com.firefly_liu.kcb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.firefly_liu.kcb.data.ScheduleRepository
import com.firefly_liu.kcb.data.db.AppDatabase
import com.firefly_liu.kcb.data.model.SemesterConfig
import com.firefly_liu.kcb.ui.HomeScreen
import com.firefly_liu.kcb.ui.SetupWizardScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = ScheduleRepository(AppDatabase.get(this))
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    AppRoot(repo)
                }
            }
        }
    }
}

@Composable
fun AppRoot(repo: ScheduleRepository) {
    var loaded by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf<SemesterConfig?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        config = repo.loadConfig()
        loaded = true
    }

    when {
        !loaded -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text("载入中…")
        }

        config == null -> SetupWizardScreen(
            onSave = { cfg ->
                scope.launch {
                    repo.saveConfig(cfg)
                    config = cfg
                }
            },
        )

        else -> HomeScreen(
            config = config!!,
            repo = repo,
            onReset = { config = null },
        )
    }
}

