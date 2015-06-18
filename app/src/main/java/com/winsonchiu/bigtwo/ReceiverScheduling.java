package com.winsonchiu.bigtwo;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.plus.Plus;

/**
 * Created by TheKeeperOfPie on 3/30/2015.
 */
public class ReceiverScheduling extends BroadcastReceiver implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String NOTIFY_HAS_TURN = "com.winsonchiu.reader.ReceiverScheduling.NOTIFY_HAS_TURN";
    private static final int NOTIFY_REQUEST_CODE = 0;
    private static final int NOTIFY_ID = 1;
    private GoogleApiClient googleApiClient;

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case Intent.ACTION_BOOT_COMPLETED:
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                Intent notifyIntent = new Intent(NOTIFY_HAS_TURN);

                alarmManager.setInexactRepeating(AlarmManager.RTC, AlarmManager.INTERVAL_HOUR * 20, AlarmManager.INTERVAL_DAY, PendingIntent.getBroadcast(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT));
                break;
            case NOTIFY_HAS_TURN:
                googleApiClient = new GoogleApiClient.Builder(context)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                        .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                        .build();
                googleApiClient.connect();
                break;
        }
    }

   @Override
    public void onConnected(Bundle bundle) {

        Games.TurnBasedMultiplayer.loadMatchesByStatus(googleApiClient, new int[]{TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN}).setResultCallback(new ResultCallback<TurnBasedMultiplayer.LoadMatchesResult>() {
            @Override
            public void onResult(TurnBasedMultiplayer.LoadMatchesResult loadMatchesResult) {
                if (loadMatchesResult != null && loadMatchesResult.getMatches().getMyTurnMatches().getCount() > 0) {

                    NotificationManager notificationManager = (NotificationManager) googleApiClient.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

                    notificationManager.notify(NOTIFY_ID, new Notification.Builder(googleApiClient.getContext()).setContentTitle("Big Two").setSubText("It's your turn in a match of BigTwo").build());
                }
                googleApiClient.disconnect();
                googleApiClient = null;
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.disconnect();
        googleApiClient = null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        googleApiClient.disconnect();
        googleApiClient = null;
    }
}
