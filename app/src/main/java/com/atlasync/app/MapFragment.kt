package com.atlasync.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.atlasync.app.databinding.FragmentMapBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MapFragment : Fragment() {

    private lateinit var userLocationPoint: ImageView

    private var _binding: FragmentMapBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMapBinding.inflate(inflater, container, false)

        userLocationPoint = binding.root.getViewById(R.id.userLocationPoint) as AppCompatImageView
        userLocationPoint.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.userLocationPointColor))

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


//        binding.buttonFirst.setOnClickListener {
//            findNavController().navigate(R.id.action_MapFragment_to_SecondFragment)
//        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (activity is MainActivity) {
            val ac = (activity as MainActivity)
            if (ac.lastRoomID != "") {
                outState.putString(ac.LAST_ROOM_ID_KEY, ac.lastRoomID)
            }
            println("SAVING FRAGMENT STATE")
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (activity is MainActivity) {
            if (savedInstanceState != null) {
                val ac = (activity as MainActivity)
                ac.lastRoomID = savedInstanceState.getString(ac.LAST_ROOM_ID_KEY) ?: ""
                if (ac.lastRoomID != "") {
                    ac.getRoomInfo(ac.lastRoomID)
                }
                println("RESTORING FRAGMENT STATE")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}