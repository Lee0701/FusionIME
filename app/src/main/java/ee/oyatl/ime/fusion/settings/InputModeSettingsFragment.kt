package ee.oyatl.ime.fusion.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import ee.oyatl.ime.fusion.Feature
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.databinding.FragmentInputModeSettingsBinding
import ee.oyatl.ime.fusion.databinding.InputModeListItemBinding
import ee.oyatl.ime.fusion.mode.IMEMode
import org.json.JSONArray
import java.util.Collections

class InputModeSettingsFragment: Fragment() {
    private lateinit var pref: SharedPreferences
    private lateinit var binding: FragmentInputModeSettingsBinding
    private lateinit var adapter: Adapter
    private var items: List<String> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        items = listOf()
        try {
            val json = pref.getString(PREF_KEY, null) ?: "[]"
            items += JSONArray(json).let { arr -> (0 until arr.length()).map { arr.getString(it) } }
        } catch (ex: ClassCastException) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInputModeSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = Adapter { onItemClicked(it) }
        binding.recyclerView.adapter = adapter
        adapter.submitList(items)
        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback())
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.fab.setOnClickListener {
            val enabled = Feature.paidVersion || items.size < FREE_INPUT_MODES_LIMIT
            if(enabled) {
                val bottomSheet = ChooseInputModeTypeBottomSheet()
                bottomSheet.show(parentFragmentManager, ChooseInputModeTypeBottomSheet.TAG)
            } else {
                Snackbar.make(binding.root, R.string.msg_input_modes_limit_reached, Snackbar.LENGTH_LONG).show()
            }
        }
        parentFragmentManager.setFragmentResultListener(
            ChooseInputModeTypeBottomSheet.KEY_INPUT_MODE_TYPE, this
        ) { resultKey, result ->
            val type = result.getString(ChooseInputModeTypeBottomSheet.FIELD_TYPE)
            if(type != null) {
                items += "type=$type"
                adapter.submitList(items)
                save()
                onItemClicked(items.lastIndex)
            }
        }
    }

    fun onItemClicked(position: Int) {
        if(position < 0) return
        val map = items[position]
            .split(';').map { it.split('=') }
            .associate { (key, value) -> key to value }.toMutableMap()
        val fragment = InputModeDetailsFragment.create(map)
        if(fragment != null) {
            parentFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right, R.anim.slide_out_left,
                    R.anim.slide_in_left, R.anim.slide_out_right
                )
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit()
            activity?.title = IMEMode.Params.parse(items[position])?.getLabel(requireContext())
            parentFragmentManager.setFragmentResultListener(
                InputModeDetailsFragment.KEY_INPUT_MODE_DETAILS, this
            ) { requestKey, result ->
                activity?.setTitle(R.string.settings_input_mode_header)
                val resultMap = result.getString(InputModeDetailsFragment.KEY_MAP)
                if(resultMap != null) {
                    val mutableList = items.toMutableList()
                    mutableList[position] = resultMap
                    items = mutableList
                    adapter.submitList(items)
                    save()
                }
            }
        }
    }

    fun save() {
        pref.edit { putString(PREF_KEY, JSONArray(items).toString()) }
    }

    class Adapter(
        val onItemClick: (Int) -> Unit
    ): ListAdapter<String, Adapter.ViewHolder>(DiffCallback()) {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = InputModeListItemBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding, onItemClick)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int
        ) {
            val item = getItem(position)
            holder.onBind(item)
        }

        class ViewHolder(
            private val binding: InputModeListItemBinding,
            private val onClick: (Int) -> Unit
        ): RecyclerView.ViewHolder(binding.root) {
            fun onBind(item: String) {
                val params = IMEMode.Params.parse(item)
                binding.title.text = params?.getLabel(binding.root.context) ?: item
                binding.root.setOnClickListener { onClick(this.bindingAdapterPosition) }
            }
        }
    }

    inner class ItemTouchHelperCallback: ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.RIGHT
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            Collections.swap(items, from, to)
            adapter.notifyItemMoved(from, to)
            save()
            return true
        }

        override fun onSwiped(
            viewHolder: RecyclerView.ViewHolder,
            direction: Int
        ) {
            val position = viewHolder.bindingAdapterPosition
            items = items.toMutableList().apply { removeAt(position) }.toList()
            adapter.submitList(items)
            save()
        }
    }

    class DiffCallback: DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(
            oldItem: String,
            newItem: String
        ): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(
            oldItem: String,
            newItem: String
        ): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        const val PREF_KEY: String = "input_modes"
        const val FREE_INPUT_MODES_LIMIT = 3
    }
}