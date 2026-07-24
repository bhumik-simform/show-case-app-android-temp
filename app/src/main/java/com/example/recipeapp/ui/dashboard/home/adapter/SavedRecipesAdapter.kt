package com.example.recipeapp.ui.dashboard.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import com.example.recipeapp.R
import com.example.recipeapp.databinding.ItemSavedRecipeBinding
import com.example.recipeapp.data.recipes.uimodel.RecipeCardUiModel

class SavedRecipesAdapter(
    private val onItemClick: (recipeId: Int) -> Unit
) : ListAdapter<RecipeCardUiModel, SavedRecipesAdapter.SavedRecipeViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedRecipeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return SavedRecipeViewHolder(ItemSavedRecipeBinding.inflate(inflater, parent, false), onItemClick)
    }

    override fun onBindViewHolder(holder: SavedRecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

     class SavedRecipeViewHolder(
        private val binding: ItemSavedRecipeBinding,
        private val onItemClick: (recipeId: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecipeCardUiModel) {
            binding.tvTitle.text = item.title
            binding.tvTime.text = itemView.context.getString(
                R.string.home_ready_minutes,
                item.readyInMinutes
            )
            binding.ivRecipe.load(item.imageUrl) {
                placeholder(R.drawable.ic_default_image)
                error(R.drawable.ic_default_image)
            }
            binding.ivRecipe.contentDescription = itemView.context.getString(
                R.string.home_recipe_image_description,
                item.title
            )
            binding.ivSave.contentDescription = itemView.context.getString(
                R.string.home_saved_recipe_content_description,
                item.title
            )
            itemView.setOnClickListener {
                onItemClick(item.id)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecipeCardUiModel>() {
            override fun areItemsTheSame(oldItem: RecipeCardUiModel, newItem: RecipeCardUiModel) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: RecipeCardUiModel, newItem: RecipeCardUiModel) =
                oldItem == newItem
        }
    }
}
