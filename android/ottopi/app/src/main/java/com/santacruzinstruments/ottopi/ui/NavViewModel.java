package com.santacruzinstruments.ottopi.ui;

import static java.lang.Math.abs;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.santacruzinstruments.ottopi.data.CalibrationData;
import com.santacruzinstruments.ottopi.data.ConnectionState;
import com.santacruzinstruments.ottopi.control.CtrlInterface;
import com.santacruzinstruments.ottopi.data.DataReceptionStatus;
import com.santacruzinstruments.ottopi.data.MeasuredDataType;
import com.santacruzinstruments.ottopi.data.db.HostNameEntry;
import com.santacruzinstruments.ottopi.data.db.BoatDataRepository;
import com.santacruzinstruments.ottopi.data.SailingState;
import com.santacruzinstruments.ottopi.data.StartLineInfo;
import com.santacruzinstruments.ottopi.data.StartType;
import com.santacruzinstruments.ottopi.data.db.HostPortEntry;
import com.santacruzinstruments.ottopi.navengine.NavComputerOutput;
import com.santacruzinstruments.ottopi.navengine.calibration.Calibrator;
import com.santacruzinstruments.ottopi.navengine.polars.PolarTable;
import com.santacruzinstruments.ottopi.navengine.route.GpxCollection;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RouteCollection;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

@HiltViewModel
public class NavViewModel extends ViewModel implements ViewInterface {

    private static final String INVALID_VALUE = "...";

    public static class Calibratable{
        public final String name;
        public final boolean isDegree;
        MutableLiveData<Integer> cal = new MutableLiveData<>(0);
        MutableLiveData<String> value = new MutableLiveData<>(INVALID_VALUE);
        boolean gotCal = false;
        double currCal = 0;
        boolean gotSuggestedCal = false;
        double suggestedCal = 0;

        Calibratable(String name, boolean isDegree) {
            this.name = name;
            this.isDegree = isDegree;
        }
    }

    private final HashMap<MeasuredDataType, Calibratable> calibratableDataMap = new HashMap<>();

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

    private final  MutableLiveData<Boolean> isN2kConnected = new MutableLiveData<>();

    @Inject
    NavViewModel(@ApplicationContext Context ctx, CtrlInterface ctrlInterface){
        this.ctrlInterface = ctrlInterface;
        BoatDataRepository boatDataRepository = new BoatDataRepository(ctx);
        hostNamesLiveData = boatDataRepository.getHostNamesLiveData();
        hostPortsLiveData = boatDataRepository.getHostPortsLiveData();

        calibratableDataMap.put(MeasuredDataType.AWA, new Calibratable("AWA", true));
        calibratableDataMap.put(MeasuredDataType.AWS, new Calibratable("AWS", false));
        calibratableDataMap.put(MeasuredDataType.SPD, new Calibratable("SPD", false));
        calibratableDataMap.put(MeasuredDataType.HDG, new Calibratable("HDG", true));
        calibratableDataMap.put(MeasuredDataType.PITCH, new Calibratable("Pitch", true));
        calibratableDataMap.put(MeasuredDataType.ROLL, new Calibratable("Roll", true));

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
        setSuggestedCalValues(calibrationData);
    }

    public MutableLiveData<CalibrationData> getCalibrationData() {
        return calibrationData;
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
    public List<MeasuredDataType> getValibratableItemsList() {
        final LinkedList<MeasuredDataType> itemsList = new LinkedList<>(calibratableDataMap.keySet());
        Collections.sort(itemsList);
        return itemsList;
    }

    public Map<MeasuredDataType, Calibratable> getCalibratableDataMap() {
        return calibratableDataMap;
    }

    public void setCal(MeasuredDataType item, int calValue) {
        Calibratable c = calibratableDataMap.get(item);
        assert c != null;
        c.cal.postValue(calValue);
    }
    public LiveData<Integer> getCal(MeasuredDataType item){
        Calibratable c = calibratableDataMap.get(item);
        assert c != null;
        return c.cal;
    }

    public LiveData<String> getValue(MeasuredDataType item){
        Calibratable c = calibratableDataMap.get(item);
        assert c != null;
        return c.value;
    }

    @Override
    public void onRcvdInstrValue(MeasuredDataType item, double value) {
        Calibratable c = calibratableDataMap.get(item);
        assert c != null;

        if ( c.gotCal ) {
            double nonCalVal = 0;

            switch (item) {
                case AWA:
                    nonCalVal = Calibrator.getUncalAwa(value, c.currCal);
                    if ( nonCalVal > 180 )
                        nonCalVal -= 360;
                    if ( value > 180 )
                        value -= 360;
                break;
                case SPD:
                    nonCalVal = Calibrator.getUncalSow(value , c.currCal);
                break;
                default:
                    if ( c.isDegree ){
                        nonCalVal = value - c.currCal;
                    }else{
                        double ratio = 1 + c.currCal / 100.;
                        nonCalVal = value / ratio;
                    }
            }

            String sign = c.currCal > 0 ? "+" : "-";
            String suggestedSign = c.suggestedCal > 0 ? "+" : "-";
            String units = c.isDegree ? "°" : "";
            String calUnits = c.isDegree ? "°" : "%";
            String currentValAndCal = String.format(Locale.getDefault(), "%.1f%s = %.1f%s %s %.1f%s",
                    value, units, nonCalVal, units, sign, abs(c.currCal), calUnits);
            if ( c.gotSuggestedCal) {
                double trueVal = 0;
                switch (item) {
                    case AWA:
                        trueVal = Calibrator.getCalAwa(value, c.currCal, c.suggestedCal);
                        if ( trueVal > 180 )
                            trueVal -= 360;
                        break;
                    case SPD:
                        trueVal = Calibrator.getCalSow(value , c.currCal, c.suggestedCal);
                        break;
                }
                String suggested = String.format(Locale.getDefault(), " (suggested: %.1f%s = %.1f%s %s %.1f%s)",
                        trueVal, units, nonCalVal, units, suggestedSign, abs(c.suggestedCal), calUnits);
                currentValAndCal += suggested;
            }
            c.value.postValue(currentValAndCal);
        } else {
            c.value.postValue(INVALID_VALUE);
        }
    }

    private void setSuggestedCalValues(CalibrationData calibrationData) {
        if( calibrationData.isSpeedValid){
            Calibratable c = calibratableDataMap.get(MeasuredDataType.SPD);
            assert c != null;
            c.gotSuggestedCal = true;
            c.suggestedCal = calibrationData.sowCalPerc;
        }
        if ( calibrationData.isAwaValid ){
            Calibratable c = calibratableDataMap.get(MeasuredDataType.AWA);
            assert c != null;
            c.gotSuggestedCal = true;
            c.suggestedCal = calibrationData.awaCalDeg;
        }
    }


    @Override
    public void onRcvdInstrCalibr(MeasuredDataType item, double calValue) {
        Calibratable c = calibratableDataMap.get(item);
        assert c != null;
        c.gotCal = true;
        c.currCal = calValue;
        c.cal.postValue((int) calValue);
    }

    public void submitCal(MeasuredDataType item) {
        Calibratable c = calibratableDataMap.get(item);

        assert c != null;
        if( c.cal.getValue() != null) {
            this.ctrlInterface.sendCal(item, c.cal.getValue());
            c.gotCal = false;
        }
    }

    @Override
    public void onN2KConnect(boolean connected) {
        isN2kConnected.postValue(connected);
    }

    public LiveData<Boolean> getIsN2kConnected() {
        return isN2kConnected;
    }

}
