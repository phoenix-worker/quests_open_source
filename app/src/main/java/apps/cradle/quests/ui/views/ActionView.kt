package apps.cradle.quests.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import apps.cradle.quests.R

class ActionView(
    context: Context,
    private val attrs: AttributeSet
) : ConstraintLayout(context, attrs) {

    private val icon: ImageView
    private val title: TextView
    private var animationOffset = 0f
    private var offscreenPadding = 0
    private var state = STATE.SHOWN
    private var gravity = GRAVITY.RIGHT

    init {
        setupGravity()
        when (gravity) {
            GRAVITY.LEFT -> inflate(context, R.layout.view_action_left, this)
            GRAVITY.RIGHT -> inflate(context, R.layout.view_action_right, this)
        }
        setBackgroundResource(
            when (gravity) {
                GRAVITY.RIGHT -> R.drawable.background_action_view_right
                GRAVITY.LEFT -> R.drawable.background_action_view_left
            }
        )
        icon = findViewById(R.id.icon)
        title = findViewById(R.id.title)
        setupIcon()
        setupTitle()
        elevation = resources.getDimension(R.dimen.barsElevation)
        offscreenPadding = resources.getDimension(R.dimen.largeMargin).toInt()
        animationOffset = offscreenPadding / 2f


        val leftPadding = if (gravity == GRAVITY.LEFT) offscreenPadding else 0
        val rightPadding = if (gravity == GRAVITY.RIGHT) offscreenPadding else 0
        setPadding(leftPadding, 0, rightPadding, 0)
        clipToPadding = false
        extractStateFromAttributes()
        setState(state)
    }

    private enum class GRAVITY { LEFT, RIGHT }

    private fun setupGravity() {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ActionView, 0, 0)
            .apply {
                try {
                    gravity = when (getInt(R.styleable.ActionView_gravity, 0)) {
                        0 -> GRAVITY.RIGHT
                        1 -> GRAVITY.LEFT
                        else -> throw IllegalArgumentException("Wrong mode.")
                    }
                } catch (_: Exception) {
                }
            }
    }

    private fun extractStateFromAttributes() {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ActionView, 0, 0)
            .apply {
                try {
                    state = when (getInt(R.styleable.ActionView_state, 1)) {
                        0 -> STATE.HIDDEN
                        1 -> STATE.SHOWN
                        else -> throw IllegalArgumentException("Wrong mode.")
                    }
                } catch (_: Exception) {
                }
            }
    }

    fun setState(state: STATE) {
        this.state = state
        doOnLayout {
            when (state) {
                STATE.SHOWN -> {
                    translationX =
                        if (gravity == GRAVITY.RIGHT) animationOffset else -animationOffset
                }

                STATE.HIDDEN -> {
                    val translation = title.width.toFloat() + offscreenPadding
                    translationX = if (gravity == GRAVITY.RIGHT) translation else -translation
                }

                else -> {}
            }
        }
        requestLayout()
    }

    private fun setupIcon() {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ActionView, 0, 0)
            .apply {
                val iconId = try {
                    getResourceId(R.styleable.ActionView_icon, 0)
                } catch (exc: Exception) {
                    0
                } finally {
                    recycle()
                }
                if (iconId != 0) icon.setImageResource(iconId)
                icon.isVisible = iconId != 0
            }
    }

    private fun setupTitle() {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ActionView, 0, 0)
            .apply {
                val titleText = try {
                    getString(R.styleable.ActionView_title)
                } catch (exc: Exception) {
                    null
                } finally {
                    recycle()
                }
                title.text = titleText
                title.isVisible = !titleText.isNullOrBlank()
            }
    }

    private var hidingSet: AnimatorSet? = null

    fun hide(startDelay: Long = 0L) {
        if (state == STATE.HIDDEN || state == STATE.HIDING) return
        showingSet?.pause()
        val firstAnimator = ObjectAnimator.ofFloat(
            if (gravity == GRAVITY.RIGHT) animationOffset else -animationOffset,
            0f
        )
        firstAnimator.addUpdateListener {
            translationX = it.animatedValue as Float
        }
        firstAnimator.interpolator = DecelerateInterpolator()
        firstAnimator.duration = 200L
        firstAnimator.startDelay = startDelay
        val offset = title.width.toFloat() + offscreenPadding
        val secondAnimator = ObjectAnimator.ofFloat(
            0f,
            if (gravity == GRAVITY.RIGHT) offset else -offset
        )
        secondAnimator.addUpdateListener {
            translationX = it.animatedValue as Float
        }
        secondAnimator.interpolator = AccelerateInterpolator()
        secondAnimator.duration = 150L
        hidingSet = AnimatorSet().apply {
            playSequentially(firstAnimator, secondAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    state = STATE.HIDING
                }

                override fun onAnimationEnd(animation: Animator) {
                    state = STATE.HIDDEN
                }
            })
            start()
        }
    }

    private var showingSet: AnimatorSet? = null

    fun show(startDelay: Long = 0L) {
        if (state == STATE.SHOWN || state == STATE.SHOWING) return
        hidingSet?.pause()
        val firstAnimator = ObjectAnimator.ofFloat(translationX, 0f)
        firstAnimator.addUpdateListener {
            translationX = it.animatedValue as Float
        }
        firstAnimator.interpolator = DecelerateInterpolator()
        firstAnimator.duration = 150L
        firstAnimator.startDelay = startDelay
        val secondAnimator = ObjectAnimator.ofFloat(
            0f,
            if (gravity == GRAVITY.RIGHT) animationOffset else -animationOffset
        )
        secondAnimator.addUpdateListener {
            translationX = it.animatedValue as Float
        }
        secondAnimator.interpolator = AccelerateInterpolator()
        secondAnimator.duration = 200L
        showingSet = AnimatorSet().apply {
            playSequentially(firstAnimator, secondAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    state = STATE.SHOWING
                }

                override fun onAnimationEnd(animation: Animator) {
                    state = STATE.SHOWN
                }
            })
            start()
        }
    }

    enum class STATE { SHOWN, SHOWING, HIDDEN, HIDING }

    fun switch(actionOnSwitch: (() -> Unit)? = null) {
        when (state) {
            STATE.HIDDEN -> {
                show()
                actionOnSwitch?.invoke()
            }

            STATE.SHOWN -> {
                hide()
                actionOnSwitch?.invoke()
            }

            else -> {}
        }
    }

    fun setTitle(titleText: String) {
        title.text = titleText
    }

}