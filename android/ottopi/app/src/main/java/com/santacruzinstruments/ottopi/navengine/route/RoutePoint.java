package com.santacruzinstruments.ottopi.navengine.route;


import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;

@Entity
public class RoutePoint {

	// The order in enum corresponds to the sorting order when read from the data base
	public enum Type {START_PORT, START_STBD, ROUNDING, WINDWARD, WINDWARD_GATE, LEEWARD, LEEWARD_GATE, FINISH_PORT, FINISH_STBD};

	public enum LeaveTo {PORT, STARBOARD};
	public enum Location {KNOWN, UNKNOWN };

	@PrimaryKey(autoGenerate = true)
	public long id;

	final public String name;
	final public GeoLoc loc;
	final public Type  type;
	final public LeaveTo leaveTo;
	final public  Location location;
	final public boolean isActive;

	final public static RoutePoint INVALID = new RoutePoint(new GeoLoc(), "----", Type.ROUNDING, LeaveTo.PORT, Location.UNKNOWN );

	@Ignore
	public RoutePoint(GeoLoc loc, String name, Type type, LeaveTo leaveTo,
			Location location) {
		this.id = 0;
		this.loc = loc;
		this.name = name;
		this.type = type;
		this.leaveTo = leaveTo;
		this.location = location;
		this.isActive = false;
	}

	public RoutePoint(long id, GeoLoc loc, String name, Type type, LeaveTo leaveTo,
			Location location, boolean isActive) {
		this.id = id;
		this.loc = loc;
		this.name = name;
		this.type = type;
		this.leaveTo = leaveTo;
		this.location = location;
		this.isActive = isActive;
	}

	public RoutePoint changeActiveStatus(boolean isActive){
		return new RoutePoint(id, loc, name, type, leaveTo, location, isActive);
	}

	public static class Builder{
		final private GeoLoc loc;
		private String name = "NONAME";
		private Type  type = Type.ROUNDING;
		private LeaveTo leaveTo = LeaveTo.PORT;
		private Location location = Location.KNOWN;
		public Builder ( GeoLoc loc ){
			this.loc = loc;
		}
		Builder  name(String name){this.name = name; return this;}
		Builder  type(Type type){this.type = type; return this;}
		Builder  leaveTo(LeaveTo leaveTo){this.leaveTo = leaveTo; return this;}
		Builder  location(Location location){this.location = location; return this;}
		RoutePoint build(){ return new RoutePoint(loc, name, type, leaveTo, location);}
	}
}
