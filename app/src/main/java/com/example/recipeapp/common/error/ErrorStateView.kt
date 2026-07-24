package com.example.recipeapp.common.error

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import com.example.recipeapp.R
import com.example.recipeapp.databinding.ViewErrorStateBinding

// Single reusable error UI: a centered message plus an optional clickable action (e.g.
// "Try Again"). Every UiState.Error branch across the app renders through this instead of
// hand-rolling its own error TextView/LinearLayout, so error UX — and any future visual
// change to it — stays consistent everywhere instead of drifting screen by screen.
class ErrorStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewErrorStateBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.spacing_lg)
        setPadding(horizontalPadding, 0, horizontalPadding, 0)
    }

    /**
     * @param actionText label for the clickable action (e.g. "Try Again"); pass null to hide it
     * @param actionColor text color for the action; defaults to the view's existing style if null
     * @param onAction invoked on tap; the action is only shown when both this and [actionText] are non-null
     */
    fun setup(
        message: String,
        actionText: String? = null,
        @ColorInt actionColor: Int? = null,
        onAction: (() -> Unit)? = null
    ) {
        binding.tvErrorStateMessage.text = message

        if (actionText != null && onAction != null) {
            binding.tvErrorStateAction.apply {
                visibility = VISIBLE
                text = actionText
                actionColor?.let { setTextColor(it) }
                setOnClickListener { onAction() }
            }
        } else {
            binding.tvErrorStateAction.apply {
                visibility = GONE
                setOnClickListener(null)
            }
        }
    }
}
