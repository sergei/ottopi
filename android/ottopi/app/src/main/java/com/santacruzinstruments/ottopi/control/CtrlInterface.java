package com.santacruzinstruments.ottopi.control;

import android.hardware.usb.UsbAccessory;

import com.santacruzinstruments.ottopi.data.StartType;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;
import com.santacruzinstruments.ottopi.ui.ViewInterface;

import java.io.File;

/// This interface exposes the controls UI can use
public interface CtrlInterface {
    void init(ViewInterface viewInterface);
    void onStartButtonPress();
    void onStopButtonPress();
    void onPinButtonPress();
    void onRcbButtonPress();
    void setStartType(StartType startType);
    void setStartTime(long startTime);
    void setGpxFile(File gpxFile);
    void addGpxFile(File gpxFile);
    void deleteGpxFile(File gpxFile);
    void addRaceRouteWpt(RoutePoint rpt);
    void addStartLineEnd(RoutePoint rpt);
    void removeRaceRouteWpt(int idx);
    void makeActiveWpt(int idx);
    void addRouteToRace(Route route);
    void refreshPolarTable(String polarName);

    void useWifi(boolean useWifi);
    void setInstrumentsSsid(String ssid);
    void setInstrumentsHostname(String hostname);
    void setInstrumentsPort(int port);

    void setUseInternalGps(boolean use);
    void stopAll();
    void toggleCalibration();
    void setCurrentLogCalValue(double value);
    void setCurrentAwaBiasValue(double value);
    void setNextMark();
    void setPrevMark();
    void setupUsbAccessory(UsbAccessory accessory);

}
