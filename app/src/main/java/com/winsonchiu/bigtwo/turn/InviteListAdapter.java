package com.winsonchiu.bigtwo.turn;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.images.ImageManager;
import com.winsonchiu.bigtwo.MatchPlayer;
import com.winsonchiu.bigtwo.R;

/**
 * Created by TheKeeperOfPie on 2/26/2015.
 */
public class InviteListAdapter extends RecyclerView.Adapter<InviteListAdapter.ViewHolder> {

    private static final String TAG = InviteListAdapter.class.getCanonicalName();
    private ImageManager imageManager;
    private ControllerInvitedPlayers controllerInvitedPlayers;

    public InviteListAdapter(Activity activity, ControllerInvitedPlayers controllerInvitedPlayers) {
        this.controllerInvitedPlayers = controllerInvitedPlayers;
        imageManager = ImageManager.create(activity);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_invite, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {

        MatchPlayer player = controllerInvitedPlayers.getVisible(i);
        viewHolder.playerName.setText(player.getName());
        if (controllerInvitedPlayers.contains(player)) {
            viewHolder.playerCheckBox.setChecked(true);
        }
        else {
            viewHolder.playerCheckBox.setChecked(false);
        }
        imageManager.loadImage(viewHolder.playerIcon, player.getIconUri(), R.drawable.app_icon);

    }

    @Override
    public int getItemCount() {
        return controllerInvitedPlayers.sizeVisible();
    }

    public void notifyChanged(MatchPlayer player) {
        int index = controllerInvitedPlayers.indexOfVisible(player);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener {

        protected ImageView playerIcon;
        protected TextView playerName;
        protected CheckBox playerCheckBox;

        public ViewHolder(View itemView) {
            super(itemView);
            playerIcon = (ImageView) itemView.findViewById(R.id.player_icon);
            playerName = (TextView) itemView.findViewById(R.id.player_name);
            playerCheckBox = (CheckBox) itemView.findViewById(R.id.player_check_box);
            playerCheckBox.setOnCheckedChangeListener(this);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            MatchPlayer player = controllerInvitedPlayers.getVisible(getAdapterPosition());
            if (isChecked) {
                controllerInvitedPlayers.addPlayer(player);
            }
            else {
                controllerInvitedPlayers.removePlayer(controllerInvitedPlayers.indexOfInvited(controllerInvitedPlayers.getVisible(getAdapterPosition())));
            }
        }
    }
}