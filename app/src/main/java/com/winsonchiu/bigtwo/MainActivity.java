package com.winsonchiu.bigtwo;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.BaseGameUtils;
import com.google.example.games.basegameutils.GameHelper;
import com.winsonchiu.bigtwo.logic.GameData;
import com.winsonchiu.bigtwo.logic.GameUtils;
import com.winsonchiu.bigtwo.logic.Turn;
import com.winsonchiu.bigtwo.turn.ActivityNewMatch;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LaunchFragment.OnFragmentInteractionListener,
        GameFragment.OnFragmentInteractionListener,
        SettingsFragment.OnFragmentInteractionListener,
        OnTurnBasedMatchUpdateReceivedListener,
        AdapterView.OnItemClickListener,
        OnInvitationReceivedListener {

    public static final String FETCH_IMAGE = "cw.kop.autobackground.LiveWallpaperService.FETCH_IMAGE";

    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final String AUTO_BACKGROUND_SEND_IMAGE = "cw.kop.autobackground.AUTO_BACKGROUND_SEND_IMAGE";
    private static final String SAVE_MATCH = "match";

    private static final int NEW_MATCH_REQUEST_CODE = 0;
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_WAITING_ROOM = 3;
    private static final int RC_SELECT_PLAYERS = 9002;

    private CustomSurfaceView glSurfaceView;
    private GoogleApiClient googleApiClient;
    private ControllerMatches controllerMatches;

    private boolean mResolvingConnectionFailure = false;
    private boolean mAutoStartSignInFlow = true;
    private boolean mSignInClicked = false;
    private CustomRenderer renderer;
    private LaunchFragment launchFragment;
    private GameFragment gameFragment;

    private String[] fragmentList;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private LinearLayout navLayout;
    private ImageView navPicture;
    private ListView drawerList;
    private int currentPosition = -1;
    private int newPosition = -1;
    private boolean isTurn;
    private GameHelper gameHelper;
    private String loadMatchId;
    private Vibrator vibrator;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceListener;
    private BroadcastReceiver autoBackgroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            glSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    renderer.loadBackground(new File(intent.getStringExtra("imageFile")));
                }
            });
        }
    };
    private GameTurnBased gameTurnBased;
    private GameRealTime gameRealTime;
    private GameSinglePlayer gameSinglePlayer;
    private Handler handler;
    private boolean loadActivityNewMatch;
    private ResultCallback initialMatchCallback = new ResultCallback<TurnBasedMultiplayer.LoadMatchResult>() {
        @Override
        public void onResult(TurnBasedMultiplayer.LoadMatchResult loadMatchResult) {
            gameTurnBased.loadMatch(loadMatchResult.getMatch());
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());

        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        Intent notifyIntent = new Intent(ReceiverScheduling.NOTIFY_HAS_TURN);

        alarmManager.setInexactRepeating(AlarmManager.RTC, AlarmManager.INTERVAL_HOUR * 20, AlarmManager.INTERVAL_DAY, PendingIntent.getBroadcast(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        controllerMatches = new ControllerMatches(googleApiClient, new ControllerMatches.MatchListener() {
            @Override
            public void onPlayClick(String matchId) {
                MainActivity.this.onPlayClicked(matchId);
            }

            @Override
            public void onLeaveClick(String matchId) {
                MainActivity.this.onLeaveClicked(matchId);
            }

            @Override
            public void onDismissClick(String matchId) {
                MainActivity.this.onDismissClicked(matchId);
            }

            @Override
            public void onClickInvitation(MatchItem matchItem, boolean accept) {
                MainActivity.this.onClickInvitation(matchItem, accept);
            }
        });

        sharedPreferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                sharedPreferenceChange(sharedPreferences, key);
            }
        };

        AppSettings.initPrefs(getApplicationContext());

        Configuration configuration = getResources().getConfiguration();
        fragmentList = new String[]{
                "Home",
                "Settings",
                "Leaderboards"
        };
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navPicture = (ImageView) findViewById(R.id.nav_drawer_picture);
        navLayout = (LinearLayout) findViewById(R.id.navigation_drawer);
        navLayout.getLayoutParams().width = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                Math.min(320, configuration.screenWidthDp - 56),
                getResources().getDisplayMetrics()));

        RelativeLayout navHeader = (RelativeLayout) findViewById(R.id.nav_drawer_header);
        navHeader.getLayoutParams().height = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                Math.min(180, (configuration.screenWidthDp - 56) / 16f * 9),
                getResources().getDisplayMetrics()));
        navHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        drawerList = (ListView) findViewById(R.id.nav_list);
        drawerList.setDividerHeight(0);
        drawerList.setAdapter(new NavListAdapter(this, fragmentList));
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getFragmentManager().popBackStack();
                selectItem(position);
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (drawerLayout != null) {
            drawerList.setOnItemClickListener(this);
            drawerToggle = new ActionBarDrawerToggle(
                    this,
                    drawerLayout,
                    toolbar,
                    R.string.drawer_open,
                    R.string.drawer_close) {

                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                    selectItem(newPosition);
                }

                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                }

                @Override
                public void onDrawerSlide(View drawerView, float slideOffset) {
                    super.onDrawerSlide(drawerView, 0);
                }

            };

            drawerLayout.setDrawerListener(drawerToggle);
            drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
        }
        else {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        getSupportActionBar().setHomeButtonEnabled(true);

        gameHelper = new GameHelper(this, GameHelper.CLIENT_GAMES);
        gameHelper.setup(new GameHelper.GameHelperListener() {
            @Override
            public void onSignInSucceeded() {

                if (!googleApiClient.isConnected()) {
                    return;
                }

                Games.setViewForPopups(googleApiClient, getWindow().getDecorView()
                        .findViewById(android.R.id.content));

                TurnBasedMatch helperMatch = gameHelper.getTurnBasedMatch();

                if (helperMatch != null) {
                    loadGameFragment();
                    gameTurnBased.loadMatch(helperMatch);
                    gameHelper.clearTurnBasedMatch();
                }
            }

            @Override
            public void onSignInFailed() {
            }
        });

        renderer = new CustomRenderer(this, new CustomRenderer.RenderCallback() {
            @Override
            public void requestRender() {
                glSurfaceView.requestRender();
            }

            @Override
            public void toast(String text) {
                toastString(text);
            }

            @Override
            public boolean isTurn() {
                return isTurn;
            }

            @Override
            public void onPlayHand() {
                gameTurnBased.playHand();
                gameRealTime.playHand();
                gameSinglePlayer.playHand();
            }

            @Override
            public void fetchImage() {
                Intent cycleIntent = new Intent(FETCH_IMAGE);
                sendBroadcast(cycleIntent);
            }
        });

        GameUtils.GameEventListener gameEventListener = new GameUtils.GameEventListener() {
            @Override
            public void toast(String s) {
                toastString(s);
            }

            @Override
            public void setTurn(boolean isTurn) {
                MainActivity.this.setTurn(isTurn);
            }

            @Override
            public CustomRenderer getRenderer() {
                return renderer;
            }

            @Override
            public void loadGameFragment() {
                MainActivity.this.loadGameFragment();
            }

            @Override
            public void returnToLaunch() {
                loadLaunchFragment();
            }

            @Override
            public void setPlayers(List<MatchPlayer> matchPlayers, GameData gameData) {
                gameFragment.setPlayers(matchPlayers, gameData);
            }

            @Override
            public void setWinners(ArrayList<MatchPlayer> winners,
                                   GameData gameData) {
                gameFragment.setWinners(winners, gameData);
            }

            @Override
            public void vibrate(int duration) {
                if (vibrator != null) {
                    vibrator.vibrate(duration);
                }
            }

            @Override
            public void loadWaitingRoom(Room room) {
                // TODO: Change to min players
                Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(googleApiClient, room, 2);
                startActivityForResult(i, RC_WAITING_ROOM);
            }

            @Override
            public void postOnUiThread(Runnable runnable) {
                runOnUiThread(runnable);
            }

            @Override
            public void postDelayedOnSurfaceView(Runnable runnable, long delayMillis) {
                glSurfaceView.postDelayed(runnable, delayMillis);
            }

            @Override
            public void setPlayer(Turn newTurn) {
                gameFragment.setPlayer(newTurn);
            }

            @Override
            public boolean isGameFragmentLoaded() {
                return gameFragment != null && gameFragment.isAdded();
            }

            @Override
            public void removeMyTurnMatch(TurnBasedMatch match) {
                controllerMatches.removeMatch(match.getMatchId());
            }
        };

        gameSinglePlayer = new GameSinglePlayer("TestName", null, gameEventListener);
        gameRealTime = new GameRealTime(googleApiClient, gameEventListener);
        gameTurnBased = new GameTurnBased(googleApiClient, gameEventListener);


        glSurfaceView = (CustomSurfaceView) findViewById(R.id.game_surface);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                renderer.onTouchEvent(event);
                return true;
            }
        });

        if (AppSettings.useVibrator()) {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (!vibrator.hasVibrator()) {
                vibrator = null;
            }
        }

        launchFragment = new LaunchFragment();
        gameFragment = new GameFragment();

        TurnBasedMatch newMatch = getIntent().getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);
        if (newMatch == null) {
            newMatch = getIntent().getParcelableExtra(Multiplayer.EXTRA_INVITATION);
        }
        Invitation invitation = getIntent().getParcelableExtra(Multiplayer.EXTRA_INVITATION);

        if (invitation != null) {
            if (invitation.getInvitationType() == Invitation.INVITATION_TYPE_REAL_TIME) {
                if (googleApiClient.isConnected()) {
                    gameRealTime.acceptInvite(invitation.getInvitationId());
                } else {
                    gameRealTime.setInvitation(invitation);
                }
            }
            else {
                gameTurnBased.loadMatch(newMatch);
            }
        }
        else if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, launchFragment, FragmentNames.LAUNCH_FRAGMENT).commit();
        }
        else {
            if (savedInstanceState.containsKey(SAVE_MATCH)) {
                loadGameFragment();
                if (googleApiClient.isConnected()) {
                    gameTurnBased.loadMatch(
                            (TurnBasedMatch) savedInstanceState.getParcelable(SAVE_MATCH));
                }
                else {
                    loadMatchId = ((TurnBasedMatch) savedInstanceState.getParcelable(SAVE_MATCH)).getMatchId();
                }
            }
        }

        if (AppSettings.useAutoRotate()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }

    }

    private void sharedPreferenceChange(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "use_auto_rotate":
                onChangeAutoRotate();
                break;
            case "use_vibrator":
                if (sharedPreferences.getBoolean("use_vibrator", false)) {
                    vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (!vibrator.hasVibrator()) {
                        vibrator = null;
                        toastString("No vibrator available");
                    }
                }
                else {
                    vibrator = null;
                }
                break;
            case "use_background_image":
                onChangeBackgroundImage();
                break;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void setTurn(boolean isTurn) {
        this.isTurn = isTurn;
    }

    private void selectItem(int newPosition) {

        if (newPosition == currentPosition) {
            return;
        }

        //TODO: Finish fragments in nav drawer

        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        switch (newPosition) {

            case 0:
                loadLaunchFragment();
                break;
            case 1:
                transaction.replace(R.id.fragment_container, new SettingsFragment()).commit();
                break;
            case 2:
                break;

        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        newPosition = position;
        drawerList.setItemChecked(position, true);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(navLayout);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(autoBackgroundReceiver, new IntentFilter(AUTO_BACKGROUND_SEND_IMAGE));
        AppSettings.registerListener(sharedPreferenceListener);
        glSurfaceView.onResume();
        if (googleApiClient.isConnected()) {
            Games.TurnBasedMultiplayer.registerMatchUpdateListener(googleApiClient, this);
            Games.Invitations.registerInvitationListener(googleApiClient, this);
        }
        if (loadMatchId != null) {
            Games.TurnBasedMultiplayer.loadMatch(googleApiClient, loadMatchId).setResultCallback(initialMatchCallback);
            loadMatchId = null;
        }
        else if (gameTurnBased.getMatch() != null) {
            Games.TurnBasedMultiplayer.loadMatch(googleApiClient, gameTurnBased.getMatch().getMatchId()).setResultCallback(initialMatchCallback);
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(autoBackgroundReceiver);
        AppSettings.unregisterListener(sharedPreferenceListener);
        if (googleApiClient.isConnected()) {
            Games.TurnBasedMultiplayer.unregisterMatchUpdateListener(googleApiClient);
            Games.Invitations.unregisterInvitationListener(googleApiClient);
        }
        glSurfaceView.onPause();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        if (gameTurnBased.hasMatch()) {
            outState.putParcelable(SAVE_MATCH, gameTurnBased.getMatch());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.registerConnectionCallbacks(this);
        googleApiClient.registerConnectionFailedListener(this);
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.release();
            }
        });
        googleApiClient.unregisterConnectionCallbacks(this);
        googleApiClient.unregisterConnectionFailedListener(this);
        gameHelper.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    public void toastString(final String toast) {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
        }
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {

        if (getFragmentManager().findFragmentByTag(FragmentNames.GAME_FRAGMENT) != null) {
            if (!gameFragment.onBackPressed()) {
                loadLaunchFragment();
            }
        }
        else {
            super.onBackPressed();
        }
    }

    private void loadGameFragment() {
        try {
            if (getFragmentManager().findFragmentByTag(FragmentNames.GAME_FRAGMENT) == null) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, gameFragment,
                                FragmentNames.GAME_FRAGMENT)
                        .commit();
            }
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }

    private void loadLaunchFragment() {
        renderer.clear();
        gameSinglePlayer.clearMatch();
        gameRealTime.clearMatch();
        gameTurnBased.clearMatch();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, launchFragment, FragmentNames.LAUNCH_FRAGMENT).commit();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Games.TurnBasedMultiplayer.registerMatchUpdateListener(googleApiClient, this);
        Games.Invitations.registerInvitationListener(googleApiClient, this);
        gameHelper.onStart(this);

        gameRealTime.onConnected(bundle);
        gameTurnBased.onConnected(bundle);

        if (loadActivityNewMatch) {
            onNewGameClicked();
            loadActivityNewMatch = false;
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        toastString("onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult.getErrorCode());
        if (mResolvingConnectionFailure) {
            // already resolving
            return;
        }

        // if the sign-in button was clicked or if auto sign-in is enabled,
        // launch the sign-in flow
        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;

            // Attempt to resolve the connection failure using BaseGameUtils.
            // The R.string.signin_other_error value should reference a generic
            // error string in your strings.xml file, such as "There was
            // an issue with sign-in, please try again later."
            if (!BaseGameUtils.resolveConnectionFailure(this,
                    googleApiClient, connectionResult,
                    RC_SIGN_IN, getResources().getString(R.string.signin_other_error))) {
                mResolvingConnectionFailure = false;
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        switch (requestCode) {

            case RC_WAITING_ROOM:
                if (resultCode == Activity.RESULT_OK) {
                    // (start game)
                }
                else if (resultCode == Activity.RESULT_CANCELED) {
                    // Waiting room was dismissed with the back button. The meaning of this
                    // action is up to the game. You may choose to leave the room and cancel the
                    // match, or do something else like minimize the waiting room and
                    // continue to connect in the background.

                    // in this example, we take the simple approach and just leave the room:
//                    gameRealTime.leave();
//                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                else if (resultCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    // player wants to leave the room.
                    gameRealTime.leave();
//                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                break;
            case RC_SIGN_IN:
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                if (resultCode == RESULT_OK) {
                    googleApiClient.connect();
                }
                else {
                    // Bring up an error dialog to alert the user that sign-in
                    // failed. The R.string.signin_failure should reference an error
                    // string in your strings.xml file that tells the user they
                    // could not be signed in, such as "Unable to sign in."
                    BaseGameUtils.showActivityResultError(this,
                            requestCode, resultCode, R.string.signin_failure);
                }
                break;
            case RC_SELECT_PLAYERS:
                if (resultCode != Activity.RESULT_OK) {
                    break;
                }

                ArrayList<String> invited = intent.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
                Intent newMatchIntent = new Intent(this, ActivityNewMatch.class);
                newMatchIntent.putStringArrayListExtra(ActivityNewMatch.INVITED_PLAYERS, invited);
                startActivityForResult(newMatchIntent, NEW_MATCH_REQUEST_CODE);

                break;
            case NEW_MATCH_REQUEST_CODE:
                if (resultCode != Activity.RESULT_OK) {
                    return;
                }

                final MatchSettings matchSettings;
                final ArrayList<String> invitedPlayers;
                try {
                    matchSettings = MatchSettings.fromJsonObject(new JSONObject(intent.getStringExtra(MatchSettings.MATCH_SETTINGS)));
                    invitedPlayers = intent.getStringArrayListExtra(MatchSettings.INVITED_PLAYERS);
                }
                catch (JSONException e) {
                    toastString("Error creating match");
                    e.printStackTrace();
                    return;
                }

//                matchSettings.setNumAI(2);
//                gameSinglePlayer.startMatch(matchSettings);

                // TODO: Add switch for match type
                if (AppSettings.TURN_BASED.equals(intent.getStringExtra(AppSettings.GAME_TYPE))) {
                    gameTurnBased.create(invitedPlayers, matchSettings);
                    toastString("Turn based match created");
                }
                else {
                    gameRealTime.create(invitedPlayers, matchSettings);
                    toastString("Real time match created");
                }

                break;
        }

    }

    @Override
    public void onNewGameClicked() {
        if (!googleApiClient.isConnected()) {
            loadActivityNewMatch = true;
            googleApiClient.connect();

        }
        else {
//            Intent intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(googleApiClient, 1, 3);
//            startActivityForResult(intent, REAL_TIME_SELECT);
            Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(googleApiClient, 1, 11, false);
            startActivityForResult(intent, RC_SELECT_PLAYERS);
//            Intent intent = new Intent(this, ActivityNewMatch.class);
//            startActivityForResult(intent, NEW_MATCH_REQUEST_CODE);
        }
    }

    public void onPlayClicked(final String matchId) {
        if (!googleApiClient.isConnected()) {
            return;
        }
        Games.TurnBasedMultiplayer.loadMatch(googleApiClient, matchId).setResultCallback(initialMatchCallback);
    }

    private void leaveMatch(final String matchId) {

        Games.TurnBasedMultiplayer.loadMatch(googleApiClient, matchId)
                .setResultCallback(
                        new ResultCallback<TurnBasedMultiplayer.LoadMatchResult>() {
                            @Override
                            public void onResult(TurnBasedMultiplayer.LoadMatchResult loadMatchResult) {
                                TurnBasedMatch deleteMatch = loadMatchResult.getMatch();

                                if (deleteMatch == null) {
                                    toastString("Error leaving deleteMatch");
                                    Games.TurnBasedMultiplayer.dismissMatch(
                                            googleApiClient, matchId);
                                    controllerMatches.removeMatch(matchId);
                                    return;
                                }

                                String playerId = deleteMatch.getParticipantId(
                                        Games.Players.getCurrentPlayerId(
                                                googleApiClient));

                                if (deleteMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {

                                    GameData gameData = null;
                                    try {
                                        gameData = GameData.fromJsonString(
                                                new String(deleteMatch.getData()));
                                    }
                                    catch (JSONException e) {
                                        e.printStackTrace();
                                        onDismissClicked(matchId);
                                        return;
                                    }

                                    if (gameData == null) {
                                        onDismissClicked(matchId);
                                        return;
                                    }

                                    for (String player : new ArrayList<>(
                                            gameData.getActivePlayers())) {
                                        if (deleteMatch.getParticipant(player)
                                                .getStatus() == Participant.STATUS_LEFT) {
                                            gameData.removePlayer(player);
                                        }
                                    }

                                    ArrayList<String> players = gameData.getRemainingActivePlayers();

                                    if (players.size() == 2 && gameData.getTurns()
                                            .size() > 0) {
                                        players.remove(playerId);
                                        gameData.addFinishedPlayer(players.get(0));

                                        List<String> winners = gameData.getFinishedPlayers();

                                        ArrayList<ParticipantResult> results = new ArrayList<>(
                                                winners.size());

                                        for (int position = 0; position < winners.size(); position++) {
                                            results.add(new ParticipantResult(winners.get(position),
                                                    ParticipantResult.MATCH_RESULT_WIN,
                                                    position + 1));
                                        }

                                        ArrayList<String> allPlayers = deleteMatch.getParticipantIds();
                                        allPlayers.removeAll(winners);
                                        for (int position = 0; position < allPlayers.size(); position++) {
                                            results.add(
                                                    new ParticipantResult(allPlayers.get(position),
                                                            ParticipantResult.MATCH_RESULT_LOSS,
                                                            ParticipantResult.PLACING_UNINITIALIZED));
                                        }

                                        gameData.removePlayer(playerId);

                                        Games.TurnBasedMultiplayer.finishMatch(
                                                googleApiClient, matchId,
                                                gameData.toJsonString()
                                                        .getBytes(), results);
                                    }
                                    else {
                                        gameData.removePlayer(playerId);

                                        Games.TurnBasedMultiplayer.leaveMatchDuringTurn(
                                                googleApiClient, matchId,
                                                gameData.getNextPlayer());
                                    }

                                }
                                else {
                                    Games.TurnBasedMultiplayer.leaveMatch(
                                            googleApiClient, matchId);
                                }
                                controllerMatches.removeMatch(matchId);
                            }
                        });
    }

    public void onLeaveClicked(final String matchId) {

        new AlertDialog.Builder(this)
                .setTitle("Leave game?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadLaunchFragment();
                        leaveMatch(matchId);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();

    }

    public void onDismissClicked(final String matchId) {
        new AlertDialog.Builder(this)
                .setTitle("Dismiss match?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Games.TurnBasedMultiplayer.dismissMatch(googleApiClient, matchId);
                        controllerMatches.removeMatch(matchId);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public void onClickInvitation(MatchItem matchItem, boolean accept) {

        switch (matchItem.getType()) {

            case INVITE_REAL_TIME:
                if (accept) {
                    gameRealTime.acceptInvite(matchItem.getId());
                }
                else {
                    Games.RealTimeMultiplayer.declineInvitation(googleApiClient, matchItem.getId());
                }
                break;
            case INVITE_TURN_BASED:
                if (accept) {
                    Games.TurnBasedMultiplayer.acceptInvitation(googleApiClient, matchItem.getId()).setResultCallback(
                            new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
                                @Override
                                public void onResult(TurnBasedMultiplayer.InitiateMatchResult initiateMatchResult) {
                                    gameTurnBased.loadMatch(initiateMatchResult.getMatch());
                                }
                            });
                }
                else {
                    Games.TurnBasedMultiplayer.declineInvitation(googleApiClient, matchItem.getId());
                    controllerMatches.removeMatch(matchItem.getId());
                }
                break;

        }
    }

    @Override
    public ControllerMatches getControllerMatches() {
        return controllerMatches;
    }

    @Override
    public CustomRenderer getRenderer() {
        return renderer;
    }

    @Override
    public void onPlayerIconClick(String playerId) {

        gameTurnBased.onIconClick(playerId);

    }

    @Override
    public void setSurfaceVisibility(int visibility) {
        if (glSurfaceView != null) {
            glSurfaceView.setVisibility(visibility);
        }
    }

    @Override
    public void onLeaveClick() {
        if (gameTurnBased.hasMatch()) {
            onLeaveClicked(gameTurnBased.getMatch().getMatchId());
        }
        else {
            // TODO: Fix leaving real time match
            gameRealTime.leave();
        }
    }


    @Override
    public void onTurnBasedMatchReceived(TurnBasedMatch turnBasedMatch) {

        Log.i(TAG, "onTurnBasedMatchReceived");

        if (gameFragment.isAdded() && gameTurnBased.hasMatch() && turnBasedMatch.getMatchId().equals(gameTurnBased.getMatch().getMatchId())) {
            gameTurnBased.loadMatch(turnBasedMatch);
        }
        else {
            controllerMatches.addMatch(turnBasedMatch);
        }

    }

    @Override
    public void onTurnBasedMatchRemoved(String matchId) {
        controllerMatches.removeMatch(matchId);
    }

    @Override
    public void onInvitationReceived(Invitation invitation) {
        controllerMatches.addInvitation(invitation);
    }

    @Override
    public void onInvitationRemoved(String inviteId) {
        controllerMatches.removeMatch(inviteId);
    }

    public void onChangeAutoRotate() {
        if (AppSettings.useAutoRotate()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }
    }

    @Override
    public void onChangeBackgroundImage() {
        if (AppSettings.useBackgroundImage() && !AppSettings.getBackgroundImage().exists()) {
            toastString("Error loading background");
        }
        renderer.setChangeBackground(true);

    }

    @Override
    public void onChangeCardImage() {
        if (AppSettings.useCardImage() && !AppSettings.getCardImage().exists()) {
            toastString("Error loading card image");
        }
        renderer.setChangeCard(true);
    }

}