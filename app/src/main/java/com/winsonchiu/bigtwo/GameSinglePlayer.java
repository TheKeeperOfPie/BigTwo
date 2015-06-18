package com.winsonchiu.bigtwo;

import android.net.Uri;
import android.util.Log;

import com.winsonchiu.bigtwo.logic.ArtificialPlayer;
import com.winsonchiu.bigtwo.logic.CardEvaluator;
import com.winsonchiu.bigtwo.logic.GameData;
import com.winsonchiu.bigtwo.logic.GameUtils;
import com.winsonchiu.bigtwo.logic.Turn;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 4/6/2015.
 */
public class GameSinglePlayer {

    private static final String PLAYER_ID = "0";
    private static final String TAG = GameSinglePlayer.class.getCanonicalName();
    private boolean isPlaying;
    private GameUtils.GameEventListener listener;
    private ArrayList<MatchPlayer> players;
    private String playerName;
    private Uri playerIcon;

    public GameSinglePlayer(String playerName, Uri playerIcon, GameUtils.GameEventListener listener) {
        this.playerName = playerName;
        this.playerIcon = playerIcon;
        this.listener = listener;
        players = new ArrayList<>();
    }

    public void startMatch(MatchSettings matchSettings) {
        listener.loadGameFragment();
        isPlaying = true;

        ArrayList<String> participants = new ArrayList<>();
        players = new ArrayList<>();
        for (int index = 0; index < matchSettings.getNumAI() + 1; index ++) {
            participants.add(String.valueOf(index));
        }

        for (String id : participants) {
            if (PLAYER_ID.equals(id)) {
                players.add(new MatchPlayer(id, playerName, playerIcon));
            }
            else {
                // TODO: Add AI names and icon URIs
                players.add(new MatchPlayer(id, id, null));
            }
        }

        final GameData gameData = listener.getRenderer().startGame(participants, matchSettings, PLAYER_ID);

        listener.setPlayers(players, gameData);

        String firstPlayer = null;
        Card card = null;

        CardEvaluator cardEvaluator = new CardEvaluator(matchSettings);

        for (String player : gameData.getActivePlayers()) {

            if (card == null) {
                firstPlayer = player;
                card = gameData.getHand(player).get(0);
            }
            else {
                Card nextCard = gameData.getHand(player).get(0);

                if (cardEvaluator.compareCards(nextCard, card) < 0) {
                    card = nextCard;
                    firstPlayer = player;
                }
            }

        }

        gameData.setNextPlayer(firstPlayer);

        processTurn(gameData);
    }

    private void processTurn(final GameData gameData) {

        if (gameData.getVersion() != GameData.CURRENT_VERSION) {
            listener.toast("Incompatible versions, unable to load match");
            return;
        }

        listener.loadGameFragment();

        boolean newTurn = false;

        String nextPlayer = gameData.getNextPlayer();

        Log.d(TAG, "activePlayers: " + gameData.getActivePlayers());
        Log.d(TAG, "nextPlayer: " + nextPlayer);

        listener.setPlayers(players, gameData);

        if (gameData.isFinished()) {
            listener.setTurn(false);
            listener.setWinners(players, gameData);
        }
        if (nextPlayer.equals(PLAYER_ID) && gameData.getRemainingActivePlayers().size() != 1) {
            listener.setTurn(true);
            listener.vibrate(250);

            Turn lastTurn = gameData.getLastTurn();

            if ((lastTurn != null && lastTurn.getPlayer().equals(PLAYER_ID)) || gameData.getActivePlayers().size() == 0) {
                gameData.setActivePlayers(gameData.getRemainingActivePlayers());
                newTurn = true;
            }
        }
        else {
            listener.setTurn(false);
        }
        gameData.setNewTurn(newTurn);
        listener.getRenderer().nextTurn(gameData, PLAYER_ID);

        if (!nextPlayer.equals(PLAYER_ID) && !gameData.isFinished()) {

            boolean forceTurn = false;

            Turn lastTurn = gameData.getLastTurn();
            if ((lastTurn != null && lastTurn.getPlayer().equals(nextPlayer)) || gameData.getActivePlayers().size() == 0) {
                gameData.setActivePlayers(gameData.getRemainingActivePlayers());
                forceTurn = true;
            }

            ArtificialPlayer artificialPlayer = new ArtificialPlayer(gameData.getHand(nextPlayer), gameData.getSettings());
            List<Card> nextHand = artificialPlayer.takeTurn(forceTurn ? new ArrayList<Card>() : gameData.getLastHand());

            if (nextHand != null && !nextHand.isEmpty()) {
                gameData.playHand(nextPlayer, nextHand);
            }
            else {
                gameData.playHand(nextPlayer, new ArrayList<Card>());
            }

            listener.postDelayedOnSurfaceView(new Runnable() {
                @Override
                public void run() {
                    processTurn(gameData);
                }
            }, 1500);
        }

    }

    public void skipHand() {
        Log.i(TAG, "skipHand");

        if (listener.getRenderer().isNewTurn()) {
            listener.toast("Must play a hand");
            return;
        }

        GameData gameData = listener.getRenderer().getLatestGameData();

        if (gameData == null) {
            return;
        }

        listener.setTurn(false);
        gameData.playHand(PLAYER_ID, new ArrayList<Card>());

        processTurn(gameData);

        listener.toast("Turn skipped");

    }


    public void playHand() {

        if (!isPlaying) {
            return;
        }

        Log.i(TAG, "onPlayHand");

        if (listener.getRenderer().getReadyHand().isEmpty()) {
            skipHand();
            return;
        }

        final GameData gameData = listener.getRenderer().playHand(PLAYER_ID);

        if (gameData == null) {
            return;
        }

        listener.setTurn(false);

        processTurn(gameData);
    }

    public void clearMatch() {
        isPlaying = false;
    }
}
