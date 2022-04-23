package com.santacruzinstruments.ottopi.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.databinding.FragmentConnectionStateBinding;

public class ConnectionStateFragment extends Fragment {

    private NavViewModel navViewModel;
    private FragmentConnectionStateBinding binding;

    public ConnectionStateFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navViewModel = new ViewModelProvider(requireActivity()).get(NavViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentConnectionStateBinding.inflate(inflater, container, false);

        // USB status
        navViewModel.getUsbConnected().observe(getViewLifecycleOwner(), connected -> {
            if( connected ){
                binding.UsbStatusImageView.setColorFilter(
                        requireActivity().getColor(R.color.primaryTextColor),
                        android.graphics.PorterDuff.Mode.MULTIPLY);
            }else{
                binding.UsbStatusImageView.setColorFilter(
                        requireActivity().getColor(android.R.color.holo_red_light),
                        android.graphics.PorterDuff.Mode.MULTIPLY);
            }
        });

        // Network status
        navViewModel.getConnectionState().observe(getViewLifecycleOwner(), connectionState -> {

            switch (connectionState.getState()){
                case DISABLED:
                    binding.ConnStateTextView.setBackgroundResource(android.R.color.transparent);
                    binding.ConnStateTextView.setTextColor(requireActivity().getColor(R.color.primaryLightColor));
                    binding.ConnStateTextView.setText(R.string.wifi_is_disabled);
                    break;
                case CONNECTED:
                    binding.ConnStateTextView.setBackgroundResource(android.R.color.transparent);
                    binding.ConnStateTextView.setTextColor(requireActivity().getColor(R.color.primaryLightColor));
                    binding.ConnStateTextView.setText(connectionState.toString());
                    break;
                case CONNECTING_TO_HOST:
                    binding.ConnStateTextView.setBackgroundResource(android.R.color.holo_orange_light);
                    binding.ConnStateTextView.setTextColor(requireActivity().getColor(R.color.primaryTextColor));
                    binding.ConnStateTextView.setText(connectionState.toString());
                    break;
                case CONNECTING_TO_WIFI:
                    binding.ConnStateTextView.setBackgroundResource(android.R.color.holo_red_light);
                    binding.ConnStateTextView.setTextColor(requireActivity().getColor(R.color.primaryTextColor));
                    binding.ConnStateTextView.setText(connectionState.toString());
                    break;
                case NOT_CONNECTED:
                    break;
            }

        });

        // Data reception status
        navViewModel.getDataReceptionStatus().observe(getViewLifecycleOwner(), dataReceptionStatus -> {

            int count = dataReceptionStatus.getCount();
            float percentDelta = (count % 15) * 0.02f;
            binding.guideline.setGuidelinePercent(0.4f + percentDelta);

            binding.DataReceptionGpsTextView.setBackgroundResource(
                    dataReceptionStatus.isHaveGps() ?  R.drawable.grey_circle : R.drawable.red_circle
            );

            binding.DataReceptionWaterTextView.setBackgroundResource(
                    dataReceptionStatus.isHaveWater() ?  R.drawable.grey_circle : R.drawable.red_circle
            );

            binding.DataReceptionWindTextView.setBackgroundResource(
                    dataReceptionStatus.isHaveWind() ?  R.drawable.grey_circle : R.drawable.red_circle
            );

            binding.DataReceptionCompassTextView.setBackgroundResource(
                    dataReceptionStatus.isHaveCompass() ?  R.drawable.grey_circle : R.drawable.red_circle
            );

            }
        );

        navViewModel.getLoggingTag().observe(getViewLifecycleOwner(),
                logTag -> binding.LogTagTextView.setText(logTag));

        return binding.getRoot();
    }

}
