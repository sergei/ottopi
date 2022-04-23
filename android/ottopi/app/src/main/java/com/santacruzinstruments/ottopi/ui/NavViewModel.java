package com.santacruzinstruments.ottopi.ui;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.santacruzinstruments.ottopi.data.CalibrationData;
import com.santacruzinstruments.ottopi.data.ConnectionState;
import com.santacruzinstruments.ottopi.control.CtrlInterface;
import com.santacruzinstruments.ottopi.data.DataReceptionStatus;
import com.santacruzinstruments.ottopi.data.db.HostNameEntry;
import com.santacruzinstruments.ottopi.data.db.BoatDataRepository;
import com.santacruzinstruments.ottopi.data.SailingState;
import com.santacruzinstruments.ottopi.data.StartLineInfo;
import com.santacruzinstruments.ottopi.data.StartType;
import com.santacruzinstruments.ottopi.data.db.HostPortEntry;
import com.santacruzinstruments.ottopi.navengine.NavComputerOutput;
import com.santacruzinstruments.ottopi.navengine.polars.PolarTable;
import com.santacruzinstruments.ottopi.navengine.route.GpxCollection;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RouteCollection;

import java.util.List;
import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

@HiltViewModel
public class NavViewModel extends ViewModel implements ViewInterface {

    // Use this class to observe SailingState and StartType simultaneously
    public static class RaceTypeStateMediatorLiveData extends MediatorLiveData<RaceTypeStateMediatorLiveData.RaceTypeState>{
        public static class RaceTypeState {
            public final SailingState state;
            public final StartType startType;

            public RaceTypeState(SailingState state, StartType startType) {
                this.state = state;
                this.startType = startType;
            }
        }
        private final MutableLiveData<SailingState>  sailingState;
        private final MutableLiveData<StartType> startType;
        RaceTypeStateMediatorLiveData(MutableLiveData<SailingState>  sailingState,  MutableLiveData<StartType> startType){
            this.sailingState = sailingState;
            this.startType = startType;
            addSource(this.sailingState, state ->
                    postValue(new RaceTypeState(state, RaceTypeStateMediatorLiveData.this.startType.getValue())));
            addSource(this.startType, type ->
                    postValue(new RaceTypeState(RaceTypeStateMediatorLiveData.this.sailingState.getValue(), type)));
        }
    }


    private final CtrlInterface ctrlInterface;
    private final MutableLiveData<Integer>  secondsToStart = new MutableLiveData<>(5);

    private final MutableLiveData<SailingState> sailingState = new MutableLiveData<>(SailingState.CRUISING);
    private final MutableLiveData<StartType> startType = new MutableLiveData<>(StartType.COUNTDOWN);
    // Combines two values above
    private final RaceTypeStateMediatorLiveData raceTypeAndState = new RaceTypeStateMediatorLiveData(sailingState, startType);

    private final MutableLiveData<Long> startTime = new MutableLiveData<>(0L);
    private final MutableLiveData<NavComputerOutput> navComputerOutput = new MutableLiveData<>();
    private final MutableLiveData<PolarTable> polarTable = new MutableLiveData<>();
    private final MutableLiveData<StartLineInfo> startLineInfo = new MutableLiveData<>();
    private final MutableLiveData<RouteCollection> routeCollection = new MutableLiveData<>();
    private final MutableLiveData<GpxCollection> gpxCollection = new MutableLiveData<>();
    private final MutableLiveData<Route> raceRoute = new MutableLiveData<>();
    private final MutableLiveData<ConnectionState> connectionState = new MutableLiveData<>();
    private final MutableLiveData<DataReceptionStatus> dataReceptionStatus = new MutableLiveData<>();
    private final MutableLiveData<List<String>> availableSsidList = new MutableLiveData<>();
    private final MutableLiveData<String> instrumentsSsid = new MutableLiveData<>();
    private final MutableLiveData<String> instrumentsHostname = new MutableLiveData<>();
    private final MutableLiveData<Integer> instrumentsPort = new MutableLiveData<>();
    private final MutableLiveData<Boolean> pinMarkValidity = new MutableLiveData<>();
    private final MutableLiveData<Boolean> rcbMarkValidity = new MutableLiveData<>();
    private final MutableLiveData<CalibrationData> calibrationData = new MutableLiveData<>();
    private final MutableLiveData<Double> currentLogCal = new MutableLiveData<>();
    private final MutableLiveData<Double>  currentMisaligned = new MutableLiveData<>();
    private final MutableLiveData<KeyMapper.CurrentScreen> currentScreen = new MutableLiveData<>();
    private final LiveData<List<HostNameEntry>> hostNamesLiveData;
    private final LiveData<List<HostPortEntry>> hostPortsLiveData;
    private final MutableLiveData<String> loggingTag = new MutableLiveData<>();
    private final MutableLiveData<Boolean> usbConnected = new MutableLiveData<>(false);

    @Inject
    NavViewModel(@ApplicationContext Context ctx, CtrlInterface ctrlInterface){
        this.ctrlInterface = ctrlInterface;
        BoatDataRepository boatDataRepository = new BoatDataRepository(ctx);
        hostNamesLiveData = boatDataRepository.getHostNamesLiveData();
        hostPortsLiveData = boatDataRepository.getHostPortsLiveData();

        ctrlInterface.init(this);
    }
    public CtrlInterface ctrl(){return this.ctrlInterface;}

    // Called when activity is destroyed
    @Override
    protected void onCleared() {
        ctrlInterface.stopAll();
    }

    @Override
    public void onTimeToStart(int secondsToStart) {
        this.secondsToStart.postValue(secondsToStart);
    }
    public LiveData<Integer> getSecondsToStart(){
        return secondsToStart;
    }

    @Override
    public void onStartType(StartType startType) {
        this.startType.postValue(startType);
    }
    public LiveData<StartType> getStartType(){
        return startType;
    }

    @Override
    public void onSailingStateChange(SailingState sailingState) {
        this.sailingState.postValue(sailingState);
    }
    public LiveData<SailingState> getSailingState(){
        return sailingState;
    }

    // Returns two values above combined
    public RaceTypeStateMediatorLiveData getRaceTypeAndState() {
        return raceTypeAndState;
    }

    @Override
    public void onNavComputerOutput(NavComputerOutput navComputerOutput) {
        this.navComputerOutput.postValue(navComputerOutput);
    }
    public LiveData<NavComputerOutput> getNavComputerOutput() { return this.navComputerOutput;}

    @Override
    public void onPolarTable(PolarTable polarTable) {
        this.polarTable.postValue(polarTable);
    }
    public LiveData<PolarTable> getPolarTable(){
        return this.polarTable;
    }

    @Override
    public void onStartLineInfo(StartLineInfo startLineInfo) {
        this.startLineInfo.postValue(startLineInfo);
    }
    public LiveData<StartLineInfo> getStartLineInfo(){ return this.startLineInfo; }

    @Override
    public void onPinMarkChange(boolean isValid) {
        this.pinMarkValidity.postValue(isValid);
    }
    public MutableLiveData<Boolean> getPinMarkValidity() {
        return pinMarkValidity;
    }

    @Override
    public void onRcbMarkChange(boolean isValid) {
        this.rcbMarkValidity.postValue(isValid);
    }

    public MutableLiveData<Boolean> getRcbMarkValidity() {
        return rcbMarkValidity;
    }

    @Override
    public void onStartTime(long startTime) {
        this.startTime.postValue(startTime);
    }
    public LiveData<Long> getStartTime(){
        return startTime;
    }

    @Override
    public void onConnectionState(ConnectionState connectionState) {
        this.connectionState.postValue(connectionState);
    }
    public LiveData<ConnectionState> getConnectionState(){
        return this.connectionState;
    }

    @Override
    public void onDataReceptionStatus(DataReceptionStatus dataReceptionStatus) {
        this.dataReceptionStatus.postValue(dataReceptionStatus);
    }
    public LiveData<DataReceptionStatus> getDataReceptionStatus(){
        return  this.dataReceptionStatus;
    }

    @Override
    public void onRouteCollection(RouteCollection routeCollection) {
        this.routeCollection.postValue(routeCollection);
    }
    public LiveData<RouteCollection> getRouteCollection(){
        return routeCollection;
    }

    @Override
    public void onGpxCollection(GpxCollection gpxCollection) {
        this.gpxCollection.postValue(gpxCollection);
    }
    public LiveData<GpxCollection> getGpxCollection(){
        return  gpxCollection;
    }

    @Override
    public void onRaceRouteChange(Route raceRoute) {
        this.raceRoute.postValue(raceRoute);
    }
    public LiveData<Route> getRaceRoute(){
        return raceRoute;
    }

    @Override
    public void onSsid(String ssid) {
        this.instrumentsSsid.postValue(ssid);
    }
    public MutableLiveData<String> getInstrumentsSsid() {
        return instrumentsSsid;
    }

    @Override
    public void onInstrHost(String hostname) {
        this.instrumentsHostname.postValue(hostname);
    }
    public MutableLiveData<String> getInstrumentsHostname() {
        return instrumentsHostname;
    }

    @Override
    public void onInstrPort(int port) {
        this.instrumentsPort.postValue(port);
    }
    public MutableLiveData<Integer> getInstrumentsPort() {
        return instrumentsPort;
    }

    @Override
    public void onSsidScan(List<String> ssids) {
        this.availableSsidList.postValue(ssids);
    }
    public MutableLiveData<List<String>> getAvailableSsidList() {
        return availableSsidList;
    }

    @Override
    public void setCalibrationData(CalibrationData calibrationData) {
        this.calibrationData.postValue(calibrationData);
    }
    public MutableLiveData<CalibrationData> getCalibrationData() {
        return calibrationData;
    }

    @Override
    public void setCurrentLogCal(double currentLogCal) {
        this.currentLogCal.postValue(currentLogCal);
    }
    public MutableLiveData<Double> getCurrentLogCal() {
        return currentLogCal;
    }

    @Override
    public void setCurrentMisaligned(double currentMisaligned) {
        this.currentMisaligned.postValue(currentMisaligned);
    }
    public  MutableLiveData<Double>  getCurrentMisaligned() {
        return currentMisaligned;
    }

    public MutableLiveData<KeyMapper.CurrentScreen> getCurrentScreen() {
        return currentScreen;
    }

    public void setCurrentScreen(KeyMapper.CurrentScreen currentScreen) {
        this.currentScreen.postValue(currentScreen);
    }

    public LiveData<List<HostNameEntry>> getRecentlyUsedHostNames() {
        return hostNamesLiveData;
    }

    public LiveData<List<HostPortEntry>> getRecentlyUsedHostPorts() {
        return hostPortsLiveData;
    }

    @Override
    public void setLoggingTag(String loggingTag){
        this.loggingTag.postValue(loggingTag);
    }
    public LiveData<String> getLoggingTag() {
        return loggingTag;
    }


    @Override
    public void onUsbConnect(boolean connected) {
        this.usbConnected.postValue(connected);
    }

    public LiveData<Boolean> getUsbConnected() {
        return usbConnected;
    }

}
