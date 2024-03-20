package apps.cradle.quests.models

import apps.cradle.quests.database.entities.DbAction
import java.util.UUID

data class Action(
    val id: String,
    val taskId: String,
    var title: String,
    val state: Int,
)

fun List<DbAction>.toActions(
    resetState: Boolean = false,
    resetIds: Boolean = false
): List<Action> {
    return map {
        val state = if (resetState) DbAction.STATE_ACTIVE else it.state
        Action(
            id = if (resetIds) UUID.randomUUID().toString() else it.id,
            taskId = it.taskId,
            title = it.title,
            state = state
        )
    }
}