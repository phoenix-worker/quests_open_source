package apps.cradle.quests.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import apps.cradle.quests.R
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class AnalogWatchView(
    context: Context,
    attrs: AttributeSet
) : View(context, attrs), Handler.Callback {

    private var watchHandler: Handler? = null
    private val handlerCalendar = Calendar.getInstance()
    private var currentColor: Int = 0

    init {
        watchHandler = Handler(Looper.getMainLooper(), this)
    }

    fun setSelected() {
        currentColor = ContextCompat.getColor(context, R.color.textPrimaryWhite)
        invalidate()
    }

    fun setUnselected() {
        currentColor = ContextCompat.getColor(context, R.color.bnvUnselected)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.let {
            setPaintsColors()
            drawHours(it)
            drawHoursDigits(it)
            drawMinutes(it)
            drawCircle(it)
            drawHourHandle(it)
            drawMinuteHandle(it)
            drawSecondHandle(it)
            drawCenterCirce(it)
        }
    }

    private fun setPaintsColors() {
        centerCirclePaint.color = currentColor
        secondHandlePaint.color = currentColor
        minuteHandlePaint.color = currentColor
        hourHandlePaint.color = currentColor
        minutesPaint.color = currentColor
        hoursPaint.color = currentColor
        hoursDigitsPaint.color = currentColor
        circlePaint.color = currentColor
    }

    private val centerCirclePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private fun drawCenterCirce(canvas: Canvas) {
        canvas.drawCircle(
            width / 2f,
            height / 2f,
            width / 2f * centerCircleRatio,
            centerCirclePaint
        )
    }

    private val secondHandlePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private fun drawSecondHandle(canvas: Canvas) {
        val calendar = Calendar.getInstance()
        val angle = 2 * Math.PI * calendar.get(Calendar.SECOND) / 60f
        val length = secondHandleRatio * width / 2
        val x = length * sin(angle)
        val y = length * cos(angle)
        secondHandlePaint.strokeWidth = width * secondHandleWidthRatio
        canvas.drawLine(
            width / 2f,
            height / 2f,
            width / 2 + x.toFloat(),
            height / 2 - y.toFloat(),
            secondHandlePaint
        )
    }

    private val minuteHandlePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private fun drawMinuteHandle(canvas: Canvas) {
        val calendar = Calendar.getInstance()
        val angle = 2 * Math.PI * calendar.get(Calendar.MINUTE) / 60f
        val length = minuteHandleRatio * width / 2
        val x = length * sin(angle)
        val y = length * cos(angle)
        minuteHandlePaint.strokeWidth = width * minutesHandleWidthRatio
        canvas.drawLine(
            width / 2f,
            height / 2f,
            width / 2 + x.toFloat(),
            height / 2 - y.toFloat(),
            minuteHandlePaint
        )
    }

    private val hourHandlePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private fun drawHourHandle(canvas: Canvas) {
        val calendar = Calendar.getInstance()
        var angle = 2 * Math.PI * calendar.get(Calendar.HOUR) / 12f
        angle += (2 * Math.PI / 12f) * (calendar.get(Calendar.MINUTE) / 60f)
        val length = hourHandleRatio * width / 2
        val x = length * sin(angle)
        val y = length * cos(angle)
        hourHandlePaint.strokeWidth = width * hourHandleWidthRatio
        canvas.drawLine(
            width / 2f,
            height / 2f,
            width / 2 + x.toFloat(),
            height / 2 - y.toFloat(),
            hourHandlePaint
        )
    }

    private val minutesPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private fun drawMinutes(canvas: Canvas) {
        for (i in 0..59) {
            if (i % 5 == 0) continue
            val angle = 2 * Math.PI * (i / 60f)
            val outerR = circleRadiusRatio * width / 2
            val innerR = minutesRadiusRatio * width / 2
            val outerX = outerR * sin(angle)
            val outerY = outerR * cos(angle)
            val innerX = innerR * sin(angle)
            val innerY = innerR * cos(angle)
            minutesPaint.strokeWidth = width * minutesWidthRatio
            canvas.drawLine(
                width / 2 + outerX.toFloat(),
                height / 2 - outerY.toFloat(),
                width / 2 + innerX.toFloat(),
                height / 2 - innerY.toFloat(),
                minutesPaint
            )
        }
    }

    private val hoursPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private fun drawHours(canvas: Canvas) {
        for (i in 0..11) {
            val angle = 2 * Math.PI * (i / 12f)
            val outerR = circleRadiusRatio * width / 2
            val innerR = hoursRadiusRatio * width / 2
            val outerX = outerR * sin(angle)
            val outerY = outerR * cos(angle)
            val innerX = innerR * sin(angle)
            val innerY = innerR * cos(angle)
            hoursPaint.strokeWidth = width * hoursWidthRatio
            canvas.drawLine(
                width / 2 + outerX.toFloat(),
                height / 2 - outerY.toFloat(),
                width / 2 + innerX.toFloat(),
                height / 2 - innerY.toFloat(),
                hoursPaint
            )
        }
    }

    private val hoursDigitsPaint = Paint().apply {
        style = Paint.Style.FILL
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private fun drawHoursDigits(canvas: Canvas) {
        hoursDigitsPaint.textSize = digitsRatio * width
        for (i in 0..11) {
            val angle = 2 * Math.PI * (i / 12f)
            val outerR = hoursDigitsRatio * width / 2
            val outerX = outerR * sin(angle)
            val outerY = outerR * cos(angle)
            val text = if (i != 0) i.toString() else "12"
            val rect = Rect()
            hoursDigitsPaint.getTextBounds(text, 0, text.length, rect)
            canvas.drawText(
                text,
                width / 2 + outerX.toFloat(),
                height / 2 - outerY.toFloat() + rect.height() / 2,
                hoursDigitsPaint
            )
        }
    }

    private val circlePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private fun drawCircle(canvas: Canvas) {
        canvas.drawCircle(
            width / 2f,
            height / 2f,
            circleRadiusRatio * width / 2,
            circlePaint
        )
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_UPDATE_WATCHES -> {
                invalidate()
                watchHandler?.sendEmptyMessageDelayed(
                    MSG_UPDATE_WATCHES,
                    getHandlerMsgDelay()
                )
            }
        }
        return true
    }

    private fun getHandlerMsgDelay(): Long {
        handlerCalendar.timeInMillis = System.currentTimeMillis()
        return UPDATE_INTERVAL - handlerCalendar.get(Calendar.MILLISECOND)
    }

    fun startWatches() {
        invalidate()
        watchHandler?.sendEmptyMessageDelayed(MSG_UPDATE_WATCHES, getHandlerMsgDelay())
    }

    fun stopWatches() {
        watchHandler?.removeMessages(MSG_UPDATE_WATCHES)
    }

    private val circleRadiusRatio = 0.8f

    private val hoursDigitsRatio = 0.62f
    private val hoursRadiusRatio = 0.7f
    private val hourHandleRatio = 0.45f
    private val hoursWidthRatio = 0.03f
    private val hourHandleWidthRatio = 0.04f

    private val minutesRadiusRatio = 0.75f
    private val minuteHandleRatio = 0.6f
    private val minutesWidthRatio = 0.01f
    private val minutesHandleWidthRatio = 0.04f

    private val secondHandleRatio = 0.68f
    private val secondHandleWidthRatio = 0.02f

    private val centerCircleRatio = 0.08f
    private val digitsRatio = 0.05f

    companion object {
        const val MSG_UPDATE_WATCHES = 123
        const val UPDATE_INTERVAL = 1000L
    }

}