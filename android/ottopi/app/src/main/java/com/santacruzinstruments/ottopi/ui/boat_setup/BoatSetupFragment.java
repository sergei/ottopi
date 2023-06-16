package com.santacruzinstruments.ottopi.ui.boat_setup;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.data.CalibrationData;
import com.santacruzinstruments.ottopi.databinding.FragmentBoatSetupBinding;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.ui.NavViewModel;

import java.util.ArrayList;
import java.util.List;
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

        navViewModel.getCalibrationData().observe(getViewLifecycleOwner(), calibrationData -> {
            // Start / Stop
            if ( calibrationData.isActive ){
                binding.startCalibrationButton.setText(R.string.stop_calibration);
            }else{
                binding.startCalibrationButton.setText(R.string.start_calibration);
            }

            plotAwaChart(calibrationData);
            plotSpdChart(calibrationData);
            plotDeviationChart(calibrationData);

        });
    }

    private void plotAwaChart(CalibrationData calibrationData) {

        List<BarEntry> portEntries = new ArrayList<>();
        List<BarEntry> stbdEntries = new ArrayList<>();

        for (int i = 0; i < calibrationData.portAwaHist.length; i++) {
            // turn your data into Entry objects
            portEntries.add(new BarEntry(i, (calibrationData.portAwaHist[i] / (float)calibrationData.portAwaCount) * 100.f));
            stbdEntries.add(new BarEntry(i+0.25f, calibrationData.stbdAwaHist[i] / (float)calibrationData.stbdAwaCount * 100.f));
        }

        BarDataSet portDataSet = new BarDataSet(portEntries, "Port"); // add entries to dataset
        portDataSet.setColor(Color.RED);
        portDataSet.setDrawValues(false);

        BarDataSet stbdDataSet = new BarDataSet(stbdEntries, "Stbd"); // add entries to dataset
        stbdDataSet.setColor(Color.GREEN);
        stbdDataSet.setDrawValues(false);


        BarData awaData = new BarData();
        awaData.addDataSet(portDataSet);
        awaData.addDataSet(stbdDataSet);
        awaData.setBarWidth(0.25f);

        BarChart chart = binding.awaChart;

        chart.setData(awaData);
        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setDrawLabels(false);
        yAxis.setDrawGridLines(false);
        yAxis.setDrawZeroLine(true);
        Legend legend = chart.getLegend();
        legend.setEnabled(false);

        yAxis = chart.getAxisRight();
        yAxis.setDrawLabels(false);

        chart.getDescription().setEnabled(false);
        chart.invalidate(); // refresh

        TextView awaOffsetText = binding.awaBiasTextLabel;
        if ( calibrationData.isAwaValid ){
            awaOffsetText.setText(String.format(Locale.getDefault(), getString(R.string.awa_bias), calibrationData.awaBias));
        }
    }

    private void plotSpdChart(CalibrationData calibrationData) {

        List<BarEntry> spdEntries = new ArrayList<>();

        for (int i = 0; i < calibrationData.spdHist.length; i++) {
            // turn your data into Entry objects
            spdEntries.add(new BarEntry(i - calibrationData.spdHist.length/2.f, (calibrationData.spdHist[i] / (float)calibrationData.spdHistCount) * 100.f));
        }

        BarDataSet dataSet = new BarDataSet(spdEntries, "Speed"); // add entries to dataset
        dataSet.setColor(Color.BLUE);
        dataSet.setDrawValues(false);

        BarData spdData = new BarData();
        spdData.addDataSet(dataSet);
        spdData.setBarWidth(0.25f);

        BarChart chart = binding.spdChart;

        chart.setData(spdData);
        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setDrawLabels(false);
        yAxis.setDrawGridLines(false);
        yAxis.setDrawZeroLine(true);

        yAxis = chart.getAxisRight();
        yAxis.setDrawLabels(false);

        Legend legend = chart.getLegend();
        legend.setEnabled(false);

        chart.getDescription().setEnabled(false);
        chart.invalidate(); // refresh

        TextView spdBiasText = binding.spdBiasTextLabel;
        if ( calibrationData.isSpeedValid ){
            spdBiasText.setText(String.format(Locale.getDefault(),
                    getString(R.string.speed_bias_value),
                    (int)Math.round(calibrationData.sowRatio)));
        }
    }

    private void plotDeviationChart(CalibrationData calibrationData) {

        List<Entry> spdEntries = new ArrayList<>();

        for (int i = 0; i < calibrationData.magDevDeg.length; i++) {
            // turn your data into Entry objects
            spdEntries.add(new Entry(i, calibrationData.magDevDeg[i]));
        }

        ScatterDataSet dataSet = new ScatterDataSet(spdEntries, "Deviation"); // add entries to dataset
        dataSet.setColor(Color.BLUE);
        dataSet.setDrawValues(false);

        ScatterData spdData = new ScatterData();
        spdData.addDataSet(dataSet);

        ScatterChart chart = binding.deviationChart;

        chart.setData(spdData);
        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setTextColor(Color.WHITE);
        yAxis.setDrawGridLines(false);
        yAxis.setDrawZeroLine(true);

        yAxis = chart.getAxisRight();
        yAxis.setDrawLabels(false);

        Legend legend = chart.getLegend();
        legend.setEnabled(false);

        chart.getDescription().setEnabled(false);
        chart.invalidate(); // refresh

    }

}