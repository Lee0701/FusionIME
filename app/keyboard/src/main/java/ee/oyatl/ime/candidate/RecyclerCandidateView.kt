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

abstract class RecyclerCandidateView(
    context: Context,
    attributeSet: AttributeSet?
): RecyclerView(context, attributeSet), CandidateView {

    override var listener: CandidateView.Listener? = null

    init {
        adapter = Adapter { listener?.onCandidateSelected(it) }
    }

    override fun submitList(list: List<Candidate>) {
        val adapter = this.adapter as Adapter
        adapter.submitList(list)
    }

    class Adapter(
        private val onItemClick: (Candidate) -> Unit
    ): ListAdapter<Candidate, ViewHolder>(DiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(CandidateItemBinding.inflate(LayoutInflater.from(parent.context)))
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.onBind(item) { onItemClick(item) }
        }
    }

    class ViewHolder(
        private val view: CandidateItemBinding
    ): RecyclerView.ViewHolder(view.root) {
        fun onBind(candidate: Candidate, onClick: () -> Unit) {
            view.text.text = candidate.text
            view.root.setOnClickListener { onClick() }
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