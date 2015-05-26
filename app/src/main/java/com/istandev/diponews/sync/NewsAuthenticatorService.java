package com.istandev.diponews.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by ADIK on 12/05/2015.
 */
public class NewsAuthenticatorService extends Service {
    // Instance field that stores the authenticator object
    private NewsAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new NewsAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
