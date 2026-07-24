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
import com.example.recipeapp.databinding.ItemRecipeBinding
import com.example.recipeapp.data.recipes.uimodel.RecipeCardUiModel

class ExploreRecipesAdapter(
    private val onSaveClick: (recipeId: Int) -> Unit,
    private val onItemClick: (recipeId: Int) -> Unit
) : ListAdapter<RecipeCardUiModel, ExploreRecipesAdapter.ExploreRecipeViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreRecipeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ExploreRecipeViewHolder(ItemRecipeBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ExploreRecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExploreRecipeViewHolder(
        private val binding: ItemRecipeBinding
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
            bindSave(item)
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                onItemClick(getItem(position).id)
            }
        }

        private fun bindSave(item: RecipeCardUiModel) {
            binding.btnSave.setImageResource(
                if (item.isSaved) R.drawable.ic_saved_filled else R.drawable.ic_saved_outlined
            )
            binding.btnSave.contentDescription = itemView.context.getString(
                R.string.home_toggle_saved_content_description,
                item.title
            )
            binding.btnSave.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                onSaveClick(getItem(position).id)
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
