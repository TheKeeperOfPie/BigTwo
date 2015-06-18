package com.winsonchiu.bigtwo;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.images.ImageManager;
import com.winsonchiu.bigtwo.logic.Turn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by TheKeeperOfPie on 3/30/2015.
 */
public class AdapterHistory extends RecyclerView.Adapter<AdapterHistory.ViewHolder> {

    private ImageManager imageManager;
    private List<Turn> listTurns;
    private Map<String, MatchPlayer> listPlayers;

    public AdapterHistory(Activity activity) {
        this.imageManager = ImageManager.create(activity);
        this.listTurns = new ArrayList<>();
        this.listPlayers = new HashMap<>();
    }

    public void setData(List<Turn> turns, ArrayList<MatchPlayer> players) {
        this.listTurns = turns;

        Map<String, MatchPlayer> matchPlayerMap = new HashMap<>();
        for (MatchPlayer matchPlayer : players) {
            matchPlayerMap.put(matchPlayer.getId(), matchPlayer);
        }

        this.listPlayers = matchPlayerMap;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_history, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Turn turn = listTurns.get(position);
        MatchPlayer player = listPlayers.get(turn.getPlayer());
        if (player == null) {
            // TODO: Add all players to winner list, remove null check
            return;
        }

        imageManager.loadImage(holder.imagePlayer, player.getIconUri(), R.drawable.app_icon);

        holder.textPlayer.setText(player.getName());
        holder.textHand.setText(turn.getHand().toString());
    }

    @Override
    public int getItemCount() {
        return listTurns.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        protected ImageView imagePlayer;
        protected TextView textPlayer;
        protected TextView textHand;

        public ViewHolder(View itemView) {
            super(itemView);

            this.imagePlayer = (ImageView) itemView.findViewById(R.id.image_player);
            this.textPlayer = (TextView) itemView.findViewById(R.id.text_player);
            this.textHand = (TextView) itemView.findViewById(R.id.text_hand);

        }
    }

}
