package com.amplifyframework.geo.maplibre.view

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.amplifyframework.geo.maplibre.R

class SearchTextField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val field: EditText by lazy {
        EditText(context).apply {
            val label = context.getText(R.string.map_search_inputPlaceholder)
            hint = label
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setImeActionLabel(label, EditorInfo.IME_ACTION_SEARCH)
            setBackgroundResource(android.R.color.transparent)
            setTextIsSelectable(true)
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.map_search_inputTextSize)
            )
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    onSearchAction?.handle(field.text.toString())
                }
                // always return false so the keyboard is dismissed
                return@setOnEditorActionListener false
            }
        }
    }

    private val searchIcon: ImageView by lazy {
        ImageView(context).apply {
            setColorFilter(ContextCompat.getColor(context, R.color.map_controls_border))
            setImageResource(R.drawable.ic_baseline_search_24)
            setOnClickListener { field.requestFocus() }
        }
    }

    private val clearIcon: ImageView by lazy {
        ImageView(context).apply {
            setColorFilter(ContextCompat.getColor(context, R.color.map_controls_foreground))
            setImageResource(R.drawable.ic_baseline_clear_24)
            setOnClickListener {
                field.text.clear()
                onSearchAction?.handle("")
            }
        }
    }

    private val searchModeIcon: ImageView by lazy {
        ImageView(context).apply {
            setColorFilter(ContextCompat.getColor(context, R.color.map_controls_foreground))
            setImageResource(R.drawable.ic_baseline_format_list_bulleted_24)
            setOnClickListener {
                searchMode = if (searchMode == SearchMode.MAP) SearchMode.LIST else SearchMode.MAP
            }
        }
    }

    /**
     *
     */
    var searchMode: SearchMode = SearchMode.MAP
        set(value) {
            field = value
            updateSearchModeIcon()
            onSearchModeChange?.onChange(field)
        }

    /**
     *
     */
    var onSearchAction: OnSearchActionListener? = null

    /**
     *
     */
    var onSearchQueryChange: OnSearchQueryChangeListener? = null

    /**
     *
     */
    var onSearchModeChange: OnSearchModeChangeListener? = null

    init {
        clipToOutline = true
        elevation = context.resources.getDimension(R.dimen.map_controls_elevation)
        orientation = HORIZONTAL
        if (attrs == null) {
            id = R.id.map_search_input
            setBackgroundResource(R.drawable.control_background)
        }
        gravity = Gravity.CENTER_VERTICAL

        // spacing
        val padding = context.resources.getDimensionPixelSize(R.dimen.map_search_inputPadding)
        setPaddingRelative(padding, padding / 2, padding, padding / 2)
        dividerDrawable = ContextCompat.getDrawable(context, R.drawable.input_icon_spacer)
        showDividers = SHOW_DIVIDER_MIDDLE

        // children
        addView(searchIcon)
        addView(field,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                weight = 1f
            }
        )
        addView(clearIcon)
        addView(searchModeIcon)

        // register internal listeners
        registerOnTextChangeListener()

        // initial UI state updates
        updateClearIconVisibility()
        updateSearchModeIcon()
    }

    /**
     *
     */
    fun onSearchAction(listener: (String) -> Unit) {
        this.onSearchAction = object : OnSearchActionListener {
            override fun handle(query: String) {
                listener(query)
            }
        }
    }

    /**
     *
     */
    fun onSearchQueryChange(listener: (String) -> Unit) {
        this.onSearchQueryChange = object : OnSearchQueryChangeListener {
            override fun onChange(query: String) {
                listener(query)
            }
        }
    }

    /**
     *
     */
    fun onSearchModeChange(listener: (SearchMode) -> Unit) {
        this.onSearchModeChange = object : OnSearchModeChangeListener {
            override fun onChange(mode: SearchMode) {
                listener(mode)
            }
        }
    }

    private fun registerOnTextChangeListener() {
        field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                text: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, after: Int) {
                this@SearchTextField.updateClearIconVisibility()
            }

            override fun afterTextChanged(content: Editable?) {
                this@SearchTextField.onSearchQueryChange?.onChange(content?.toString() ?: "")
            }

        })
    }

    private fun updateClearIconVisibility() {
        clearIcon.visibility = if (field.text.isEmpty()) GONE else VISIBLE
    }

    private fun updateSearchModeIcon() {
        val icon = when (searchMode) {
            SearchMode.MAP -> R.drawable.ic_baseline_format_list_bulleted_24
            SearchMode.LIST -> R.drawable.ic_baseline_map_24
        }
        searchModeIcon.setImageResource(icon)
    }

    /**
     *
     */
    enum class SearchMode {
        LIST,
        MAP
    }

    /**
     *
     */
    interface OnSearchQueryChangeListener {
        fun onChange(query: String)
    }

    /**
     *
     */
    interface OnSearchModeChangeListener {
        fun onChange(mode: SearchMode)
    }

    /**
     *
     */
    interface OnSearchActionListener {
        fun handle(query: String)
    }

}
