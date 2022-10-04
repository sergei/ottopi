package com.santacruzinstruments.ottopi.ui.race_setup;

import static com.santacruzinstruments.ottopi.navengine.route.RoutePoint.Type.ROUNDING;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.data.SailingState;
import com.santacruzinstruments.ottopi.data.StartType;
import com.santacruzinstruments.ottopi.databinding.FragmentRaceSetupBinding;
import com.santacruzinstruments.ottopi.init.PathsConfig;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;
import com.santacruzinstruments.ottopi.ui.NavViewModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import timber.log.Timber;

@AndroidEntryPoint
public class RaceSetupFragment extends Fragment {

    private NavViewModel navViewModel;
    private FragmentRaceSetupBinding binding;
    private long selectedDateUtcMs=0;

    public RaceSetupFragment() {
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
        binding = FragmentRaceSetupBinding.inflate(inflater, container, false);

        configureRaceStartStop();

        configureRaceType();

        configureRoute();

        String startupGpx = RaceSetupFragmentArgs.fromBundle(getArguments()).getGpxName();
        if (startupGpx != null){
            navViewModel.ctrl().addGpxFile(new File(PathsConfig.getGpxDir(), startupGpx));
        }

        return binding.getRoot();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void configureRoute() {

        // GPX collection selection
        navViewModel.getGpxCollection().observe(getViewLifecycleOwner(), gpxCollection -> {
            List<File> gpxFiles = gpxCollection.getFiles();
            if( gpxFiles.isEmpty() ){
                binding.gpxListMenu.setVisibility(View.INVISIBLE);
                binding.gpxDeleteButton.setVisibility(View.INVISIBLE);
            }else{
                binding.gpxListMenu.setVisibility(View.VISIBLE);
                binding.gpxDeleteButton.setVisibility(View.VISIBLE);
            }

            String [] names = new String[gpxFiles.size()];
            for(int i=0; i < names.length; i++){
                names[i] = gpxFiles.get(i).getName();
            }
            ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(this.getContext(),
                    R.layout.start_type_list_item, names);
            binding.gpxNameText.setAdapter(itemsAdapter);

            if( gpxCollection.getDefaultIdx() < gpxFiles.size() ) {
                final File gpxFile = gpxFiles.get(gpxCollection.getDefaultIdx());
                binding.gpxNameText.setText(gpxFile.getName(), false);
                Timber.d("Gpx file %s set as default", gpxFile);
                navViewModel.ctrl().setGpxFile(gpxFile);
                binding.gpxDeleteButton.setOnClickListener(view -> deleteGpxFile(gpxFile));
            }

            binding.gpxNameText.setOnItemClickListener((adapterView, view, pos, id) -> {
                final File gpxFile = gpxFiles.get(pos);
                binding.gpxDeleteButton.setOnClickListener(v -> deleteGpxFile(gpxFile));
                Timber.d("Gpx file %s selected", gpxFile);
                navViewModel.ctrl().setGpxFile(gpxFile);
            });

        });


        navViewModel.getRouteCollection().observe(getViewLifecycleOwner(), routeCollection -> {
            ExpandableListView routesListView = binding.routesListView;

            if ( routeCollection.getRoutes().isEmpty() ){
                routesListView.setVisibility(View.INVISIBLE);
            }else{
                routesListView.setVisibility(View.VISIBLE);
            }

            RoutesExpandableListAdapter listAdapter = new RoutesExpandableListAdapter(this.getContext(), routeCollection);
            routesListView.setAdapter(listAdapter);

            routesListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {

                if ( groupPosition == 0 ){  // Inflatable marks folder
                    switch (childPosition){
                        case 0:  // Windward mark
                            navViewModel.ctrl().addRaceRouteWpt(new RoutePoint.Builder().name("WM").type(ROUNDING).leaveTo(RoutePoint.LeaveTo.PORT).build());
                            break;
                        case 1: // Leeward mark
                            navViewModel.ctrl().addRaceRouteWpt(new RoutePoint.Builder().name("LM").type(ROUNDING).leaveTo(RoutePoint.LeaveTo.PORT).build());
                            break;
                    }
                }else{
                    Route route = routeCollection.getRoutes().get(groupPosition-1);
                    if( childPosition == 0 ){
                        Timber.d("Route %s selected", route.getName());
                        navViewModel.ctrl().addRouteToRace(route);
                    }else{
                        RoutePoint wpt = route.getRpt(childPosition - 1);
                        Timber.d("%s from %s selected", wpt.name, route.getName());
                        navViewModel.ctrl().addRaceRouteWpt(wpt);
                    }
                }
                return false;
            });

            routesListView.setOnItemLongClickListener((adapterView, view, position, id) -> {
                int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                int childPosition = ExpandableListView.getPackedPositionChild(id);
                if ( childPosition > 0 ) {
                    Route route = routeCollection.getRoutes().get(groupPosition-1);
                    RoutePoint wpt = route.getRpt(childPosition-1);
                    RoutePoint rpt = new RoutePoint.Builder()
                            .copy(wpt)
                            .build();
                    Timber.d("%s from %s selected as starboard", wpt.name, route.getName());
                    navViewModel.ctrl().addStartLineEnd(rpt);
                }
                return false;
            });
        });

        binding.raceRouteRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        final RaceRouteAdapter raceRouteAdapter = new RaceRouteAdapter(new Route(), navViewModel);
        binding.raceRouteRecyclerView.setAdapter(raceRouteAdapter);

        navViewModel.getRaceRoute().observe(getViewLifecycleOwner(),
            route -> {
                raceRouteAdapter.updateRoute(route);
                raceRouteAdapter.notifyDataSetChanged();
        });
    }

    private void deleteGpxFile(final File gpxFile) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.do_you_want_to_delete_gpx, gpxFile.getName()))
                .setNegativeButton(R.string.no, (dialog, which) -> {})
                .setPositiveButton(R.string.yes, (dialog, which) -> navViewModel.ctrl().deleteGpxFile(gpxFile))
                .show();
    }

    private void configureRaceStartStop() {
        navViewModel.getSailingState().observe(getViewLifecycleOwner(), sailingState -> {
            if ( sailingState == SailingState.PREPARATORY || sailingState == SailingState.RACING){
                binding.finishRaceButton.setVisibility(View.VISIBLE);
            }else{
                binding.finishRaceButton.setVisibility(View.GONE);
            }
        });

        binding.finishRaceButton.setOnClickListener(view -> navViewModel.ctrl().onStopButtonPress());
    }

    private void configureRaceType() {
        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(this.getContext(),
                R.layout.start_type_list_item, getResources().getStringArray(R.array.race_types_array));
        binding.raceTypeText.setAdapter(itemsAdapter);

        navViewModel.getStartType().observe(getViewLifecycleOwner(), startType -> {
            String startTypeName = "";
            switch (startType){
                case COUNTDOWN:
                    startTypeName = getResources().getString(R.string.countdown);
                    binding.startAtButton.setVisibility(View.GONE);
                    break;
                case START_AT:
                    startTypeName = getResources().getString(R.string.start_at);
                    binding.startAtButton.setVisibility(View.VISIBLE);
                    break;
                case NO_START:
                    startTypeName = getResources().getString(R.string.cruising);
                    binding.startAtButton.setVisibility(View.GONE);
                    break;
            }
            binding.raceTypeText.setText(startTypeName, false);
        });

        binding.raceTypeText.setOnItemClickListener((adapterView, view, pos, id) -> {
            String raceType = getResources().getStringArray(R.array.race_types_array)[pos];
            if (raceType.equals(getResources().getString(R.string.countdown))){
                navViewModel.onStartType(StartType.COUNTDOWN);
                navViewModel.ctrl().setStartType(StartType.COUNTDOWN);
            }else if (raceType.equals(getResources().getString(R.string.start_at))){
                navViewModel.onStartType(StartType.START_AT);
                navViewModel.ctrl().setStartType(StartType.START_AT);
            }else if (raceType.equals(getResources().getString(R.string.cruising))){
                navViewModel.onStartType(StartType.NO_START);
                navViewModel.ctrl().setStartType(StartType.NO_START);
            }
        });

        final MyTimePicker timePicker = new MyTimePicker();
        timePicker.setTitle("Select time");

        timePicker.setOnTimeSetOption("Set time", (hour, minute, second) -> {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(this.selectedDateUtcMs);
            cal.setTimeZone(TimeZone.getDefault());
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, second);
            final long timeInMillis = cal.getTimeInMillis();
            navViewModel.onStartTime(timeInMillis);
            navViewModel.ctrl().setStartTime(timeInMillis);
            return  Unit.INSTANCE;
        });

        final MaterialDatePicker.Builder<Long> dateSelectionBuilder = MaterialDatePicker.Builder.datePicker();
        dateSelectionBuilder.setTitleText("Pick date");

        navViewModel.getStartTime().observe(getViewLifecycleOwner(),
                startTime -> setStartTime(timePicker, dateSelectionBuilder, startTime));

        binding.startAtButton.setOnClickListener(view -> {
            MaterialDatePicker<Long> datePicker = dateSelectionBuilder.build();
            datePicker.addOnPositiveButtonClickListener(utcMs -> {
                selectedDateUtcMs = utcMs;
                timePicker.show(getParentFragmentManager(), "tag");
            });
            datePicker.show(getParentFragmentManager(), datePicker.toString());
        });
    }

    private void setStartTime(MyTimePicker timePicker, MaterialDatePicker.Builder<Long> dateSelectionBuilder, Long startTime) {

        Calendar cal = Calendar.getInstance();
        if ( startTime != 0 ) {
            cal.setTimeInMillis(startTime);
        }
        dateSelectionBuilder.setSelection(cal.getTimeInMillis());

        cal.setTimeZone(TimeZone.getDefault());

        timePicker.setInitialHour(cal.get(Calendar.HOUR_OF_DAY));
        timePicker.setInitialMinute(cal.get(Calendar.MINUTE));
        timePicker.setInitialSeconds(cal.get(Calendar.SECOND));

        if ( startTime == 0 ) {
            binding.startAtButton.setText(R.string.click_here_to_set_time);
        } else {
            final SimpleDateFormat DTF = new SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault());
            String s = DTF.format(cal.getTime());
            binding.startAtButton.setText(s);
        }
    }
}
