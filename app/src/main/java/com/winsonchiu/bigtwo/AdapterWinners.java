package com.winsonchiu.bigtwo;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.images.ImageManager;

import java.util.ArrayList;

/**
 * Created by TheKeeperOfPie on 4/14/2015.
 */
public class AdapterWinners extends RecyclerView.Adapter<AdapterWinners.ViewHolder> {

    private ImageManager imageManager;
    private ArrayList<MatchPlayer> players;

    public AdapterWinners(Activity activity) {
        imageManager = ImageManager.create(activity);
        players = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_winner, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        MatchPlayer player = players.get(position);
        imageManager.loadImage(holder.winnerIcon, player.getIconUri(), R.drawable.app_icon);
        holder.winnerName.setText(player.getName());
        holder.winnerPlace.setText("#" + (position + 1));
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    public void setPlayers(ArrayList<MatchPlayer> players) {
        this.players = players;
        notifyDataSetChanged();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        protected final ImageView winnerIcon;
        protected final TextView winnerName;
        protected final TextView winnerPlace;

        public ViewHolder(View itemView) {
            super(itemView);

            winnerIcon = (ImageView) itemView.findViewById(R.id.winner_icon);
            winnerName = (TextView) itemView.findViewById(R.id.winner_name);
            winnerPlace = (TextView) itemView.findViewById(R.id.winner_place);
        }
    }

}
