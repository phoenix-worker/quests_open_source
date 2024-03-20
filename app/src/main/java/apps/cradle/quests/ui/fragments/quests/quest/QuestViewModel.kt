package apps.cradle.quests.ui.fragments.quests.quest

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbCategory
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.models.DataItem
import apps.cradle.quests.models.Quest
import apps.cradle.quests.models.TaskItem
import apps.cradle.quests.models.toQuest
import apps.cradle.quests.utils.QuestsUtils
import apps.cradle.quests.utils.events.EmptyEvent
import apps.cradle.quests.utils.toLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class QuestViewModel : ViewModel() {

    private val _quest = MutableLiveData<Quest?>()
    val quest = _quest.toLiveData()

    private val _tasks = MutableLiveData<List<TaskItem>?>()
    val tasks = _tasks.toLiveData()

    private val _data = MutableLiveData<List<DataItem>?>()
    val data = _data.toLiveData()

    private val _categoryTitle = MutableLiveData<String?>()
    val categoryTitle = _categoryTitle.toLiveData()

    private var showFinishedTasks = false

    fun initShowFinishedTasks(questId: String) = runBlocking(Dispatchers.IO) {
        val questTasks = App.db.tasksDao().getAllQuestTasks(questId)
        val activeTasks = questTasks.filter { it.state == DbTask.STATE_ACTIVE }
        showFinishedTasks = questTasks.isNotEmpty() && activeTasks.isEmpty()
    }

    fun switchShowFinishedTasks() {
        _quest.value?.id?.run {
            showFinishedTasks = !showFinishedTasks
            update(this)
        }
    }

    fun update(questId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            App.db.questsDao().getQuest(questId)?.toQuest()?.let { quest ->
                _quest.postValue(quest)
                _tasks.postValue(
                    when (quest.categoryId) {
                        DbCategory.ARCHIVE_CATEGORY ->
                            QuestsUtils.getQuestTaskItemsForArchivedQuest(questId)

                        else -> QuestsUtils.getQuestTaskItems(questId, showFinishedTasks)
                    }
                )
                _data.postValue(QuestsUtils.getQuestDataItems(questId))
                _categoryTitle.postValue(when (quest.categoryId) {
                    DbCategory.WITHOUT_CATEGORY -> null
                    DbCategory.ARCHIVE_CATEGORY -> App.instance.getString(R.string.archiveCategoryTitle)

                    else -> {
                        App.db.categoriesDao().getAll()
                            .firstOrNull { it.id == quest.categoryId }
                            ?.title ?: App.instance.getString(R.string.withoutCategoryTitle)
                    }

                })
            }
        }
    }

    fun renameQuest(title: String) = runBlocking(Dispatchers.IO) {
        quest.value?.let {
            App.db.questsDao().setQuestTitle(questId = it.id, title = title)
        }
    }

    private val _exitEvent = MutableLiveData<EmptyEvent>()
    val exitEvent = _exitEvent.toLiveData()

    fun sendExitEvent() {
        _exitEvent.postValue(EmptyEvent())
    }

    fun deleteQuest(questId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            questId?.let {
                QuestsUtils.deleteQuestAndAllItsData(it)
                _exitEvent.postValue(EmptyEvent())
            }
        }
    }

    fun clear() {
        _quest.value = null
        _tasks.value = null
        _categoryTitle.value = null
        _data.value = null
        showFinishedTasks = false
    }

    private val _contentScrolling = MutableLiveData(false)
    val contentScrolling = _contentScrolling.toLiveData()

    fun sendContentScrolling(isScrolling: Boolean) {
        _contentScrolling.value = isScrolling
    }

    fun archiveQuestAndExit(questId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            QuestsUtils.archiveQuest(questId)
            _exitEvent.postValue(EmptyEvent())
        }
    }

    val isQuestArchived: Boolean
        get() {
            return quest.value?.categoryId == DbCategory.ARCHIVE_CATEGORY
        }
}