package com.santacruzinstruments.ottopi.control;

import android.content.Context;
import android.hardware.usb.UsbAccessory;

import androidx.annotation.UiThread;

import com.santacruzinstruments.ottopi.data.CalItem;
import com.santacruzinstruments.ottopi.data.ConnectionState;
import com.santacruzinstruments.ottopi.data.MeasuredDataType;
import com.santacruzinstruments.ottopi.data.StartType;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RouteCollection;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;
import com.santacruzinstruments.ottopi.ui.ViewInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class UiCtrlManager implements CtrlInterface {

    Context ctx;

    private ViewInterface viewInterface;
    private MainController mainController;

    @Inject
    UiCtrlManager(@ApplicationContext Context ctx) {
        this.ctx = ctx;
        // Don't do anything here
        // all initialisation should be done in the init() method
    }

    @UiThread
    @Override
    public void init(ViewInterface viewInterface) {
        this.viewInterface = viewInterface;

        this.viewInterface.onConnectionState(
                new ConnectionState(ConnectionState.NetworkStatus.NOT_CONNECTED, ""));

        mainController = new MainController(this.ctx, viewInterface);
        mainController.start();
    }

    @UiThread
    @Override
    public void setStartType(StartType startType) {
        mainController.offer(MainController.MessageId.setStartType,startType);
    }

    @UiThread
    @Override
    public void setStartTime(long startTime) {
        mainController.offer(MainController.MessageId.setStartTime,startTime);
    }

    @UiThread
    @Override
    public void setGpxFile(File gpxFile) {
        try {
            InputStream  is = new FileInputStream( gpxFile);
            String baseName = gpxFile.getName();
            RouteCollection routeCollection = new RouteCollection(baseName);
            routeCollection.loadFromGpx(is);
            this.viewInterface.onRouteCollection(routeCollection);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @UiThread
    @Override
    public void addGpxFile(File gpxFile) {
        // Update GPX collection and set newly added GPX file
        mainController.offer(MainController.MessageId.readGpxCollection, gpxFile);
    }

    @UiThread
    @Override
    public void deleteGpxFile(File gpxFile) {
        if ( gpxFile.delete() ) {
            mainController.offer(MainController.MessageId.readGpxCollection,gpxFile);
        }
    }

    @UiThread
    @Override
    public void addRaceRouteWpt(RoutePoint rpt) {
        mainController.offer(MainController.MessageId.addRaceRouteWpt, rpt);
    }

    @Override
    public void addStartLineEnd(RoutePoint rpt) {
        mainController.offer(MainController.MessageId.addStartLineEnd, rpt);
    }

    @UiThread
    @Override
    public void removeRaceRouteWpt(int idx) {
        mainController.offer(MainController.MessageId.removeRaceRouteWpt, idx);
    }

    // User selected new destination on the screen
    @UiThread
    @Override
    public void makeActiveWpt(int idx) {
        mainController.offer(MainController.MessageId.makeActiveWpt, idx);
    }

    @UiThread
    @Override
    public void addRouteToRace(Route route) {
        mainController.offer(MainController.MessageId.addRouteToRace, route);
    }

    @UiThread
    @Override
    public void onPinButtonPress() {
        mainController.offer(MainController.MessageId.onStartLineEnd, RoutePoint.LeaveTo.PORT);
    }

    @UiThread
    @Override
    public void onRcbButtonPress() {
        mainController.offer(MainController.MessageId.onStartLineEnd, RoutePoint.LeaveTo.STARBOARD);
    }

    @UiThread
    @Override
    public void onStartButtonPress() {
        mainController.offer(MainController.MessageId.onStartButtonPress);
    }

    @UiThread
    @Override
    public void onStopButtonPress() {
        mainController.offer(MainController.MessageId.onStopButtonPress);
    }

    @UiThread
    @Override
    public void refreshPolarTable(String polarName) {
        mainController.offer(MainController.MessageId.refreshPolarTable,polarName);
    }

    @UiThread
    @Override
    public void useWifi(boolean useWifi){
        mainController.offer( MainController.MessageId.setUseWifi, useWifi);
    }

    @UiThread
    @Override
    public void setInstrumentsSsid(String ssid) {
        mainController.offer( MainController.MessageId.setInstrumentsSsid, ssid);
        viewInterface.onSsid(ssid);
    }

    @UiThread
    @Override
    public void setInstrumentsHostname(String hostname) {
        mainController.offer(MainController.MessageId.setInstrumentsHostname,hostname);
    }

    @UiThread
    @Override
    public void setInstrumentsPort(int port) {
        mainController.offer(MainController.MessageId.setInstrumentsPort, port);
    }

    @UiThread
    @Override
    public void setUseInternalGps(boolean use) {
        mainController.offer(MainController.MessageId.setUseInternalGps, use);
    }

    @Override
    public void stopAll() {
        mainController.offer(MainController.MessageId.stopAll);
    }

    @Override
    public void toggleCalibration() {
        mainController.offer(MainController.MessageId.toggleCalibration);
    }

    @Override
    public void setCurrentLogCalValue(double value) {
        mainController.offer(MainController.MessageId.setCurrentLogCalValue, value);
    }

    @Override
    public void setCurrentAwaBiasValue(double value) {
        mainController.offer(MainController.MessageId.setCurrentAwaBiasValue, value);
    }

    @Override
    public void setNextMark() {
        mainController.offer(MainController.MessageId.setNextMark);
    }

    @Override
    public void setPrevMark() {
        mainController.offer(MainController.MessageId.setPrevMark);
    }

    @Override
    public void setupUsbAccessory(UsbAccessory accessory){
        mainController.offer(MainController.MessageId.setupUsbAccessory, accessory);
    }

    @Override
    public void sendCal(MeasuredDataType item, float calValue) {
        CalItem calItem = new CalItem(item, calValue);
        mainController.offer(MainController.MessageId.sendCal, calItem);
    }

}
