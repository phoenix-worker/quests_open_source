package apps.cradle.quests.utils

import android.util.Patterns
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.core.content.ContextCompat
import apps.cradle.quests.App
import apps.cradle.quests.R
import java.util.regex.Pattern

object TextUtils {

    const val TAG_WEB_URL = "WEB_URL"
    const val TAG_EMAIL_ADDRESS = "EMAIL_ADDRESS"

    private val webUrl = Patterns.WEB_URL
    private val email = Patterns.EMAIL_ADDRESS

    private val linkColor = Color(ContextCompat.getColor(App.instance, R.color.colorAccent))

    fun getAnnotatedString(source: String): AnnotatedString {
        val builder = AnnotatedString.Builder(source)
        addAnnotations(source, builder, webUrl, TAG_WEB_URL)
        addAnnotations(source, builder, email, TAG_EMAIL_ADDRESS)
        return builder.toAnnotatedString()
    }

    private fun addAnnotations(
        source: String,
        builder: AnnotatedString.Builder,
        pattern: Pattern,
        tag: String
    ) {
        val matcher = pattern.matcher(source)
        while (matcher.find()) {
            val link = source.substring(matcher.start(), matcher.end())
            if (pattern == webUrl) {
                val incorrectLink = !(link.startsWith("https://") || link.startsWith("http://"))
                if (incorrectLink) continue
            }
            builder.addStringAnnotation(
                tag,
                link,
                matcher.start(), matcher.end()
            )
            builder.addStyle(
                SpanStyle(
                    color = linkColor
                ),
                matcher.start(), matcher.end()
            )
        }
    }
}