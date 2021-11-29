package com.amplifyframework.geo.maplibre.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.amplifyframework.geo.maplibre.R

private const val NOT_AVAILABLE = "N/A"

@SuppressLint("ViewConstructor")
class SearchResultItemView(
    context: Context,
) : LinearLayout(context) {

    internal val label: TextView by lazy {
        TextView(context).apply {
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.search_inputTextSize)
            )
        }
    }

    internal val address: TextView by lazy {
        TextView(context).apply {
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.search_inputTextSize)
            )
        }
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.search_itemBackground))

        val padding = context.resources.getDimensionPixelSize(R.dimen.search_itemPadding)
        setPaddingRelative(padding, padding, padding, padding)

        addView(label)
        addView(address)
    }

}
