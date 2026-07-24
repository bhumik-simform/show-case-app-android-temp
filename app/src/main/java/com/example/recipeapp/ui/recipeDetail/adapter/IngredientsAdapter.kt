package com.example.recipeapp.ui.recipeDetail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import com.example.recipeapp.R
import com.example.recipeapp.databinding.ItemIngredientBinding
import com.example.recipeapp.data.recipes.uimodel.IngredientUiModel

class IngredientsAdapter : ListAdapter<IngredientUiModel, IngredientsAdapter.IngredientViewHolder>(DIFF_CALLBACK) {

    private var baseServings: Int = 1
    private var targetServings: Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return IngredientViewHolder(ItemIngredientBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        holder.bind(getItem(position), baseServings, targetServings)
    }

    fun submitIngredients(ingredients: List<IngredientUiModel>, baseServings: Int) {
        this.baseServings = baseServings
        this.targetServings = baseServings
        submitList(ingredients)
    }

    fun updateTargetServings(targetServings: Int) {
        this.targetServings = targetServings
        notifyItemRangeChanged(0, itemCount)
    }

    class IngredientViewHolder(
        private val binding: ItemIngredientBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IngredientUiModel, baseServings: Int, targetServings: Int) {
            binding.tvIngredientName.text = item.name
            binding.tvIngredientAmount.text = item.displayAmount(baseServings, targetServings)
            binding.ivIngredient.load(item.imageUrl) {
                placeholder(R.drawable.ic_default_image)
                error(R.drawable.ic_default_image)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<IngredientUiModel>() {
            override fun areItemsTheSame(oldItem: IngredientUiModel, newItem: IngredientUiModel) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: IngredientUiModel, newItem: IngredientUiModel) =
                oldItem == newItem
        }
    }
}
