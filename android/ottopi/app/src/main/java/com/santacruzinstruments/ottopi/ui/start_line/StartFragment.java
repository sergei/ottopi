package com.santacruzinstruments.ottopi.ui.start_line;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.data.SailingState;
import com.santacruzinstruments.ottopi.data.StartType;
import com.santacruzinstruments.ottopi.databinding.FragmentStartBinding;
import com.santacruzinstruments.ottopi.ui.KeyMapper;
import com.santacruzinstruments.ottopi.ui.NavViewModel;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class StartFragment extends Fragment {

    private NavViewModel navViewModel;
    private FragmentStartBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navViewModel = new ViewModelProvider(requireActivity()).get(NavViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentStartBinding.inflate(inflater, container, false);

        binding.timerButton.setOnClickListener(view -> navViewModel.ctrl().onStartButtonPress());
        binding.pinStartButton.setOnClickListener(view -> navViewModel.ctrl().onPinButtonPress());
        binding.committeeStartButton.setOnClickListener(view -> navViewModel.ctrl().onRcbButtonPress());

        navViewModel.getRcbMarkValidity().observe(getViewLifecycleOwner(), isValid
                -> showHalfLine(binding.rcbHalfStartLineView, isValid));

        navViewModel.getPinMarkValidity().observe(getViewLifecycleOwner(), isValid
                -> showHalfLine(binding.pinHalfStartLineView, isValid));

        // Either race state or start type has changed
        navViewModel.getRaceTypeAndState().observe(getViewLifecycleOwner(), raceTypeState -> {
            // If we are already racing, or start type is not configured go to main screen
            if (raceTypeState.startType == StartType.NO_START || raceTypeState.state == SailingState.RACING) {
                Timber.d("Go to main screen");
                NavDirections action = StartFragmentDirections.actionStartFragmentToNavFragment();
                Navigation.findNavController(binding.getRoot()).navigate(action);
            }
        });

        navViewModel.getSecondsToStart().observe(getViewLifecycleOwner(), secondsToStart ->
                binding.timerButton.setText(String.format(Locale.getDefault(),
                "%02d:%02d", secondsToStart / 60, secondsToStart % 60)));

        navViewModel.getStartLineInfo().observe(getViewLifecycleOwner(), startLineInfo -> {
            if ( startLineInfo.distToLine.isValid() ){
                TextView distanceToLineTextView;
                if ( startLineInfo.isOcs ){
                    distanceToLineTextView = binding.distanceToLineOcsTextView;
                    binding.distanceToLineOcsTextView.setVisibility(View.VISIBLE);
                    binding.distanceToLineTextView.setVisibility(View.INVISIBLE);
                }else{
                    distanceToLineTextView = binding.distanceToLineTextView;
                    binding.distanceToLineOcsTextView.setVisibility(View.INVISIBLE);
                    binding.distanceToLineTextView.setVisibility(View.VISIBLE);
                }
                double dtl = startLineInfo.distToLine.toMeters();
                if ( dtl < 999 ) {
                    String text = String.format(Locale.getDefault(), "%.0f", dtl);
                    distanceToLineTextView.setText(text);

                    if( startLineInfo.pinFavoredBy.isValid() ){
                        double angle = startLineInfo.pinFavoredBy.toDegrees();
                        String s = String.format(Locale.getDefault(),"%.0f°", Math.abs(angle));
                        if ( angle > 0){
                            binding.pinFavorTextView.setVisibility(View.VISIBLE);
                            binding.pinFavorTextView.setText(s);
                            binding.rcbFavorTextView.setVisibility(View.INVISIBLE);
                        }else{
                            binding.pinFavorTextView.setVisibility(View.INVISIBLE);
                            binding.rcbFavorTextView.setVisibility(View.VISIBLE);
                            binding.rcbFavorTextView.setText(s);
                        }
                    }else{
                        binding.pinFavorTextView.setVisibility(View.INVISIBLE);
                        binding.rcbFavorTextView.setVisibility(View.INVISIBLE);
                    }
                }else{
                    distanceToLineTextView.setText(R.string.far);
                    binding.pinFavorTextView.setVisibility(View.INVISIBLE);
                    binding.rcbFavorTextView.setVisibility(View.INVISIBLE);
                }
            }else{
                binding.distanceToLineOcsTextView.setVisibility(View.INVISIBLE);
                binding.distanceToLineTextView.setVisibility(View.INVISIBLE);
                binding.pinFavorTextView.setVisibility(View.INVISIBLE);
                binding.rcbFavorTextView.setVisibility(View.INVISIBLE);
            }

        });

        return binding.getRoot();
    }

    private void showHalfLine(ImageView halLine, Boolean isValid) {
        halLine.setVisibility(isValid ? View.VISIBLE: View.INVISIBLE);
        if( isValid ){
            halLine.setImageResource(R.drawable.half_start_line_dotted);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    requireActivity().runOnUiThread(() -> halLine.setImageResource(R.drawable.half_start_line_dashed));
                }
            }, 100);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    requireActivity().runOnUiThread(() -> halLine.setImageResource(R.drawable.half_start_line_solid));
                }
            }, 200);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        navViewModel.setCurrentScreen(KeyMapper.CurrentScreen.START_SCREEN);
    }

    @Override
    public void onPause() {
        super.onPause();
        navViewModel.setCurrentScreen(KeyMapper.CurrentScreen.OTHER);
    }
}