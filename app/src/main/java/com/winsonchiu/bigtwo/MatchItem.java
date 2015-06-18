package com.winsonchiu.bigtwo;

import java.util.List;

/**
 * Created by TheKeeperOfPie on 1/24/2015.
 */
public class MatchItem {

    private static final String TAG = MatchItem.class.getCanonicalName();
    private String matchId;
    private String description;
    private MatchType type;
    private String creatorId;
    private List<MatchPlayer> matchPlayers;
    private MatchSettings matchSettings;
    private boolean isCreatorCurrentPlayer;

    public MatchItem(String matchId, String description, MatchType type) {

        this.matchId = matchId;
        this.description = description;
        this.type = type;
    }

    public MatchType getType() {
        return type;
    }

    public String getId() {
        return matchId;
    }

    public String getDescription() {
        return description;
    }

    public MatchSettings getMatchSettings() {
        return matchSettings;
    }

    public void setMatchSettings(MatchSettings matchSettings) {
        this.matchSettings = matchSettings;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public List<MatchPlayer> getPlayers() {
        return matchPlayers;
    }

    public void setPlayers(List<MatchPlayer> players) {
        this.matchPlayers = players;
    }

    public boolean isCreatorCurrentPlayer() {
        return isCreatorCurrentPlayer;
    }

    public void setIsCreatorCurrentPlayer(boolean isCreatorCurrentPlayer) {
        this.isCreatorCurrentPlayer = isCreatorCurrentPlayer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MatchItem matchItem = (MatchItem) o;

        return !(getId() != null ? !getId().equals(matchItem.getId()) :
                matchItem.getId() != null);

    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
}
