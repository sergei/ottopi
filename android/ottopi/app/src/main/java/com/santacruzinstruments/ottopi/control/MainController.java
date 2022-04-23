package com.santacruzinstruments.ottopi.control;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbAccessory;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.room.Room;

import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.data.ConnectionState;
import com.santacruzinstruments.ottopi.data.DataReceptionStatus;
import com.santacruzinstruments.ottopi.data.RaceRouteDao;
import com.santacruzinstruments.ottopi.data.RaceRouteDatabase;
import com.santacruzinstruments.ottopi.data.SailingState;
import com.santacruzinstruments.ottopi.data.StartLineInfo;
import com.santacruzinstruments.ottopi.data.StartType;
import com.santacruzinstruments.ottopi.data.db.HostNameEntry;
import com.santacruzinstruments.ottopi.data.db.BoatDataRepository;
import com.santacruzinstruments.ottopi.data.db.HostPortEntry;
import com.santacruzinstruments.ottopi.logging.OttopiLogger;
import com.santacruzinstruments.ottopi.navengine.InstrumentInput;
import com.santacruzinstruments.ottopi.navengine.NavComputer;
import com.santacruzinstruments.ottopi.navengine.NavComputerOutput;
import com.santacruzinstruments.ottopi.navengine.StartLineComputer;
import com.santacruzinstruments.ottopi.navengine.calibration.Calibrator;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.nmea.NmeaEpochAssembler;
import com.santacruzinstruments.ottopi.navengine.nmea.NmeaFormatter;
import com.santacruzinstruments.ottopi.navengine.nmea.NmeaParser;
import com.santacruzinstruments.ottopi.navengine.nmea.NmeaReader;
import com.santacruzinstruments.ottopi.navengine.polars.PolarTable;
import com.santacruzinstruments.ottopi.navengine.route.GpxCollection;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RouteManager;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;
import com.santacruzinstruments.ottopi.ui.ViewInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class MainController {

    public enum MessageId{
        heartBeat
        ,onNavComputerOutput
        ,addRaceRouteWpt
        ,removeRaceRouteWpt
        ,makeActiveWpt
        , addRouteToRace
        ,onStartLineEnd
        ,onNetworkNmeaMessage
        ,onInternalNmeaMessage
        ,setStartTime
        ,setStartType
        ,onStartButtonPress
        ,onStopButtonPress
        ,setUseInternalGps
        ,setUseWifi
        ,setInstrumentsSsid
        ,setInstrumentsHostname
        ,setInstrumentsPort
        ,readGpxCollection
        ,refreshPolarTable
        ,stopAll
        ,toggleCalibration
        ,setCurrentLogCalValue
        ,setCurrentAwaBiasValue
        ,setNextMark
        ,setPrevMark
        , setupUsbAccessory
    }

    public static class Message {
        final public MessageId id;
        final public Object arg;
        public Message(MessageId id, Object arg) {
            this.id = id;
            this.arg = arg;
        }
        public Message(MessageId id) {
            this.id = id;
            this.arg = null;
        }

        @NonNull
        @Override
        public String toString() {
            if( arg != null){
                return String.format(Locale.US, "%s,%s", this.id.toString(), this.arg);
            }else{
                return String.format(Locale.US, "%s", this.id.toString());
            }
        }
    }

    private class NetworkNmeaCmdListener implements NmeaParser.NmeaMsgListener {

        @Override
        public void onVhw(NmeaParser.VHW vhw) {}

        @Override
        public void onVwr(NmeaParser.VWR vwr) {}

        @Override
        public void onMwv(NmeaParser.MWV mwv) {}

        @Override
        public void onHdg(NmeaParser.HDG hdg) {}

        @Override
        public void onRmc(NmeaParser.RMC rmc) {}

        @Override
        public void onPscirTST(NmeaParser.PscirTST obj) {}

        @Override
        public void onPmacrSEV(NmeaParser.PmacrSev obj) {}

        @Override
        public void PracrLIN(NmeaParser.PracrLIN lin) {
            if( lin.bPinValid ){
                RoutePoint rpt = new RoutePoint(new GeoLoc(lin.dPinLat, lin.dPinLon),
                        "PIN", RoutePoint.Type.START_PORT,
                        RoutePoint.LeaveTo.PORT,
                        RoutePoint.Location.KNOWN);
                updateStartFinishRpt(rpt);
            }
            if( lin.bCmteValid ){
                RoutePoint rpt = new RoutePoint(new GeoLoc(lin.dCmteLat, lin.dCmteLon),
                        "RCB", RoutePoint.Type.START_STBD,
                        RoutePoint.LeaveTo.STARBOARD,
                        RoutePoint.Location.KNOWN);
                updateStartFinishRpt(rpt);
            }
        }

        @Override
        public void PracrSTR(NmeaParser.PracrSTR start) {
            if ( start.bTimestampValid){
                setStartTime( start.dtTimestamp.getTime());
            }
        }

        @Override
        public void onUnknownMessage(String msg) {}
    }

    public static final String POLAR_FILE_NAME = "polars.pol";
    private static final String NMEA_FMT_STRING = "NET_NMEA:%s";
    private static final int NAV_DATA_TIMEOUT_MS = 5000;
    private static final int DEFAULT_PREP_SEC = 300;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
    private static final SimpleDateFormat LSF =  new SimpleDateFormat("HH:mm:ss", Locale.US);
    private static final LinkedList<String> IGNORE_POS_NMEA_LIST = new LinkedList<String>(){{add("RMC");}};
    private static final LinkedList<String> EMPTY_IGNORE_NMEA_LIST = new LinkedList<>();

    private final LinkedBlockingDeque<Message> messageQueue = new LinkedBlockingDeque<>();
    private final RouteManager routeManager = new RouteManager();
    private final ViewInterface viewInterface;
    private final NavComputer navComputer = new NavComputer();
    private final StartLineComputer startLineComputer = new StartLineComputer();
    private final Calibrator speedCalibrator = new Calibrator();
    private final NmeaParser networkNmeaParser = new NmeaParser();
    private final NmeaParser internalNmeaParser = new NmeaParser();
    private final NmeaReader externalNmeaReader = new NmeaReader();
    private final NetworkNmeaCmdListener networkNmeaCmdListener = new NetworkNmeaCmdListener();
    private final LocationListener internalGpsLocationListener = location -> {};
    private final Context ctx;
    private Timer heartBeatTimer;


    private StartType startType = StartType.COUNTDOWN;
    private SailingState sailingState = SailingState.CRUISING;
    private long startTime = 0;

    private GpxCollection gpxCollection;
    private RaceRouteDao raceRouteDao;
    private GeoLoc lastKnownLoc = new GeoLoc();
    private int epochCount = 0;
    private long lastNavEngOutRcvdAtMs = 0;
    private NetworkManager networkManager;
    private UsbReader usbReader;

    private final Handler handler = new Handler(Looper.myLooper());
    private final BoatDataRepository boatDataRepository;

    @UiThread
    public MainController(Context ctx, ViewInterface viewInterface) {
        this.ctx = ctx;
        this.boatDataRepository = new BoatDataRepository(ctx);
        this.viewInterface = viewInterface;
    }

    @UiThread
    public void start(){
        final Thread thread = new Thread(this::managerThread);
        thread.setName("Main controller thread");
        thread.start();
    }

    boolean bKeepRunning = true;
    private void managerThread(){

        routeManager.addRouteManagerListener((utc, rptIdx, arrivalType)
                -> messageQueue.offer(new Message(MessageId.makeActiveWpt, rptIdx)));

        navComputer.addListener(out ->
                messageQueue.offer(new Message(MessageId.onNavComputerOutput, out)));

        externalNmeaReader.addListener(msg
                ->  offer(MainController.MessageId.onNetworkNmeaMessage, msg));

        NmeaEpochAssembler nmeaEpochAssembler = new NmeaEpochAssembler();
        nmeaEpochAssembler.addInstrumentInputListener(navComputer);

        // Both network and internal NMEA parsers feed the same epoch assembler
        networkNmeaParser.addListener(nmeaEpochAssembler);
        internalNmeaParser.addListener(nmeaEpochAssembler);

        // Only network NMEA contains special commands
        networkNmeaParser.addListener(networkNmeaCmdListener);

        // Read and distribute start time
        final SharedPreferences prefs = ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        startTime = prefs.getLong(ctx.getString(R.string.pref_key_start_time), 0L);
        this.viewInterface.onStartTime(startTime);

        // Read and distribute start type
        int startTypeOrd = prefs.getInt(ctx.getString(R.string.pref_key_start_type), StartType.COUNTDOWN.ordinal());
        if (startTypeOrd < StartType.values().length) {
            startType = StartType.values()[startTypeOrd];
            this.viewInterface.onStartType(startType);
        }else{
            startType = StartType.COUNTDOWN;
        }

        // Infer the sail state
        long now = new Date().getTime();
        long secondsToStart = (int) (startTime - now) / 1000;
        if ( startTime == 0 ){
            sailingState = SailingState.CRUISING;
            this.viewInterface.onTimeToStart(DEFAULT_PREP_SEC);
        }else if ( secondsToStart < 0 ){
            sailingState = SailingState.RACING;
            this.viewInterface.onTimeToStart(DEFAULT_PREP_SEC);
        }else{
            sailingState = SailingState.PREPARATORY;
            this.viewInterface.onTimeToStart((int) secondsToStart);
        }
        this.viewInterface.onSailingStateChange(sailingState);

        // Read calibration data
        double currentLogCal = prefs.getFloat(ctx.getString(R.string.pref_key_current_log_cal), (float) speedCalibrator.getCurrentLogCal());
        speedCalibrator.setCurrentLogCal(currentLogCal);
        double currentMisaligned = prefs.getFloat(ctx.getString(R.string.pref_key_current_log_cal), (float) speedCalibrator.getCurrentMisaligned());
        speedCalibrator.setCurrentMisaligned(currentMisaligned);

        // Update view interface
        this.viewInterface.setCurrentLogCal(currentLogCal);
        this.viewInterface.setCurrentMisaligned(currentMisaligned);
        this.viewInterface.setCalibrationData(speedCalibrator.getCalibrationData());

        // Read polar table
        refreshPolarTable(POLAR_FILE_NAME);

        // Read list of GPX file
        readGpxCollection(null);

        // Read current race route
        RaceRouteDatabase db = Room.databaseBuilder(this.ctx,
                        RaceRouteDatabase.class, "race_route")
                .fallbackToDestructiveMigration()
                .build();

        raceRouteDao = db.raceRouteDao();

        readRaceRoute();

        postStartLineEndsState();

        // Finally start heartbeat
        startHeartbeat();

        // Now start network thread
        Thread networkThread = new Thread(this::networkThread);
        networkThread.setName("NMEA Network thread");
        networkThread.start();

        // Now start usb thread
        Thread usbThread = new Thread(this::usbThread);
        usbThread.setName("USB thread");
        usbThread.start();

        // NMEA looper
        Looper.prepare();

        // Now go to the main loop
        mainLoop();

        // We exited the main loop, meaning we were stopped
        // Stop all child threads

        networkManager.stop();
        networkThread.interrupt();

        try {
            networkThread.join(1000);
        } catch (InterruptedException ignore) {}

        usbReader.stop();
        usbThread.interrupt();
        try {
            usbThread.join(1000);
        } catch (InterruptedException ignore) {}

    }

    private void startHeartbeat() {
        heartBeatTimer = new Timer();
        heartBeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                messageQueue.offer(new Message(MessageId.heartBeat));
            }
        }, 0, 1000);
    }

    private void stopHeartBeat(){
        heartBeatTimer.cancel();
    }

    private void mainLoop() {
        while (bKeepRunning){
            try {
                Message msg = messageQueue.poll(1000, TimeUnit.MILLISECONDS);
                if ( msg != null ){
                    Timber.d("%s MSG,%s", DTF.format(LocalDateTime.now()), msg.toString());
                    switch (msg.id){
                        case heartBeat:
                            heartBeat();
                            break;
                        case addRaceRouteWpt:
                            assert msg.arg != null;
                            doAddRaceRouteWpt((RoutePoint)msg.arg);
                            break;
                        case removeRaceRouteWpt:
                            assert msg.arg != null;
                            doRemoveRaceRouteWpt((int)msg.arg);
                            break;
                        case makeActiveWpt:
                            assert msg.arg != null;
                            doMakeActiveWpt((int)msg.arg);
                            break;
                        case setPrevMark:
                            doSetPrevMark();
                            break;
                        case setNextMark:
                            doSetNextMark();
                            break;
                        case addRouteToRace:
                            assert msg.arg != null;
                            doAddRouteToRace((Route)msg.arg);
                            break;
                        case onStartLineEnd:
                            assert msg.arg != null;
                            doSetStartLineEnd((RoutePoint.Type) msg.arg);
                            break;
                        case onNavComputerOutput:
                            assert msg.arg != null;
                            doNavComputerOutput((NavComputerOutput) msg.arg);
                            break;
                        case onNetworkNmeaMessage:
                            assert msg.arg != null;
                            processNetworkNmeaMessage((String)msg.arg);
                            break;
                        case onInternalNmeaMessage:
                            assert msg.arg != null;
                            processInternalNmeaMessage((String)msg.arg);
                            break;
                        case setStartTime:
                            assert msg.arg != null;
                            setStartTime((long)msg.arg);
                            break;
                        case setStartType:
                            assert msg.arg != null;
                            setStartType((StartType)msg.arg);
                            break;
                        case onStartButtonPress:
                            onStartButtonPress();
                            break;
                        case onStopButtonPress:
                            onStopButtonPress();
                            break;
                        case setUseInternalGps:
                            assert msg.arg != null;
                            setUseInternalGps((boolean)msg.arg);
                            break;
                        case setUseWifi:
                            assert msg.arg != null;
                            setUseWifi((boolean)msg.arg);
                            break;
                        case setInstrumentsSsid:
                            setInstrumentsSsid((String)msg.arg);
                            break;
                        case setInstrumentsHostname:
                            setInstrumentsHostname((String)msg.arg);
                            break;
                        case setInstrumentsPort:
                            assert msg.arg != null;
                            setInstrumentsPort((int)msg.arg);
                            break;
                        case readGpxCollection:
                            assert msg.arg != null;
                            readGpxCollection((File)msg.arg);
                            break;
                        case refreshPolarTable:
                            assert msg.arg != null;
                            refreshPolarTable((String) msg.arg);
                            break;
                        case toggleCalibration:
                            speedCalibrator.toggle();
                            this.viewInterface.setCalibrationData(speedCalibrator.getCalibrationData());
                            break;
                        case setCurrentLogCalValue:
                            assert msg.arg != null;
                            speedCalibrator.setCurrentLogCal((double) msg.arg);
                            this.viewInterface.setCurrentLogCal((double) msg.arg);
                            this.viewInterface.setCalibrationData(speedCalibrator.getCalibrationData());
                            break;
                        case setCurrentAwaBiasValue:
                            assert msg.arg != null;
                            speedCalibrator.setCurrentMisaligned((double) msg.arg);
                            this.viewInterface.setCurrentMisaligned((double) msg.arg);
                            this.viewInterface.setCalibrationData(speedCalibrator.getCalibrationData());
                            break;
                        case stopAll:
                            bKeepRunning = false;
                            break;
                        case setupUsbAccessory:
                            usbReader.setAccessory((UsbAccessory)msg.arg);
                            break;
                    }
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void offer(MessageId id, Object arg) {
        messageQueue.offer(new MainController.Message(id, arg));
    }

    public void offer(MessageId id) {
        messageQueue.offer(new MainController.Message(id));
    }

    private void usbThread() {
        usbReader =  new UsbReader(this.ctx, new UsbReader.UsbConnectionListener() {
            @Override
            public void OnConnectionStatus(boolean connected) {
                Timber.d("USB Accessory %s", connected ? "Connected" : "Not connected");
                viewInterface.onUsbConnect(connected);
            }

            @Override
            public void onDataReceived(byte[] buff, int size) {
                externalNmeaReader.read(buff, size);
            }
        });
        usbReader.run();
    }

    private void networkThread() {
        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);

        SharedPreferences prefs = ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        boolean useWifi = prefs.getBoolean(ctx.getString(R.string.pref_key_use_wifi), false);
        String hostname = prefs.getString(ctx.getString(R.string.pref_key_instr_host), "localhost");
        String ssid = prefs.getString(ctx.getString(R.string.pref_key_instr_ssid), "");
        if( ssid.length() > 0 ) {
            viewInterface.onSsid(ssid);
        }
        int port = prefs.getInt(ctx.getString(R.string.pref_key_instr_port), 12346);

        this.viewInterface.onInstrHost(hostname);
        this.viewInterface.onInstrPort(port);

        networkManager = new NetworkManager(new NetworkManager.NetworkListener() {
            @Override
            public void onUseWifi(boolean usWifi) {
                if ( !useWifi ){
                    ConnectionState connectionState = new ConnectionState(ConnectionState.NetworkStatus.DISABLED, "");
                    viewInterface.onConnectionState(connectionState);
                }
            }

            @Override
            public void onConnectingToWifi(String ssid) {
                ConnectionState connectionState = new ConnectionState(ConnectionState.NetworkStatus.CONNECTING_TO_WIFI, ssid);
                viewInterface.onConnectionState(connectionState);
            }

            @Override
            public void OnConnectingToHost(String url) {
                ConnectionState connectionState = new ConnectionState(ConnectionState.NetworkStatus.CONNECTING_TO_HOST, url);
                viewInterface.onConnectionState(connectionState);
            }

            @Override
            public void OnConnected(String url) {
                ConnectionState connectionState = new ConnectionState(ConnectionState.NetworkStatus.CONNECTED, url);
                viewInterface.onConnectionState(connectionState);
            }

            @Override
            public void onDataReceived(byte[] buff, int size) {
                externalNmeaReader.read(buff, size);
            }

            @Override
            public void onSsidScan(List<String> ssids) {
                viewInterface.onSsidScan(ssids);
            }
        }, useWifi, hostname, port, ssid, wifiManager);

        networkManager.run();
    }


    private void readRaceRoute() {
        List<RoutePoint> pts = raceRouteDao.getAll();
        Route raceRoute = new Route(pts);

        routeManager.setRoute(raceRoute);
        navComputer.setDestinations(raceRoute.getActivePoint(), raceRoute.getAfterActivePoint());

        StartLineInfo startLineInfo = startLineComputer.setRoute(raceRoute);
        Timber.d(NMEA_FMT_STRING, NmeaFormatter.fmtLin(startLineInfo));

        this.viewInterface.onRaceRouteChange(raceRoute);
        this.viewInterface.onStartLineInfo(startLineInfo);
    }

    private void doAddRaceRouteWpt(RoutePoint rpt) {
        List<RoutePoint> pts = raceRouteDao.getAllActive();
        if ( pts.isEmpty()){
            rpt = rpt.changeActiveStatus(true);
        }
        raceRouteDao.insert(rpt);
        readRaceRoute();
    }

    private void doRemoveRaceRouteWpt(int idx) {
        List<RoutePoint> pts = raceRouteDao.getAll();
        final RoutePoint rpt = pts.get(idx);
        raceRouteDao.delete(rpt);
        if ( rpt.isActive ){
            // We just deleted the active point, if we have points left let's make the next one active
            if ( pts.size() > 1 ) {
                int newActiveIdx = (idx + 1) % (pts.size() - 1);
                RoutePoint newActive = pts.get(newActiveIdx).changeActiveStatus(true);
                raceRouteDao.update(newActive);
            }
        }
        readRaceRoute();
        postStartLineEndsState();
    }

    public void doMakeActiveWpt(int idx) {
        List<RoutePoint> pts = raceRouteDao.getAll();
        for( RoutePoint pt : pts){
            if( pt.isActive ){
                RoutePoint notActive = pt.changeActiveStatus(false);
                raceRouteDao.update(notActive);
            }
        }

        RoutePoint newActive = pts.get(idx).changeActiveStatus(true);
        raceRouteDao.update(newActive);
        readRaceRoute();
    }

    private void doSetNextMark() {
        List<RoutePoint> pts = raceRouteDao.getAll();

        int oldActiveIdx = getActiveIdx(pts);
        if ( oldActiveIdx != -1 ){
            int newActiveIdx = (oldActiveIdx + 1) % pts.size();
            updateActivePoint(pts.get(oldActiveIdx), false);
            updateActivePoint(pts.get(newActiveIdx), true);
        }else{
            Timber.d("No active mark found, ignore doSetNextMark");
        }
        readRaceRoute();
    }

    private void doSetPrevMark() {
        List<RoutePoint> pts = raceRouteDao.getAll();

        int oldActiveIdx = getActiveIdx(pts);
        if ( oldActiveIdx != -1 ){
            int newActiveIdx = oldActiveIdx - 1;
            if( newActiveIdx < 0 )
                newActiveIdx = pts.size() - 1;
            updateActivePoint(pts.get(oldActiveIdx), false);
            updateActivePoint(pts.get(newActiveIdx), true);
        }else{
            Timber.d("No active mark found, ignore doSetPrevMark");
        }
        readRaceRoute();
    }

    private int getActiveIdx(@NonNull List<RoutePoint> pts) {
        int idx;
        for(idx = 0; idx < pts.size(); idx++ ){
            if( pts.get(idx).isActive)
                break;
        }
        if ( idx == pts.size())
            return -1;
        else
            return idx;
    }

    private void updateActivePoint(@NonNull RoutePoint pt, boolean isActive) {
        Timber.d("Set %s as %s", pt.name, isActive ? "ACTIVE" : "NOT ACTIVE");
        RoutePoint updated = pt.changeActiveStatus(isActive);
        raceRouteDao.update(updated);
    }

    public void doAddRouteToRace(@NonNull Route newRoute) {
        // We want to keep RCB and PIN
        List<RoutePoint> oldRoute = raceRouteDao.getAll();

        // Get the existing start line
        List<RoutePoint> startLine = new LinkedList<>();
        for( RoutePoint rpt: oldRoute ){
            if ( rpt.type == RoutePoint.Type.START_PORT || rpt.type == RoutePoint.Type.START_STBD ){
                startLine.add(rpt);
            }
        }

        // Delete old route
        raceRouteDao.deleteAll();

        // Now save the start line
        for( RoutePoint rpt: startLine ){
            rpt.id = (int)raceRouteDao.insert(rpt);
        }

        // and then the res of the route
        for( RoutePoint rpt : newRoute) {
            rpt.id = (int)raceRouteDao.insert(rpt);
        }

        readRaceRoute();
    }

    private void updateStartFinishRpt(@NonNull RoutePoint startPt) {
        // Delete current point with the same type
        raceRouteDao.deleteByType(startPt.type);
        // Insert the new one
        raceRouteDao.insert(startPt);

        // Now update corresponding finish mark
        RoutePoint.Type finishType = startPt.type == RoutePoint.Type.START_PORT ? RoutePoint.Type.FINISH_STBD : RoutePoint.Type.FINISH_PORT;
        RoutePoint.LeaveTo finishLeaveTo = startPt.leaveTo == RoutePoint.LeaveTo.PORT ? RoutePoint.LeaveTo.STARBOARD : RoutePoint.LeaveTo.PORT;
        RoutePoint finishPt = new RoutePoint(startPt.id, startPt.loc, startPt.name, finishType, finishLeaveTo,
                startPt.location, startPt.isActive);

        // Delete current point with the same type
        raceRouteDao.deleteByType(finishPt.type);
        // Insert the new one
        raceRouteDao.insert(finishPt);

        readRaceRoute();
    }

    private void doSetStartLineEnd(RoutePoint.Type type){
        if ( lastKnownLoc.isValid() ) {
            String name = (type == RoutePoint.Type.START_PORT)
                    ? "Pin" : "Rcb";
            final RoutePoint.LeaveTo leaveTo = (type == RoutePoint.Type.START_PORT)
                    ? RoutePoint.LeaveTo.PORT : RoutePoint.LeaveTo.STARBOARD;
            RoutePoint rpt = new RoutePoint(lastKnownLoc, name, type, leaveTo, RoutePoint.Location.KNOWN);
            updateStartFinishRpt(rpt);

            if ( type == RoutePoint.Type.START_PORT )
                this.viewInterface.onPinMarkChange(true);
            if ( type == RoutePoint.Type.START_STBD )
                this.viewInterface.onRcbMarkChange(true);
        }
    }

    void postStartLineEndsState() {
        this.viewInterface.onPinMarkChange(startLineComputer.getStartLineInfo().pin.isValid());
        this.viewInterface.onRcbMarkChange(startLineComputer.getStartLineInfo().rcb.isValid());
    }

    private void refreshPolarTable(String polarName) {
        try {
            File polarFile = new File( ctx.getExternalFilesDir(null) , polarName);
            PolarTable pt = new PolarTable(new FileInputStream( polarFile ));
            this.viewInterface.onPolarTable(pt);
        } catch (IOException e) {
            Timber.e("Failed to read polar table %s", e.getMessage());
        }
    }

    private void heartBeat() {
        // Update logging status
        String logTag = OttopiLogger.getLogFileId() + " " + LSF.format(new Date());
        this.viewInterface.setLoggingTag(logTag);
        
        // Check nav data expiration
        long now = SystemClock.elapsedRealtime();
        if( now - lastNavEngOutRcvdAtMs > NAV_DATA_TIMEOUT_MS){
            // Supply empty data
            InstrumentInput ii = new InstrumentInput.Builder().build();
            NavComputerOutput nout = new NavComputerOutput.Builder(ii).build();
            doNavComputerOutput(nout);
            // Invalidate last known location
            lastKnownLoc = GeoLoc.INVALID;
        }

        // Update race timer
        processRaceTimer();
    }

    // Processing thread
    private void doNavComputerOutput(NavComputerOutput nout) {
        lastNavEngOutRcvdAtMs = SystemClock.elapsedRealtime();
        epochCount++;
        DataReceptionStatus dataReceptionStatus = new DataReceptionStatus(
                epochCount,
                nout.ii.loc.isValid(),
                nout.ii.aws.isValid(),
                nout.ii.sow.isValid(),
                nout.ii.mag.isValid()
        );

        if ( nout.ii.loc.isValid() ){
            lastKnownLoc = nout.ii.loc;
        }

        StartLineInfo  startLineInfo = startLineComputer.updateStartLineInfo(nout.ii.loc, nout.twd);

        routeManager.onNavComputerOutput(nout);

        this.viewInterface.onDataReceptionStatus(dataReceptionStatus);
        this.viewInterface.onNavComputerOutput(nout);
        this.viewInterface.onStartLineInfo(startLineInfo);


        speedCalibrator.onInstrumentInput(nout.ii);
        this.viewInterface.setCalibrationData(speedCalibrator.getCalibrationData());
    }


    private void processNetworkNmeaMessage(String msg) {
        networkNmeaParser.onValidMessage(msg);
    }

    private void processInternalNmeaMessage(String msg) {
        internalNmeaParser.onValidMessage(msg);
    }

    private boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.ctx.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    private void startInternalGps() {
        Timber.d("Requesting internal GPS");
        if ( checkLocationPermission() ){
            LocationManager locationManager =
                    (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.addNmeaListener(ctx.getMainExecutor(), (nmea, timestamp)
                        -> offer(MainController.MessageId.onInternalNmeaMessage, nmea));
            }else{
                locationManager.addNmeaListener((nmea, timestamp) ->
                        offer(MessageId.onInternalNmeaMessage, nmea), handler);
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, internalGpsLocationListener);
        }else{
            Timber.e("No GPS permissions");
        }
    }

    private void stopInternalGps() {
        Timber.d("Starting internal GPS");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            LocationManager locationManager =
                    (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(internalGpsLocationListener);
        }
    }

    private void setStartTime(long startTime) {
        SharedPreferences prefs = ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(ctx.getString(R.string.pref_key_start_time), startTime);
        editor.apply();
        this.startTime = startTime;
        NmeaFormatter.fmtSTR(startTime);
    }

    private void setStartType(StartType startType) {
        SharedPreferences prefs = ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(ctx.getString(R.string.pref_key_start_type), startType.ordinal());
        editor.apply();
        this.startType = startType;
    }

    private void processRaceTimer() {
        long now = new Date().getTime();
        if ( startTime > 0 ) {
            long secondsToStart = (int) (startTime - now) / 1000;
            switch (sailingState){
                case PREPARATORY:
                    this.viewInterface.onTimeToStart((int)secondsToStart);
                    if (secondsToStart <= 0 ){
                        sailingState = SailingState.RACING;
                        this.viewInterface.onSailingStateChange(sailingState);
                    }
                    break;
                case RACING:
                    break;
            }

            Timber.d("%s RACE_TIMER STS: %d sec, SS: %s, ST: %s,"
                    ,DTF.format(LocalDateTime.now())
                    ,secondsToStart
                    ,sailingState.toString()
                    ,startType.toString()
            );
        }
    }

    private void onStartButtonPress() {
        switch (sailingState) {
            case CRUISING:  // Start
                sailingState = SailingState.PREPARATORY;
                long startTime = new Date().getTime() + DEFAULT_PREP_SEC * 1000;
                setStartTime(startTime);
                // Restart the heartbeat
                stopHeartBeat();
                startHeartbeat();
                break;
            case PREPARATORY:  // Sync
                long now = new Date().getTime();
                long secondsToStart = (int) (this.startTime - now) / 1000;
                // Round down to integer number of minutes
                secondsToStart = (secondsToStart / 60) * 60;
                long newStartTime = new Date().getTime() + secondsToStart * 1000;
                setStartTime(newStartTime);
                this.viewInterface.onTimeToStart((int)secondsToStart);
                // Restart the heartbeat
                stopHeartBeat();
                startHeartbeat();
                break;
        }
    }

    private void onStopButtonPress() {
        setStartTime(0);

        this.viewInterface.onTimeToStart(DEFAULT_PREP_SEC);

        sailingState = SailingState.CRUISING;
        this.viewInterface.onSailingStateChange(sailingState);
    }

    private void setUseInternalGps(boolean use) {
        if ( use ){
            startInternalGps();
            // Ignore position received from the network
            networkNmeaParser.setMsgToIgnore(IGNORE_POS_NMEA_LIST);
        }else{
            networkNmeaParser.setMsgToIgnore(EMPTY_IGNORE_NMEA_LIST);
            stopInternalGps();
        }
    }

    private void setUseWifi(boolean useWifi) {
        SharedPreferences prefs = ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(ctx.getString(R.string.pref_key_use_wifi), useWifi);
        editor.apply();
        networkManager.useWifi(useWifi);
    }

    private void setInstrumentsSsid(String ssid) {
        SharedPreferences prefs = ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ctx.getString(R.string.pref_key_instr_ssid), ssid);
        editor.apply();
        networkManager.setSsid(ssid);
    }

    private void setInstrumentsHostname(String hostname) {
        SharedPreferences prefs = ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ctx.getString(R.string.pref_key_instr_host), hostname);
        editor.apply();
        boatDataRepository.insert(new HostNameEntry(hostname, new Date().getTime()));
        this.viewInterface.onInstrHost(hostname);
        networkManager.setHostname(hostname);
    }

    private void setInstrumentsPort(int port) {
        SharedPreferences prefs = ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(ctx.getString(R.string.pref_key_instr_port), port);
        editor.apply();
        boatDataRepository.insert(new HostPortEntry(port, new Date().getTime()));
        this.viewInterface.onInstrPort(port);
        networkManager.setPort(port);
    }

    private void readGpxCollection(File gpxFile) {
        File [] files = this.ctx.getExternalFilesDir(null).listFiles((file, name) -> name.toLowerCase().endsWith(".gpx"));
        if ( files != null) {
            gpxCollection = new GpxCollection(Arrays.asList(files));

            if ( gpxFile != null ) { // Set newly added file as current
                for( int i = 0; i < gpxCollection.getFiles().size(); i++) {
                    File f = gpxCollection.getFiles().get(i);
                    if ( f.getName().equalsIgnoreCase(gpxFile.getName()))
                        gpxCollection.setDefaultIdx( i );
                }
            }
        }
        this.viewInterface.onGpxCollection(gpxCollection);
    }

}
