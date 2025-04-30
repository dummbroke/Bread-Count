package com.dummbroke.bread_count.transaction

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dummbroke.bread_count.R
import com.dummbroke.bread_count.databinding.FragmentTransactionPageBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import android.util.Log

class TransactionPage : Fragment() {

    private var _binding: FragmentTransactionPageBinding? = null
    private val binding get() = _binding!! // Property delegate to ensure non-null access after onCreateView

    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    // ActivityResultLauncher for saving the file
    private lateinit var createFileLauncher: ActivityResultLauncher<String>

    // Store CSV content temporarily when launching the file saver
    private var csvContentToSave: String? = null

    // Add a TAG for logging
    private val TAG = "TransactionPage"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the activity result launcher in onCreate
        createFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
            uri?.let { targetUri ->
                csvContentToSave?.let { content ->
                    writeCsvToFile(targetUri, content)
                }
            }
            // Clear the temporary content after attempting to save
            csvContentToSave = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated") // Log view creation
        setupRecyclerView()
        setupSpinner()
        setupExportButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.transactionRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
            // Optional: Add item decoration for dividers if desired
            // addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    private fun setupSpinner() {
        // Use the same categories as InventoryPage, including "All Option"
        val categories = resources.getStringArray(R.array.bread_categories)
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.modern_spinner_item, // Use the modern layout for spinner item
            categories
        )
        adapter.setDropDownViewResource(R.layout.modern_spinner_dropdown_item) // Use modern dropdown layout
        binding.filterSpinner.adapter = adapter
        binding.filterSpinner.setSelection(0) // Default to "All Option"

        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = parent?.getItemAtPosition(position).toString()
                // Log the selected category
                Log.d(TAG, "Spinner item selected: $selectedCategory") 
                viewModel.setCategoryFilter(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { 
                Log.d(TAG, "Spinner nothing selected")
            }
        }
    }

    private fun setupExportButton() {
        binding.exportButton.setOnClickListener { buttonView ->
            buttonView.isEnabled = false
            viewModel.exportTodaysTransactionsToCsv { result ->
                activity?.runOnUiThread {
                    result.onSuccess { (filename, csvContent) ->
                        csvContentToSave = csvContent
                        createFileLauncher.launch(filename)
                        buttonView.isEnabled = true
                    }.onFailure { error ->
                        Toast.makeText(requireContext(), "Export failed: ${error.message}", Toast.LENGTH_LONG).show()
                        buttonView.isEnabled = true
                    }
                }
            }
        }
    }

    private fun writeCsvToFile(uri: Uri, csvData: String) {
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use {
                    it.write(csvData)
                }
                showViewConfirmationDialog(uri)
            }
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "An unexpected error occurred: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showViewConfirmationDialog(fileUri: Uri) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_view_data_confirmation, null)
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val noButton = dialogView.findViewById<MaterialButton>(R.id.noButton)
        val viewButton = dialogView.findViewById<MaterialButton>(R.id.viewButton)

        noButton.setOnClickListener {
            dialog.dismiss()
        }

        viewButton.setOnClickListener {
            openCsvFile(fileUri)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openCsvFile(fileUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No application found to open CSV files.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        Log.d(TAG, "Setting up ViewModel observer") // Log observer setup
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                // Log the received state
                Log.d(TAG, "Observed UI State: ${state::class.java.simpleName}") 
                when (state) {
                    is TransactionUiState.Loading -> {
                        binding.salesDisplayText.text = "Loading..."
                    }
                    is TransactionUiState.Success -> {
                        Log.d(TAG, "Updating UI with ${state.transactions.size} transactions for filter: ${state.filterCategory}")
                        transactionAdapter.submitList(state.transactions)
                        val formattedSales = String.format(Locale.getDefault(), "₱%.2f", state.totalSales)
                        binding.salesDisplayText.text = formattedSales
                    }
                    is TransactionUiState.Error -> {
                        Log.e(TAG, "Received error state: ${state.message}") // Log errors
                        binding.salesDisplayText.text = "Error"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView") // Log view destruction
        _binding = null // Avoid memory leaks by clearing the binding reference
    }
} 