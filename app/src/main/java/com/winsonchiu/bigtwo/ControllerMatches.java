package com.winsonchiu.bigtwo;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationBuffer;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.turnbased.LoadMatchesResponse;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchBuffer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.winsonchiu.bigtwo.logic.GameData;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 4/1/2015.
 */
public class ControllerMatches {

    private GoogleApiClient googleApiClient;
    private MatchListener matchListener;
    private List<EventListener> listeners;
    private List<MatchItem> listMyTurnMatches;
    private List<MatchItem> listTheirTurnMatches;
    private List<MatchItem> listFinishedMatches;

    public ControllerMatches(GoogleApiClient googleApiClient, MatchListener matchListener) {
        this.matchListener = matchListener;
        this.googleApiClient = googleApiClient;
        listeners = new ArrayList<>();
        listMyTurnMatches = new ArrayList<>();
        listTheirTurnMatches = new ArrayList<>();
        listFinishedMatches = new ArrayList<>();
    }

    public List<MatchItem> getMatches(MatchType type) {

        switch (type) {
            case MATCH_MY_TURN:
                return listMyTurnMatches;
            case MATCH_THEIR_TURN:
                return listTheirTurnMatches;
            case MATCH_FINISHED:
                return listFinishedMatches;
        }

        throw new IllegalArgumentException();

    }

    public void loadMatches() {

        for (EventListener listener : listeners) {
            listener.setRefreshing(true);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (!googleApiClient.blockingConnect().isSuccess()) {
                    return;
                }

                final String playerId = Games.Players.getCurrentPlayerId(googleApiClient);

                Games.TurnBasedMultiplayer
                        .loadMatchesByStatus(googleApiClient, Multiplayer.SORT_ORDER_MOST_RECENT_FIRST, new int[]{
                                TurnBasedMatch.MATCH_TURN_STATUS_INVITED,
                                TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN,
                                TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN,
                                TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE
                        }).setResultCallback(new ResultCallback<TurnBasedMultiplayer.LoadMatchesResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.LoadMatchesResult loadMatchesResult) {

                        LoadMatchesResponse matchesResponse = loadMatchesResult.getMatches();

                        ArrayList<MatchItem> myTurnMatches = new ArrayList<>();
                        ArrayList<MatchItem> theirTurnMatches = new ArrayList<>();
                        ArrayList<MatchItem> finishedMatches = new ArrayList<>();

                        InvitationBuffer invitationBuffer = matchesResponse.getInvitations();
                        if (invitationBuffer != null) {
                            for (Invitation invitation : invitationBuffer) {
                                myTurnMatches.add(getMatchItem(invitation));
                            }
                            invitationBuffer.release();
                        }

                        TurnBasedMatchBuffer myTurnMatchBuffer = matchesResponse.getMyTurnMatches();
                        if (myTurnMatchBuffer != null) {
                            for (TurnBasedMatch match : myTurnMatchBuffer) {
                                myTurnMatches.add(getMatchItem(match, playerId));
                            }
                            myTurnMatchBuffer.release();
                        }
                        listMyTurnMatches = myTurnMatches;

                        TurnBasedMatchBuffer theirTurnMatchBuffer = matchesResponse.getTheirTurnMatches();
                        if (theirTurnMatchBuffer != null) {
                            for (TurnBasedMatch match : theirTurnMatchBuffer) {
                                theirTurnMatches.add(getMatchItem(match, playerId));
                            }
                            theirTurnMatchBuffer.release();
                        }
                        listTheirTurnMatches = theirTurnMatches;

                        TurnBasedMatchBuffer finishedMatchBuffer = matchesResponse.getCompletedMatches();
                        if (finishedMatchBuffer != null) {
                            for (TurnBasedMatch match : finishedMatchBuffer) {
                                finishedMatches.add(getMatchItem(match, playerId));
                            }
                            finishedMatchBuffer.release();
                        }
                        listFinishedMatches = finishedMatches;

                        for (EventListener listener : listeners) {
                            listener.setRefreshing(false);
                            listener.getAdapter()
                                    .notifyDataSetChanged();
                            listener.updateEmptyText();
                        }
                    }
                });
            }
        }).start();
    }


    private MatchItem getMatchItem(Invitation invitation) {

        MatchType matchType = invitation.getInvitationType() == Invitation.INVITATION_TYPE_REAL_TIME ? MatchType.INVITE_REAL_TIME : MatchType.INVITE_TURN_BASED;

        MatchItem matchItem = new MatchItem(invitation.getInvitationId(),
                invitation.getInviter().getDisplayName(), matchType);

        ArrayList<MatchPlayer> players = new ArrayList<>();

        for (Participant participant : invitation.getParticipants()) {
            MatchPlayer matchPlayer = new MatchPlayer(participant.getParticipantId(), participant.getDisplayName(), participant.getHiResImageUri());
            players.add(matchPlayer);
        }

        matchItem.setCreatorId(invitation.getInviter()
                .getParticipantId());
        matchItem.setPlayers(players);

        return matchItem;
    }

    private MatchItem getMatchItem(TurnBasedMatch match, String playerId) {

        MatchType matchType = MatchType.MATCH_FINISHED;

        switch (match.getTurnStatus()) {
            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                matchType = MatchType.MATCH_MY_TURN;
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                matchType = MatchType.MATCH_THEIR_TURN;
                break;
        }

        MatchItem matchItem = new MatchItem(match.getMatchId(), match.getDescription(), matchType);

        ArrayList<MatchPlayer> players = new ArrayList<>();

        for (Participant participant : match.getParticipants()) {
            MatchPlayer matchPlayer = new MatchPlayer(participant.getParticipantId(), participant.getDisplayName(), participant.getHiResImageUri());
            players.add(matchPlayer);
        }

        matchItem.setCreatorId(match.getCreatorId());
        matchItem.setIsCreatorCurrentPlayer(match.getCreatorId()
                .equals(match.getParticipantId(playerId)));
        matchItem.setPlayers(players);

        try {
            JSONObject jsonObject = new JSONObject(new String(match.getData()));
            if (jsonObject.has(GameData.MATCH_SETTINGS)) {
                matchItem.setMatchSettings(MatchSettings.fromJsonObject(
                        new JSONObject(jsonObject.getString(GameData.MATCH_SETTINGS))));
            }
            else {
                matchItem.setMatchSettings(MatchSettings.getDefaultSettings());
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
            matchItem.setMatchSettings(MatchSettings.getDefaultSettings());
        }

        return matchItem;
    }

    public void addInvitation(Invitation invitation) {
        listMyTurnMatches.add(getMatchItem(invitation));
    }

    public void addMatch(TurnBasedMatch turnBasedMatch) {
        String playerId = Games.Players.getCurrentPlayerId(googleApiClient);
        MatchType matchType;

        if (turnBasedMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            listMyTurnMatches.add(getMatchItem(turnBasedMatch, playerId));
            matchType = MatchType.MATCH_MY_TURN;
        }
        else if (turnBasedMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN) {
            listTheirTurnMatches.add(getMatchItem(turnBasedMatch, playerId));
            matchType = MatchType.MATCH_THEIR_TURN;
        }
        else {
            listFinishedMatches.add(getMatchItem(turnBasedMatch, playerId));
            matchType = MatchType.MATCH_FINISHED;
        }

        for (EventListener listener : listeners) {
            if (listener.getMatchType() == matchType) {
                listener.getAdapter().notifyItemInserted(0);
                listener.updateEmptyText();
            }
        }
    }

    public void removeMatch(String matchId) {
        MatchItem matchToRemove = new MatchItem(matchId, "", MatchType.MATCH_FINISHED);
        int index = listMyTurnMatches.indexOf(matchToRemove);
        if (index >= 0) {
            listMyTurnMatches.remove(matchToRemove);
            for (EventListener listener : listeners) {
                if (listener.getMatchType() == MatchType.MATCH_MY_TURN) {
                    listener.getAdapter().notifyItemRemoved(index);
                    listener.updateEmptyText();
                }
            }
        }
        index = listTheirTurnMatches.indexOf(matchToRemove);
        if (index >= 0) {
            listTheirTurnMatches.remove(matchToRemove);
            for (EventListener listener : listeners) {
                if (listener.getMatchType() == MatchType.MATCH_THEIR_TURN) {
                    listener.getAdapter().notifyItemRemoved(index);
                    listener.updateEmptyText();
                }
            }
        }
        index = listFinishedMatches.indexOf(matchToRemove);
        if (index >= 0) {
            listFinishedMatches.remove(matchToRemove);
            for (EventListener listener : listeners) {
                if (listener.getMatchType() == MatchType.MATCH_FINISHED) {
                    listener.getAdapter().notifyItemRemoved(index);
                    listener.updateEmptyText();
                }
            }
        }
    }

    public void onPlayClick(MatchType matchType, int position) {
        matchListener.onPlayClick(getMatches(matchType).get(position).getId());
    }

    public void onDeleteClick(MatchType matchType, int position) {
        if (matchType == MatchType.MATCH_FINISHED) {
            matchListener.onDismissClick(getMatches(matchType).get(position)
                    .getId());
        }
        else {
            matchListener.onLeaveClick(getMatches(matchType).get(position)
                    .getId());
        }
    }

    public void onClickInvitation(MatchType matchType, int position, boolean accept) {
        matchListener.onClickInvitation(getMatches(matchType).get(position), accept);
    }

    public void addListener(EventListener eventListener) {
        listeners.add(eventListener);
    }

    public void removeListener(EventListener eventListener) {
        listeners.remove(eventListener);
    }

    public interface EventListener {

        MatchType getMatchType();
        void setRefreshing(boolean refreshing);
        AdapterMatch getAdapter();
        void updateEmptyText();
    }

    public interface MatchListener {
        void onPlayClick(String matchId);
        void onLeaveClick(String matchId);
        void onDismissClick(String matchId);
        void onClickInvitation(MatchItem matchItem, boolean accept);
    }

}
