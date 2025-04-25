package com.dummbroke.bread_count.inventory

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dummbroke.bread_count.R
import com.dummbroke.bread_count.databinding.FragmentInventoryPageBinding
import com.dummbroke.bread_count.model.InventoryItem
import com.dummbroke.bread_count.repository.FirebaseInventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

// ViewModel class definition
class InventoryViewModel : ViewModel() {
    private val repository = FirebaseInventoryRepository()

    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()

    private val _currentCategory = MutableStateFlow("display_bread")
    val currentCategory: StateFlow<String> = _currentCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getInventoryItems(_currentCategory.value).collect { items ->
                _inventoryItems.update { items }
            }
        }
    }

    fun setCategory(category: String) {
        if (category == "All Option") {
            // Fetch all categories in parallel and combine results
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                try {
                    kotlinx.coroutines.flow.combine(
                        repository.getInventoryItems("display_bread"),
                        repository.getInventoryItems("display_beverages"),
                        repository.getInventoryItems("delivery_bread")
                    ) { bread, beverages, delivery ->
                        bread + beverages + delivery
                    }.collect { allItems ->
                        _inventoryItems.value = allItems
                    }
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to fetch all items"
                } finally {
                    _isLoading.value = false
                }
            }
            _currentCategory.value = "all_option"
        } else {
            _currentCategory.value = when (category) {
                "Display Bread" -> "display_bread"
                "Display Beverages" -> "display_beverages"
                "Delivery Bread" -> "delivery_bread"
                else -> "display_bread"
            }
            viewModelScope.launch {
                repository.getInventoryItems(_currentCategory.value).collect { items ->
                    _inventoryItems.update { items }
                }
            }
        }
    }

    fun addItem(name: String, price: Double, category: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val item = InventoryItem(
                    name = name,
                    price = price,
                    category = category,
                    quantity = 0 // Default quantity
                )
                repository.addItem(item, when (category) {
                    "Display Bread" -> "display_bread"
                    "Display Beverages" -> "display_beverages"
                    "Delivery Bread" -> "delivery_bread"
                    else -> "display_bread"
                })
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add item"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateItem(item: InventoryItem) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                repository.updateItem(item, _currentCategory.value)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update item"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                repository.deleteItem(item.id, _currentCategory.value)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete item"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add method to update quantity
    fun updateQuantity(item: InventoryItem, newQuantity: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val updatedItem = item.copy(quantity = newQuantity)
                repository.updateItem(updatedItem, _currentCategory.value)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update quantity"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Adapter class definition
class InventoryAdapter(
    private val onItemClick: (InventoryItem) -> Unit,
    private val onDeleteClick: (InventoryItem) -> Unit
) : ListAdapter<InventoryItem, InventoryAdapter.InventoryViewHolder>(InventoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory_data, parent, false)
        return InventoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameValue: TextView = itemView.findViewById(R.id.nameValue)
        private val priceValue: TextView = itemView.findViewById(R.id.priceValue)
        private val quantityValue: TextView = itemView.findViewById(R.id.quantityValue)
        private val editButton: View = itemView.findViewById(R.id.editButton)
        private val deleteButton: View = itemView.findViewById(R.id.deleteButton)

        init {
            editButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            deleteButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }

        fun bind(item: InventoryItem) {
            nameValue.text = item.name
            priceValue.text = String.format("%.2f", item.price)
            quantityValue.text = item.quantity.toString()
        }
    }

    private class InventoryDiffCallback : DiffUtil.ItemCallback<InventoryItem>() {
        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * A simple [Fragment] subclass.
 * Use the [InventoryPage.newInstance] factory method to
 * create an instance of this fragment.
 */
class InventoryPage : Fragment() {
    private var _binding: FragmentInventoryPageBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<InventoryViewModel>()
    private lateinit var adapter: InventoryAdapter
    private var param1: String? = null
    private var param2: String? = null

    companion object {
        private const val DISPLAY_BREAD = "Display Bread"
        private const val DISPLAY_BEVERAGES = "Display Beverages"
        private const val DELIVERY_BREAD = "Delivery Bread"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment InventoryPage.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            InventoryPage().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup RecyclerView
        setupRecyclerView()

        // Setup category spinner
        setupSpinner()

        // Setup add item button
        setupAddItemButton()

        // Observe inventory items
        lifecycleScope.launch {
            viewModel.inventoryItems.collectLatest { items ->
                adapter.submitList(items)
            }
        }

        // Observe loading state
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                if (isLoading) {
                    Toast.makeText(requireContext(), "Loading...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observe errors
        lifecycleScope.launch {
            viewModel.error.collectLatest { errorMsg ->
                errorMsg?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = InventoryAdapter(
            onItemClick = { item ->
                showEditItemDialog(item)
            },
            onDeleteClick = { item ->
                viewModel.deleteItem(item)
            }
        )
        binding.inventoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.inventoryRecyclerView.adapter = adapter
    }

    private fun setupSpinner() {
        // Create a custom adapter with better text visibility
        val categories = resources.getStringArray(R.array.bread_categories)
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.modern_spinner_item,
            categories
        )
        adapter.setDropDownViewResource(R.layout.modern_spinner_dropdown_item)
        binding.inventoryCategorySpinner.adapter = adapter
        binding.inventoryCategorySpinner.setSelection(0)

        binding.inventoryCategorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val category = parent?.getItemAtPosition(position).toString()
                viewModel.setCategory(category)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupAddItemButton() {
        binding.addItemButton.setOnClickListener {
            showAddItemDialog()
        }
    }

    private fun showAddItemDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_inventory_item)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val optionSpinner = dialog.findViewById<Spinner>(R.id.optionSpinner)
        val itemNameInput = dialog.findViewById<EditText>(R.id.itemNameInput)
        val itemPriceInput = dialog.findViewById<EditText>(R.id.itemPriceInput)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val confirmButton = dialog.findViewById<Button>(R.id.confirmButton)

        // Setup option spinner using string array resource, but exclude 'All Option'
        val allCategories = resources.getStringArray(R.array.bread_categories)
        val realCategories = allCategories.filter { it != "All Option" }
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.modern_spinner_item,
            realCategories
        )
        spinnerAdapter.setDropDownViewResource(R.layout.modern_spinner_dropdown_item)
        optionSpinner.adapter = spinnerAdapter

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmButton.setOnClickListener {
            val name = itemNameInput.text.toString().trim()
            val price = itemPriceInput.text.toString().trim()
            val category = optionSpinner.selectedItem.toString()
            
            if (name.isEmpty() || price.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val priceValue = price.toDouble()
                // Default quantity to 0
                viewModel.addItem(name, priceValue, category)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Item added successfully", Toast.LENGTH_SHORT).show()
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showEditItemDialog(item: InventoryItem) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_inventory_item)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val itemNameInput = dialog.findViewById<EditText>(R.id.itemNameInput)
        val itemPriceInput = dialog.findViewById<EditText>(R.id.itemPriceInput)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val confirmButton = dialog.findViewById<Button>(R.id.confirmButton)

        // Set current values
        itemNameInput.setText(item.name)
        itemPriceInput.setText(item.price.toString())

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmButton.setOnClickListener {
            val name = itemNameInput.text.toString().trim()
            val price = itemPriceInput.text.toString().trim()
            
            if (name.isEmpty() || price.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val priceValue = price.toDouble()
                // Preserve the original quantity
                val updatedItem = item.copy(name = name, price = priceValue)
                viewModel.updateItem(updatedItem)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Item updated successfully", Toast.LENGTH_SHORT).show()
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}