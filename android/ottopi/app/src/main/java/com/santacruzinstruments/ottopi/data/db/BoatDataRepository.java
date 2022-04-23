package com.santacruzinstruments.ottopi.data.db;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

public class BoatDataRepository {
    private final HostNameEntryDao hostNameEntryDao;
    private final LiveData<List<HostNameEntry>> hostNamesLiveData;
    private final HostPortEntryDao hostPortEntryDao;
    private final LiveData<List<HostPortEntry>> hostPortsLiveData;

    public BoatDataRepository(Context context){
        HostNameEntryDatabase db = HostNameEntryDatabase.getDatabase(context);
        hostNameEntryDao = db.hostNameEntryDao();
        hostNamesLiveData = hostNameEntryDao.getAllHostnames();

        HostPortEntryDatabase dbp = HostPortEntryDatabase.getDatabase(context);
        hostPortEntryDao = dbp.hostPortEntryDao();
        hostPortsLiveData = hostPortEntryDao.getAllHostPorts();
    }

    public LiveData<List<HostNameEntry>> getHostNamesLiveData() {
        return hostNamesLiveData;
    }

    public void insert(HostNameEntry hostName){
        hostNameEntryDao.insert(hostName);
    }

    public LiveData<List<HostPortEntry>> getHostPortsLiveData() {
        return hostPortsLiveData;
    }

    public void insert(HostPortEntry port){
        hostPortEntryDao.insert(port);
    }
}
