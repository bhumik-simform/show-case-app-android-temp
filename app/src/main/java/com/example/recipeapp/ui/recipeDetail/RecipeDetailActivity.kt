package com.example.recipeapp.ui.recipeDetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import com.example.recipeapp.R
import com.example.recipeapp.common.itemdecor.VerticalSpaceItemDecoration
import com.example.recipeapp.common.popup.StyledPopupMenu
import com.example.recipeapp.common.toast.DummyDataToast
import com.example.recipeapp.core.base.UiState
import com.example.recipeapp.domain.recipe.repository.DummyDataSignal
import com.example.recipeapp.databinding.ActivityRecipeDetailBinding
import com.example.recipeapp.databinding.DialogRecipeLinkBinding
import com.example.recipeapp.ui.recipeDetail.adapter.IngredientsAdapter
import com.example.recipeapp.ui.recipeDetail.adapter.InstructionStepsAdapter
import com.example.recipeapp.ui.recipeDetail.viewmodel.RecipeDetailViewModel
import com.example.recipeapp.data.recipes.uimodel.RecipeDetailUiModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailBinding
    private val viewModel: RecipeDetailViewModel by viewModel()

    private val ingredientsAdapter = IngredientsAdapter()
    private val instructionStepsAdapter = InstructionStepsAdapter()

    private var currentRecipe: RecipeDetailUiModel? = null
    private var recipeId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recipeId = intent.getIntExtra(EXTRA_RECIPE_ID, -1)
        if (recipeId == -1) {
            finish()
            return
        }

        setupLists()
        configureOnClicks()
        observeUiState()
        viewModel.loadRecipeDetail(recipeId)
    }

    private fun setupLists() {
        val itemSpacing = resources.getDimensionPixelSize(R.dimen.spacing_sm)

        binding.rvIngredients.apply {
            layoutManager = LinearLayoutManager(this@RecipeDetailActivity)
            adapter = ingredientsAdapter
            itemAnimator = null

            if (itemDecorationCount == 0) {
                addItemDecoration(VerticalSpaceItemDecoration(itemSpacing = itemSpacing))
            }
        }
        binding.rvInstructions.apply {
            layoutManager = LinearLayoutManager(this@RecipeDetailActivity)
            adapter = instructionStepsAdapter
            itemAnimator = null

            if (itemDecorationCount == 0) {
                addItemDecoration(VerticalSpaceItemDecoration(itemSpacing = itemSpacing))
            }
        }
    }

    private fun configureOnClicks() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnMore.setOnClickListener { showOptionsMenu(it) }
        binding.btnDecreaseServings.setOnClickListener { viewModel.decrementServings() }
        binding.btnIncreaseServings.setOnClickListener { viewModel.incrementServings() }
        binding.tabRecipeSections.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = updateSectionVisibility(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun showOptionsMenu(anchor: View) {
        val recipe = currentRecipe ?: return
        val popupMenu = StyledPopupMenu(this, anchor)

        // Share menu item
        popupMenu.addMenuItem(
            itemId = R.id.action_share,
            title = getString(R.string.recipe_detail_menu_share),
            iconRes = R.drawable.ic_share
        )

        // Save/Unsave menu item with dynamic title and icon tint
        val toggleSaveTitle = getString(
            if (recipe.isSaved) R.string.recipe_detail_menu_unsave else R.string.recipe_detail_menu_save
        )
        val saveIconColor = if (recipe.isSaved) {
            getColor(R.color.primary)  // Green when saved
        } else {
            getColor(R.color.gray_2)   // Grey when unsaved
        }

        popupMenu.addMenuItem(
            itemId = R.id.action_toggle_save,
            title = toggleSaveTitle,
            iconRes = R.drawable.ic_saved_outlined,
            iconTint = saveIconColor
        )

        popupMenu.setOnMenuItemClickListener { itemId ->
            when (itemId) {
                R.id.action_share -> recipe.shareUrl?.let { showRecipeLinkDialog(it) }
                R.id.action_toggle_save -> viewModel.onSaveToggled()
            }
        }

        popupMenu.show()
    }

    private fun showRecipeLinkDialog(shareUrl: String) {
        val dialogBinding = DialogRecipeLinkBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.tvRecipeLink.text = shareUrl
        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnCopyLink.setOnClickListener { copyRecipeLink(dialogBinding, shareUrl) }

        dialog.show()
    }

    private fun copyRecipeLink(dialogBinding: DialogRecipeLinkBinding, shareUrl: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.recipe_detail_link_dialog_title), shareUrl))

        dialogBinding.btnCopyLink.isEnabled = false
        dialogBinding.btnCopyLink.text = getString(R.string.recipe_detail_link_copied)

        lifecycleScope.launch {
            delay(COPY_FEEDBACK_DURATION_MS)
            dialogBinding.btnCopyLink.isEnabled = true
            dialogBinding.btnCopyLink.text = getString(R.string.recipe_detail_copy_link)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {
                                binding.pbLoading.visibility = View.VISIBLE
                                binding.errorStateView.visibility = View.GONE
                                binding.nsvContent.visibility = View.GONE
                            }
                            is UiState.Success -> {
                                binding.pbLoading.visibility = View.GONE
                                binding.errorStateView.visibility = View.GONE
                                binding.nsvContent.visibility = View.VISIBLE
                                bindRecipe(state.data)
                            }
                            is UiState.Error -> {
                                binding.pbLoading.visibility = View.GONE
                                binding.nsvContent.visibility = View.GONE
                                binding.errorStateView.visibility = View.VISIBLE
                                binding.errorStateView.setup(
                                    message = state.message,
                                    actionText = getString(R.string.error_try_again),
                                    actionColor = getColor(R.color.primary),
                                    onAction = { viewModel.loadRecipeDetail(recipeId) }
                                )
                            }
                            is UiState.Idle -> Unit
                        }
                    }
                }
                launch {
                    viewModel.targetServings.collect { servings ->
                        binding.tvServingsCount.text = servings.toString()
                        ingredientsAdapter.updateTargetServings(servings)
                    }
                }
                launch {
                    DummyDataSignal.events.collect {
                        DummyDataToast.show(this@RecipeDetailActivity)
                    }
                }
            }
        }
    }

    private fun bindRecipe(recipe: RecipeDetailUiModel) {
        currentRecipe = recipe

        binding.tvTitle.text = recipe.title
        binding.ivRecipe.load(recipe.imageUrl) {
            placeholder(R.drawable.ic_default_image)
            error(R.drawable.ic_default_image)
        }
        binding.ivRecipe.contentDescription = getString(
            R.string.recipe_detail_image_content_description,
            recipe.title
        )

        // Set save indicator icon tint color based on saved state
        val indicatorTintColor = if (recipe.isSaved) {
            getColor(R.color.primary)
        } else {
            getColor(R.color.gray_2)
        }
        binding.ivSaveIndicator.setImageTintList(
            android.content.res.ColorStateList.valueOf(indicatorTintColor)
        )

        if (recipe.attribution.isNullOrBlank()) {
            binding.tvAttribution.visibility = View.GONE
        } else {
            binding.tvAttribution.visibility = View.VISIBLE
            binding.tvAttribution.text = getString(R.string.recipe_detail_attribution, recipe.attribution)
        }

        binding.tvReadyTime.text = getString(R.string.recipe_detail_ready_minutes, recipe.readyInMinutes)

        ingredientsAdapter.submitIngredients(recipe.ingredients, recipe.servings)
        instructionStepsAdapter.submitList(recipe.instructionSteps)

        updateSectionVisibility(binding.tabRecipeSections.selectedTabPosition)
    }

    private fun updateSectionVisibility(tabPosition: Int) {
        val ingredientsSelected = tabPosition == TAB_INDEX_INGREDIENTS
        binding.rvIngredients.visibility = if (ingredientsSelected) View.VISIBLE else View.GONE

        val hasInstructions = currentRecipe?.instructionSteps?.isNotEmpty() == true
        val proceduresSelected = !ingredientsSelected
        binding.rvInstructions.visibility =
            if (proceduresSelected && hasInstructions) View.VISIBLE else View.GONE

        val showEmptyState = proceduresSelected && !hasInstructions
        binding.ivEmptyInstructions.visibility = if (showEmptyState) View.VISIBLE else View.GONE
        binding.tvEmptyInstructionsTitle.visibility = if (showEmptyState) View.VISIBLE else View.GONE
        binding.tvEmptyInstructionsBody.visibility = if (showEmptyState) View.VISIBLE else View.GONE

        // Update count label based on selected tab and current recipe data
        currentRecipe?.let { recipe ->
            binding.tvItemsCount.text = if (ingredientsSelected) {
                getString(R.string.recipe_detail_ingredients_count, recipe.ingredients.size)
            } else {
                getString(R.string.recipe_detail_steps_count, recipe.instructionSteps.size)
            }
        }
    }

    companion object {
        private const val EXTRA_RECIPE_ID = "extra_recipe_id"
        private const val TAB_INDEX_INGREDIENTS = 0
        private const val COPY_FEEDBACK_DURATION_MS = 2000L

        fun newIntent(context: Context, recipeId: Int): Intent =
            Intent(context, RecipeDetailActivity::class.java)
                .putExtra(EXTRA_RECIPE_ID, recipeId)
    }
}
