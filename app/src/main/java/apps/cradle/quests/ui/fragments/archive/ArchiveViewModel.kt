package apps.cradle.quests.ui.fragments.archive

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbCategory
import apps.cradle.quests.models.QuestItem
import apps.cradle.quests.models.toQuestsElements
import apps.cradle.quests.utils.toLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArchiveViewModel : ViewModel() {

    private val _archiveQuests = MutableLiveData<List<QuestItem>>()
    val archiveQuests = _archiveQuests.toLiveData()

    fun updateArchive() {
        viewModelScope.launch(Dispatchers.IO) {
            _archiveQuests.postValue(
                App.db
                    .questsDao()
                    .getQuestsInCategory(DbCategory.ARCHIVE_CATEGORY)
                    .toQuestsElements()
            )
        }
    }

}