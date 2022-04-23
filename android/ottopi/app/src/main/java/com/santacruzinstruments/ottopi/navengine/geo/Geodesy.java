package com.santacruzinstruments.ottopi.navengine.geo;


import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import nsidc.mapx.MapMaker;
import nsidc.mapx.Mapx;


public class Geodesy {
	final static String TAG="Geodesy";
	final static boolean D = false;
	public static final double METERS_IN_NM = 1852;

	private static Geodesy instance = null;
	
	private Mapx map;
	private GeometryFactory mGeomFactory;
	private MagDecl magDecl;
	
	float u1[]={0};
	float v1[]={0};
	float u2[]={0};
	float v2[]={0};
	float u3[]={0};
	float v3[]={0};
	
	public  static Geodesy geodesyFactory(GeoLoc refloc)
	{
		if ( instance == null )
		{
			instance = new Geodesy(refloc);
		}
		else 
		{
			//TODO check if the reference location is significantly different now
		}
		return instance;
	}
	// Prevent anyone to use the default constructor 
	private Geodesy(){}
	private Geodesy(GeoLoc refloc)
	{
		MapMaker maker = new MapMaker();
		map = maker.createMapx("SINUSOIDAL", 
				(float)refloc.lat, (float)refloc.lon, 
				(float)refloc.lat, (float)refloc.lon, 
				(float)0, // Do not rotate  
				(float)0.001, // (kilometers per map unit)
				(float)refloc.lat, (float)refloc.lon,  // center_lat center_lon (for map)
				(float)-90, (float)90, (float)-180, (float)180, 
				(float)30, (float)30, 
				(float)0, (float)0, 
				1, 0, 0);
		
		mGeomFactory = new GeometryFactory();
		
		magDecl = MagDecl.getInstance();
	}
	
	public MagDecl getMagDecl()
	{
		return magDecl;
	}

	public final Distance dist(GeoLoc from, GeoLoc to)
	{
		map.geo_to_map((float)from.lat, (float)from.lon, u1, v1);
		map.geo_to_map((float)to.lat,   (float)to.lon,   u2, v2);
		
		Coordinate c1 = new Coordinate(u1[0], v1[0]);
		Coordinate c2 = new Coordinate(u2[0], v2[0]);
		
		Point p1 = mGeomFactory.createPoint(c1);
		Point p2 = mGeomFactory.createPoint(c2);
		
		double distMeters = p1.distance(p2);

		return new Distance(distMeters / 1852.0); 
	} 
	
	public final Direction bearing(GeoLoc from, GeoLoc to)
	{
		map.geo_to_map((float)from.lat, (float)from.lon, u1, v1);
		map.geo_to_map((float)to.lat,   (float)to.lon,   u2, v2);
		
		Coordinate c1 = new Coordinate(u1[0], v1[0]);
		Coordinate c2 = new Coordinate(u2[0], v2[0]);
		
		double phi = org.locationtech.jts.algorithm.Angle.angle(c1, c2);
		phi = 90.0 - org.locationtech.jts.algorithm.Angle.toDegrees(phi);
		phi = magDecl.fromTrueToMag(phi);

		return new Direction(phi);
	}
	
	public final Coordinate toCoordinate(GeoLoc loc)
	{
		map.geo_to_map((float)loc.lat, (float)loc.lon, u1, v1);
		
		return new Coordinate(u1[0], v1[0]);
	}
	
	public Mapx getMap()
	{ 
		return map;
	}
}
