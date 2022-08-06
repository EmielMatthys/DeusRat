package com.example.deusrat

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.deusrat.client.TcpAuthenticator
import com.example.deusrat.databinding.FragmentWolBinding
import kotlinx.coroutines.runBlocking

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class WolFragment : Fragment() {

    private var _binding: FragmentWolBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentWolBinding.inflate(inflater, container, false)
        enterTransition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.slide_right)
        return binding.root

    }

    external fun sendMagicPacketNative(): String
    external fun remoteLoginNative(): String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSendMagic.setOnClickListener {
            val logs = sendMagicPacketNative()
            binding.wolLogview.append("\n" + logs)
        }

        binding.buttonClearLog.setOnClickListener {
            binding.wolLogview.text = getString(R.string.default_console_text)
        }

        binding.buttonLogin.setOnClickListener {
            runBlocking {  TcpAuthenticator().connect { message ->
                binding.wolLogview.append("\n" + message)
            }}
        }

        binding.wolLogview.movementMethod = ScrollingMovementMethod()
    }

    override fun onStop() {
        super.onStop()
        (activity as MainActivity).supportActionBar?.hide()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    suspend fun addToTextView(text: String) : Int
    {
        binding.wolLogview.append("\n" + text)
        return 0;
    }
}