package com.santacruzinstruments.ottopi.navengine.route;

import static com.santacruzinstruments.ottopi.navengine.route.RouteCollection.GPX_TIME_FORMAT;

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;

import timber.log.Timber;

public class GpxBuilder {
    private final LinkedList<RoutePoint> wpts = new LinkedList<>();
    private final static HashMap<RoutePoint.Type,String> TYPES = new HashMap<RoutePoint.Type,String>(){{
       put(RoutePoint.Type.START,"start");
       put(RoutePoint.Type.ROUNDING,"rounding");
       put(RoutePoint.Type.FINISH,"finish");
    }};
    public GpxBuilder(){
        GPX_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void addPoint(RoutePoint pt, Writer writer) {
        wpts.add(pt);
        if( ! storeCurrentMarks(writer) ){
            wpts.remove(pt);
        }
    }

    public boolean storeCurrentMarks(Writer writer) {
        try {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.startTag(null, "gpx");
            xmlSerializer.attribute(null, "xmlns:gybetime", "http://www.gybetime.com");

            for(RoutePoint p : wpts){
                xmlSerializer.startTag(null, "wpt");
                xmlSerializer.attribute(null, "lat", String.format(Locale.US, "%.5f", p.loc.lat));
                xmlSerializer.attribute(null, "lon", String.format(Locale.US, "%.5f", p.loc.lon));
                xmlSerializer.startTag(null,"name");
                xmlSerializer.text(p.name);
                xmlSerializer.endTag(null, "name");
                if( p.time.isValid()){
                    xmlSerializer.startTag(null,"time");
                    xmlSerializer.text(GPX_TIME_FORMAT.format(p.time.getDate()));
                    xmlSerializer.endTag(null, "time");
                }

                xmlSerializer.startTag(null, "extensions");
                xmlSerializer.startTag(null, "gybetime:raceinfo");

                xmlSerializer.attribute(null, "leave_to", p.leaveTo == RoutePoint.LeaveTo.PORT ? "port": "starboard");
                xmlSerializer.attribute(null, "type", TYPES.get(p.type));


                xmlSerializer.endTag(null, "gybetime:raceinfo");
                xmlSerializer.endTag(null, "extensions");

                xmlSerializer.endTag(null, "wpt");
            }

            xmlSerializer.endTag(null, "gpx");
            xmlSerializer.endDocument();
            xmlSerializer.flush();
            return true;
        } catch (IOException e) {
            Timber.e("Write failed (%s)", e.getMessage());
            return false;
        }
    }
}
