package com.henry.iwagenda;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class iwSyncService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction().equals("com.henry.iwagenda.action.start_sync")) {
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(iwSyncService.this);
                                mBuilder.setContentTitle(getString(R.string.syncing))
                                        .setOngoing(true)
                                        .setSmallIcon(R.drawable.ic_sync);

                                mBuilder.setProgress(0, 0, true);
                                startForeground(1, mBuilder.build());

                                Offline off = new Offline(iwSyncService.this);

                                off.syncOffline(LoginActivity.cookiejar);

                                Intent intent = new Intent();
                                intent.setAction("OFFLINE_SYNC_COMPLETE");
                                sendBroadcast(intent);

                                stopForeground(true);
                            }
                        }
                ).start();
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }
}