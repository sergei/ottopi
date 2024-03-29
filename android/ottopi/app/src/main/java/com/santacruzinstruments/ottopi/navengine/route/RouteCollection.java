package com.santacruzinstruments.ottopi.navengine.route;

import android.annotation.SuppressLint;

import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import timber.log.Timber;


public class RouteCollection implements ContentHandler {

	enum State {UNKNOWN, WAITING_FOR_ROUTE_NAME, WAITING_FOR_POINT_NAME}

	@SuppressLint("SimpleDateFormat")
	static final SimpleDateFormat GPX_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset

	final private String name;
	final private LinkedList<Route> routes;

	private State state; 
	private Route currentRoute;
	private final Route loosePts = new Route();
	private RoutePoint.Builder pointBuilder;
	private StringBuilder textBuilder;
	
	public RouteCollection(String name){
		this.name = name;
		this.routes = new LinkedList<>();
		GPX_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	public String getName() {
		return name;
	}

	public LinkedList<Route> getRoutes() {
		return routes;
	}

	public void loadFromGpx(InputStream is) {
		
		textBuilder = new StringBuilder();
		state = State.UNKNOWN;
		
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp;
		try {
			
			sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			xr.setContentHandler(this);
			xr.parse(new InputSource( is ));

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startDocument() {
		routes.clear();
	}

	@Override
	public void endDocument() {
		if ( !loosePts.isEmpty()){
			loosePts.setName("Misc Points");
			routes.add(loosePts);
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) {
		String name = (qName.length() > 0) ? qName : localName; // Beats me if I understand why sometimes I have qName and sometimes localName on different platforms
		switch (name) {
			case "rte":
				currentRoute = new Route();
				state = State.WAITING_FOR_ROUTE_NAME;
				break;
			case "rtept":
			case "wpt": {
				double lat = Double.parseDouble(atts.getValue("lat"));
				double lon = Double.parseDouble(atts.getValue("lon"));
				pointBuilder = new RoutePoint.Builder().loc(new GeoLoc(lat, lon));
				state = State.WAITING_FOR_POINT_NAME;
				break;
			}
			case "gybetime:raceinfo":
				String leaveTo = atts.getValue("leave_to");
				if("port".equals(leaveTo)){
					pointBuilder.leaveTo(RoutePoint.LeaveTo.PORT);
				} else if("starboard".equals(leaveTo)){
					pointBuilder.leaveTo(RoutePoint.LeaveTo.STARBOARD);
				}
				String type = atts.getValue("type");
				if("start".equals(type)){
					pointBuilder.type(RoutePoint.Type.START);
				} else if("finish".equals(type)){
					pointBuilder.type(RoutePoint.Type.FINISH);
				}
				break;
		}
		
		textBuilder.setLength(0);
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		String name = (qName.length() > 0) ? qName : localName; // Beats me if I understand why sometimes I have qName and sometimes localName on different platforms

		switch (name) {
			case "rte":
				routes.add(currentRoute);
				break;
			case "rtept": {
				RoutePoint pt = pointBuilder.build();
				currentRoute.addRpt(pt);
				break;
			}
			case "wpt": {
				RoutePoint pt = pointBuilder.build();
				loosePts.addRpt(pt);
				break;
			}
			case "time": {
				String s = textBuilder.toString();
				try {
					pointBuilder.time(new UtcTime(GPX_TIME_FORMAT.parse(s)));
				} catch (ParseException e) {
					Timber.e("Failed to parse time string [%s] (%s)", s, e.getMessage());
				}
				break;
			}
			case "name":
				switch (state) {
					case UNKNOWN:
						// Do nothing
						break;
					case WAITING_FOR_POINT_NAME:
						pointBuilder.name(textBuilder.toString());
						break;
					case WAITING_FOR_ROUTE_NAME:
						currentRoute.setName(textBuilder.toString());
						break;
				}
				break;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		textBuilder.append(ch, start, length);
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) {
	}

	@Override
	public void processingInstruction(String target, String data) {
	}

	@Override
	public void skippedEntity(String name) {
	}
	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) {
	}

	@Override
	public void endPrefixMapping(String prefix) {
	}
	
}
