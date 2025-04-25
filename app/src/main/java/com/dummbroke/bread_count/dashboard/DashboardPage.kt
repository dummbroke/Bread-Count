package com.dummbroke.bread_count.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.dummbroke.bread_count.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class DashboardPage : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up category spinner
        val categorySpinner = view.findViewById<Spinner>(R.id.categorySpinner)
        val categories = resources.getStringArray(R.array.bread_categories)
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            R.layout.modern_spinner_item,
            categories
        )
        categoryAdapter.setDropDownViewResource(R.layout.modern_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        
        // Set up item spinner with placeholder data
        val itemSpinner = view.findViewById<Spinner>(R.id.itemSpinner)
        val items = arrayOf("Item 1", "Item 2", "Item 3", "Item 4")
        val itemAdapter = ArrayAdapter(
            requireContext(),
            R.layout.modern_spinner_item,
            items
        )
        itemAdapter.setDropDownViewResource(R.layout.modern_spinner_dropdown_item)
        itemSpinner.adapter = itemAdapter
    }
}

