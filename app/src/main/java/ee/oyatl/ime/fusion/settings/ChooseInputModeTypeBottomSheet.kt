package ee.oyatl.ime.fusion.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ee.oyatl.ime.fusion.databinding.FragmentInputModeTypeBinding
import ee.oyatl.ime.fusion.databinding.InputModeTypeListItemBinding

class ChooseInputModeTypeBottomSheet: BottomSheetDialogFragment() {
    private lateinit var binding: FragmentInputModeTypeBinding
    private lateinit var adapter: Adapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInputModeTypeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = Adapter { type ->
            val result = Bundle()
            result.putString(FIELD_TYPE, type)
            setFragmentResult(KEY_INPUT_MODE_TYPE, result)
            dismiss()
        }
        binding.recyclerView.adapter = adapter
        val list = listOf(
            Item("Latin", "latin"),
            Item("Korean", "korean")
        )
        adapter.submitList(list)
    }

    class Adapter(
        val onItemClick: (String) -> Unit
    ): ListAdapter<Item, ViewHolder>(DiffCallback()) {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = InputModeTypeListItemBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding, onItemClick)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int
        ) {
            holder.onBind(getItem(position))
        }
    }

    class ViewHolder(
        val binding: InputModeTypeListItemBinding,
        val onClick: (String) -> Unit
    ): RecyclerView.ViewHolder(binding.root) {
        fun onBind(item: Item) {
            binding.label.text = item.label
            binding.root.setOnClickListener { onClick(item.data) }
        }
    }

    class DiffCallback: DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(
            oldItem: Item,
            newItem: Item
        ): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(
            oldItem: Item,
            newItem: Item
        ): Boolean {
            return oldItem == newItem
        }
    }

    data class Item(
        val label: String,
        val data: String
    )

    companion object {
        const val TAG: String = "ChooseInputModeType"
        const val KEY_INPUT_MODE_TYPE: String = "inputModeType"
        const val FIELD_TYPE: String = "type"
    }
}