package apps.cradle.quests.ui.activities

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import apps.cradle.quests.BuildConfig
import apps.cradle.quests.R
import apps.cradle.quests.databinding.ActivityMainBinding
import apps.cradle.quests.receivers.AlarmsReceiver
import apps.cradle.quests.ui.dialogs.InfoDialog
import apps.cradle.quests.ui.fragments.quests.quest.QuestFragment
import apps.cradle.quests.ui.fragments.schedule.ScheduleViewModel
import apps.cradle.quests.utils.Locator
import apps.cradle.quests.utils.TutorialsUtils
import apps.cradle.quests.utils.events.EventObserver
import apps.cradle.quests.utils.scheduleAlarms
import apps.cradle.quests.utils.updateWidgets
import apps.cradle.quests.workers.AlarmsWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val scheduleVM: ScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TutorialsUtils.addTutorials()
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        createNotificationChannels()
        checkAlarmsPermission()
        checkNotificationsPermission()
        scheduleAlarmsWork()
        binding.root.doOnLayout { setupBnvHeight() }
    }

    private fun setupBnvHeight() {
        val screenWidth = resources.displayMetrics.widthPixels
        val height = (screenWidth / BNV_ASPECT_RATIO).toInt()
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            height
        )
        params.gravity = Gravity.BOTTOM
        binding.bnv.layoutParams = params
    }

    override fun onStart() {
        super.onStart()
        clearNotifications()
        setListeners()
        binding.bnv.startWatches()
        registerBroadcastReceiver()
        setObservers()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerBroadcastReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                midnightBroadcastReceiver,
                IntentFilter(AlarmsReceiver.ACTION_MIDNIGHT),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                midnightBroadcastReceiver,
                IntentFilter(AlarmsReceiver.ACTION_MIDNIGHT)
            )
        }
    }

    override fun onStop() {
        super.onStop()
        scheduleAlarms()
        binding.bnv.stopWatches()
        updateWidgets()
        unregisterReceiver(midnightBroadcastReceiver)
    }

    private fun setObservers() {
        Locator.conditionsChanged.observe(this) { scheduleVM.updateHasFinishedQuests() }
        Locator.openQuestEvent.observe(this, openQuestEventObserver)
        scheduleVM.hasFinishedQuests.observe(this) { binding.bnv.setFinishedQuestsHint(it) }
    }

    private val openQuestEventObserver = EventObserver<String> { questId ->
        val bundle = Bundle()
        bundle.putString(QuestFragment.EXTRA_QUEST_ID, questId)
        findNavController(R.id.navHostFragment).navigate(R.id.action_global_questFragment, bundle)
    }

    private fun setListeners() {
        binding.bnv.setOnItemSelectedListener(onBottomNavItemSelected)
        findNavController(R.id.navHostFragment).addOnDestinationChangedListener(onDestinationChange)
    }

    private val onDestinationChange: (NavController, NavDestination, Bundle?) -> Unit =
        { _, navDestination, _ ->
            when (navDestination.id) {
                R.id.categoriesFragment, R.id.scheduleFragment, R.id.searchFragment -> {
                    showNavigation()
                    setCheckedBnvItem(
                        when (navDestination.id) {
                            R.id.categoriesFragment -> R.id.quests
                            R.id.scheduleFragment -> R.id.schedule
                            R.id.searchFragment -> R.id.search
                            else -> throw IllegalArgumentException()
                        }
                    )
                }

                else -> hideNavigation()
            }
        }

    private val onBottomNavItemSelected: (Int) -> Unit = {
        val navController = findNavController(R.id.navHostFragment)
        when (it) {
            R.id.quests -> navController.navigate(R.id.action_global_categoriesFragment)
            R.id.schedule -> navController.navigate(R.id.action_global_scheduleFragment)
            R.id.search -> navController.navigate(R.id.action_global_searchFragment)
        }
    }

    private fun setCheckedBnvItem(itemId: Int) {
        binding.bnv.selectItem(itemId)
    }

    private fun hideNavigation() {
        val animator = binding.bnv.animate().alpha(0f)
        animator.setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.bnv.visibility = View.GONE
            }
        })
    }

    private fun showNavigation() {
        val animator = binding.bnv.animate().alpha(1f)
        animator.setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                binding.bnv.visibility = View.VISIBLE
            }
        })
    }

    private fun createNotificationChannels() {
        createDailyNotificationChannel()
        createRemindersNotificationChannel()
        if (BuildConfig.DEBUG) createDebugNotificationChannel()
    }

    private fun createDailyNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.dailyNotificationsChannelName)
            val channelDescription = getString(R.string.dailyNotificationsChannelDescription)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                NOTIFICATIONS_CHANNEL_ID_DAILY,
                channelName,
                importance
            )
            channel.description = channelDescription
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createRemindersNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.reminderNotificationsChannelName)
            val channelDescription = getString(R.string.reminderNotificationsChannelDescription)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                NOTIFICATIONS_CHANNEL_ID_REMINDERS,
                channelName,
                importance
            )
            channel.description = channelDescription
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createDebugNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.debugNotificationsChannelName)
            val channelDescription = getString(R.string.debugNotificationsChannelDescription)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                NOTIFICATIONS_CHANNEL_ID_DEBUG,
                channelName,
                importance
            )
            channel.description = channelDescription
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun checkAlarmsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                InfoDialog(
                    title = getString(R.string.alarmsPermissionsDialogTitle),
                    message = getString(R.string.alarmsPermissionsDialogMessage),
                ).show(supportFragmentManager, "alarms_permissions_dialog")
            }
        }
    }

    private fun checkNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (PackageManager.PERMISSION_DENIED) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                        .launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun clearNotifications() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).run {
            cancelAll()
        }
    }

    private fun scheduleAlarmsWork() {
        val builder = PeriodicWorkRequestBuilder<AlarmsWorker>(24, TimeUnit.HOURS)
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                WORK_ALARMS_SCHEDULER,
                ExistingPeriodicWorkPolicy.KEEP,
                builder.build()
            )
    }

    private val midnightBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            scheduleVM.updateSchedule()
        }
    }

    companion object {

        const val NOTIFICATIONS_CHANNEL_ID_DEBUG = "notifications_channel_id_debug"
        const val NOTIFICATIONS_CHANNEL_ID_DAILY = "notifications_channel_id_daily"
        const val NOTIFICATIONS_CHANNEL_ID_REMINDERS = "notifications_channel_id_reminders"

        const val BNV_ASPECT_RATIO = 500f / 140f

        const val WORK_ALARMS_SCHEDULER = "WORK_ALARMS_SCHEDULER"

    }

}