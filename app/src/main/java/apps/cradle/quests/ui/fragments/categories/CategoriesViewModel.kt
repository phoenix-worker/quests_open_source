package apps.cradle.quests.ui.fragments.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbCategory
import apps.cradle.quests.database.entities.DbQuest
import apps.cradle.quests.models.Category
import apps.cradle.quests.models.toCategories
import apps.cradle.quests.utils.Locator
import apps.cradle.quests.utils.events.EmptyEvent
import apps.cradle.quests.utils.events.Event
import apps.cradle.quests.utils.toLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class CategoriesViewModel : ViewModel() {

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _activeQuestsCount = MutableLiveData<Int>()
    val activeQuestsCount: LiveData<Int> = _activeQuestsCount

    private val _hasArchive = MutableLiveData<Boolean>()
    val hasArchive = _hasArchive.toLiveData()

    fun updateCategories(showEmpty: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val categoriesList = App.db.categoriesDao().getAll().toCategories().toMutableList()
            val result = when (showEmpty) {
                true -> categoriesList
                else -> {
                    val questsCategories = App.db.questsDao().getAll().map { it.categoryId }
                    categoriesList.filter { questsCategories.contains(it.id) }
                }
            }
            _categories.postValue(result)
            _activeQuestsCount.postValue(
                App.db.tasksDao().getAllActiveTasks()
                    .filter { it.questId != DbQuest.HEAP_QUEST_ID }
                    .distinctBy { it.questId }.size
            )
            _hasArchive.postValue(
                App.db.questsDao().getAll().any { it.categoryId == DbCategory.ARCHIVE_CATEGORY }
            )
        }
    }

    fun createNewCategory(newCategoryTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            App.db.categoriesDao().insert(
                DbCategory(
                    id = UUID.randomUUID().toString(),
                    title = newCategoryTitle
                )
            )
            updateCategories()
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            App.db.categoriesDao().deleteCategoryById(categoryId)
            updateCategories()
        }
    }

    fun changeQuestCategory(questId: String, categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            App.db.questsDao().setQuestCategory(questId, categoryId)
            Locator.sendConditionsChangedEvent()
            updateCategories()
        }
    }

    private val _newQuestCreatedEvent = MutableLiveData<EmptyEvent>()
    val newQuestCreatedEvent = _newQuestCreatedEvent.toLiveData()

    fun createNewQuest(questTitle: String, categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val questId = UUID.randomUUID().toString()
            val quest = DbQuest(id = questId, categoryId = categoryId, title = questTitle)
            App.db.questsDao().insert(quest)
            _newQuestCreatedEvent.postValue(EmptyEvent())
        }
    }

    private val _questsScrolling = MutableLiveData(false)
    val questsScrolling = _questsScrolling.toLiveData()

    fun sendQuestsScrolling(areScrolling: Boolean) {
        _questsScrolling.value = areScrolling
    }

    private val _questClickedEvent = MutableLiveData<Event<String>>()
    val questClickedEvent = _questClickedEvent.toLiveData()

    fun sendQuestClickedEvent(questId: String) {
        _questClickedEvent.value = Event(questId)
    }

    fun renameCategory(categoryId: String, categoryTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            App.db.categoriesDao().setCategoryTitle(categoryId, categoryTitle)
            updateCategories()
        }
    }
}