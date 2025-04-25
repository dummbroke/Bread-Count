package com.dummbroke.bread_count.inventory

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dummbroke.bread_count.R
import com.dummbroke.bread_count.adapter.InventoryAdapter
import com.dummbroke.bread_count.model.InventoryItem
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [InventoryPage.newInstance] factory method to
 * create an instance of this fragment.
 */
class InventoryPage : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private val viewModel: InventoryViewModel by viewModels()
    private lateinit var categorySpinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InventoryAdapter

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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_inventory_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        categorySpinner = view.findViewById(R.id.categorySpinner)
        recyclerView = view.findViewById(R.id.inventoryRecyclerView)
        val addItemButton = view.findViewById<ExtendedFloatingActionButton>(R.id.addItemButton)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup category spinner
        setupCategorySpinner()

        // Setup add item button
        addItemButton.setOnClickListener {
            showAddItemDialog()
        }

        // Observe inventory items
        lifecycleScope.launch {
            viewModel.inventoryItems.collectLatest { items ->
                adapter.updateItems(items)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = InventoryAdapter(
            onItemClick = { item ->
                // Handle item click (edit)
                showEditItemDialog(item)
            },
            onDeleteClick = { item ->
                // Handle delete click
                viewModel.deleteItem(item)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf(
            InventoryViewModel.DISPLAY_BREAD,
            InventoryViewModel.DISPLAY_BEVERAGES,
            InventoryViewModel.DELIVERY_BREAD
        )
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_selected_item, categories)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)
        categorySpinner.adapter = spinnerAdapter

        categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setCategory(categories[position])
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
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
        val itemNameInput = dialog.findViewById<android.widget.EditText>(R.id.itemNameInput)
        val itemPriceInput = dialog.findViewById<android.widget.EditText>(R.id.itemPriceInput)
        val cancelButton = dialog.findViewById<android.widget.Button>(R.id.cancelButton)
        val confirmButton = dialog.findViewById<android.widget.Button>(R.id.confirmButton)

        // Setup option spinner
        val categories = arrayOf(
            InventoryViewModel.DISPLAY_BREAD,
            InventoryViewModel.DISPLAY_BEVERAGES,
            InventoryViewModel.DELIVERY_BREAD
        )
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_selected_item, categories)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)
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
                viewModel.addItem(name, priceValue, category)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Item added successfully", Toast.LENGTH_SHORT).show()
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "Please enter a valid price", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showEditItemDialog(item: InventoryItem) {
        // TODO: Implement edit dialog
    }
}