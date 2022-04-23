package com.santacruzinstruments.ottopi.data;

import androidx.annotation.NonNull;

public class DataReceptionStatus {

    private final int count;
    private final boolean haveGps;
    private final boolean haveWind;
    private final boolean haveWater;
    private final boolean haveCompass;

    public DataReceptionStatus(int count, boolean haveGps, boolean haveWind, boolean haveWater, boolean haveCompass) {
        this.count = count;
        this.haveGps = haveGps;
        this.haveWind = haveWind;
        this.haveWater = haveWater;
        this.haveCompass = haveCompass;
    }

    public int getCount() {
        return count;
    }

    public boolean isHaveGps() {
        return haveGps;
    }

    public boolean isHaveWind() {
        return haveWind;
    }

    public boolean isHaveWater() {
        return haveWater;
    }

    public boolean isHaveCompass() {
        return haveCompass;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        final int groupsNum = 6;
        for(int i = 0; i < groupsNum; i++ ){
            if ( (count % groupsNum) == i){
                sb.append(haveGps ? 'G': 'g');
                sb.append(haveWind ? 'W': 'w');
                sb.append(haveWater ? 'S': 's');
                sb.append(haveCompass ? 'M': 'm');
            }else{
                sb.append("----");
            }
        }

        return sb.toString();
    }
}
