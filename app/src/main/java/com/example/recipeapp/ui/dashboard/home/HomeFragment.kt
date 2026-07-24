package com.example.recipeapp.ui.dashboard.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.common.filter.DietFilterBottomSheet
import com.example.recipeapp.common.toast.DummyDataToast
import com.example.recipeapp.core.base.UiState
import com.example.recipeapp.databinding.FragmentHomeBinding
import com.example.recipeapp.domain.recipe.repository.DummyDataSignal
import com.example.recipeapp.ui.dashboard.DashboardActivity
import com.example.recipeapp.ui.dashboard.home.adapter.ChipsAdapter
import com.example.recipeapp.ui.dashboard.home.adapter.ExploreHeaderAdapter
import com.example.recipeapp.ui.dashboard.home.adapter.ExploreRecipesAdapter
import com.example.recipeapp.ui.recipeDetail.RecipeDetailActivity
import com.example.recipeapp.ui.search.SearchActivity
import com.example.recipeapp.ui.dashboard.home.adapter.SavedRecipesAdapter
import com.example.recipeapp.ui.dashboard.home.adapter.SavedSectionAdapter
import com.example.recipeapp.common.itemdecor.HorizontalSpaceItemDecoration
import com.example.recipeapp.common.itemdecor.VerticalSpaceItemDecoration
import com.example.recipeapp.ui.dashboard.home.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.android.ext.android.inject
import com.example.recipeapp.core.permissions.AppPermission
import com.example.recipeapp.core.permissions.PermissionManager
import com.example.recipeapp.core.permissions.PermissionRequester

// Home tab: greeting header, search bar entry point, cuisine chip row, saved-recipes
// carousel, and the paginated explore-recipes list. Filter selection itself is owned by
// HomeViewModel (see FilterState) so it survives fragment view recreation.
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModel()

    // Permission dependencies required for POST_NOTIFICATIONS
    private val permissionManager: PermissionManager by inject()
    private val permissionRequester by lazy { PermissionRequester(this) }

    // Cuisine chip tap just forwards the tapped label to the ViewModel, which owns the
    // multi-select toggle logic (see HomeViewModel.toggleFilter).
    private val chipsAdapter = ChipsAdapter { cuisine ->
        viewModel.toggleFilter(cuisine)
    }
    private val savedRecipesAdapter = SavedRecipesAdapter(
        onItemClick = { recipeId -> openRecipeDetail(recipeId) }
    )
    private val savedSectionAdapter = SavedSectionAdapter(savedRecipesAdapter)
    private val exploreHeaderAdapter = ExploreHeaderAdapter()
    private val exploreRecipesAdapter = ExploreRecipesAdapter(
        onSaveClick = { recipeId -> viewModel.onSaveToggled(recipeId) },
        onItemClick = { recipeId -> openRecipeDetail(recipeId) }
    )



    private var lastScrollDirectionDown: Boolean? = null

    private val homeScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (dy == 0) return

            val scrollingDown = dy > 0
            if (lastScrollDirectionDown == scrollingDown) return

            lastScrollDirectionDown = scrollingDown
            (activity as? DashboardActivity)?.updateBottomBarForScroll(scrollingDown)
        }
    }

    private val paginationScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (dy <= 0) return

            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
            val totalItemCount = layoutManager.itemCount
            val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

            if (lastVisibleItem >= totalItemCount - 3) {
                viewModel.loadNextExplorePage()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tvGreeting.text = getString(R.string.home_greeting_title, viewModel.userName)
        
        setupSearchBar()
        setupChipList()
        setupHomeContent()
        setupFilterButton()
        observeUiState()
        viewModel.loadInitial()
        
        requestNotificationPermissionIfNeeded()
    }

    /**
     * Checks if the Notification permission is granted. If not, it requests it from the user.
     * This ensures we only prompt when necessary (and respects API 33+ requirement checks internally).
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (!permissionManager.isGranted(AppPermission.NOTIFICATIONS)) {
            permissionRequester.request(AppPermission.NOTIFICATIONS) { granted ->
                // Optional: handle permission result (e.g. logging)
                // Notifications are additive, so we do not block UI if denied.
            }
        }
    }

    override fun onDestroyView() {
        binding.rvHome.removeOnScrollListener(homeScrollListener)
        binding.rvHome.removeOnScrollListener(paginationScrollListener)
        _binding = null
        lastScrollDirectionDown = null
        super.onDestroyView()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.exploreUiState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                binding.pbLoading.visibility = View.GONE
                                binding.errorStateView.visibility = View.GONE
                                binding.rvHome.visibility = View.VISIBLE
                                exploreRecipesAdapter.submitList(state.data)
                            }
                            is UiState.Loading -> {
                                // rv_home is hidden (not just covered by the spinner) while
                                // loading so an empty list never flashes underneath it, and
                                // so the RecyclerView skips measure/layout/draw entirely
                                // during this window.
                                binding.pbLoading.visibility = View.VISIBLE
                                binding.errorStateView.visibility = View.GONE
                                binding.rvHome.visibility = View.GONE
                            }
                            is UiState.Error -> {
                                binding.pbLoading.visibility = View.GONE
                                binding.rvHome.visibility = View.GONE
                                binding.errorStateView.visibility = View.VISIBLE
                                binding.errorStateView.setup(
                                    message = state.message,
                                    actionText = getString(R.string.error_try_again),
                                    actionColor = requireContext().getColor(R.color.primary),
                                    onAction = { viewModel.loadInitial() }
                                )
                            }
                            else -> Unit
                        }
                    }
                }
                launch {
                    viewModel.savedUiState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                savedRecipesAdapter.submitList(state.data)
                                savedSectionAdapter.setHasItems(state.data.isNotEmpty())
                            }
                            else -> Unit
                        }
                    }
                }
                launch {
                    // Single collector drives both the cuisine chip row and the filter-active
                    // badge dot from one FilterState emission, keeping them from ever
                    // disagreeing about what's currently selected.
                    viewModel.filterState.collect { state ->
                        chipsAdapter.submitList(state.cuisineChips)
                        binding.viewFilterActiveDot.visibility =
                            if (state.isFilterActive) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun setupSearchBar() {
        binding.svSearch.root.setOnSearchClickListener {
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupFilterButton() {
        // DietFilterBottomSheet has no ViewModel of its own; it reports the user's Apply tap
        // back through this FragmentResult listener instead.
        childFragmentManager.setFragmentResultListener(
            DietFilterBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedDiets = bundle.getStringArrayList(DietFilterBottomSheet.RESULT_SELECTED_DIETS).orEmpty()
            viewModel.applyDietFilter(selectedDiets)
        }

        binding.btnFilter.setOnClickListener {
            // Pass the ViewModel's current diet selection so the sheet reopens with the
            // right chips pre-checked instead of resetting every time it's shown.
            DietFilterBottomSheet.show(childFragmentManager, viewModel.filterState.value.selectedDiets.toList())
        }
    }

    private fun openRecipeDetail(recipeId: Int) {
        startActivity(RecipeDetailActivity.newIntent(requireContext(), recipeId))
    }
    private fun setupChipList() {
        val chipSpacing = resources.getDimensionPixelSize(R.dimen.spacing_sm)

        binding.rvChips.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = chipsAdapter
            itemAnimator = null

            if (itemDecorationCount == 0) {
                addItemDecoration(
                    HorizontalSpaceItemDecoration(
                        itemSpacing = chipSpacing,
                        edgeSpacing = 0
                    )
                )
            }
        }
    }


    private fun setupHomeContent() {
        val exploreSpacing = resources.getDimensionPixelSize(R.dimen.spacing_md)

        binding.rvHome.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ConcatAdapter(savedSectionAdapter, exploreHeaderAdapter, exploreRecipesAdapter)
            itemAnimator = null

            if (itemDecorationCount == 0) {
                addItemDecoration(
                    VerticalSpaceItemDecoration(
                        itemSpacing = exploreSpacing,
                        startPosition = 2
                    )
                )
            }

            removeOnScrollListener(homeScrollListener)
            addOnScrollListener(homeScrollListener)
            removeOnScrollListener(paginationScrollListener)
            addOnScrollListener(paginationScrollListener)
        }
    }
}
