/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.geo.maplibre.view

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.amplifyframework.geo.maplibre.R

/**
 * The location search text field, with built-in search mode and clear search capabilities.
 */
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
                    handleSearchAction()
                    return@setOnEditorActionListener true
                }
                // always return false so the keyboard is dismissed
                return@setOnEditorActionListener false
            }
        }
    }

    private fun handleSearchAction() {
        onSearchActionListener?.handle(field.text.toString())

        // dismiss the keyboard once action is handled
        field.clearFocus()
        val inputManager: InputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
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
                onSearchActionListener?.handle("")
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
     * The search display mode, map or list.
     * @see onSearchModeChange
     * @see onSearchModeChangeListener
     */
    var searchMode: SearchMode = SearchMode.MAP
        set(value) {
            field = value
            updateSearchModeIcon()
            onSearchModeChangeListener?.onChange(field)
        }

    /**
     * Action listener invoked when the search action is executed.
     */
    var onSearchActionListener: OnSearchActionListener? = null

    /**
     *  Action listener invoked when the search query changes.
     */
    var onSearchQueryChangeListener: OnSearchQueryChangeListener? = null

    /**
     * Action listener invoked when the search mode changes.
     */
    var onSearchModeChangeListener: OnSearchModeChangeListener? = null

    init {
        clipToOutline = true
        elevation = context.resources.getDimension(R.dimen.map_controls_elevation)
        orientation = HORIZONTAL
        if (attrs == null) {
            id = R.id.map_search_input
            setBackgroundResource(R.drawable.map_control_background)
        }
        gravity = Gravity.CENTER_VERTICAL

        // spacing
        val padding = context.resources.getDimensionPixelSize(R.dimen.map_search_inputPadding)
        setPaddingRelative(padding, padding / 2, padding, padding / 2)
        dividerDrawable =
            ContextCompat.getDrawable(context, R.drawable.map_search_input_icon_spacer)
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
     * Functional listener of [onSearchActionListener].
     */
    fun onSearchAction(listener: (String) -> Unit) {
        this.onSearchActionListener = object : OnSearchActionListener {
            override fun handle(query: String) {
                listener(query)
            }
        }
    }

    /**
     * Functional listener of [onSearchQueryChangeListener].
     */
    fun onSearchQueryChange(listener: (String) -> Unit) {
        this.onSearchQueryChangeListener = object : OnSearchQueryChangeListener {
            override fun onChange(query: String) {
                listener(query)
            }
        }
    }

    /**
     * Functional listener of [onSearchModeChangeListener].
     */
    fun onSearchModeChange(listener: (SearchMode) -> Unit) {
        this.onSearchModeChangeListener = object : OnSearchModeChangeListener {
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
                this@SearchTextField.onSearchQueryChangeListener?.onChange(
                    content?.toString() ?: ""
                )
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
     * The search mode enum. It can be either `LIST` or `MAP`.
     */
    enum class SearchMode {
        LIST,
        MAP
    }

    /**
     * Listener interface for search query change events.
     */
    interface OnSearchQueryChangeListener {
        fun onChange(query: String)
    }

    /**
     * Listener interface for search mode change events.
     */
    interface OnSearchModeChangeListener {
        fun onChange(mode: SearchMode)
    }

    /**
     * Listener interface for search action events.
     */
    interface OnSearchActionListener {
        fun handle(query: String)
    }

}
