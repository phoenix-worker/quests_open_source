package apps.cradle.quests

import android.app.Application
import android.util.Log
import androidx.room.Room
import apps.cradle.quests.database.*
import com.bosphere.filelogger.FL
import com.bosphere.filelogger.FLConfig
import com.bosphere.filelogger.FLConst
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaConfig

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeYandexAppMetrica()
        initializeFileLogger()
        initializeDatabase()
        instance = this
    }

    fun initializeDatabase() {
        db = Room.databaseBuilder(this, QuestsDatabase::class.java, QuestsDatabase.dbName)
            .addMigrations(QuestsDatabase.MIGRATION_8_9, QuestsDatabase.MIGRATION_9_10)
            .addCallback(QuestsDatabase.createHeapQuestCallback)
            .build()
    }

    private fun initializeFileLogger() {
        if (BuildConfig.DEBUG) {
            FL.init(
                FLConfig.Builder(this)
                    .logger(FLConfig.DefaultLog())
                    .defaultTag(DEBUG_TAG)
                    .minLevel(FLConst.Level.D)
                    .logToFile(true)
                    .dir(externalCacheDir)
                    .formatter(FLConfig.DefaultFormatter())
                    .retentionPolicy(FLConst.RetentionPolicy.FILE_COUNT)
                    .maxFileCount(10)
                    .maxTotalSize(FLConst.DEFAULT_MAX_TOTAL_SIZE)
                    .build()
            )
            FL.setEnabled(true)
        }
    }

    private fun initializeYandexAppMetrica() {
        if (!BuildConfig.DEBUG) {
            try {
                val config = YandexMetricaConfig.newConfigBuilder(APP_METRICA_API_KEY).build()
                YandexMetrica.activate(this, config)
                YandexMetrica.enableActivityAutoTracking(this)
                log("Yandex AppMetrica initialized successfully.")
            } catch (exc: Exception) {
                log("Error in Yandex AppMetrica initialization.")
            }
        }
    }

    @Suppress("unused")
    companion object {

        private const val APP_METRICA_API_KEY = "38ac6449-78cb-4f1a-a6bb-9e532f8f9b10"
        val LOG_ENABLED = BuildConfig.DEBUG
        const val DEBUG_TAG = "QUESTS_DEBUG"
        lateinit var instance: App
        lateinit var db: QuestsDatabase

        fun log(message: String) {
            if (LOG_ENABLED) Log.d(DEBUG_TAG, message)
        }

        fun fLog(message: String) {
            if (LOG_ENABLED) FL.d(message)
        }

    }

}