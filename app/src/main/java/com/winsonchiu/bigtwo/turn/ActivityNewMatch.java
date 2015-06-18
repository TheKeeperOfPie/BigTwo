package com.winsonchiu.bigtwo.turn;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerBuffer;
import com.google.android.gms.games.Players;
import com.google.android.gms.plus.Plus;
import com.winsonchiu.bigtwo.AppSettings;
import com.winsonchiu.bigtwo.MainActivity;
import com.winsonchiu.bigtwo.MatchPlayer;
import com.winsonchiu.bigtwo.MatchSettings;
import com.winsonchiu.bigtwo.R;

import java.util.ArrayList;

public class ActivityNewMatch extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        InviteFragment.OnFragmentInteractionListener,
        DeckFragment.OnFragmentInteractionListener,
        CardOrderFragment.OnFragmentInteractionListener,
        SuitOrderFragment.OnFragmentInteractionListener {

    private static final int NUM_FRAGMENTS = 4;
    private static final long EXPAND_ANIM_DURATION = 150;
    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final long CIRCLE_ANIMATION_TIME = 350;
    public static final String INVITED_PLAYERS = "invitedPlayers";
    private ViewPager viewPager;
    private ImageView playButton;
    private ImageView playButtonBackground;
    private MatchSettings matchSettings;
    private GoogleApiClient googleApiClient;
    private InviteFragment inviteFragment;
    private DeckFragment deckFragment;
    private CardOrderFragment cardOrderFragment;
    private SuitOrderFragment suitOrderFragment;
    private RecyclerView recyclerInvitedPlayers;
    private AdapterInvitedPlayers adapterInvitedPlayers;
    private ControllerInvitedPlayers controllerInvitedPlayers;
    private ControllerInvitedPlayers.InvitedPlayersListener invitedPlayersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_match);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        controllerInvitedPlayers = new ControllerInvitedPlayers();

        matchSettings = MatchSettings.getDefaultSettings();

        inviteFragment = InviteFragment.newInstance();
        deckFragment = DeckFragment.newInstance();
        suitOrderFragment = SuitOrderFragment.newInstance();
        cardOrderFragment = CardOrderFragment.newInstance();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("New Match");

        adapterInvitedPlayers = new AdapterInvitedPlayers(this, controllerInvitedPlayers);
        invitedPlayersListener = new ControllerInvitedPlayers.InvitedPlayersListener() {
            @Override
            public void notifyInvitedAdded(MatchPlayer player) {
                adapterInvitedPlayers.notifyItemInserted(adapterInvitedPlayers.getItemCount());
                notifyInvitedChanged();
            }

            @Override
            public void notifyInvitedRemoved(MatchPlayer player, int position) {
                adapterInvitedPlayers.notifyItemRemoved(position);
                notifyInvitedChanged();
            }

            @Override
            public void notifyAllPlayersChanged() {

            }
        };

        recyclerInvitedPlayers = (RecyclerView) findViewById(R.id.recycler_invited_players);
        recyclerInvitedPlayers.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerInvitedPlayers.setAdapter(adapterInvitedPlayers);

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(NUM_FRAGMENTS - 1);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return inviteFragment;
                    case 1:
                        return deckFragment;
                    case 2:
                        return suitOrderFragment;
                    case 3:
                        return cardOrderFragment;
                }
                return null;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return "Players";
                    case 1:
                        return "Settings";
                    case 2:
                        return "Suits";
                    case 3:
                        return "Cards";
                }

                return super.getPageTitle(position);
            }

            @Override
            public int getCount() {
                return NUM_FRAGMENTS;
            }
        });

        PagerSlidingTabStrip tabStrip = (PagerSlidingTabStrip) findViewById(R.id.pager_tab_strip);
        tabStrip.setViewPager(viewPager);
        tabStrip.requestDisallowInterceptTouchEvent(true);

        playButtonBackground = (ImageView) findViewById(R.id.floating_button_background);
        playButton = (ImageView) findViewById(R.id.start_match_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapterInvitedPlayers.getItemCount() == 0) {
                    Toast.makeText(ActivityNewMatch.this, "Must invite a least one player",
                            Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                matchSettings.setNumCards(deckFragment.getNumCards());
                matchSettings.setNumDecks(deckFragment.getNumDecks());
                matchSettings.setSuitOrder(suitOrderFragment.getSuitOrder());
                matchSettings.setCardOrder(cardOrderFragment.getCardOrder());
                matchSettings.setUseCardOrderStraight(deckFragment.useCardOrderStraight());

                final GradientDrawable circleDrawable = (GradientDrawable) getResources().getDrawable(
                        R.drawable.floating_button_accent);

                if (circleDrawable == null) {
                    finishSettings(matchSettings);
                    return;
                }

                final float scale = (float) ((Math.hypot(playButtonBackground.getX(),
                        playButtonBackground.getY()) + playButtonBackground.getWidth()) / playButtonBackground.getWidth() * 2);
                Animation animation = new Animation() {

                    private float pivot;

                    @Override
                    public void initialize(int width,
                                           int height,
                                           int parentWidth,
                                           int parentHeight) {
                        super.initialize(width, height, parentWidth, parentHeight);

                        pivot = resolveSize(RELATIVE_TO_SELF, 0.5f, width, parentWidth);
                    }

                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        float scaleFactor = 1.0f + scale * interpolatedTime;
                        t.getMatrix()
                                .setScale(scaleFactor, scaleFactor, pivot, pivot);
                    }

                    @Override
                    public boolean willChangeBounds() {
                        return true;
                    }


                };

                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        circleDrawable.setColor(getResources().getColor(R.color.COLOR_ACCENT));
                        playButtonBackground.setImageDrawable(circleDrawable);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        finishSettings(matchSettings);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }

                });

                ValueAnimator buttonColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                        getResources().getColor(R.color.COLOR_ACCENT),
                        getResources().getColor(R.color.DARK_THEME_BACKGROUND));
                buttonColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        circleDrawable.setColor((Integer) animation.getAnimatedValue());
                        playButtonBackground.setImageDrawable(circleDrawable);
                    }

                });

                DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();

                animation.setDuration(CIRCLE_ANIMATION_TIME);
                animation.setFillAfter(true);
                buttonColorAnimation.setDuration((long) (CIRCLE_ANIMATION_TIME * 0.9));
                buttonColorAnimation.setInterpolator(decelerateInterpolator);
                animation.setInterpolator(decelerateInterpolator);

                playButton.setVisibility(View.GONE);
                buttonColorAnimation.start();
                playButtonBackground.startAnimation(animation);
            }
        });

        final ArrayList<String> invitedPlayers = getIntent().getExtras().getStringArrayList(INVITED_PLAYERS);

        if (invitedPlayers != null && !invitedPlayers.isEmpty()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    googleApiClient.blockingConnect();
                    recyclerInvitedPlayers.post(new Runnable() {
                        @Override
                        public void run() {

                            for (String playerId : invitedPlayers) {
                                Games.Players.loadPlayer(googleApiClient, playerId)
                                        .setResultCallback(
                                                new ResultCallback<Players.LoadPlayersResult>() {
                                                    @Override
                                                    public void onResult(Players.LoadPlayersResult loadPlayersResult) {
                                                        Player player = loadPlayersResult.getPlayers()
                                                                .get(0);
                                                        controllerInvitedPlayers.addPlayer(
                                                                new MatchPlayer(
                                                                        player.getPlayerId(),
                                                                        player.getDisplayName(),
                                                                        player.getHiResImageUri()));
                                                        loadPlayersResult.release();
                                                    }
                                                });
                            }
                        }
                    });
                }
            }).start();
        }
    }

    private void finishSettings(MatchSettings matchSettings) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(AppSettings.GAME_TYPE, deckFragment.isRealTime() ? AppSettings.REAL_TIME : AppSettings.TURN_BASED);
        resultIntent.putExtra(MatchSettings.MATCH_SETTINGS, matchSettings.toJsonString());
        resultIntent.putExtra(MatchSettings.INVITED_PLAYERS,
                controllerInvitedPlayers.getInvitedPlayerIds());
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private void notifyInvitedChanged() {
        Log.d(TAG, "notifyInvitedChanged");
        deckFragment.onPlayersChanged();
        recyclerInvitedPlayers.post(new Runnable() {
            @Override
            public void run() {
                if (adapterInvitedPlayers.getItemCount() == 0) {
                    if (recyclerInvitedPlayers.getVisibility() == View.VISIBLE) {
                        final float targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());

                        Animation animation = new Animation() {
                            @Override
                            protected void applyTransformation(float interpolatedTime,
                                                               Transformation t) {
                                recyclerInvitedPlayers.getLayoutParams().height = (int) ((1.0f - interpolatedTime) * targetHeight);
                                recyclerInvitedPlayers.requestLayout();
                            }

                            @Override
                            public boolean willChangeBounds() {
                                return true;
                            }
                        };
                        animation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                recyclerInvitedPlayers.setVisibility(View.GONE);
                                inviteFragment.setPaddingTop(0);
                                deckFragment.setPaddingTop(0);
                                suitOrderFragment.setPaddingTop(0);
                                cardOrderFragment.setPaddingTop(0);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });

                        animation.setDuration(EXPAND_ANIM_DURATION);
                        recyclerInvitedPlayers.startAnimation(animation);
                    }
                }
                else {
                    if (recyclerInvitedPlayers.getVisibility() == View.GONE) {

                        final float targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());

                        Animation animation = new Animation() {
                            @Override
                            protected void applyTransformation(float interpolatedTime,
                                                               Transformation t) {
                                recyclerInvitedPlayers.getLayoutParams().height = (int) (interpolatedTime * targetHeight);
                                recyclerInvitedPlayers.requestLayout();
                            }

                            @Override
                            public boolean willChangeBounds() {
                                return true;
                            }
                        };
                        animation.setDuration(EXPAND_ANIM_DURATION);
                        int paddingTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());
                        inviteFragment.setPaddingTop(paddingTop);
                        deckFragment.setPaddingTop(paddingTop);
                        suitOrderFragment.setPaddingTop(paddingTop);
                        cardOrderFragment.setPaddingTop(paddingTop);
                        recyclerInvitedPlayers.getLayoutParams().height = 0;
                        recyclerInvitedPlayers.setVisibility(View.VISIBLE);
                        recyclerInvitedPlayers.startAnimation(animation);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        GradientDrawable circleDrawable = (GradientDrawable) getResources().getDrawable(
                R.drawable.floating_button_accent);
        if (circleDrawable != null) {
            circleDrawable.setColor(getResources().getColor(R.color.COLOR_ACCENT));
        }
        playButtonBackground.setImageDrawable(circleDrawable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_match, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.registerConnectionCallbacks(this);
        googleApiClient.registerConnectionFailedListener(this);
        googleApiClient.connect();
        controllerInvitedPlayers.addListener(invitedPlayersListener);
    }

    @Override
    protected void onStop() {
        googleApiClient.unregisterConnectionCallbacks(this);
        googleApiClient.unregisterConnectionFailedListener(this);
        googleApiClient.disconnect();
        controllerInvitedPlayers.removeListener(invitedPlayersListener);
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        refreshInviteList(false);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Error, could not connect", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getNumPlayers() {
        return adapterInvitedPlayers.getItemCount() + 1;
    }

    @Override
    public MatchSettings getMatchSettings() {
        return matchSettings;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallow) {
        viewPager.requestDisallowInterceptTouchEvent(disallow);
    }

    @Override
    public void refreshInviteList(final boolean force) {
        // TODO: Support loading more pages of players
        Games.Players.loadRecentlyPlayedWithPlayers(googleApiClient, 25, force).setResultCallback(
                new ResultCallback<Players.LoadPlayersResult>() {
                    @Override
                    public void onResult(final Players.LoadPlayersResult recentPlayers) {
                        Games.Players.loadInvitablePlayers(googleApiClient, 25, force).setResultCallback(new ResultCallback<Players.LoadPlayersResult>() {
                            @Override
                            public void onResult(Players.LoadPlayersResult allPlayers) {

                                PlayerBuffer recentBuffer = recentPlayers.getPlayers();
                                PlayerBuffer allBuffer = allPlayers.getPlayers();

                                ArrayList<MatchPlayer> recentPlayersList = new ArrayList<>(recentBuffer.getCount());
                                for (Player player : recentBuffer) {
                                    recentPlayersList.add(new MatchPlayer(player.getPlayerId(), player.getDisplayName(),
                                            player.getHiResImageUri()));
                                }


                                ArrayList<MatchPlayer> allPlayersList = new ArrayList<>(allBuffer.getCount());
                                for (Player player : allBuffer) {
                                    allPlayersList.add(new MatchPlayer(player.getPlayerId(), player.getDisplayName(),
                                            player.getHiResImageUri()));
                                }
                                recentBuffer.close();
                                allBuffer.close();
                                recentPlayers.release();
                                allPlayers.release();

                                allPlayersList.removeAll(recentPlayersList);
                                recentPlayersList.addAll(allPlayersList);
                                controllerInvitedPlayers.setAllPlayers(recentPlayersList);
                            }
                        });
                    }
                });
    }

    @Override
    public void addListener(ControllerInvitedPlayers.InvitedPlayersListener listener) {
        controllerInvitedPlayers.addListener(listener);
    }

    @Override
    public void removeListener(ControllerInvitedPlayers.InvitedPlayersListener listener) {
        controllerInvitedPlayers.removeListener(listener);
    }

    @Override
    public ControllerInvitedPlayers getControllerInvitedPlayers() {
        return controllerInvitedPlayers;
    }
}