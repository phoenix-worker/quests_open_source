package apps.cradle.quests.ui.adapters

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import apps.cradle.quests.R

class ClosedTaskActionsAdapter(
    private val actions: List<String>
) : RecyclerView.Adapter<ClosedTaskActionsAdapter.ClosedTaskActionVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClosedTaskActionVH {
        return ClosedTaskActionVH(
            TextView(parent.context).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                val textSizePixels = resources.getDimension(R.dimen.textSizeNormal)
                val dp = (textSizePixels / resources.displayMetrics.density)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, dp)
                setTextColor(ContextCompat.getColor(context, R.color.textHintWhite))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimension(R.dimen.closedTaskActionHeight).toInt()
                )
                layoutParams = params
            }
        )
    }

    override fun onBindViewHolder(holder: ClosedTaskActionVH, position: Int) {
        holder.bind(actions[position], position)
    }

    override fun getItemCount(): Int {
        return actions.size
    }

    class ClosedTaskActionVH(private val textView: TextView) : ViewHolder(textView) {

        @SuppressLint("SetTextI18n")
        fun bind(title: String, position: Int) {
            textView.text = "${position + 1}. $title"
        }
    }
}