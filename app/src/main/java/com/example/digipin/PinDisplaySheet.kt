package com.example.digipin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PinDisplaySheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_DIGIPIN = "digipin"

        fun newInstance(digipin: String): PinDisplaySheet {
            val fragment = PinDisplaySheet()
            val args = Bundle()
            args.putString(ARG_DIGIPIN, digipin)
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_pin_display_sheet, container, false)
        val digipinTextView: TextView = view.findViewById(R.id.pin)

        val digipin = arguments?.getString(ARG_DIGIPIN)
        digipinTextView.text = digipin ?: "No DigiPIN found"

        return view
//        return inflater.inflate(R.layout.fragment_pin_display_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textView = view.findViewById<TextView>(R.id.pin)
        val copyButton = view.findViewById<ImageButton>(R.id.copyBtn)

        copyButton?.setOnClickListener {
            val textToCopy = textView?.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", textToCopy)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

}