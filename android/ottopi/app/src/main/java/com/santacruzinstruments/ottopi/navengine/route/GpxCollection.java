package com.santacruzinstruments.ottopi.navengine.route;

import java.io.File;
import java.util.List;

public class GpxCollection {
    private final List<File> gpxFiles;
    private int defaultIdx;

    public GpxCollection(List<File> gpxFiles) {
        this.gpxFiles = gpxFiles;
        this.defaultIdx = 0;
    }

    public List<File> getFiles(){return gpxFiles;}

    public int getDefaultIdx() {
        return defaultIdx;
    }

    public void setDefaultIdx(int defaultIdx) {
        this.defaultIdx = defaultIdx;
    }
}
