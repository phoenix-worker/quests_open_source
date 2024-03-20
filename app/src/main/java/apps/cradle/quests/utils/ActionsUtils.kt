package apps.cradle.quests.utils

import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object ActionsUtils {

    fun getAllActions(): List<DbAction> = runBlocking(Dispatchers.IO) {
        App.db.actionsDao().getAllActions()
    }

}