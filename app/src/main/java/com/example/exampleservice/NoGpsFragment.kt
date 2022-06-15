package com.example.exampleservice

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.exampleservice.databinding.FragmentNoGpsBinding

/**
 * Created by Nicola Luigi Piriottu
 */
class NoGpsFragment : Fragment() {

    /**
     * Binding
     */
    private lateinit var binding: FragmentNoGpsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate & Bind
        binding = FragmentNoGpsBinding.inflate(inflater, container, false)

        return binding.root
    }

}