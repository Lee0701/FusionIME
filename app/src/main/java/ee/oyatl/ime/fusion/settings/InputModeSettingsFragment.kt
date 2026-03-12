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
    private val items: MutableList<String> = mutableListOf()
    private val itemListener: Adapter.ViewHolder.Listener = ItemListener()
    private var position: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(savedInstanceState != null) {
            this.position = savedInstanceState.getInt(KEY_POSITION)
        }

        pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        items.clear()
        try {
            val json = pref.getString(PREF_KEY, null) ?: "[]"
            items += JSONArray(json).let { arr -> (0 until arr.length()).map { arr.getString(it) } }
        } catch (_: ClassCastException) {
        }

        // result callback for input mode type chooser
        parentFragmentManager.setFragmentResultListener(
            ChooseInputModeTypeBottomSheet.KEY_INPUT_MODE_TYPE, this
        ) { _, result ->
            val type = result.getString(ChooseInputModeTypeBottomSheet.FIELD_TYPE)
            if(type != null) {
                val position = items.size
                items += "type=$type"
                adapter.notifyItemInserted(position)
                if(position - 1 in items.indices) adapter.notifyItemChanged(position - 1)
                save()
                itemListener.onClick(items.lastIndex)
            }
        }

        // result callback for input mode details
        parentFragmentManager.setFragmentResultListener(
            InputModeDetailsFragment.KEY_INPUT_MODE_DETAILS, this
        ) { _, result ->
            activity?.setTitle(R.string.settings_input_mode_header)
            val resultMap = result.getString(InputModeDetailsFragment.KEY_MAP)
            if(resultMap != null) {
                items[position] = resultMap
                adapter.notifyItemChanged(position)
                save()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_POSITION, position)
        save()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInputModeSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = Adapter(itemListener)
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
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        adapter.notifyItemRemoved(position)
        if(position - 1 in items.indices) adapter.notifyItemChanged(position - 1)
        if(position in items.indices) adapter.notifyItemChanged(position)
        save()
    }

    fun swapItems(from: Int, to: Int) {
        Collections.swap(items, from, to)
        adapter.notifyItemMoved(from, to)
        if(from in items.indices) adapter.notifyItemChanged(from)
        if(to in items.indices) adapter.notifyItemChanged(to)
        save()
    }

    fun save() {
        pref.edit { putString(PREF_KEY, JSONArray(items).toString()) }
    }

    inner class ItemListener: Adapter.ViewHolder.Listener {
        override fun onClick(position: Int) {
            if(position < 0) return
            val map = InputModeDetailsFragment.parseMap(items[position])
            val fragment = InputModeDetailsFragment.create(map)
            if(fragment != null) {
                parentFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right
                    )
                    .add(R.id.settings, fragment)
                    .hide(this@InputModeSettingsFragment)
                    .addToBackStack(null)
                    .commit()
                this@InputModeSettingsFragment.position = position
            }
        }

        override fun onMoveUp(position: Int) {
            if(position - 1 in items.indices) {
                swapItems(position, position - 1)
            }
        }

        override fun onMoveDown(position: Int) {
            if(position + 1 in items.indices) {
                swapItems(position, position + 1)
            }
        }

        override fun onRemove(position: Int) {
            removeItem(position)
        }
    }

    class Adapter(
        val itemListener: ViewHolder.Listener
    ): ListAdapter<String, Adapter.ViewHolder>(DiffCallback()) {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = InputModeListItemBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding, itemListener)
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
            private val listener: Listener
        ): RecyclerView.ViewHolder(binding.root) {
            fun onBind(item: String) {
                val params = IMEMode.Params.parse(item)
                binding.title.text = params?.getLabel(binding.root.context) ?: item
                binding.root.setOnClickListener { listener.onClick(this.bindingAdapterPosition) }
                binding.moveUp.setOnClickListener { listener.onMoveUp(this.bindingAdapterPosition) }
                binding.moveDown.setOnClickListener { listener.onMoveDown(this.bindingAdapterPosition)}
                binding.remove.setOnClickListener { listener.onRemove(this.bindingAdapterPosition)}
                val count = this.bindingAdapter?.itemCount ?: 0
                binding.moveUp.isEnabled = this.bindingAdapterPosition > 0
                binding.moveDown.isEnabled = this.bindingAdapterPosition < count - 1
            }
            interface Listener {
                fun onClick(position: Int)
                fun onMoveUp(position: Int)
                fun onMoveDown(position: Int)
                fun onRemove(position: Int)
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
            swapItems(from, to)
            return true
        }

        override fun onSwiped(
            viewHolder: RecyclerView.ViewHolder,
            direction: Int
        ) {
            val position = viewHolder.bindingAdapterPosition
            removeItem(position)
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
        const val KEY_POSITION = "position"
    }
}