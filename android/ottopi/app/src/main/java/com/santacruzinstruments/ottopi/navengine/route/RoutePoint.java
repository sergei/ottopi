package com.santacruzinstruments.ottopi.navengine.route;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;

@Entity
public class RoutePoint {

	// The order in enum corresponds to the sorting order when read from the data base
	public enum Type {START, ROUNDING, FINISH}

	public enum LeaveTo {PORT, STARBOARD}

	@PrimaryKey(autoGenerate = true)
	public long id;

	final public String name;
	final public GeoLoc loc;
	final public Type  type;
	final public LeaveTo leaveTo;
	final public boolean isActive;
	final public UtcTime time;

	final public static RoutePoint INVALID = new RoutePoint(0, new GeoLoc(), "----", Type.ROUNDING, LeaveTo.PORT, false, UtcTime.INVALID);

	public RoutePoint(long id, GeoLoc loc, String name, Type type, LeaveTo leaveTo, boolean isActive, UtcTime time) {
		this.id = id;
		this.loc = loc;
		this.name = name;
		this.type = type;
		this.leaveTo = leaveTo;
		this.isActive = isActive;
		this.time = time;
	}

	public RoutePoint changeActiveStatus(boolean isActive){
		return new RoutePoint(id, loc, name, type, leaveTo, isActive, time);
	}

	public static class Builder{
		private long id = 0;
		private GeoLoc loc = GeoLoc.INVALID;
		private String name = "NONAME";
		private Type  type = Type.ROUNDING;
		private LeaveTo leaveTo = LeaveTo.PORT;
		private boolean isActive = false;
		private UtcTime time = UtcTime.INVALID;

		public Builder (){
		}
		public Builder  loc(GeoLoc loc){this.loc = loc; return this;}
		public Builder  id(long id){this.id = id; return this;}
		public Builder  name(String name){this.name = name; return this;}
		public Builder  type(Type type){this.type = type; return this;}
		public Builder  leaveTo(LeaveTo leaveTo){this.leaveTo = leaveTo; return this;}
		public Builder  isActive(boolean isActive){this.isActive = isActive; return this;}
		public Builder  time(UtcTime time){this.time = time; return this;}
		public Builder  copy(RoutePoint pt){
			this.id = pt.id;
			this.loc = new GeoLoc(pt.loc.lat, pt.loc.lon);
			this.name = pt.name;
			this.type = pt.type;
			this.leaveTo = pt.leaveTo;
			this.isActive = pt.isActive;
			this.time = pt.time;
			return this;
		}
		public RoutePoint build(){ return new RoutePoint(id, loc, name, type, leaveTo, isActive, time);}
	}
}
