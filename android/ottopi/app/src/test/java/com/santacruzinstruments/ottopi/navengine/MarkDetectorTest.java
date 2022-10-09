package com.santacruzinstruments.ottopi.navengine;

import androidx.annotation.NonNull;

import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.nmea.NmeaEpochAssembler;
import com.santacruzinstruments.ottopi.navengine.nmea.NmeaParser;
import com.santacruzinstruments.ottopi.navengine.nmea.NmeaReader;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import junit.framework.TestCase;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class MarkDetectorTest extends TestCase {

    static class KmzFile implements ContentHandler {
        String wptName = "";
        private final StringBuilder textBuilder = new StringBuilder();
        double wptLat = 0;
        double wptLon = 0;
        boolean pointDetected = false;
        int markId = 0;
        LinkedList<RoutePoint> marks = new LinkedList<>();

        KmzFile(String name) throws IOException {

            InputStream is = Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream(name);
            assertNotNull ( is );

            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry zipEntry = zis.getNextEntry();
            if (zipEntry != null) {
                if (!zipEntry.isDirectory()){
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser sp;
                    try {
                        sp = spf.newSAXParser();
                        XMLReader xr = sp.getXMLReader();
                        xr.setContentHandler(this);
                        xr.parse(new InputSource( zis ));

                    } catch (ParserConfigurationException | SAXException  e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes){
            textBuilder.setLength(0);
        }

        @Override
        public void characters(char[] ch, int start, int length){
            textBuilder.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName){
            String name = (qName.length() > 0) ? qName : localName;
            if (name.equalsIgnoreCase("name")){
                wptName = textBuilder.toString();
            }else if (name.equalsIgnoreCase("Point")){
                pointDetected = true;
            }else if (name.equalsIgnoreCase("coordinates")){
                String[] lngLatAlt = textBuilder.toString().split(",");
                wptLon = Double.parseDouble(lngLatAlt[0]);
                wptLat = Double.parseDouble(lngLatAlt[1]);
            }else if (name.equalsIgnoreCase("Placemark") && pointDetected) {
                RoutePoint.Type type = RoutePoint.Type.ROUNDING;
                RoutePoint.LeaveTo leaveTo = RoutePoint.LeaveTo.STARBOARD;

                if ( wptName.startsWith("PIN")) {
                    type = RoutePoint.Type.START;
                    leaveTo = RoutePoint.LeaveTo.PORT;
                }else if ( wptName.startsWith("RCB")){
                    type = RoutePoint.Type.START;
                }else if ( wptName.startsWith("LGP")  ){
                    leaveTo = RoutePoint.LeaveTo.PORT;
                }
                boolean isActive = markId == 2;
                RoutePoint rtpt = new RoutePoint.Builder()
                        .id(++markId).loc(new GeoLoc(wptLat, wptLon))
                .name(wptName).type(type).leaveTo(leaveTo).isActive(isActive).build();
                marks.add(rtpt);
                pointDetected = false;
            }
        }

        @Override
        public void ignorableWhitespace(char[] chars, int i, int i1){
        }

        @Override
        public void processingInstruction(String s, String s1){
        }

        @Override
        public void skippedEntity(String s){
        }
        @Override
        public void setDocumentLocator(Locator locator) {
        }

        @Override
        public void startDocument(){
        }

        @Override
        public void endDocument(){
        }

        @Override
        public void startPrefixMapping(String s, String s1) {
        }

        @Override
        public void endPrefixMapping(String s) {
        }

    }


    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    int markDetectionCount = 0;

    public void testKmzReader() throws IOException {
        KmzFile kmz = new KmzFile("mark-detection/set1/set.kmz");
        assertEquals(5, kmz.marks.size());
    }

    public void testMarksCreation1() throws IOException {
        runMarkDetector("set1");
    }

    public void testMarksCreation2() throws IOException {
        runMarkDetector("set2");
    }

    public void testMarksCreation3() throws IOException {
        runMarkDetector("set3");
    }

    public void testMarksCreation4() throws IOException {
        runMarkDetector("set4");
    }

    public void testMarksCreation5() throws IOException {
        runMarkDetector("set5");
    }

    public void testMarksCreation6() throws IOException {
        runMarkDetector("set6");
    }

    private void runMarkDetector(String setName) throws IOException {

        KmzFile kmzFile = new KmzFile("mark-detection/" + setName + "/set.kmz");
        String nmeaFile = "mark-detection/" + setName + "/race.nmea.gz";

        Route route = readRoute(kmzFile);

        markDetectionCount = 0;
        MarkDetector markDetector = new MarkDetector((loc) -> {
            int markIdx = markDetectionCount + 2;
            markDetectionCount ++;
            System.out.printf("%s Detected %s at %s\n",
                    setName, markDetectionCount,  loc);
            GeoLoc expectedLoc = kmzFile.marks.get(markIdx).loc;
            double dist = expectedLoc.distTo(loc).toMeters();
            assertTrue(
                    String.format(Locale.getDefault(),"Failed detection #%d: distance to mark %s is %f",
                            markDetectionCount, route.getRpt(markIdx).name, dist),
                    dist < 80 );

        });

        markDetector.setStartLine(route);
        markDetector.start();

        feedNmea(markDetector, nmeaFile);

        assertEquals(kmzFile.marks.size() - 2, markDetectionCount);  // Excludeing PIN and RCB
    }

    @NonNull
    static Route readRoute(KmzFile kmz) {
        Route route = new Route();
        for( RoutePoint pt : kmz.marks){
            if( pt.type == RoutePoint.Type.START){
                route.addRpt(pt);  // Ad as it is with known location
            }else{
                // Make location unknown
                RoutePoint upt = new RoutePoint.Builder().copy(pt).loc(GeoLoc.INVALID).build();
                route.addRpt(upt);
            }
        }
        route.makeActiveWpt(2);
        return route;
    }

    private void feedNmea(MarkDetector markDetector, String nmeaFile) throws IOException {
        NavComputer nc = new NavComputer();
        nc.addListener(markDetector::onNavData);
        NmeaEpochAssembler na = new NmeaEpochAssembler();
        na.addInstrumentInputListener(nc);

        NmeaParser nmeaParser = new NmeaParser();
        nmeaParser.addListener(na);

        NmeaReader nmeaReader = new NmeaReader();
        nmeaReader.addListener(nmeaParser);

        GZIPInputStream gzIs = new GZIPInputStream(Objects.requireNonNull(getClass()
                .getClassLoader()).getResourceAsStream(nmeaFile));

        byte [] buffer = new byte[2048];
        int nRead;

        while ( (nRead = gzIs.read(buffer, 0, buffer.length)) != -1 ){
            nmeaReader.read(buffer, nRead);
        }
    }

}
