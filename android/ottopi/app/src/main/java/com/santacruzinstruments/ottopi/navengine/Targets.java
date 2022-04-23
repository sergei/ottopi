package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;

public class Targets {
    public final Speed bsp;
    public final Angle twa;

    public Targets(Speed bsp, Angle twa) {
        this.bsp = bsp;
        this.twa = twa;
    }
}
