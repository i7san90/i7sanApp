package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.VaccineRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.VaccineTrackerApp
import com.example.ui.VaccineViewModel
import com.example.ui.VaccineViewModelFactory

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import com.example.worker.VaccineNotificationWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("NotificationPermission", "Notification permission granted")
        } else {
            android.util.Log.d("NotificationPermission", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val file = java.io.File(applicationContext.filesDir, "crash.txt")
            val sw = java.io.StringWriter()
            throwable.printStackTrace(java.io.PrintWriter(sw))
            file.writeText(sw.toString())
            android.util.Log.e("CRASH_LOG", sw.toString())
            kotlin.system.exitProcess(1)
        }
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseInit", "Firebase initialization failed", e)
        }
        
        // Schedule daily background notification worker
        val notificationWorkRequest = PeriodicWorkRequestBuilder<VaccineNotificationWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "VaccineNotificationWork",
            ExistingPeriodicWorkPolicy.KEEP,
            notificationWorkRequest
        )
        
        // Initialize local Room database and Vaccine Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = VaccineRepository(database.vaccineDao())
        
        // Instantiate the Vaccine view model with modern factory injection
        val factory = VaccineViewModelFactory(repository, application)
        val viewModel = androidx.lifecycle.ViewModelProvider(this, factory)[VaccineViewModel::class.java]
        
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            
            MyApplicationTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VaccineTrackerApp(viewModel = viewModel)
                }
            }
        }
    }
}
