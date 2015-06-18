package com.winsonchiu.bigtwo;

import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.images.ImageManager;
import com.winsonchiu.bigtwo.logic.GameData;
import com.winsonchiu.bigtwo.logic.Turn;

import java.util.ArrayList;
import java.util.List;

public class GameFragment extends Fragment {

    private static final String TAG = GameFragment.class.getCanonicalName();

    private OnFragmentInteractionListener listener;
    private Activity activity;
    private LinearLayout layoutPlayer;
    private RelativeLayout layoutTutorial;
    private LinearLayout layoutWinners;
    private RecyclerView recyclerWinners;
    private AdapterWinners adapterWinners;
    private Menu toolbarMenu;
    private TextView winnerTitle;
    private TextView lastPlayerText;
    private RecyclerView recyclerHistory;

    private List<MatchPlayer> currentParticipants;
    private View buttonWinners;
    private AdapterHistory adapterHistory;
    private List<MatchPlayer> matchPlayers;
    private ArrayList<MatchPlayer> winners;

    public GameFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentParticipants = new ArrayList<>();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_game, container, false);
        layoutPlayer = (LinearLayout) view.findViewById(R.id.player_layout);
        layoutTutorial = (RelativeLayout) view.findViewById(R.id.tutorial_layout);
        layoutWinners = (LinearLayout) view.findViewById(R.id.layout_winners);

        view.findViewById(R.id.tutorial_done_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutTutorial.setVisibility(View.GONE);
                AppSettings.resetFirstGame(false);
            }
        });

        if (AppSettings.isFirstGame()) {
            layoutTutorial.setVisibility(View.VISIBLE);
        }

        lastPlayerText = (TextView) view.findViewById(R.id.last_player_text);

        buttonWinners = view.findViewById(R.id.winner_button);
        buttonWinners.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutPlayer.setVisibility(View.INVISIBLE);
                layoutWinners.setVisibility(View.VISIBLE);
                buttonWinners.setVisibility(View.GONE);
            }
        });

        adapterWinners = adapterWinners != null ? adapterWinners : new AdapterWinners(activity);
        recyclerWinners = (RecyclerView) view.findViewById(R.id.recycler_winners);
        recyclerWinners.setLayoutManager(new LinearLayoutManager(activity));
        recyclerWinners.setHasFixedSize(true);
        recyclerWinners.addItemDecoration(new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST));
        recyclerWinners.setAdapter(adapterWinners);

        winnerTitle = (TextView) view.findViewById(R.id.winner_title);

        adapterHistory = adapterHistory != null ? adapterHistory : new AdapterHistory(activity);
        recyclerHistory = (RecyclerView) view.findViewById(R.id.recycler_history);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(activity));
        recyclerHistory.setHasFixedSize(true);
        recyclerHistory.addItemDecoration(new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST));
        recyclerHistory.setAdapter(adapterHistory);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_game, menu);
        super.onCreateOptionsMenu(menu, inflater);
        toolbarMenu = menu;

        int colorFilterInt = getResources().getColor(R.color.ICON_COLOR);

        menu.findItem(R.id.item_leave)
                .getIcon()
                .setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);

        menu.findItem(R.id.item_help)
                .getIcon()
                .setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.item_leave:
                listener.onLeaveClick();
                break;
            case R.id.item_help:
                layoutTutorial.setVisibility(
                        layoutTutorial.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (matchPlayers != null) {
            setPlayers(matchPlayers, listener.getRenderer().getLatestGameData());
        }
        if (winners != null) {
            setWinners(winners, listener.getRenderer().getLatestGameData());
        }
    }

    public void setPlayer(Turn newTurn) {

        boolean skipped = false;
        if (newTurn.getHand().isEmpty()) {
            skipped = true;
        }

        for (int index = 0; index < layoutPlayer.getChildCount(); index++) {

            View view = layoutPlayer.getChildAt(index);
            if (view.getTag().equals(newTurn.getPlayer())) {
                ImageView playerImage = (ImageView) view.findViewById(R.id.player_image);
                TextView playerCardText = (TextView) view.findViewById(R.id.player_card_text);

                if (skipped) {
                    playerImage.setColorFilter(getResources().getColor(R.color.DARK_GRAY_TRANSPARENT), PorterDuff.Mode.MULTIPLY);
                }
                else {
                    playerImage.clearColorFilter();
                    playerCardText.setText("" + (Integer.parseInt(playerCardText.getText()
                            .toString()) - newTurn.getHand()
                            .size()));
                }
                break;
            }

        }

    }


    public boolean setPlayers(List<MatchPlayer> matchPlayers, GameData gameData) {

        if (layoutPlayer == null || gameData == null || !isAdded()) {
            this.matchPlayers = matchPlayers;
            return false;
        }

        this.matchPlayers = null;

        if (gameData.isNewTurn()) {
            lastPlayerText.setText("");
        }
        else {
            for (MatchPlayer player : matchPlayers) {
                if (player.getId().equals(gameData.getLastPlayer())) {
                    lastPlayerText.setText(player.getName());
                    break;
                }
            }
        }

        ImageManager imageManager = ImageManager.create(activity);

        // TODO: Move to ViewHolder pattern

        if (!currentParticipants.containsAll(matchPlayers)
            || currentParticipants.size() != matchPlayers.size()) {

            layoutPlayer.removeAllViews();
            layoutPlayer.removeAllViewsInLayout();

            for (MatchPlayer player : matchPlayers) {

                View playerIcon = View.inflate(activity, R.layout.player_icon, null);
                ImageView playerImage = (ImageView) playerIcon.findViewById(R.id.player_image);
                TextView playerCardText = (TextView) playerIcon.findViewById(R.id.player_card_text);

                playerIcon.setTag(player.getId());

                playerIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onPlayerIconClick((String) v.getTag());
                    }
                });

                imageManager.loadImage(playerImage, player.getIconUri(), R.drawable.app_icon);

                playerCardText.setText("" + gameData.getHand(player.getId()).size());

                layoutPlayer.addView(playerIcon, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                        1.0f));

            }
            currentParticipants = matchPlayers;
        }

        String pendingPlayerId = gameData.getNextPlayer();
        int highlightColor = getResources().getColor(R.color.PLAYER_HIGHLIGHT);
        int baseColor = getResources().getColor(R.color.PLAYER_BASE);

        for (int index = 0; index < layoutPlayer.getChildCount(); index++) {

            String playerId = matchPlayers.get(index).getId();

            View playerIcon = layoutPlayer.getChildAt(index);

            ImageView playerImage = (ImageView) playerIcon.findViewById(R.id.player_image);

            if (gameData.getActivePlayers().contains(playerId)) {
                playerImage.clearColorFilter();
            }
            else {
                playerImage.setColorFilter(getResources().getColor(R.color.DARK_GRAY_TRANSPARENT), PorterDuff.Mode.MULTIPLY);
            }


            TextView playerCardText = (TextView) playerIcon.findViewById(R.id.player_card_text);

            if (!gameData.getRemainingPlayers().contains(playerId)) {
                playerCardText.setText("LEFT");
            }
            else {
                playerCardText.setText(
                        gameData.getHand(playerId) != null ? "" + gameData.getHand(playerId).size()
                                : "");
            }
            playerCardText
                    .setTextColor(playerId.equals(pendingPlayerId) ? highlightColor : baseColor);

        }

        layoutPlayer.invalidate();
        layoutPlayer.setVisibility(View.VISIBLE);
        layoutWinners.setVisibility(View.GONE);

        // TODO: Add GameData check for match complete
        buttonWinners.setVisibility(View.GONE);

        return true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        try {
            listener = (OnFragmentInteractionListener) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                                         + " must implement OnFragmentInteractionListener");
        }
        listener.setSurfaceVisibility(View.VISIBLE);
    }

    @Override
    public void onDetach() {
        listener.setSurfaceVisibility(View.INVISIBLE);
        listener = null;
        activity = null;
        super.onDetach();
    }

    public void setWinners(ArrayList<MatchPlayer> winners, GameData gameData) {

        if (!isAdded()) {
            this.winners = winners;
            return;
        }

        this.winners = null;
        adapterHistory.setData(gameData.getTurns(), winners);
        adapterWinners.setPlayers(winners);
        buttonWinners.setVisibility(View.VISIBLE);

    }

    public boolean onBackPressed() {

        if (!isAdded()) {
            return false;
        }

        if (recyclerWinners.isShown()) {
            layoutPlayer.setVisibility(View.VISIBLE);
            layoutWinners.setVisibility(View.GONE);
            buttonWinners.setVisibility(View.VISIBLE);
            return true;
        }

        return false;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {

        CustomRenderer getRenderer();
        void onPlayerIconClick(String playerId);
        void setSurfaceVisibility(int visibility);
        void onLeaveClick();
    }

}