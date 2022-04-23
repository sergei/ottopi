package com.santacruzinstruments.ottopi.ui;

import com.santacruzinstruments.ottopi.data.CalibrationData;
import com.santacruzinstruments.ottopi.data.ConnectionState;
import com.santacruzinstruments.ottopi.data.DataReceptionStatus;
import com.santacruzinstruments.ottopi.data.SailingState;
import com.santacruzinstruments.ottopi.data.StartLineInfo;
import com.santacruzinstruments.ottopi.data.StartType;
import com.santacruzinstruments.ottopi.navengine.NavComputerOutput;
import com.santacruzinstruments.ottopi.navengine.polars.PolarTable;
import com.santacruzinstruments.ottopi.navengine.route.GpxCollection;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RouteCollection;

import java.util.List;

/// This interface is used to change the displayed data
public interface ViewInterface {
    void onTimeToStart(int secondsToStart);
    void onSailingStateChange(SailingState sailingState);
    void onStartType(StartType startType);
    void onStartTime(long startTime);

    void onNavComputerOutput(NavComputerOutput navOut);
    void onPolarTable(PolarTable polarTable);
    void onStartLineInfo(StartLineInfo startLineInfo);

    void onRouteCollection(RouteCollection routeCollection);
    void onGpxCollection(GpxCollection gpxCollection);
    void onRaceRouteChange(Route raceRoute);

    void onConnectionState(ConnectionState connectionState);
    void onDataReceptionStatus(DataReceptionStatus dataReceptionStatus);
    void onSsidScan(List<String> ssids);
    void onSsid(String ssid);
    void onInstrHost(String host);
    void onInstrPort(int port);

    void onPinMarkChange(boolean isValid);
    void onRcbMarkChange(boolean isValid);

    void setCalibrationData(CalibrationData calibrationData);
    void setCurrentLogCal(double currentLogCal);
    void setCurrentMisaligned(double currentMisaligned);

    void setLoggingTag(String loggingTag);
    void onUsbConnect(boolean connected);
}
