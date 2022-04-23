package com.santacruzinstruments.ottopi.ui.boat_setup;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.databinding.FragmentBoatSetupBinding;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.ui.NavViewModel;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BoatSetupFragment extends Fragment {

    private NavViewModel navViewModel;
    private FragmentBoatSetupBinding binding;

    public BoatSetupFragment() {
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
        binding = FragmentBoatSetupBinding.inflate(inflater, container, false);

        // Polar plot
        String polarName = BoatSetupFragmentArgs.fromBundle(getArguments()).getPolarName();
        if ( polarName != null){
            navViewModel.ctrl().refreshPolarTable(polarName);
        }

        navViewModel.getPolarTable().observe(getViewLifecycleOwner(), polarTable -> {
                binding.polarView.onPolarTable(polarTable);
                binding.polarView.onTws( new Speed(10));
        });

        setupNetworkUi();

        setupCalibrationUi();

        return binding.getRoot();
    }

    private void setupNetworkUi() {
        // List of SSIDs
        navViewModel.getAvailableSsidList().observe(getViewLifecycleOwner(), ssids -> {
            String [] ssidArray = new String[ssids.size() + 1];
            ssidArray[0] = getString(R.string.dont_use_wifi);
            for( int i=0; i < ssids.size(); i++)
                ssidArray[i+1] = ssids.get(i);
            ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(this.getContext(),
                    R.layout.start_type_list_item, ssidArray);
            binding.ssidText.setAdapter(itemsAdapter);

           binding.ssidText.setOnItemClickListener((adapterView, view, pos, id) -> {
               if ( pos == 0 ){
                   navViewModel.ctrl().useWifi(false);
                   navViewModel.ctrl().setInstrumentsSsid("");
               }else {
                   navViewModel.ctrl().useWifi(true);
                   navViewModel.ctrl().setInstrumentsSsid(ssidArray[pos]);
               }
           });
        });

        // SSID
        navViewModel.getInstrumentsSsid().observe(getViewLifecycleOwner(), ssid -> {
                if ( "".endsWith(ssid) ){
                    binding.ssidText.setText(getString(R.string.dont_use_wifi), false);
                }else{
                    binding.ssidText.setText(ssid, false);
                }
            }
        );

        // Host name
        navViewModel.getInstrumentsHostname().observe(getViewLifecycleOwner(),
                hostname -> binding.hostNameTextView.setText(hostname));

        // Set autocompletion for host name
        navViewModel.getRecentlyUsedHostNames().observe(getViewLifecycleOwner(), hostNameEntries -> {
            String [] names = new String[hostNameEntries.size()];
            for(int i = 0; i < hostNameEntries.size();  i++ )
                names[i] = hostNameEntries.get(i).getName();

            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    names);

            binding.hostNameTextView.setAdapter(adapter);
        });

        binding.hostNameTextView.setOnFocusChangeListener((v, hasFocus) -> {
            if( !hasFocus ){
                navViewModel.ctrl().setInstrumentsHostname(((EditText)v).getText().toString());
            }
        });

        // Port number
        navViewModel.getInstrumentsPort().observe(getViewLifecycleOwner(),
                port -> binding.portTextView.setText( String.format(Locale.getDefault(), "%d", port)));

        // Set autocompletion for host name
        navViewModel.getRecentlyUsedHostPorts().observe(getViewLifecycleOwner(), portEntries -> {
            String [] ports = new String[portEntries.size()];
            for(int i = 0; i < portEntries.size();  i++ )
                ports[i] = String.format(Locale.getDefault(), "%d", portEntries.get(i).getPort());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    ports);

            binding.portTextView.setAdapter(adapter);
        });

        binding.portTextView.setOnFocusChangeListener((v, hasFocus) -> {
            if( !hasFocus ){
                try {
                    int port = Integer.parseInt(((EditText) v).getText().toString());
                    navViewModel.ctrl().setInstrumentsPort(port);
                }catch (NumberFormatException ignore){}
            }
        });
    }

    private void setupCalibrationUi() {

        binding.startCalibrationButton.setOnClickListener(view -> navViewModel.ctrl().toggleCalibration());

        // Current speed calibration adjustment
        binding.currentLogCalSlider.addOnChangeListener((slider, value, fromUser) -> {
            if ( fromUser ){
                navViewModel.ctrl().setCurrentLogCalValue(value);
            }
        });
        navViewModel.getCurrentLogCal().observe(getViewLifecycleOwner(), v -> {
            binding.currentLogCalLabel.setText(requireContext().getString(R.string.current_log_cal, v));
            binding.currentLogCalSlider.setValue(v.floatValue());
        });

        // Current wind calibration adjustment
        binding.currentAwaBiasSlider.addOnChangeListener((slider, value, fromUser) -> {
            if ( fromUser ){
                navViewModel.ctrl().setCurrentAwaBiasValue(value);
            }
        });
        navViewModel.getCurrentMisaligned().observe(getViewLifecycleOwner(), v -> {
            binding.currentAwaBiasLabel.setText(requireContext().getString(R.string.current_offset, (int)v.doubleValue()));
            binding.currentAwaBiasSlider.setValue(v.floatValue());
        });

        navViewModel.getCalibrationData().observe(getViewLifecycleOwner(), calibrationData -> {
            // Start / Stop
            if ( calibrationData.isActive ){
                binding.startCalibrationButton.setText(R.string.stop_calibration);
            }else{
                binding.startCalibrationButton.setText(R.string.start_calibration);
            }

            // Speed calibration
            if( calibrationData.isSpeedValid ) {
                final int sowPerc = (int) (calibrationData.sowRatio * 100) - 100;
                binding.speedBias.setText(requireContext().getString(R.string.speed_bias_value, sowPerc));
                binding.suggestedLogCal.setText(requireContext().getString(R.string.suggested_log_cal, calibrationData.logCalValue));
            } else {
                binding.speedBias.setText(R.string.speed_bias_invalid);
                binding.suggestedLogCal.setText(R.string.suggested_log_cal_not_valid);
            }

            // Wind angle calibration
            if( calibrationData.isAwaValid ) {
                final int awaBias = (int) (calibrationData.awaBias);
                binding.awaBias.setText(requireContext().getString(R.string.awa_port_awa_stbd, awaBias));
                final int misalignmentValue = (int) Math.round(calibrationData.misalignmentValue);
                binding.suggestedAwaBiasLabel.setText(requireContext().getString(R.string.suggested_offset, misalignmentValue));
            } else {
                binding.awaBias.setText(R.string.awa_port_awa_stbd_not_valid);
                binding.suggestedAwaBiasLabel.setText(R.string.suggested_offset_not_valid);
            }
        });

    }


}