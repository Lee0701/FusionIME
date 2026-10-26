package ee.oyatl.ime.candidate

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ee.oyatl.ime.candidate.CandidateView.Candidate
import ee.oyatl.ime.keyboard.databinding.CandidateItemBinding
import ee.oyatl.ime.theme.CandidateItemType
import ee.oyatl.ime.theme.KeyboardTheme
import ee.oyatl.ime.theme.KeyboardThemes

abstract class RecyclerCandidateView(
    context: Context,
    attributeSet: AttributeSet?
): RecyclerView(context, attributeSet), CandidateView {

    override var listener: CandidateView.Listener? = null

    init {
        val keyboardTheme = KeyboardThemes.StaticLight
        setBackgroundColor(context.getColor(keyboardTheme.candidatesTheme.backgroundColor))

        adapter = Adapter(keyboardTheme) { listener?.onCandidateSelected(it) }
        itemAnimator = null
    }

    override fun submitList(list: List<Candidate>) {
        val adapter = this.adapter as Adapter
        adapter.submitList(list)
        val focusedIndex = list.indexOfFirst { it is CandidateView.FocusableCandidate && it.focused }
        if(focusedIndex > -1) smoothScrollToPosition(focusedIndex)
    }

    class Adapter(
        private val keyboardTheme: KeyboardTheme,
        private val onItemClick: (Candidate) -> Unit
    ): ListAdapter<Candidate, ViewHolder>(DiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                CandidateItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                keyboardTheme
            )
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.onBind(item) { onItemClick(item) }
        }
    }

    class ViewHolder(
        private val view: CandidateItemBinding,
        private val keyboardTheme: KeyboardTheme
    ): RecyclerView.ViewHolder(view.root) {
        fun onBind(candidate: Candidate, onClick: () -> Unit) {
            val itemTheme = keyboardTheme.candidatesTheme.itemThemes[CandidateItemType.Default]
            if(itemTheme != null) {
                view.root.setBackgroundColor(view.root.context.getColor(itemTheme.backgroundColor))
                view.text.setTextColor(view.root.context.getColor(itemTheme.foregroundColor))
                view.extra.setTextColor(view.root.context.getColor(itemTheme.extraColor))
            }

            view.text.text = candidate.text
            if(candidate is CandidateView.ExtraCandidate) {
                view.extra.text = candidate.extra
                view.extra.visibility = VISIBLE
            } else {
                view.extra.visibility = GONE
            }
            view.root.setOnClickListener { onClick() }
            if(candidate is CandidateView.FocusableCandidate) {
                view.root.isHovered = candidate.focused
            }
        }
    }

    class DiffCallback: DiffUtil.ItemCallback<Candidate>() {
        override fun areItemsTheSame(oldItem: Candidate, newItem: Candidate): Boolean {
            return oldItem === newItem
        }
        override fun areContentsTheSame(oldItem: Candidate, newItem: Candidate): Boolean {
            return oldItem == newItem
        }
    }
}