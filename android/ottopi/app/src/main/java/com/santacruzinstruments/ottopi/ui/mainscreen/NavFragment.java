package com.santacruzinstruments.ottopi.ui.mainscreen;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.data.SailingState;
import com.santacruzinstruments.ottopi.data.StartType;
import com.santacruzinstruments.ottopi.databinding.FragmentNavBinding;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;
import com.santacruzinstruments.ottopi.ui.KeyMapper;
import com.santacruzinstruments.ottopi.ui.NavViewModel;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
@AndroidEntryPoint
public class NavFragment extends Fragment {

    public NavFragment() {
        // Required empty public constructor
    }

    private FragmentNavBinding binding;
    private NavViewModel navViewModel;
    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = this::hideSplashScreen;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navViewModel = new ViewModelProvider(requireActivity()).get(NavViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentNavBinding.inflate(inflater, container, false);

        navViewModel.getNavComputerOutput().observe(getViewLifecycleOwner(),
                out -> {
                    // Polar diagram
                    binding.polarView.onNavComputerOutput(out);

                    // Current diagram
                    Angle tideAngle = Direction.angleBetween(out.ii.cog, out.dot);
                    binding.currentView.setCurrent(out.sot, tideAngle);

                    // Next mark
                    if ( out.atm.isValid()){
                        binding.textViewMarkName.setVisibility(View.VISIBLE);
                        binding.textViewMarkAngle.setVisibility(View.VISIBLE);
                        double angle = out.atm.toDegrees();
                        if ( angle >=0 ){
                            binding.textViewMarkLeft.setVisibility(View.GONE);
                            binding.textViewMarkRight.setVisibility(View.VISIBLE);
                        }else{
                            binding.textViewMarkLeft.setVisibility(View.VISIBLE);
                            binding.textViewMarkRight.setVisibility(View.GONE);
                        }
                        binding.textViewMarkName.setText(out.destName);
                        binding.textViewMarkAngle.setText(String.format(Locale.getDefault(),"%.0f°", Math.abs(angle)));

                        if ( out.dtm.isValid() ){
                            binding.textViewMarkDistance.setVisibility(View.VISIBLE);
                            double dist = out.dtm.toMeters();
                            String text;
                            if( dist < 100 ){
                                text = String.format(Locale.getDefault(), "%.0f m", dist);
                            }else{
                                dist = out.dtm.toNauticalMiles();
                                if ( dist < 1000){
                                    text = String.format(Locale.getDefault(), "%.1f", dist);
                                }else{
                                    text = getString(R.string.far);
                                }
                            }
                            binding.textViewMarkDistance.setText(text);
                        }else{
                            binding.textViewMarkDistance.setVisibility(View.GONE);
                        }

                    }else{
                        binding.textViewMarkName.setVisibility(View.GONE);
                        binding.textViewMarkLeft.setVisibility(View.GONE);
                        binding.textViewMarkAngle.setVisibility(View.GONE);
                        binding.textViewMarkRight.setVisibility(View.GONE);
                        binding.textViewMarkDistance.setVisibility(View.GONE);
                    }

                    // Next leg
                    if ( out.nextLegTwa.isValid()){
                        binding.textViewNextMarkName.setVisibility(View.VISIBLE);
                        binding.textViewNextMarkAngle.setVisibility(View.VISIBLE);

                        double angle = out.nextLegTwa.toDegrees();
                        binding.textViewNextMarkName.setText( out.nextDestName );
                        if ( angle > 0){
                            binding.textViewNextMarkStarboardTack.setVisibility(View.VISIBLE);
                            binding.textViewNextMarkPortTack.setVisibility(View.GONE);
                        }else{
                            binding.textViewNextMarkStarboardTack.setVisibility(View.GONE);
                            binding.textViewNextMarkPortTack.setVisibility(View.VISIBLE);
                        }

                        binding.textViewNextMarkAngle.setText(String.format(Locale.getDefault(),"%.0f°", Math.abs(angle)));
                    }else{
                        binding.textViewNextMarkName.setVisibility(View.GONE);
                        binding.textViewNextMarkStarboardTack.setVisibility(View.GONE);
                        binding.textViewNextMarkPortTack.setVisibility(View.GONE);
                        binding.textViewNextMarkAngle.setVisibility(View.GONE);
                    }

                });

        navViewModel.getPolarTable().observe(getViewLifecycleOwner(),
                polarTable -> binding.polarView.onPolarTable(polarTable));

        // Either race state or start type has changed
        navViewModel.getRaceTypeAndState().observe(getViewLifecycleOwner(), raceTypeState -> {
            // If we have start enabled
            if (raceTypeState.startType != StartType.NO_START) {
                // The race either finished or we in preparatory, show the start screen
                if (raceTypeState.state == SailingState.CRUISING || raceTypeState.state == SailingState.PREPARATORY) {
                    Timber.d("Go to start line screen");
                    NavDirections action = NavFragmentDirections.actionNavFragmentToStartFragment();
                    Navigation.findNavController(binding.getRoot()).navigate(action);
                }
                // If we are in RACING state we stay in this screen
            }
            // If start type is NO_START we stay here
        });

        navViewModel.getRaceRoute().observe(getViewLifecycleOwner(), route -> {
            RoutePoint currPt = route.getActivePoint();
            String currentPoint = currPt == null ? "----" : currPt.name;
            RoutePoint nextPt = route.getAfterActivePoint();
            String nextPoint = nextPt == null ? "----"  : nextPt.name;
            splashMarksNames(currentPoint, nextPoint);
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        navViewModel.setCurrentScreen(KeyMapper.CurrentScreen.NAV_SCREEN);
    }

    @Override
    public void onPause() {
        super.onPause();
        navViewModel.setCurrentScreen(KeyMapper.CurrentScreen.OTHER);
    }

    private void hideSplashScreen() {
        binding.waypointsSplashLayout.setVisibility(View.INVISIBLE);
    }

    private void splashMarksNames(String currentPoint, String nextPoint){
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, 2000);
        binding.textViewLargeMarkName.setText(currentPoint);
        binding.textViewLargeNextMarkName.setText(nextPoint);
        binding.waypointsSplashLayout.setVisibility(View.VISIBLE);
    }

}