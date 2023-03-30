@file:Suppress("DEPRECATION")

package com.example.moaiplanner.ui.main


import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.moaiplanner.BuildConfig
import com.example.moaiplanner.R
import com.example.moaiplanner.model.MarkdownViewModel
import com.example.moaiplanner.util.Utils.Companion.toHtml
import kotlinx.coroutines.*

class PreviewFragment : Fragment() {
    private val viewModel: MarkdownViewModel by viewModels({ requireParentFragment() })
    private var markdownPreview: WebView? = null
    private var style: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        updateWebContent(viewModel.markdownUpdates.value ?: "")
        viewModel.markdownUpdates.observe(viewLifecycleOwner) {
            updateWebContent(it)
        }
        return inflater.inflate(R.layout.note_preview_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        markdownPreview = view.findViewById(R.id.markdown_view)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        lifecycleScope.launch {
            val isNightMode = AppCompatDelegate.getDefaultNightMode() ==
                    AppCompatDelegate.MODE_NIGHT_YES
                    || requireContext().resources.configuration.uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
            val defaultCssId = if (isNightMode) {
                R.string.pref_custom_css_default_dark
            } else {
                R.string.pref_custom_css_default
            }
            val css = withContext(Dispatchers.IO) {
                val context = context ?: return@withContext null
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(
                        getString(R.string.pref_custom_css),
                        getString(defaultCssId)
                    )
            }
            style = String.format(FORMAT_CSS, css ?: "")
        }
    }

    private fun updateWebContent(markdown: String) {
        markdownPreview?.post {
            lifecycleScope.launch {
                markdownPreview?.loadDataWithBaseURL(null,
                    style + markdown.toHtml(),
                    "text/html",
                    "UTF-8", null
                )
            }
        }
    }

    override fun onDestroyView() {
        markdownPreview?.let {
            (it.parent as ViewGroup).removeView(it)
            it.destroy()
            markdownPreview = null
        }
        super.onDestroyView()
    }

    companion object {
        var FORMAT_CSS = "<style>" +
                "%s" +
                "</style>"
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        updateWebContent(viewModel.markdownUpdates.value ?: "")
        viewModel.markdownUpdates.observe(requireActivity()) {
            updateWebContent(it)
        }
    }
}
