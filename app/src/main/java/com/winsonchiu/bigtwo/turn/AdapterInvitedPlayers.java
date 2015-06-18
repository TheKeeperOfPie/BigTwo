package com.winsonchiu.bigtwo.turn;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.images.ImageManager;
import com.winsonchiu.bigtwo.MatchPlayer;
import com.winsonchiu.bigtwo.R;

/**
 * Created by TheKeeperOfPie on 3/6/2015.
 */
public class AdapterInvitedPlayers extends RecyclerView.Adapter<AdapterInvitedPlayers.ViewHolder>{

    private ImageManager imageManager;
    private ControllerInvitedPlayers controllerInvitedPlayers;

    public AdapterInvitedPlayers(Activity activity, ControllerInvitedPlayers controllerInvitedPlayers) {
        this.imageManager = ImageManager.create(activity);
        this.controllerInvitedPlayers = controllerInvitedPlayers;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_invited, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        MatchPlayer player = controllerInvitedPlayers.getInvited(position);
        imageManager.loadImage(holder.playerIcon, player.getIconUri(), R.drawable.app_icon);
        holder.playerName.setText(player.getName());
    }

    @Override
    public int getItemCount() {
        return controllerInvitedPlayers.sizeInvited();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        protected ImageView playerIcon;
        protected TextView playerName;

        public ViewHolder(View itemView) {
            super(itemView);
            playerIcon = (ImageView) itemView.findViewById(R.id.player_icon);
            playerName = (TextView) itemView.findViewById(R.id.player_name);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    controllerInvitedPlayers.removePlayer(getAdapterPosition());
                }
            });
        }
    }

    public interface OnInvitedClickListener {
        void onInvitedClicked(MatchPlayer player);
    }

}