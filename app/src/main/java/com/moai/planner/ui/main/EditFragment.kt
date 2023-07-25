package com.moai.planner.ui.main
// Check https://github.com/wbrawner/SimpleMarkdown for Similar Class
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.moai.planner.R
import com.moai.planner.model.MarkdownViewModel
import com.moai.planner.util.Utils.Companion.hideKeyboard
import com.moai.planner.util.Utils.Companion.showKeyboard
import com.moai.planner.util.ViewPagerPage
import kotlinx.coroutines.*
import kotlin.math.abs

class EditFragment : Fragment(), ViewPagerPage {
    private var markdownEditor: EditText? = null
    private var markdownEditorScroller: NestedScrollView? = null
    private val viewModel: MarkdownViewModel by viewModels({ requireParentFragment() })
    private val markdownWatcher = object : TextWatcher {
        private var searchFor = ""

        override fun afterTextChanged(s: Editable?) {
            val searchText = s.toString().trim()
            if (searchText == searchFor)
                return

            searchFor = searchText

            lifecycleScope.launch {
                delay(50)
                if (searchText != searchFor)
                    return@launch
                viewModel.updateMarkdown(searchText)
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.note_edit_fragment, container, false)

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        markdownEditor = view.findViewById(R.id.markdown_edit)
        markdownEditorScroller = view.findViewById(R.id.markdown_edit_container)
        markdownEditor?.addTextChangedListener(markdownWatcher)

        var touchDown = 0L
        var oldX = 0f
        var oldY = 0f
        markdownEditorScroller!!.setOnTouchListener { _, event ->

            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchDown = System.currentTimeMillis()
                    oldX = event.rawX
                    oldY = event.rawY
                }
                MotionEvent.ACTION_UP -> {
                    if (System.currentTimeMillis() - touchDown < 150
                        && abs(event.rawX - oldX) < 25
                        && abs(event.rawY - oldY) < 25)
                        markdownEditor?.showKeyboard()
                }
            }
            false
        }
        markdownEditor?.setText(viewModel.markdownUpdates.value)
        viewModel.editorActions.observe(viewLifecycleOwner, Observer {
            if (it.consumed.getAndSet(true)) return@Observer
            if (it is MarkdownViewModel.EditorAction.Load) {
                markdownEditor?.apply {
                    removeTextChangedListener(markdownWatcher)
                    setText(it.markdown)
                    addTextChangedListener(markdownWatcher)
                }
            }
        })

    }

    override fun onSelected() {
        markdownEditor?.showKeyboard()
    }

    override fun onDeselected() {
        markdownEditor?.hideKeyboard()
    }



}
