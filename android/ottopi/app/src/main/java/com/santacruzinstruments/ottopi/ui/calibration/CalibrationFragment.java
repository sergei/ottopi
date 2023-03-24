package com.santacruzinstruments.ottopi.ui.calibration;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.data.CalibrationData;
import com.santacruzinstruments.ottopi.data.MeasuredDataType;
import com.santacruzinstruments.ottopi.databinding.FragmentCalibrationBinding;
import com.santacruzinstruments.ottopi.ui.NavViewModel;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CalibrationFragment  extends Fragment {

    static class CalItemViewHolder extends RecyclerView.ViewHolder{
        TextView itemLabel;
        TextView itemValue;
        Button buttonCalibrate;
        Button buttonCancel;
        TextView calValue;
        SeekBar seekBar;
        public CalItemViewHolder(@NonNull View itemView) {
            super(itemView);

            itemLabel = itemView.findViewById(R.id.itemLabel);
            itemValue = itemView.findViewById(R.id.itemValue);
            buttonCalibrate = itemView.findViewById(R.id.buttonCalibrate);
            buttonCancel = itemView.findViewById(R.id.buttonCancel);
            calValue = itemView.findViewById(R.id.calValue);
            seekBar = itemView.findViewById(R.id.seekBar);
        }
    }

    class CalItemViewAdapter extends RecyclerView.Adapter<CalItemViewHolder>{

        private final NavViewModel viewModel;
        private final List<MeasuredDataType> calibratableItems;
        public CalItemViewAdapter(NavViewModel viewModel, List<MeasuredDataType> calibratableItems) {
            this.viewModel = viewModel;
            this.calibratableItems = calibratableItems;
        }

        @NonNull
        @Override
        public CalItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            // Inflate the layout
            View calItemView = inflater.inflate(R.layout.cal_ietm, parent, false);
            return new CalItemViewHolder(calItemView);
        }

        @Override
        public void onBindViewHolder(@NonNull CalItemViewHolder holder, int position) {
            MeasuredDataType it = this.calibratableItems.get(position);
            final NavViewModel.Calibratable calibratable = this.viewModel.getCalibratableDataMap().get(it);
            assert calibratable != null;
            holder.itemLabel.setText(calibratable.name);
            holder.buttonCalibrate.setTag("calibrate"+calibratable.name); // For espresso testing
            holder.buttonCancel.setTag("cancel"+calibratable.name); // For espresso testing

            holder.buttonCalibrate.setOnClickListener(view -> {
                if( holder.seekBar.getVisibility() == View.VISIBLE){
                    holder.seekBar.setVisibility(View.GONE);
                    holder.buttonCancel.setVisibility(View.GONE);
                    holder.calValue.setVisibility(View.GONE);
                    ((Button)view).setText(R.string.calibrate);
                    viewModel.submitCal(it);
                }else{
                    holder.seekBar.setVisibility(View.VISIBLE);
                    holder.buttonCancel.setVisibility(View.VISIBLE);
                    holder.calValue.setVisibility(View.VISIBLE);
                    ((Button)view).setText(R.string.submit);
                }
            });

            holder.buttonCancel.setOnClickListener(view -> {
                holder.seekBar.setVisibility(View.GONE);
                holder.calValue.setVisibility(View.GONE);
                holder.buttonCancel.setVisibility(View.GONE);
                holder.buttonCalibrate.setText(R.string.calibrate);
            });

            if ( calibratable.isDegree ){
                holder.seekBar.setMin(-45);
                holder.seekBar.setMax(45);
            }else{
                holder.seekBar.setMin(-50);
                holder.seekBar.setMax(50);
            }

            holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if ( fromUser ){
                        viewModel.setCal(it, progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            viewModel.getCal(it).observe(getViewLifecycleOwner(),
                    val -> {
                        holder.calValue.setText(getString(R.string.cal_angle, val));
                        holder.seekBar.setProgress(val);
                    });

            viewModel.getValue(it).observe(getViewLifecycleOwner(),
                    val -> holder.itemValue.setText(val));
        }

        @Override
        public int getItemCount() {
            return this.calibratableItems.size();
        }
    }

    private NavViewModel navViewModel;

    private FragmentCalibrationBinding binding;
    private List<MeasuredDataType> mCalibratableItems;
    CalItemViewAdapter mCalItemViewAdapter;
    public CalibrationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navViewModel = new ViewModelProvider(requireActivity()).get(NavViewModel.class);
        mCalibratableItems = navViewModel.getValibratableItemsList();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentCalibrationBinding.inflate(inflater, container, false);

        binding.startCalibrationButton.setOnClickListener(view -> navViewModel.ctrl().toggleCalibration());

        navViewModel.getIsN2kConnected().observe(getViewLifecycleOwner(), isConnected -> {
            if ( isConnected ){
                binding.notConnectedIndicator.setVisibility(View.GONE);
                binding.recyclerView.setVisibility(View.VISIBLE);
            }else{
                binding.notConnectedIndicator.setVisibility(View.VISIBLE);
                binding.recyclerView.setVisibility(View.GONE);
            }
        });

        RecyclerView recyclerView = binding.recyclerView;
        mCalItemViewAdapter = new CalItemViewAdapter(navViewModel, mCalibratableItems);
        recyclerView.setAdapter(mCalItemViewAdapter);
        recyclerView.setLayoutManager( new LinearLayoutManager(getContext()));

        navViewModel.getCalibrationData().observe(getViewLifecycleOwner(), calibrationData -> {
            // Start / Stop
            if ( calibrationData.isActive ){
                binding.startCalibrationButton.setText(R.string.stop_calibration);
            }else{
                binding.startCalibrationButton.setText(R.string.start_calibration);
            }
        });

        return binding.getRoot();
    }
}
