package apps.cradle.quests.ui.views

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import apps.cradle.quests.R
import apps.cradle.quests.databinding.LayoutQuestInfoBinding
import apps.cradle.quests.models.QuestElement
import apps.cradle.quests.models.QuestState
import kotlin.math.atan
import kotlin.math.sqrt

class QuestView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    private val maxTitleLines = 2
    private var radius = 0f
    private var cx = 0f
    private var cy = 0f
    private var innerPadding = 0
    private var shadowRadius = 6f
    private var shadowOffsetY = 6f
    private var circleStrokeWidth = 0f
    private lateinit var titleTextView: TextView
    private lateinit var infoBinding: LayoutQuestInfoBinding

    init {
        setWillNotDraw(false)
        innerPadding = resources.getDimension(R.dimen.tinyMargin).toInt()
        circleStrokeWidth = resources.getDimension(R.dimen.scheduleWidgetCounterCircleStrokeWidth)
        addQuestInfo()
        addTitle()
        setBackgroundResource(R.drawable.background_quest_view)
        doOnPreDraw { updateTitleMargins() }
    }

    private fun addQuestInfo() {
        infoBinding = LayoutQuestInfoBinding.inflate(
            LayoutInflater.from(context), this, false
        )
        val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        params.startToStart = LayoutParams.PARENT_ID
        params.bottomToBottom = LayoutParams.PARENT_ID
        params.topToBottom = R.id.title
        infoBinding.root.layoutParams = params
        addView(infoBinding.root)
    }

    private fun addTitle() {
        titleTextView = TextView(context).apply {
            val size = resources.getDimension(R.dimen.textSizeLarge)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
            setTextColor(ContextCompat.getColor(context, R.color.textPrimaryWhite))
            ellipsize = TextUtils.TruncateAt.END
            maxLines = maxTitleLines
            id = R.id.title
        }
        val params = LayoutParams(
            0,
            LayoutParams.WRAP_CONTENT
        )
        params.topToTop = LayoutParams.PARENT_ID
        params.bottomToTop = R.id.infoContainer
        params.verticalChainStyle = LayoutParams.CHAIN_PACKED
        params.startToStart = LayoutParams.PARENT_ID
        params.endToEnd = LayoutParams.PARENT_ID
        titleTextView.layoutParams = params
        addView(titleTextView)
    }

    private fun updateTitleMargins() {
        val normalMargin = resources.getDimension(R.dimen.normalMargin).toInt() + innerPadding
        val smallMargin = resources.getDimension(R.dimen.tinyMargin).toInt() + innerPadding
        val leftMargin = (radius * 2 + innerPadding + normalMargin).toInt()
        (titleTextView.layoutParams as LayoutParams)
            .setMargins(leftMargin, 0, normalMargin, 0)
        (infoBinding.root.layoutParams as LayoutParams)
            .setMargins(leftMargin, smallMargin, 0, 0)
        if (!titleTextView.isInLayout) titleTextView.requestLayout()
        else titleTextView.doOnLayout { titleTextView.requestLayout() }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        radius = measuredHeight / 2f - innerPadding
        cx = radius + innerPadding
        cy = radius + innerPadding
        val imageSize = (radius).toInt()
        imageBounds.set(
            cx.toInt() - imageSize / 2,
            cy.toInt() - imageSize / 2,
            cx.toInt() + imageSize / 2,
            cy.toInt() + imageSize / 2
        )
    }

    private val circleBackgroundPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.sheetBackground)
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(
            shadowRadius,
            0f,
            shadowOffsetY,
            ContextCompat.getColor(context, R.color.shadowColor)
        )
    }

    private val backgroundPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.sheetBackground)
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(
            shadowRadius,
            0f,
            shadowOffsetY,
            ContextCompat.getColor(context, R.color.shadowColor)
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.run {
            drawBackground(this)
            drawInfoCircle(this)
            drawQuest(this)
        }
        super.draw(canvas)
    }

    private fun drawInfoCircle(canvas: Canvas) {
        canvas.drawCircle(
            cx,
            cy,
            radius,
            circleBackgroundPaint
        )
    }

    private val path = Path()

    private fun drawBackground(canvas: Canvas) {
        path.reset()
        val offset = radius / 1.8f
        val bigRadius = sqrt(radius * radius + offset * offset)
        val angle = (atan(radius / offset) * 180) / Math.PI
        val cx = innerPadding + radius
        val cy = innerPadding + radius
        path.arcTo(
            cx - bigRadius,
            cy - bigRadius,
            cx + bigRadius,
            cy + bigRadius,
            angle.toFloat(),
            -(angle.toFloat() * 2),
            false
        )
        val cornerRadius = resources.getDimension(R.dimen.normalCorners)
        path.lineTo(width.toFloat() - cornerRadius - innerPadding, innerPadding.toFloat())
        path.arcTo(
            width - cornerRadius * 2 - innerPadding,
            innerPadding.toFloat(),
            width.toFloat() - innerPadding,
            cornerRadius * 2 + innerPadding,
            270f,
            90f,
            false
        )
        path.lineTo(width.toFloat() - innerPadding, height.toFloat() - cornerRadius - innerPadding)
        path.arcTo(
            width - cornerRadius * 2 - innerPadding,
            height - cornerRadius * 2 - innerPadding,
            width.toFloat() - innerPadding,
            height.toFloat() - innerPadding,
            0f,
            90f,
            false
        )
        path.close()
        canvas.drawPath(path, backgroundPaint)
    }


    private val countTextPaint = Paint().apply {
        typeface = Typeface.DEFAULT_BOLD
        color = ContextCompat.getColor(context, R.color.textPrimaryWhite)
        textAlign = Paint.Align.CENTER
    }

    private var countTextBounds = Rect()

    private fun drawActiveTasksCount(activeTasksCount: Int, canvas: Canvas) {
        val text =
            if (activeTasksCount < 100) activeTasksCount.toString() else resources.getString(R.string.activeTasksCountOverflow)
        countTextPaint.textSize = radius / 1.2f
        countTextPaint.getTextBounds(text, 0, text.length, countTextBounds)
        canvas.drawText(
            text,
            cx,
            cy + countTextBounds.height() / 2,
            countTextPaint
        )
    }

    private var quest: QuestElement? = null

    fun setQuest(quest: QuestElement) {
        this.quest = quest
        setFinishedTasksCount(quest.finishedTasksCount)
        setNotesCount(quest.notesCount)
        invalidate()
    }

    private fun drawQuest(canvas: Canvas) {
        quest?.run {
            titleTextView.text = title
            when (state) {
                QuestState.ACTIVE -> drawActiveTasksCount(activeTasksCount, canvas)
                QuestState.EMPTY -> drawImage(R.drawable.image_empty, canvas)
                QuestState.DATA -> drawImage(R.drawable.image_books, canvas)
                QuestState.FINISHED -> drawImage(R.drawable.image_finish, canvas)
            }
        }
    }

    private val imagePaint = Paint().apply {
        isAntiAlias = true
    }

    private val imageBounds = Rect()

    private fun drawImage(resource: Int, canvas: Canvas) {
        val bitmap = BitmapFactory.decodeResource(resources, resource)
        canvas.drawBitmap(
            bitmap,
            null,
            imageBounds,
            imagePaint
        )
    }

    private fun setFinishedTasksCount(count: Int) {
        infoBinding.apply {
            tasksCount.text = count.toString()
            tasksCount.isVisible = count > 0
            tasks.isVisible = count > 0
        }
    }

    private fun setNotesCount(count: Int) {
        infoBinding.apply {
            notesCount.text = count.toString()
            notesCount.isVisible = count > 0
            notes.isVisible = count > 0
        }
    }

}