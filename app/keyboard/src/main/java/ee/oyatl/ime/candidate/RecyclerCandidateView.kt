package ee.oyatl.ime.candidate

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ee.oyatl.ime.candidate.CandidateView.Candidate
import ee.oyatl.ime.keyboard.R
import ee.oyatl.ime.keyboard.databinding.CandidateItemBinding
import ee.oyatl.ime.theme.CandidateItemType
import ee.oyatl.ime.theme.KeyboardTheme

abstract class RecyclerCandidateView(
    context: Context,
    attributeSet: AttributeSet?
): RecyclerView(context, attributeSet), CandidateView {

    override var keyboardTheme: KeyboardTheme? = null
        set(value) {
            field = value
            initialize()
        }
    override var listener: CandidateView.Listener? = null

    fun initialize() {
        val keyboardTheme = keyboardTheme ?: return
        setBackgroundColor(context.getColor(keyboardTheme.candidatesTheme.backgroundColor))

        adapter = Adapter(
            keyboardTheme,
            onItemClick = { listener?.onCandidateSelected(it) },
            onItemLongClick = { listener?.onCandidateLongPressed(it) }
        )
        itemAnimator = null
    }

    override fun submitList(list: List<Candidate>) {
        val adapter = this.adapter as? Adapter ?: return
        adapter.submitList(list)
        val focusedIndex = list.indexOfFirst { it is CandidateView.FocusableCandidate && it.focused }
        if(focusedIndex > -1) smoothScrollToPosition(focusedIndex)
    }

    class Adapter(
        private val keyboardTheme: KeyboardTheme,
        private val onItemClick: (Candidate) -> Unit,
        private val onItemLongClick: (Candidate) -> Unit = {}
    ): ListAdapter<Candidate, ViewHolder>(DiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                CandidateItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                keyboardTheme
            )
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.onBind(item, { onItemClick(item) }, { onItemLongClick(item) })
        }
    }

    class ViewHolder(
        private val view: CandidateItemBinding,
        private val keyboardTheme: KeyboardTheme
    ): RecyclerView.ViewHolder(view.root) {
        fun onBind(candidate: Candidate, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
            val itemTheme = keyboardTheme.candidatesTheme.itemThemes[CandidateItemType.Default]
            if(itemTheme != null) {
                view.root.setBackgroundColor(view.root.context.getColor(itemTheme.backgroundColor))
                view.text.setTextColor(view.root.context.getColor(itemTheme.foregroundColor))
                view.extra.setTextColor(view.root.context.getColor(itemTheme.extraColor))
                view.icon.imageTintList = view.root.context.getColorStateList(itemTheme.foregroundColor)
            }

            view.text.text = candidate.text
            if(candidate is CandidateView.ExtraCandidate) {
                view.extra.text = candidate.extra
                view.text.visibility = VISIBLE
                view.extra.visibility = VISIBLE
                view.icon.visibility = GONE
            } else if(candidate is CandidateView.DeletedCandidate) {
                view.icon.setImageResource(R.drawable.candic_delete)
                view.text.visibility = GONE
                view.extra.visibility = GONE
                view.icon.visibility = VISIBLE
            } else {
                view.text.visibility = VISIBLE
                view.extra.visibility = GONE
                view.icon.visibility = GONE
            }
            view.root.setOnClickListener { onClick() }
            view.root.setOnLongClickListener {
                onLongClick()
                true
            }
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