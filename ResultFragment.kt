package com.example.facedetection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.facedetection.databinding.FragmentResultBinding

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CameraViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel.bitmap.observe(viewLifecycleOwner) { bmp ->
            if (bmp != null) binding.imageView.setImageBitmap(bmp)
        }

        viewModel.mood.observe(viewLifecycleOwner) { mood ->
            binding.tvMood.text = "Mood: $mood"
        }

        viewModel.foodSuggestions.observe(viewLifecycleOwner) { foods ->
            binding.tvFoods.text = foods.joinToString("\n")
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
