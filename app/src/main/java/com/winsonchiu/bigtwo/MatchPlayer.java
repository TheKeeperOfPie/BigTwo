package com.winsonchiu.bigtwo;

import android.net.Uri;

/**
 * Created by TheKeeperOfPie on 2/7/2015.
 */
public class MatchPlayer {

    private String id;
    private String name;
    private Uri iconUri;

    public MatchPlayer(String id, String name, Uri iconUri) {
        this.id = id;
        this.name = name;
        this.iconUri = iconUri;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Uri getIconUri() {
        return iconUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MatchPlayer player = (MatchPlayer) o;

        return !(getId() != null ? !getId().equals(player.getId()) : player.getId() != null);

    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MatchPlayer{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", iconUri=" + iconUri +
                '}';
    }
}
