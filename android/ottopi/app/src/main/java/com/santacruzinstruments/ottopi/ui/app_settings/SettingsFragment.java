package com.santacruzinstruments.ottopi.ui.app_settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.databinding.FragmentSettingsBinding;
import com.santacruzinstruments.ottopi.logging.OttopiLogger;

import java.io.File;

public class SettingsFragment extends Fragment {

    FragmentSettingsBinding binding;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        // Share files button
        binding.shareFilesButton.setOnClickListener(view -> {
            File zipFile = OttopiLogger.createUploadZip();

            if ( zipFile != null ){
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);

                Uri uri = FileProvider.getUriForFile(requireContext(),
                        getString(R.string.file_provider_authority), zipFile);

                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.setType("application/zip");
                startActivity(Intent.createChooser(shareIntent, null));
            }else{
                new MaterialAlertDialogBuilder(requireContext())
                        .setMessage(R.string.no_logs_to_share)
                        .setNeutralButton(R.string.oh_well, (dialog, which) -> {})
                        .show();
            }
        });

        // Delete files button
        binding.deleteSharedFilesButton.setOnClickListener(view
                -> new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.are_you_sure)
                .setPositiveButton(R.string.yes_delete_them, (dialog, which)
                        -> OttopiLogger.deleteUploadedFiles())
                .setNegativeButton(R.string.no_i_changed_my_mind, (dialog, which) -> {})
                .show());

        return binding.getRoot();
    }

}