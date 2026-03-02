package com.krystalshard.lifemelodyapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.krystalshard.lifemelodyapp.databinding.DialogSleepTimerBinding
import java.util.concurrent.TimeUnit

class SleepTimerDialogFragment : DialogFragment() {

    private var _binding: DialogSleepTimerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSleepTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        val timerOptions = mapOf(
            binding.tv10Minutes to TimeUnit.MINUTES.toMillis(10),
            binding.tv20Minutes to TimeUnit.MINUTES.toMillis(20),
            binding.tv30Minutes to TimeUnit.MINUTES.toMillis(30),
            binding.tv2Hours to TimeUnit.MINUTES.toMillis(120),
            binding.tv3Hours to TimeUnit.MINUTES.toMillis(180)
        )

        for ((textView, duration) in timerOptions) {
            textView.setOnClickListener {
                sendTimerAction(duration)
            }
        }

        binding.tvCancelTimer.setOnClickListener {
            sendTimerAction(0L)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun sendTimerAction(durationMillis: Long) {
        val timerIntent = Intent(requireContext(), MusicService::class.java).apply {
            putExtra("ACTION", MusicService.ACTION_SET_SLEEP_TIMER)
            putExtra("DURATION_MILLIS", durationMillis)
        }

        requireContext().startService(timerIntent)

        val message = if (durationMillis > 0) {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
            getString(R.string.timer_set_confirmation, minutes)
        } else {
            getString(R.string.timer_cancel_confirmation)
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}