package apps.cradle.quests.utils

import androidx.preference.PreferenceManager
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbAction
import apps.cradle.quests.database.entities.DbCategory
import apps.cradle.quests.database.entities.DbNote
import apps.cradle.quests.database.entities.DbQuest
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.ui.fragments.moveTask.MoveTaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.UUID

object TutorialsUtils {

    fun addTutorials() = runBlocking(Dispatchers.IO) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(App.instance)
        if (!prefs.getBoolean(PREF_TUTORIALS_INITIALIZED, false)) {
            val categoryId = addTutorialCategories()
            addTutorialQuests(categoryId)
            addTutorialTasks()
            addTutorialNotes()
            prefs.edit().putBoolean(PREF_TUTORIALS_INITIALIZED, true).apply()
        }
    }

    private fun addTutorialCategories(): String {
        val categoryId = UUID.randomUUID().toString()
        App.db.categoriesDao().insert(
            DbCategory(
                id = categoryId,
                title = App.instance.getString(R.string.categoriesDifferentTitle)
            )
        )
        return categoryId
    }

    private fun addTutorialQuests(categoryId: String) {
        App.db.questsDao().insert(
            DbQuest(
                id = USER_MANUAL_QUEST_ID,
                categoryId = categoryId,
                title = App.instance.getString(R.string.userManualQuestTitle)
            )
        )
    }

    private fun addTutorialTasks() {
        App.db.tasksDao().run {
            insert(transferTasksTutorialTask())
            insert(taskWithActionsTutorialTask())
            insert(finishTasksTutorialTask())
            insert(moveTasksTutorialTask())
            insert(feedbackTutorialTask())
        }
    }

    private fun transferTasksTutorialTask(): DbTask {
        return DbTask(
            id = UUID.randomUUID().toString(),
            questId = USER_MANUAL_QUEST_ID,
            title = App.instance.getString(R.string.transferTasksTutorialTaskTitle),
            date = resetTimeInMillis(Date().time),
            time = DbTask.NO_TIME,
            reminder = DbTask.REMINDER_DEFAULT,
            state = DbTask.STATE_ACTIVE,
            deadline = DbTask.NO_DEADLINE
        )
    }

    private fun moveTasksTutorialTask(): DbTask {
        return DbTask(
            id = UUID.randomUUID().toString(),
            questId = USER_MANUAL_QUEST_ID,
            title = App.instance.getString(R.string.moveTasksTutorialTaskTitle),
            date = resetTimeInMillis(Date().time),
            time = DbTask.NO_TIME,
            reminder = DbTask.REMINDER_DEFAULT,
            state = DbTask.STATE_ACTIVE,
            deadline = DbTask.NO_DEADLINE
        )
    }

    private fun finishTasksTutorialTask(): DbTask {
        return DbTask(
            id = UUID.randomUUID().toString(),
            questId = USER_MANUAL_QUEST_ID,
            title = App.instance.getString(R.string.finishTasksTutorialTaskTitle),
            date = resetTimeInMillis(Date().time),
            time = DbTask.NO_TIME,
            reminder = DbTask.REMINDER_DEFAULT,
            state = DbTask.STATE_ACTIVE,
            deadline = DbTask.NO_DEADLINE
        )
    }

    private fun taskWithActionsTutorialTask(): DbTask {
        val taskId = UUID.randomUUID().toString()
        App.db.actionsDao().run {
            insert(
                DbAction(
                    id = UUID.randomUUID().toString(),
                    taskId = taskId,
                    title = App.instance.getString(R.string.taskWithActionsTutorialFirstActionTitle),
                    state = DbAction.STATE_ACTIVE
                )
            )
            insert(
                DbAction(
                    id = UUID.randomUUID().toString(),
                    taskId = taskId,
                    title = App.instance.getString(R.string.taskWithActionsTutorialSecondActionTitle),
                    state = DbAction.STATE_ACTIVE
                )
            )
            insert(
                DbAction(
                    id = UUID.randomUUID().toString(),
                    taskId = taskId,
                    title = App.instance.getString(R.string.taskWithActionsTutorialThirdActionTitle),
                    state = DbAction.STATE_ACTIVE
                )
            )
        }
        return DbTask(
            id = taskId,
            questId = USER_MANUAL_QUEST_ID,
            title = App.instance.getString(R.string.taskWithActionsTutorialTaskTitle),
            date = resetTimeInMillis(Date().time),
            time = DbTask.NO_TIME,
            reminder = DbTask.REMINDER_DEFAULT,
            state = DbTask.STATE_ACTIVE,
            deadline = DbTask.NO_DEADLINE
        )
    }

    private fun feedbackTutorialTask(): DbTask {
        return DbTask(
            id = UUID.randomUUID().toString(),
            questId = USER_MANUAL_QUEST_ID,
            title = App.instance.getString(R.string.feedbackTutorialTask),
            date = MoveTaskViewModel.getNextMonth(),
            time = DbTask.NO_TIME,
            reminder = DbTask.REMINDER_DEFAULT,
            state = DbTask.STATE_ACTIVE,
            deadline = DbTask.NO_DEADLINE
        )
    }

    private fun addTutorialNotes() {
        App.db.notesDao().insert(
            DbNote(
                id = UUID.randomUUID().toString(),
                questId = USER_MANUAL_QUEST_ID,
                title = App.instance.getString(R.string.tutorialNoteAboutTitle),
                content = App.instance.getString(R.string.tutorialNoteAboutContent),
                created = System.currentTimeMillis()
            )
        )
    }

    private const val PREF_TUTORIALS_INITIALIZED = "pref_tutorials_initialized"
    private const val USER_MANUAL_QUEST_ID = "user_manual_quest_id"

}