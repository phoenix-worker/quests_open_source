package apps.cradle.quests.ui.fragments.quests.questsList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.models.QuestItem
import apps.cradle.quests.utils.QuestsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuestsListViewModel : ViewModel() {

    private val _questsItems = MutableLiveData<List<QuestItem>>()
    val questsItems: LiveData<List<QuestItem>> = _questsItems

    fun updateQuestsList(categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _questsItems.postValue(QuestsUtils.getQuestItems(categoryId))
        }
    }

}