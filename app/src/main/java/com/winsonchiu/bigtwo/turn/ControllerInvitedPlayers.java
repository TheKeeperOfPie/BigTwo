package com.winsonchiu.bigtwo.turn;

import com.winsonchiu.bigtwo.MatchPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by TheKeeperOfPie on 3/23/2015.
 */
public class ControllerInvitedPlayers {

    private List<MatchPlayer> allPlayers;
    private List<MatchPlayer> visiblePlayers;
    private List<MatchPlayer> invitedPlayers;
    private List<InvitedPlayersListener> listeners;

    public ControllerInvitedPlayers() {
        super();
        listeners = new ArrayList<>();
        allPlayers = new ArrayList<>();
        visiblePlayers = new ArrayList<>();
        invitedPlayers = new ArrayList<>();
    }

    public void setAllPlayers(List<MatchPlayer> players) {
        this.allPlayers = players;
        this.visiblePlayers = allPlayers;
        for (InvitedPlayersListener listener : listeners) {
            listener.notifyAllPlayersChanged();
        }
    }

    public void addListener(InvitedPlayersListener listener) {
        listeners.add(listener);
    }

    public void removeListener(InvitedPlayersListener listener) {
        listeners.remove(listener);
    }

    public boolean addPlayer(MatchPlayer player) {

        if (invitedPlayers.contains(player)) {
            return false;
        }

        invitedPlayers.add(player);
        for (InvitedPlayersListener listener : listeners) {
            listener.notifyInvitedAdded(player);
        }
        return true;
    }

    public void removePlayer(int position) {
        if (position < 0 || position >= invitedPlayers.size()) {
            return;
        }
        MatchPlayer player = invitedPlayers.get(position);
        invitedPlayers.remove(position);
        for (InvitedPlayersListener listener : listeners) {
            listener.notifyInvitedRemoved(player, position);
        }
    }

    public boolean contains(MatchPlayer player) {
        return invitedPlayers.contains(player);
    }

    public MatchPlayer getVisible(int position) {
        return visiblePlayers.get(position);
    }

    public MatchPlayer getInvited(int position) {
        return invitedPlayers.get(position);
    }

    public int sizeVisible() {
        return visiblePlayers.size();
    }

    public int sizeInvited() {
        return invitedPlayers.size();
    }

    public void setFilter(String query) {

        visiblePlayers = new ArrayList<>();

        Pattern pattern = Pattern.compile(".*" + query + "+.*", Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

        for (MatchPlayer player : allPlayers) {
            if (pattern.matcher(player.getName()).find()) {
                visiblePlayers.add(player);
            }
        }

        for (InvitedPlayersListener listener : listeners) {
            listener.notifyAllPlayersChanged();
        }

    }

    public ArrayList<String> getInvitedPlayerIds() {

        ArrayList<String> playerIds = new ArrayList<>();

        for (MatchPlayer player : invitedPlayers) {
            playerIds.add(player.getId());
        }

        return playerIds;
    }

    public int indexOfVisible(MatchPlayer player) {
        return visiblePlayers.indexOf(player);
    }

    public int indexOfInvited(MatchPlayer player) {
        return invitedPlayers.indexOf(player);
    }

    public void resetFilter() {
        visiblePlayers = allPlayers;
    }

    public interface InvitedPlayersListener {
        void notifyInvitedAdded(MatchPlayer player);
        void notifyInvitedRemoved(MatchPlayer player, int position);
        void notifyAllPlayersChanged();
    }

}
