package com.example.exampleservice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.exampleservice.databinding.ActivityMainBinding

/**
 * Created by Nicola Luigi Piriottu
 */
class MainActivity : AppCompatActivity() {

    /**
     * Binding
     */
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}