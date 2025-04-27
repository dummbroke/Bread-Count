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

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

// DashboardViewModel
class DashboardViewModel : ViewModel() {
    private val repository = FirebaseInventoryRepository()

    private val _displayBreadItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val displayBreadItems: StateFlow<List<InventoryItem>> = _displayBreadItems.asStateFlow()

    private val _displayBeveragesItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val displayBeveragesItems: StateFlow<List<InventoryItem>> = _displayBeveragesItems.asStateFlow()

    private val _deliveryBreadItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val deliveryBreadItems: StateFlow<List<InventoryItem>> = _deliveryBreadItems.asStateFlow()

    init {
        loadAllCategories()
    }

    private fun loadAllCategories() {
        viewModelScope.launch {
            // Load Display Bread
            repository.getInventoryItems("display_bread").collect { items ->
                _displayBreadItems.value = items
            }
        }

        viewModelScope.launch {
            // Load Display Beverages
            repository.getInventoryItems("display_beverages").collect { items ->
                _displayBeveragesItems.value = items
            }
        }

        viewModelScope.launch {
            // Load Delivery Bread
            repository.getInventoryItems("delivery_bread").collect { items ->
                _deliveryBreadItems.value = items
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
    }
}

