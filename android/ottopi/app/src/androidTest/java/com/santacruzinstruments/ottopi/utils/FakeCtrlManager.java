package com.santacruzinstruments.ottopi.utils;

import android.hardware.usb.UsbAccessory;

import com.santacruzinstruments.ottopi.control.CtrlInterface;
import com.santacruzinstruments.ottopi.data.CalibrationData;
import com.santacruzinstruments.ottopi.data.MeasuredDataType;
import com.santacruzinstruments.ottopi.data.SailingState;
import com.santacruzinstruments.ottopi.data.StartLineInfo;
import com.santacruzinstruments.ottopi.data.StartType;
import com.santacruzinstruments.ottopi.ui.ViewInterface;
import com.santacruzinstruments.ottopi.navengine.NavComputerOutput;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.polars.PolarTable;
import com.santacruzinstruments.ottopi.navengine.route.GpxCollection;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RouteCollection;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;


public class FakeCtrlManager implements CtrlInterface {

    StartLineInfo startLineInfo = new StartLineInfo();
    Route raceRoute = new Route();
    ViewInterface viewInterface;

    public FakeCtrlManager() {
    }

    @Override
    public void init(ViewInterface viewInterface) {
        this.viewInterface = viewInterface;
    }

    @Override
    public void onStartButtonPress() {

    }

    @Override
    public void onStopButtonPress() {

    }

    @Override
    public void setStartType(StartType startType) {

    }

    @Override
    public void setStartTime(long startTime) {

    }

    public void onGpxCollectionUpdate(GpxCollection gpxCollection) {
        this.viewInterface.onGpxCollection(gpxCollection);
    }

    @Override
    public void setGpxFile(File gpxFile) {
        String baseName = gpxFile.getName();
        InputStream is = Objects.requireNonNull(getClass().getClassLoader())
                .getResourceAsStream(baseName);

        RouteCollection routeCollection = new RouteCollection(baseName);
        routeCollection.loadFromGpx(is);
        this.viewInterface.onRouteCollection(routeCollection);
    }

    @Override
    public void addGpxFile(File gpxFile) {

    }

    @Override
    public void deleteGpxFile(File gpxFile) {

    }

    @Override
    public void addRaceRouteWpt(RoutePoint rpt) {
        raceRoute.addRpt(rpt);
        this.viewInterface.onRaceRouteChange(raceRoute);
    }

    @Override
    public void addStartLineEnd(RoutePoint rpt) {
    }

    @Override
    public void addRouteToRace(Route route) {
        raceRoute = route;
        this.viewInterface.onRaceRouteChange(raceRoute);
    }

    @Override
    public void refreshPolarTable(String polarName) {

    }

    @Override
    public void useWifi(boolean useWifi) {

    }

    @Override
    public void setInstrumentsSsid(String ssid) {

    }

    @Override
    public void setInstrumentsHostname(String hostname) {

    }

    @Override
    public void setInstrumentsPort(int port) {

    }

    @Override
    public void setUseInternalGps(boolean use) {

    }

    @Override
    public void stopAll() {

    }

    @Override
    public void toggleCalibration() {

    }

    @Override
    public void setNextMark() {

    }

    @Override
    public void setPrevMark() {

    }

    @Override
    public void setupUsbAccessory(UsbAccessory accessory) {
    }

    @Override
    public void sendCal(MeasuredDataType item, float calValue) {
    }

    @Override
    public void removeRaceRouteWpt(int idx) {
        Route newRoute = new Route();
        for( int i=0; i < raceRoute.getRptsNum(); i++){
            if ( i != idx){
                newRoute.addRpt(raceRoute.getRpt(i));
            }
        }
        this.viewInterface.onRaceRouteChange(newRoute);
    }

    @Override
    public void makeActiveWpt(int idx) {
        raceRoute.makeActiveWpt(idx);
        this.viewInterface.onRaceRouteChange(raceRoute);
    }

    public void setSailingState(SailingState sailingState){
        this.viewInterface.onSailingStateChange(sailingState);
    }

    public void setNavComputerOutput(NavComputerOutput out){
        this.viewInterface.onNavComputerOutput(out);
    }

    public void setPolarTable(PolarTable pt) {
        this.viewInterface.onPolarTable(pt);
    }

    public void setPinFavoredBy(Angle angle){
        startLineInfo.pinFavoredBy = angle;
        updateStartLine();
    }

    public void setDistToLine(Distance dist, boolean isOcs){
        startLineInfo.distToLine = dist;
        startLineInfo.isOcs = isOcs;
        updateStartLine();
    }

    @Override
    public void onPinButtonPress() {
        startLineInfo.pin = new GeoLoc(37, -122);
        updateStartLine();
    }

    @Override
    public void onRcbButtonPress() {
        startLineInfo.rcb = new GeoLoc(37, -122);
        updateStartLine();
    }

    private void updateStartLine() {
        this.viewInterface.onStartLineInfo(startLineInfo);
    }

    public void setN2KConnect(boolean connected) {
        this.viewInterface.onN2KConnect(connected);
    }
    public void setRcvdInstrValue(MeasuredDataType item, double value){
        this.viewInterface.onRcvdInstrValue(item, value);
    }
    public void setRcvdInstrCalibr(MeasuredDataType item, double cal){
        this.viewInterface.onRcvdInstrCalibr(item, cal);
    }
    public void setCalibrationData(CalibrationData calibrationData) {
        this.viewInterface.setCalibrationData(calibrationData);
    }
}
