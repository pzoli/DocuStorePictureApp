package hu.infokristaly.docustorepictureapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import hu.infokristaly.docustorepictureapp.databinding.DialogSearchFilterBinding

// 1. Definiálj egy interface-t az eredmények visszaküldéséhez (ajánlott)
interface FilterDialogListener {
    fun onFilterApplied(searchText: String)
}

class SearchFilterDialogFragment : DialogFragment() {

    private var _binding: DialogSearchFilterBinding? = null
    private val binding get() = _binding!!

    // A Listener, ami az Activity vagy Fragment felé kommunikál
    var listener: FilterDialogListener? = null

    // Vagy beállíthatod a listener-t a cél Fragment-re
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Ha Fragment-ben hívod meg:
        // listener = targetFragment as? FilterDialogListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSearchFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Beállítjuk a mentés gombot
        binding.btnSaveFilter.setOnClickListener {
            val searchText = binding.editSearchText.text.toString()

            // 3. Visszaküldjük az eredményt a Listener-en keresztül
            listener?.onFilterApplied(searchText)

            // 4. Bezárjuk a dialógust
            dismiss()
        }

        // Ha van korábbi érték, töltsd be (argumentumokkal)
        arguments?.getString("currentSearch")?.let {
            binding.editSearchText.setText(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(currentSearchText: String): SearchFilterDialogFragment {
            val fragment = SearchFilterDialogFragment()
            val args = Bundle().apply {
                putString("currentSearch", currentSearchText)
            }
            fragment.arguments = args
            return fragment
        }
    }
}