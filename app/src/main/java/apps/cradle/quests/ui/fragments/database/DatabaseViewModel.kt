package apps.cradle.quests.ui.fragments.database

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import apps.cradle.quests.App
import apps.cradle.quests.utils.events.Event
import apps.cradle.quests.utils.toLiveData
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

class DatabaseViewModel : ViewModel() {

    private val _exportStateEvent = MutableLiveData<Event<State>>()
    val exportStateEvent = _exportStateEvent.toLiveData()

    fun exportDatabase(currentDb: File, outputUri: Uri) {
        _exportStateEvent.value = Event(State.WORKING)
        viewModelScope.launch(Dispatchers.IO) {
            App.db.openHelper.close()
            try {
                if (currentDb.exists()) {
                    val resolver = App.instance.contentResolver
                    resolver.openFileDescriptor(outputUri, "w")?.use {
                        FileOutputStream(it.fileDescriptor).use { stream ->
                            stream.write(currentDb.readBytes())
                        }
                    }
                    _exportStateEvent.postValue(Event(State.SUCCESS))
                } else _exportStateEvent.postValue(Event(State.ERROR))
            } catch (exc: Exception) {
                _exportStateEvent.postValue(Event(State.ERROR))
            }
        }
    }

    private val _importStateEvent = MutableLiveData<Event<State>>()
    val importStateEvent = _importStateEvent.toLiveData()

    fun importDatabase(currentDb: File, newDBUri: Uri) {
        _importStateEvent.value = Event(State.WORKING)
        viewModelScope.launch(Dispatchers.IO) {
            App.db.openHelper.close()
            val dbDir = File(currentDb.parent.orEmpty())
            val files = dbDir.list()
            files?.run {
                for (file in files) {
                    val toDelete = File(file)
                    if (toDelete.exists()) toDelete.delete()
                }
            }
            var src: FileChannel? = null
            var dst: FileChannel? = null
            try {
                val contentResolver = App.instance.contentResolver
                src = (contentResolver.openInputStream(newDBUri) as FileInputStream).channel
                dst = FileOutputStream(currentDb).channel
                dst.transferFrom(src, 0, src.size())
                _importStateEvent.postValue(Event(State.SUCCESS))
            } catch (exc: Exception) {
                _importStateEvent.postValue(Event(State.ERROR))
            } finally {
                try {
                    src?.close()
                } catch (exc: Exception) {
                    if (App.LOG_ENABLED) exc.printStackTrace()
                }
                try {
                    dst?.close()
                } catch (exc: Exception) {
                    if (App.LOG_ENABLED) exc.printStackTrace()
                }
                App.instance.initializeDatabase()
            }
        }
    }

    enum class State { WORKING, SUCCESS, ERROR }

}