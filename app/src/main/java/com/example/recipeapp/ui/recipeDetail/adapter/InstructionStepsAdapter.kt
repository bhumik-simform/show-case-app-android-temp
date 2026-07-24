package com.example.recipeapp.ui.recipeDetail.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.databinding.ItemInstructionStepBinding
import com.example.recipeapp.data.recipes.uimodel.StepUiModel

class InstructionStepsAdapter : ListAdapter<StepUiModel, InstructionStepsAdapter.StepViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return StepViewHolder(ItemInstructionStepBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StepViewHolder(
        private val binding: ItemInstructionStepBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StepUiModel) {
            binding.tvStepNumber.text = itemView.context.getString(
                R.string.recipe_detail_step_number,
                item.number
            )
            binding.tvStepInstruction.text = item.instruction

            if (item.requiredIngredientNames.isEmpty()) {
                binding.tvStepRequiredIngredients.visibility = View.GONE
            } else {
                binding.tvStepRequiredIngredients.visibility = View.VISIBLE
                binding.tvStepRequiredIngredients.text = itemView.context.getString(
                    R.string.recipe_detail_required_ingredients,
                    item.requiredIngredientNames.joinToString(", ")
                )
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<StepUiModel>() {
            override fun areItemsTheSame(oldItem: StepUiModel, newItem: StepUiModel) =
                oldItem.number == newItem.number

            override fun areContentsTheSame(oldItem: StepUiModel, newItem: StepUiModel) =
                oldItem == newItem
        }
    }
}
