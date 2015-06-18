package com.winsonchiu.bigtwo;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.winsonchiu.bigtwo.logic.CardEvaluator;
import com.winsonchiu.bigtwo.logic.GameData;
import com.winsonchiu.bigtwo.logic.GameUtils;
import com.winsonchiu.bigtwo.logic.Turn;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 4/3/2015.
 */
public class GameTurnBased implements ResultCallback {

    private static final String TAG = GameTurnBased.class.getCanonicalName();
    private GoogleApiClient googleApiClient;
    private GameUtils.GameEventListener listener;
    private TurnBasedMatch match;

    public GameTurnBased(GoogleApiClient googleApiClient, GameUtils.GameEventListener listener) {
        this.googleApiClient = googleApiClient;
        this.listener = listener;
    }

    public void startMatch(TurnBasedMatch newMatch, MatchSettings matchSettings) {
        match = newMatch;

        listener.loadGameFragment();

        final GameData gameData = listener.getRenderer().startGame(match.getParticipantIds(), matchSettings,
                match.getParticipantId(Games.Players.getCurrentPlayerId(googleApiClient)));

        setPlayers(match, gameData);

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

        Games.TurnBasedMultiplayer.takeTurn(googleApiClient, match.getMatchId(),
                gameData.toJsonString().getBytes(), firstPlayer).setResultCallback(this);
    }

    public void loadMatch(TurnBasedMatch newMatch) {

        if (newMatch == null || newMatch.getData() == null || newMatch.getStatus() == TurnBasedMatch.MATCH_STATUS_CANCELED ||
                newMatch.getStatus() == TurnBasedMatch.MATCH_STATUS_EXPIRED) {
            listener.toast("Players left, match is now invalid");
            listener.toast("Match data is invalid");
            listener.returnToLaunch();
            return;
        }

        if (match != null && !newMatch.getMatchId().equals(match.getMatchId())) {
            return;
        }

        try {
            GameData gameData = GameData.fromJsonString(new String(newMatch.getData()));

            if (gameData == null) {
                listener.toast("Incompatible versions, unable to load match");
                return;
            }

            listener.loadGameFragment();

            match = newMatch;

            boolean newTurn = gameData.isNewTurn();

            String playerId = match.getParticipantId(Games.Players.getCurrentPlayerId(googleApiClient));

            setPlayers(match, gameData);

            if (match.getStatus() == TurnBasedMatch.MATCH_STATUS_COMPLETE || gameData.isFinished()) {
                listener.setTurn(false);
                setWinners(match, gameData);
                if (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                    Games.TurnBasedMultiplayer.finishMatch(googleApiClient, match.getMatchId());
                }
            }
            else if (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                listener.setTurn(true);

                listener.vibrate(250);

                Turn lastTurn = gameData.getLastTurn();

                if ((lastTurn != null && lastTurn.getPlayer().equals(playerId)) || gameData.getActivePlayers().size() == 0) {
                    gameData.setActivePlayers(gameData.getRemainingActivePlayers());
                    newTurn = true;
                }
            }
            else {
                listener.setTurn(false);
            }
            gameData.setNewTurn(newTurn);
            listener.getRenderer().nextTurn(gameData, playerId);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void skipHand() {
        Log.i(TAG, "skipHand");

        final TurnBasedMatch finalMatch = match;

        if (listener.getRenderer().isNewTurn()) {
            listener.toast("Must play a hand");
            return;
        }

        GameData gameData = listener.getRenderer().getLatestGameData();

        if (gameData == null || finalMatch == null) {
            return;
        }

        String playerId = finalMatch.getParticipantId(
                Games.Players.getCurrentPlayerId(googleApiClient));

        listener.setTurn(false);

        // TODO: Move left check to data class
        for (String player : new ArrayList<>(gameData.getActivePlayers())) {
            if (finalMatch.getParticipant(player).getStatus() == Participant.STATUS_LEFT) {
                gameData.removePlayer(player);
            }
        }

        gameData.playHand(playerId, new ArrayList<Card>());

        Games.TurnBasedMultiplayer.takeTurn(googleApiClient, finalMatch.getMatchId(), gameData.toJsonString().getBytes(), gameData.getNextPlayer()).setResultCallback(
                this);

        listener.removeMyTurnMatch(match);

        listener.toast("Turn skipped");

    }

    public void playHand() {

        if (!hasMatch()) {
            return;
        }

        Log.i(TAG, "onPlayHand");

        final TurnBasedMatch finalMatch = match;

        if (listener.getRenderer().getReadyHand().isEmpty()) {
            skipHand();
            return;
        }


        GameData gameData = listener.getRenderer().getLatestGameData();

        for (String player : new ArrayList<>(gameData.getActivePlayers())) {
            if (finalMatch.getParticipant(player).getStatus() == Participant.STATUS_LEFT) {
                gameData.removePlayer(player);
            }
        }

        String playerId = finalMatch.getParticipantId(
                Games.Players.getCurrentPlayerId(googleApiClient));
        gameData = listener.getRenderer().playHand(playerId);

        if (gameData == null) {
            return;
        }

        listener.setTurn(false);

        byte[] dataBytes = gameData.toJsonString().getBytes();

        if (gameData.getRemainingActivePlayers().size() == 1) {

            final GameData finalGameData = gameData;
            Games.TurnBasedMultiplayer.takeTurn(googleApiClient, finalMatch.getMatchId(), dataBytes,
                    playerId).setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                @Override
                public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
                    List<String> winners = finalGameData.getFinishedPlayers();

                    setPlayers(finalMatch, finalGameData);

                    ArrayList<ParticipantResult> results = new ArrayList<>(winners.size());

                    for (int position = 0; position < winners.size(); position++) {
                        results.add(new ParticipantResult(winners.get(position), ParticipantResult.MATCH_RESULT_WIN, position + 1));
                    }

                    ArrayList<String> allPlayers = finalMatch.getParticipantIds();
                    allPlayers.removeAll(winners);
                    for (int position = 0; position < allPlayers.size(); position++) {
                        results.add(new ParticipantResult(allPlayers.get(position), ParticipantResult.MATCH_RESULT_LOSS, ParticipantResult.PLACING_UNINITIALIZED));
                    }

                    Games.TurnBasedMultiplayer.finishMatch(googleApiClient, finalMatch.getMatchId(), finalGameData.toJsonString().getBytes(), results).setResultCallback(
                            new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                                @Override
                                public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
                                    try {
                                        TurnBasedMatch newMatch = updateMatchResult.getMatch();
                                        if (newMatch == null) {
                                            return;
                                        }
                                        setWinners(newMatch, GameData.fromJsonString(
                                                new String(newMatch.getData())));
                                    }
                                    catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                }
            });
        }
        else {
            Games.TurnBasedMultiplayer.takeTurn(googleApiClient, finalMatch.getMatchId(), dataBytes, gameData.getNextPlayer()).setResultCallback(
                    this);
        }
        listener.removeMyTurnMatch(match);
    }

    private void setPlayers(TurnBasedMatch match, GameData gameData) {
        listener.setPlayers(GameUtils.getPlayers(match), gameData);
    }

    private void setWinners(TurnBasedMatch match, GameData gameData) {
        listener.setWinners(GameUtils.getWinners(match, gameData), gameData);
    }

    @Override
    public void onResult(Result result) {

        if (!listener.isGameFragmentLoaded()) {
            return;
        }

        if (result instanceof TurnBasedMultiplayer.UpdateMatchResult) {
            loadMatch(((TurnBasedMultiplayer.UpdateMatchResult) result).getMatch());
        }
        else if (result instanceof TurnBasedMultiplayer.LoadMatchResult) {
            loadMatch(((TurnBasedMultiplayer.LoadMatchResult) result).getMatch());
        }
        else if (result instanceof TurnBasedMultiplayer.InitiateMatchResult) {
            loadMatch(((TurnBasedMultiplayer.InitiateMatchResult) result).getMatch());
        }
    }

    public void onConnected(Bundle bundle) {
        if (match != null) {
            loadMatch(match);
        }
        else if (bundle != null) {
            TurnBasedMatch newMatch = bundle.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);
            if (newMatch == null) {
                Object object = bundle.getParcelable(Multiplayer.EXTRA_INVITATION);
                if (object instanceof  TurnBasedMatch) {
                    newMatch = bundle.getParcelable(Multiplayer.EXTRA_INVITATION);
                }
            }
            if (newMatch != null) {
                loadMatch(newMatch);
            }
        }
    }

    public void onIconClick(String playerId) {
        if (match == null) {
            return;
        }

        listener.toast(match.getParticipant(playerId)
                .getDisplayName());
    }

    public void clearMatch() {
        match = null;
        Log.d(TAG, "clearMatch");
    }

    public boolean hasMatch() {
        return match != null;
    }

    public TurnBasedMatch getMatch() {
        return match;
    }

    public void create(ArrayList<String> invitedPlayers, final MatchSettings matchSettings) {

        final TurnBasedMatchConfig matchConfig = TurnBasedMatchConfig.builder()
                .addInvitedPlayers(invitedPlayers)
                .build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (googleApiClient.blockingConnect().isSuccess()) {
                    listener.postOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.loadGameFragment();

                            Games.TurnBasedMultiplayer.createMatch(googleApiClient, matchConfig).setResultCallback(
                                    new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
                                        @Override
                                        public void onResult(
                                                TurnBasedMultiplayer.InitiateMatchResult initiateMatchResult) {

                                            Status status = initiateMatchResult.getStatus();
                                            if (!status.isSuccess()) {
                                                listener.toast("Error creating match:\n" + status.getStatusCode());
                                                return;
                                            }
                                            startMatch(initiateMatchResult.getMatch(), matchSettings);
                                        }
                                    });
                        }
                    });
                }
            }
        }).start();
    }
}