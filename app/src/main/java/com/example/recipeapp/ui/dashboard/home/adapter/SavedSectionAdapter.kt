package com.example.recipeapp.ui.dashboard.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.databinding.ItemSavedSectionBinding
import com.example.recipeapp.common.itemdecor.HorizontalSpaceItemDecoration

class SavedSectionAdapter(
    private val savedRecipesAdapter: SavedRecipesAdapter
) : RecyclerView.Adapter<SavedSectionAdapter.SavedSectionViewHolder>() {

    private var hasItems = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedSectionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return SavedSectionViewHolder(ItemSavedSectionBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: SavedSectionViewHolder, position: Int) {
        holder.bind(savedRecipesAdapter)
    }

    override fun getItemCount(): Int = if (hasItems) 1 else 0

    fun setHasItems(hasItems: Boolean) {
        if (this.hasItems == hasItems) return
        this.hasItems = hasItems
        if (hasItems) notifyItemInserted(0) else notifyItemRemoved(0)
    }

    class SavedSectionViewHolder(
        private val binding: ItemSavedSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(adapter: SavedRecipesAdapter) {
            val spacing = itemView.resources.getDimensionPixelSize(R.dimen.spacing_md)

            binding.rvSaved.apply {
                layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                this.adapter = adapter
                itemAnimator = null

                if (itemDecorationCount == 0) {
                    addItemDecoration(
                        HorizontalSpaceItemDecoration(
                            itemSpacing = spacing,
                            edgeSpacing = 0
                        )
                    )
                }
            }
        }
    }
}
