package com.example.recipeapp.common.toast

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.example.recipeapp.R

// Custom-styled Toast shown whenever FallbackRecipeRepository serves bundled dummy data
// (Spoonacular quota exhausted on every rotated key) — a plain Toast.makeText() would use the
// system's default look, but this notice needs to visually read as an app-level notice, not
// a stray system message.
object DummyDataToast {
    fun show(context: Context) {
        val view = LayoutInflater.from(context).inflate(R.layout.toast_dummy_data, null)
        Toast(context.applicationContext).apply {
            duration = Toast.LENGTH_LONG
            @Suppress("DEPRECATION")
            this.view = view
            show()
        }
    }
}
