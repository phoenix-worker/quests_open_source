package apps.cradle.quests.ui.fragments.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.ui.adapters.SearchItemsAdapter.Searchable
import apps.cradle.quests.utils.EMPTY
import apps.cradle.quests.utils.toLiveData
import apps.cradle.quests.utils.totalSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val _searchItems = MutableLiveData<List<Searchable>>(listOf())
    val searchItems = _searchItems.toLiveData()

    private var searchJob: Job? = null
    var searchClause: String = EMPTY
        private set

    fun search(userInput: String) {
        searchJob?.cancel()
        searchClause = userInput
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            val result = if (searchClause.isBlank()) listOf() else totalSearch(searchClause)
            _searchItems.postValue(result)
        }
    }

}