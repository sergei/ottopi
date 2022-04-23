package com.santacruzinstruments.ottopi.data;

import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;

public class StartLineInfo {
    public GeoLoc pin = GeoLoc.INVALID;
    public GeoLoc rcb = GeoLoc.INVALID;
    public Distance distToLine = Distance.INVALID;
    public Angle pinFavoredBy = Angle.INVALID;
    public boolean isOcs = false;
}
