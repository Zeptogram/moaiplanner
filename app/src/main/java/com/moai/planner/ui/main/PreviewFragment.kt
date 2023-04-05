@file:Suppress("DEPRECATION")

package com.moai.planner.ui.main
import android.annotation.SuppressLint
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
import com.moai.planner.BuildConfig
import com.moai.planner.R
import com.moai.planner.model.MarkdownViewModel
import com.moai.planner.util.Utils.Companion.toHtml
import kotlinx.coroutines.*


class PreviewFragment : Fragment() {
    private val viewModel: MarkdownViewModel by viewModels({ requireParentFragment() })
    private var markdownPreview: WebView? = null

    private var style: String = ""
    private var mathjax =
        "<script> MathJax = { tex: { displayMath: [['\$\$','\$\$'], ['\\\\[', '\\\\]']], processEscapes: true, processEnvironments: true }, options: { skipHtmlTags: ['script', 'noscript', 'style', 'textarea', 'pre'] } };  </script>" +
        "<script src=\"https://polyfill.io/v3/polyfill.min.js?features=es6\"></script>" +
        "<script id=\"MathJax-script\" async src=\"https://cdn.jsdelivr.net/npm/mathjax@3.0.1/es5/tex-mml-chtml.js\"></script>"

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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        markdownPreview = view.findViewById(R.id.markdown_view)
        markdownPreview?.settings?.domStorageEnabled = true
        markdownPreview?.settings?.javaScriptEnabled = true
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
                markdownPreview?.loadDataWithBaseURL("http://localhost",
                    style + markdown.toHtml() + mathjax,
                    "text/html",
                    "UTF-8", null,
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
