package com.buhito.alarm

import android.app.Application
import com.buhito.alarm.data.AppDatabase
import com.buhito.alarm.engine.AlarmRepository
import com.buhito.alarm.engine.AlarmScheduler
import com.buhito.alarm.engine.Backup
import com.buhito.alarm.engine.StopwatchRepository
import com.buhito.alarm.engine.Notifications
import com.buhito.alarm.engine.SleepRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Inyección de dependencias manual: un único repositorio compartido por toda la app. */
class BuhitoApp : Application() {
    lateinit var repository: AlarmRepository
        private set
    lateinit var stopwatches: StopwatchRepository
        private set
    lateinit var backup: Backup
        private set
    lateinit var sleep: SleepRepository
        private set

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        val database = AppDatabase.get(this)
        val scheduler = AlarmScheduler(this)
        repository = AlarmRepository(
            context = this,
            dao = database.alarmDao(),
            templateDao = database.templateDao(),
            categoryDao = database.customCategoryDao(),
            historyDao = database.historyDao(),
            scheduler = scheduler,
        )
        backup = Backup(this, database, repository, scheduler)
        sleep = SleepRepository(this, database.sleepDao())
        stopwatches = StopwatchRepository(database.stopwatchDao())
        CoroutineScope(Dispatchers.IO).launch { repository.deleteOrphanSounds() }
    }
}
