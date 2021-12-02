package com.amplifyframework.geo.maplibre.view

import android.annotation.SuppressLint
import android.content.Context
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

class SearchResultListView(context: Context) : LinearLayout(context) {

    var places: List<AmazonLocationPlace> = listOf()
        set(value) {
            field = value
            dataAdapter.update(value)
        }

    var addressFormatter: AddressFormatter = DefaultAddressFormatter

//    private val topHandle by lazy {
//        View(context).apply {
//            elevation = 10f
//            setBackgroundResource(R.drawable.sheet_handle_background)
//        }
//    }

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
//        setBackgroundColor(ContextCompat.getColor(context, R.color.search_itemBackground))
//        setPadding(0, 20, 0, 0)
//        addView(topHandle, LayoutParams(LayoutParams.MATCH_PARENT, 80))
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
            return ItemViewHolder(SearchResultItemView(parent.context))
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val place = result[position]
            holder.bind(place)
        }

        override fun getItemCount() = result.size

        @SuppressLint("NotifyDataSetChanged")
        fun update(places: List<AmazonLocationPlace>) {
            result.clear()
            result.addAll(places)
            notifyDataSetChanged()
        }

    }

}
