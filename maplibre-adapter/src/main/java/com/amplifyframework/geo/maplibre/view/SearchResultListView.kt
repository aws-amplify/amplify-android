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

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amplifyframework.geo.location.models.AmazonLocationPlace
import com.amplifyframework.geo.maplibre.R
import com.amplifyframework.geo.maplibre.util.AddressFormatter
import com.amplifyframework.geo.maplibre.util.DefaultAddressFormatter

internal class SearchResultListView(context: Context) : LinearLayout(context) {

    var places: List<AmazonLocationPlace> = listOf()
        set(value) {
            field = value
            dataAdapter.update(value)
        }

    var addressFormatter: AddressFormatter = DefaultAddressFormatter

    var onItemClickListener: OnItemClickListener? = null

    fun onItemClick(listener: (place: AmazonLocationPlace) -> Unit) {
        onItemClickListener = object : OnItemClickListener {
            override fun onClick(place: AmazonLocationPlace) {
                listener(place)
            }
        }
    }

    private val topHandle by lazy {
        View(context).apply {
            setBackgroundResource(R.drawable.map_search_sheet_handle_background)
        }
    }

    private val recyclerView by lazy {
        RecyclerView(context).apply {
            adapter = dataAdapter
            layoutManager = LinearLayoutManager(context)
            setBackgroundColor(ContextCompat.getColor(context, R.color.map_search_itemBackground))
            addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
            setHasFixedSize(true)
        }
    }

    private val dataAdapter by lazy {
        SearchResultAdapter(places.toMutableList())
    }

    init {
        orientation = VERTICAL
        val handleHeight =
            context.resources.getDimensionPixelSize(R.dimen.map_search_resultTopHandleHeight)
        addView(topHandle, LayoutParams(LayoutParams.MATCH_PARENT, handleHeight))
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    inner class ItemViewHolder(private val view: SearchResultItemView) :
        RecyclerView.ViewHolder(view) {

        internal fun bind(place: AmazonLocationPlace) {
            view.label.text = addressFormatter.formatName(place)
            view.address.text = addressFormatter.formatAddress(place)
        }
    }

    inner class SearchResultAdapter(
        private val result: MutableList<AmazonLocationPlace>
    ) : RecyclerView.Adapter<ItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            return ItemViewHolder(
                SearchResultItemView(parent.context).apply {
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                    )
                }
            )
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val place = result[position]
            holder.bind(place)
            holder.itemView.setOnClickListener {
                onItemClickListener?.onClick(place)
            }
        }

        override fun getItemCount() = result.size

        @SuppressLint("NotifyDataSetChanged")
        fun update(places: List<AmazonLocationPlace>) {
            result.clear()
            result.addAll(places)
            notifyDataSetChanged()
        }

    }

    interface OnItemClickListener {
        fun onClick(place: AmazonLocationPlace)
    }

}
