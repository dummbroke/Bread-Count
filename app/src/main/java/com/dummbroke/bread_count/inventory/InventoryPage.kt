package com.dummbroke.bread_count.inventory

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
import com.dummbroke.bread_count.signup.SignInActivity
import com.google.firebase.auth.FirebaseAuth
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

    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Initial)
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private var currentCategory = "display_bread"

    init {
        loadInventoryItems()
    }

    private fun loadInventoryItems() {
        viewModelScope.launch {
            _uiState.value = InventoryUiState.Loading
            try {
                repository.getInventoryItems(currentCategory).collect { items ->
                    _uiState.value = InventoryUiState.Success(items, currentCategory)
                }
            } catch (e: Exception) {
                _uiState.value = InventoryUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun setCategory(category: String) {
        currentCategory = when (category) {
            "All Option" -> "all_option"
            "Display Bread" -> "display_bread"
            "Display Beverages" -> "display_beverages"
            "Delivery Bread" -> "delivery_bread"
            else -> "display_bread"
        }

        if (category == "All Option") {
            viewModelScope.launch {
                _uiState.value = InventoryUiState.Loading
                try {
                    kotlinx.coroutines.flow.combine(
                        repository.getInventoryItems("display_bread"),
                        repository.getInventoryItems("display_beverages"),
                        repository.getInventoryItems("delivery_bread")
                    ) { bread, beverages, delivery ->
                        bread + beverages + delivery
                    }.collect { allItems ->
                        _uiState.value = InventoryUiState.Success(allItems, currentCategory)
                    }
                } catch (e: Exception) {
                    _uiState.value = InventoryUiState.Error(e.message ?: "Failed to fetch all items")
                }
            }
        } else {
            loadInventoryItems()
        }
    }

    fun addItem(name: String, price: Double, category: String, quantity: Int = 0) {
        viewModelScope.launch {
            _uiState.value = InventoryUiState.Loading
            try {
                val item = InventoryItem(
                    name = name,
                    price = price,
                    category = category,
                    quantity = quantity
                )
                val categoryKey = when (category) {
                    "Display Bread" -> "display_bread"
                    "Display Beverages" -> "display_beverages"
                    "Delivery Bread" -> "delivery_bread"
                    else -> "display_bread"
                }
                repository.addItem(item, categoryKey)
                // Refresh the view based on current category
                if (currentCategory == "all_option") {
                    kotlinx.coroutines.flow.combine(
                        repository.getInventoryItems("display_bread"),
                        repository.getInventoryItems("display_beverages"),
                        repository.getInventoryItems("delivery_bread")
                    ) { bread, beverages, delivery ->
                        bread + beverages + delivery
                    }.collect { allItems ->
                        _uiState.value = InventoryUiState.Success(allItems, currentCategory)
                    }
                } else {
                    loadInventoryItems()
                }
            } catch (e: Exception) {
                _uiState.value = InventoryUiState.Error(e.message ?: "Failed to add item")
            }
        }
    }

    fun updateItem(originalItem: InventoryItem, updatedItem: InventoryItem) {
        viewModelScope.launch {
            _uiState.value = InventoryUiState.Loading
            try {
                val originalCategoryKey = when (originalItem.category) {
                    "Display Bread" -> "display_bread"
                    "Display Beverages" -> "display_beverages"
                    "Delivery Bread" -> "delivery_bread"
                    else -> originalItem.category // Assume it's already a key
                }
                val updatedCategoryKey = when (updatedItem.category) {
                    "Display Bread" -> "display_bread"
                    "Display Beverages" -> "display_beverages"
                    "Delivery Bread" -> "delivery_bread"
                    else -> updatedItem.category // Assume it's already a key
                }

                if (originalCategoryKey == updatedCategoryKey) {
                    // Category hasn't changed, just update the item using its ID
                    Log.d("InventoryViewModel", "Updating item ${updatedItem.id} in category $updatedCategoryKey")
                    repository.updateItem(updatedItem, updatedCategoryKey) // updatedItem has the original ID
                } else {
                    // Category has changed: Use the atomic moveItem operation
                    Log.d("InventoryViewModel", "Moving item ${updatedItem.id} from $originalCategoryKey to $updatedCategoryKey using batch write")
                    // Pass the updatedItem (which contains the new category display name) and the key paths
                    repository.moveItem(updatedItem, originalCategoryKey, updatedCategoryKey)
                    Log.d("InventoryViewModel", "Successfully moved item ${updatedItem.id}")
                }

                // Refresh based on the globally selected category
                Log.d("InventoryViewModel", "Update successful, refreshing UI for category: $currentCategory")
                if (currentCategory == "all_option") {
                    kotlinx.coroutines.flow.combine(
                        repository.getInventoryItems("display_bread"),
                        repository.getInventoryItems("display_beverages"),
                        repository.getInventoryItems("delivery_bread")
                    ) { bread, beverages, delivery ->
                        bread + beverages + delivery
                    }.collect { allItems ->
                        _uiState.value = InventoryUiState.Success(allItems, currentCategory)
                    }
                } else {
                    loadInventoryItems() // Reload items for the currently selected category
                }
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Overall failure in updateItem for item ${originalItem.id}", e)
                _uiState.value = InventoryUiState.Error(e.message ?: "Failed to update item")
            }
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            _uiState.value = InventoryUiState.Loading
            try {
                // Convert display category to storage category
                val categoryKey = when (item.category) {
                    "Display Bread" -> "display_bread"
                    "Display Beverages" -> "display_beverages"
                    "Delivery Bread" -> "delivery_bread"
                    else -> item.category // Use the raw category if it's already in storage format
                }
                repository.deleteItem(item.id, categoryKey)
                
                // Refresh the view based on current category
                if (currentCategory == "all_option") {
                    kotlinx.coroutines.flow.combine(
                        repository.getInventoryItems("display_bread"),
                        repository.getInventoryItems("display_beverages"),
                        repository.getInventoryItems("delivery_bread")
                    ) { bread, beverages, delivery ->
                        bread + beverages + delivery
                    }.collect { allItems ->
                        _uiState.value = InventoryUiState.Success(allItems, currentCategory)
                    }
                } else {
                    loadInventoryItems()
                }
            } catch (e: Exception) {
                _uiState.value = InventoryUiState.Error(e.message ?: "Failed to delete item")
            }
        }
    }

    fun updateQuantity(item: InventoryItem, newQuantity: Int) {
        viewModelScope.launch {
            _uiState.value = InventoryUiState.Loading
            try {
                val updatedItem = item.copy(quantity = newQuantity)
                repository.updateItem(updatedItem, currentCategory)
                loadInventoryItems()
            } catch (e: Exception) {
                _uiState.value = InventoryUiState.Error(e.message ?: "Failed to update quantity")
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

        // Setup menu button
        setupMenuButton(view)

        // Observe UI state
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is InventoryUiState.Initial -> {
                        // Initial state, no action needed
                    }
                    is InventoryUiState.Loading -> {
                    }
                    is InventoryUiState.Success -> {
                        adapter.submitList(state.items)
                    }
                    is InventoryUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
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
                showDeleteConfirmationDialog(item)
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

    private fun setupMenuButton(view: View) {
        view.findViewById<ImageButton>(R.id.menuButton).setOnClickListener {
            (activity as? com.dummbroke.bread_count.MainActivity)?.openDrawer()
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
            val quantity = dialog.findViewById<EditText>(R.id.itemQuantityInput).text.toString().trim()
            val category = optionSpinner.selectedItem.toString()
            
            if (name.isEmpty() || price.isEmpty() || quantity.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val priceValue = price.toDouble()
                val quantityValue = quantity.toInt()
                viewModel.addItem(name, priceValue, category, quantityValue)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Item added successfully", Toast.LENGTH_SHORT).show()
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "Invalid price or quantity", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(item: InventoryItem) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_delete_confirmation)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val confirmButton = dialog.findViewById<Button>(R.id.confirmButton)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmButton.setOnClickListener {
            viewModel.deleteItem(item)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditItemDialog(item: InventoryItem) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_inventory_item)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        val optionSpinner = dialog.findViewById<Spinner>(R.id.optionSpinner)
        val itemNameInput = dialog.findViewById<EditText>(R.id.itemNameInput)
        val itemPriceInput = dialog.findViewById<EditText>(R.id.itemPriceInput)
        val itemQuantityInput = dialog.findViewById<EditText>(R.id.itemQuantityInput)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val confirmButton = dialog.findViewById<Button>(R.id.confirmButton)

        // Setup spinner options
        val spinnerOptions = listOf("Display Bread", "Display Beverages", "Delivery Bread")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.modern_spinner_item, spinnerOptions)
        spinnerAdapter.setDropDownViewResource(R.layout.modern_spinner_dropdown_item)
        optionSpinner.adapter = spinnerAdapter

        // Pre-fill fields with current item data
        itemNameInput.setText(item.name)
        itemPriceInput.setText(item.price.toString())
        itemQuantityInput.setText(item.quantity.toString())
        val categoryIndex = spinnerOptions.indexOf(item.category)
        if (categoryIndex >= 0) optionSpinner.setSelection(categoryIndex)

        cancelButton.setOnClickListener { dialog.dismiss() }
        confirmButton.setOnClickListener {
            val name = itemNameInput.text.toString().trim()
            val price = itemPriceInput.text.toString().trim().toDoubleOrNull() ?: 0.0
            val quantity = itemQuantityInput.text.toString().trim().toIntOrNull() ?: 0
            val category = optionSpinner.selectedItem.toString()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val updatedItem = item.copy(name = name, price = price, quantity = quantity, category = category)
            Log.d("InventoryPage", "Original Item ID: ${item.id}, Updated Item ID: ${updatedItem.id}") // Add logging here
            // Pass both the original item and the updated item to the ViewModel
            viewModel.updateItem(item, updatedItem)
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}