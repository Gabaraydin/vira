package io.github.Gabaraydin.vira

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import io.github.Gabaraydin.vira.data.local.dao.ExerciseDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ViraApplication : Application() {

    @Inject
    lateinit var exerciseDao: ExerciseDao

    override fun onCreate() {
        super.onCreate()
        // Opening the database on first launch is what fires Room's onCreate callback,
        // which seeds the exercise table. Nothing else touches the database yet.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { exerciseDao.count() }
        // Initialization itself does a one-time network fetch of ad config; the SDK
        // handles this off the main thread internally.
        MobileAds.initialize(this)
    }
}
