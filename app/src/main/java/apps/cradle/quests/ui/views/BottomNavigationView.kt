package apps.cradle.quests.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import apps.cradle.quests.R
import kotlin.math.cos
import kotlin.math.sin

class BottomNavigationView(context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {

    private val schedule: AnalogWatchView
    private val quests: ImageView
    private val search: ImageView
    private val finishedQuestsHint: ImageView

    private var currentDestination: Int = 0

    private var onItemSelectedListener: ((Int) -> Unit)? = null

    fun setOnItemSelectedListener(listener: ((Int) -> Unit)?) {
        onItemSelectedListener = listener
    }

    init {
        setWillNotDraw(false)
        inflate(context, R.layout.view_bottom_navigation, this)
        schedule = findViewById(R.id.schedule)
        quests = findViewById(R.id.quests)
        search = findViewById(R.id.search)
        finishedQuestsHint = findViewById(R.id.finishedQuests)
        setListeners()
    }

    fun startWatches() {
        schedule.startWatches()
    }

    fun stopWatches() {
        schedule.stopWatches()
    }

    private fun setListeners() {
        schedule.setOnClickListener { onItemSelected(it.id) }
        quests.setOnClickListener { onItemSelected(it.id) }
        search.setOnClickListener { onItemSelected(it.id) }
    }

    private fun onItemSelected(viewId: Int) {
        if (currentDestination != viewId) {
            currentDestination = viewId
            onItemSelectedListener?.invoke(viewId)
        }
    }

    fun selectItem(viewId: Int) {
        currentDestination = viewId
        val selectedColor = ContextCompat.getColor(context, R.color.textPrimaryWhite)
        val unselectedColor = ContextCompat.getColor(context, R.color.bnvUnselected)
        val selectedTint = ColorStateList.valueOf(selectedColor)
        val unselectedTint = ColorStateList.valueOf(unselectedColor)
        schedule.setUnselected()
        quests.imageTintList = unselectedTint
        search.imageTintList = unselectedTint
        when (viewId) {
            schedule.id -> schedule.setSelected()
            quests.id -> quests.imageTintList = selectedTint
            search.id -> search.imageTintList = selectedTint
        }
    }

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val ovalRectF1 = RectF()
    private val ovalRectF2 = RectF()
    private val path = Path()
    private val mOutlineProvider = OutlineProvider()

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        calculateBackgroundPath(measuredWidth, measuredHeight)
        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            measuredHeight.toFloat(),
            ContextCompat.getColor(context, R.color.gray850),
            ContextCompat.getColor(context, R.color.windowBackground),
            Shader.TileMode.REPEAT
        )
    }

    private fun calculateBackgroundPath(width: Int, height: Int) {
        val ovalRadius1 = width / 4f
        val centerX1 = width / 5f
        ovalRectF1.set(
            centerX1 - ovalRadius1,
            0f,
            centerX1 + ovalRadius1,
            ovalRadius1 * 2
        )
        val ovalRadius2 = (width / (500 / 410f)) / 2
        val angle = Math.PI / 3
        val centerX2 =
            ovalRectF1.centerX() + ((ovalRadius1 + ovalRadius2) * cos(angle)).toFloat()
        val centerY =
            ovalRectF1.centerY() - ((ovalRadius1 + ovalRadius2) * sin(angle)).toFloat()
        ovalRectF2.set(
            centerX2 - ovalRadius2,
            centerY - ovalRadius2,
            centerX2 + ovalRadius2,
            centerY + ovalRadius2
        )
        path.reset()
        path.arcTo(ovalRectF1, 90f, 210f)
        path.arcTo(ovalRectF2, 120f, -30f)
        path.rLineTo(width.toFloat(), 0f)
        path.lineTo(width.toFloat(), height.toFloat())
        path.close()
        mOutlineProvider.setPath(path)
        outlineProvider = mOutlineProvider
    }

    override fun onDraw(canvas: Canvas) {
        canvas.run {
            drawPath(path, backgroundPaint)
        }
    }

    class OutlineProvider : ViewOutlineProvider() {

        private lateinit var path: Path

        fun setPath(path: Path) {
            this.path = path
        }

        override fun getOutline(view: View?, outline: Outline?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                outline?.setPath(path)
            }
        }
    }

    fun setFinishedQuestsHint(show: Boolean) {
        finishedQuestsHint.isVisible = show
    }
}