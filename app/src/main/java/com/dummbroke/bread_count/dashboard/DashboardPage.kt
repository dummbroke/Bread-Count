package com.dummbroke.bread_count.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dummbroke.bread_count.R
import com.dummbroke.bread_count.model.InventoryItem
import com.dummbroke.bread_count.repository.FirebaseInventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.content.Context

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

// DashboardViewModel
class DashboardViewModel : ViewModel() {
    private val repository = FirebaseInventoryRepository()

    // Existing dashboard lists (for display)
    private val _displayBreadItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val displayBreadItems: StateFlow<List<InventoryItem>> = _displayBreadItems.asStateFlow()
    private val _displayBeveragesItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val displayBeveragesItems: StateFlow<List<InventoryItem>> = _displayBeveragesItems.asStateFlow()
    private val _deliveryBreadItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val deliveryBreadItems: StateFlow<List<InventoryItem>> = _deliveryBreadItems.asStateFlow()

    // Recorder UI state
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val categoryKeyMap = mapOf(
        "Display Bread" to "display_bread",
        "Display Beverages" to "display_beverages",
        "Delivery Bread" to "delivery_bread"
    )

    init {
        loadAllCategories()
    }

    private fun loadAllCategories() {
        viewModelScope.launch {
            repository.getInventoryItems("display_bread").collect { items ->
                _displayBreadItems.value = items
            }
        }
        viewModelScope.launch {
            repository.getInventoryItems("display_beverages").collect { items ->
                _displayBeveragesItems.value = items
            }
        }
        viewModelScope.launch {
            repository.getInventoryItems("delivery_bread").collect { items ->
                _deliveryBreadItems.value = items
            }
        }
    }

    fun onCategorySelected(category: String) {
        val key = categoryKeyMap[category] ?: return
        viewModelScope.launch {
            repository.getInventoryItems(key).collect { items ->
                _uiState.value = _uiState.value.copy(
                    selectedCategory = category,
                    items = items,
                    selectedItem = items.firstOrNull()
                )
            }
        }
    }

    fun onItemSelected(item: InventoryItem) {
        _uiState.value = _uiState.value.copy(selectedItem = item)
    }

    fun onQuantityChanged(quantity: Int) {
        val safeQuantity = if (quantity < 1) 1 else quantity
        _uiState.value = _uiState.value.copy(quantity = safeQuantity)
    }

    fun onOwnerSwitchChanged(isOwner: Boolean) {
        _uiState.value = _uiState.value.copy(isOwner = isOwner, ownerName = if (!isOwner) "" else _uiState.value.ownerName)
    }

    fun onOwnerNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(ownerName = name)
    }

    fun submitTransaction() {
        val state = _uiState.value
        val item = state.selectedItem ?: return
        val quantity = state.quantity
        val isOwner = state.isOwner
        val ownerName = state.ownerName
        if (isOwner && ownerName.isBlank()) return // Require owner name if owner
        viewModelScope.launch {
            _uiState.value = state.copy(isRecording = true)
            try {
                // Record transaction (pseudo-code, implement in repository as needed)
                repository.recordTransaction(
                    item = item,
                    quantity = quantity,
                    isOwner = isOwner,
                    ownerName = ownerName
                )
                // Show confirmation for 1 second
                _uiState.value = state.copy(showConfirmation = true, isRecording = false)
                kotlinx.coroutines.delay(1000)
                // Reset only quantity, owner switch, and owner name, but keep category and item
                _uiState.value = state.copy(
                    quantity = 1,
                    isOwner = false,
                    ownerName = "",
                    isRecording = false,
                    showConfirmation = false
                )
            } catch (e: Exception) {
                // Handle error (could add error state to DashboardUiState if needed)
                _uiState.value = state.copy(isRecording = false)
            }
        }
    }
}

// DashboardItemAdapter
class DashboardItemAdapter : ListAdapter<InventoryItem, DashboardItemAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)

        fun bind(item: InventoryItem) {
            titleTextView.text = item.name
            quantityTextView.text = "${item.quantity} left"
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<InventoryItem>() {
        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean {
            return oldItem == newItem
        }
    }
}

// DashboardPage Fragment
class DashboardPage : Fragment() {
    private val viewModel: DashboardViewModel by viewModels()
    
    private lateinit var displayBreadAdapter: DashboardItemAdapter
    private lateinit var displayBeveragesAdapter: DashboardItemAdapter
    private lateinit var deliveryBreadAdapter: DashboardItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews(view)
        observeViewModel()
        setupTransactionRecorder(view)
    }

    private fun setupRecyclerViews(view: View) {
        // Setup Display Bread RecyclerView
        val displayBreadRecycler = view.findViewById<RecyclerView>(R.id.displayBreadRecycler)
        displayBreadAdapter = DashboardItemAdapter()
        displayBreadRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = displayBreadAdapter
        }

        // Setup Display Beverages RecyclerView
        val displayBeveragesRecycler = view.findViewById<RecyclerView>(R.id.displayBeveragesRecycler)
        displayBeveragesAdapter = DashboardItemAdapter()
        displayBeveragesRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = displayBeveragesAdapter
        }

        // Setup Delivery Bread RecyclerView
        val deliveryBreadRecycler = view.findViewById<RecyclerView>(R.id.deliveryBreadRecycler)
        deliveryBreadAdapter = DashboardItemAdapter()
        deliveryBreadRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = deliveryBreadAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.displayBreadItems.collectLatest { items ->
                displayBreadAdapter.submitList(items)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.displayBeveragesItems.collectLatest { items ->
                displayBeveragesAdapter.submitList(items)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deliveryBreadItems.collectLatest { items ->
                deliveryBreadAdapter.submitList(items)
            }
        }
        // Observe transaction recorder UI state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                updateTransactionRecorderUI(requireView(), state)
            }
        }
    }

    private fun setupTransactionRecorder(view: View) {
        val categorySpinner = view.findViewById<android.widget.Spinner>(R.id.categorySpinner)
        val itemSpinner = view.findViewById<android.widget.Spinner>(R.id.itemSpinner)
        val quantityEditText = view.findViewById<android.widget.EditText>(R.id.quantityEditText)
        val plusButton = view.findViewById<android.widget.ImageButton>(R.id.plusButton)
        val minusButton = view.findViewById<android.widget.ImageButton>(R.id.minusButton)
        val ownerSwitch = view.findViewById<android.widget.Switch>(R.id.ownerSwitch)
        val ownerNameEditText = view.findViewById<android.widget.EditText>(R.id.ownerName)
        val submitButton = view.findViewById<android.widget.Button>(R.id.submitButton)

        // Setup category spinner with custom layout for visibility
        val categories = listOf("Display Bread", "Display Beverages", "Delivery Bread")
        val categoryAdapter = android.widget.ArrayAdapter(
            requireContext(),
            R.layout.modern_spinner_item,
            categories
        )
        categoryAdapter.setDropDownViewResource(R.layout.modern_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        categorySpinner.setSelection(0)
        categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val category = categories[position]
                viewModel.onCategorySelected(category)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Setup item spinner (adapter will be set in updateTransactionRecorderUI)
        itemSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = (parent?.adapter?.getItem(position) as? com.dummbroke.bread_count.model.InventoryItem)
                if (item != null) viewModel.onItemSelected(item)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Quantity edit text and plus/minus buttons
        quantityEditText.setText("1")
        quantityEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val qty = quantityEditText.text.toString().toIntOrNull() ?: 1
                viewModel.onQuantityChanged(qty)
            }
        }
        plusButton.setOnClickListener {
            val qty = quantityEditText.text.toString().toIntOrNull() ?: 1
            viewModel.onQuantityChanged(qty + 1)
        }
        minusButton.setOnClickListener {
            val qty = quantityEditText.text.toString().toIntOrNull() ?: 1
            viewModel.onQuantityChanged(qty - 1)
        }

        // Owner switch and name logic
        ownerSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onOwnerSwitchChanged(isChecked)
            // Enable/disable ownerNameEditText and update background/alpha
            if (isChecked) {
                ownerNameEditText.isEnabled = true
                ownerNameEditText.alpha = 1.0f
                ownerNameEditText.setBackgroundResource(R.drawable.edit_text_bg) // normal background
            } else {
                ownerNameEditText.isEnabled = false
                ownerNameEditText.alpha = 0.5f
                ownerNameEditText.setBackgroundResource(R.drawable.disabled_edit_text_bg) // use a muted/disabled color
            }
            // Always set text color to spinner text color for visibility
            ownerNameEditText.setTextColor(requireContext().getColor(R.color.graphite_black))
        }
        // Initial state for ownerNameEditText
        if (ownerSwitch.isChecked) {
            ownerNameEditText.isEnabled = true
            ownerNameEditText.alpha = 1.0f
            ownerNameEditText.setBackgroundResource(R.drawable.edit_text_bg)
        } else {
            ownerNameEditText.isEnabled = false
            ownerNameEditText.alpha = 0.5f
            ownerNameEditText.setBackgroundResource(R.drawable.disabled_edit_text_bg)
        }
        // Always set text color to spinner text color for visibility
        ownerNameEditText.setTextColor(requireContext().getColor(R.color.graphite_black))
        // Add TextWatcher to update ViewModel as user types
        ownerNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newText = s?.toString() ?: ""
                if (ownerNameEditText.isEnabled && newText != viewModel.uiState.value.ownerName) {
                    viewModel.onOwnerNameChanged(newText)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Submit button
        submitButton.setOnClickListener {
            // Hide keyboard and clear focus from ownerNameEditText
            ownerNameEditText.clearFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(ownerNameEditText.windowToken, 0)
            viewModel.submitTransaction()
        }
    }

    private fun updateTransactionRecorderUI(view: View, state: DashboardUiState) {
        val itemSpinner = view.findViewById<android.widget.Spinner>(R.id.itemSpinner)
        val quantityEditText = view.findViewById<android.widget.EditText>(R.id.quantityEditText)
        val ownerSwitch = view.findViewById<android.widget.Switch>(R.id.ownerSwitch)
        val ownerNameEditText = view.findViewById<android.widget.EditText>(R.id.ownerName)
        val submitButton = view.findViewById<android.widget.Button>(R.id.submitButton)

        // Update item spinner with custom layout for visibility
        val itemAdapter = object : android.widget.ArrayAdapter<com.dummbroke.bread_count.model.InventoryItem>(
            requireContext(),
            R.layout.modern_spinner_item,
            state.items
        ) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as? android.widget.TextView)?.text = getItem(position)?.name ?: ""
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                (v as? android.widget.TextView)?.text = getItem(position)?.name ?: ""
                return v
            }
        }
        itemAdapter.setDropDownViewResource(R.layout.modern_spinner_dropdown_item)
        itemSpinner.adapter = itemAdapter
        val selectedIndex = state.items.indexOfFirst { it.id == state.selectedItem?.id }
        if (selectedIndex >= 0) itemSpinner.setSelection(selectedIndex)

        // Update quantity
        if (quantityEditText.text.toString() != state.quantity.toString()) {
            quantityEditText.setText(state.quantity.toString())
        }

        // Owner switch and name logic
        ownerSwitch.isChecked = state.isOwner
        if (state.isOwner) {
            ownerNameEditText.isEnabled = true
            ownerNameEditText.alpha = 1.0f
            ownerNameEditText.setBackgroundResource(R.drawable.edit_text_bg)
        } else {
            ownerNameEditText.isEnabled = false
            ownerNameEditText.alpha = 0.5f
            ownerNameEditText.setBackgroundResource(R.drawable.disabled_edit_text_bg)
        }
        // Always set text color to spinner text color for visibility
        ownerNameEditText.setTextColor(requireContext().getColor(R.color.graphite_black))
        // Only set text if different to avoid cursor jump/backwards typing
        if (ownerNameEditText.text.toString() != state.ownerName) {
            ownerNameEditText.setText(state.ownerName)
            ownerNameEditText.setSelection(state.ownerName.length)
        }

        // Disable submit if owner is checked but name is blank
        submitButton.isEnabled = !state.isOwner || state.ownerName.isNotBlank()

        // Show confirmation dialog if needed
        if (state.showConfirmation) {
            showConfirmationDialog()
        }
    }

    private fun showConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction_success, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(1000)
            dialog.dismiss()
        }
    }
}

