package com.istandev.diponews.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by ADIK on 12/05/2015.
 */
public class NewsSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static NewsSyncAdapter sNewsSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("NewsSyncService", "onCreate - NewsSyncService");
        synchronized (sSyncAdapterLock) {
            if (sNewsSyncAdapter == null) {
                sNewsSyncAdapter = new NewsSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sNewsSyncAdapter.getSyncAdapterBinder();
    }
}
