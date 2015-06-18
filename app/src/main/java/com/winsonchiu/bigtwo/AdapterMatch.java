package com.winsonchiu.bigtwo;

import android.app.Activity;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.images.ImageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 4/1/2015.
 */
public class AdapterMatch extends RecyclerView.Adapter<AdapterMatch.ViewHolder> {

    private ImageManager imageManager;
    private ControllerMatches controllerMatches;
    private MatchType matchType;

    public AdapterMatch(Activity activity, ControllerMatches controllerMatches, MatchType matchType) {
        this.imageManager = ImageManager.create(activity);
        this.controllerMatches = controllerMatches;
        this.matchType = matchType;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_match, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        final MatchItem matchItem = controllerMatches.getMatches(matchType).get(position);
        final MatchSettings matchSettings = matchItem.getMatchSettings();

        List<String> playerNames = new ArrayList<>();
        for (MatchPlayer player : matchItem.getPlayers()) {
            playerNames.add(player.getName());
        }

        holder.textTitle.setText(matchItem.getDescription());

        holder.buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controllerMatches.onPlayClick(matchType, holder.getAdapterPosition());
            }
        });

        holder.buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controllerMatches.onDeleteClick(matchType, holder.getAdapterPosition());
            }
        });

        holder.buttonLeft.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        holder.buttonRight.setImageResource(R.drawable.ic_delete_white_24dp);

        switch (matchItem.getType()) {
            case MATCH_MY_TURN:
            case MATCH_THEIR_TURN:
            case MATCH_FINISHED:
                List<Integer> cardOrder = matchSettings.getCardOrder();
                holder.textRules.setText(
                        "Players: " + playerNames.toString() + "\n\n" +
                        "Suit Order: " + matchSettings.getSuitOrder() + "\n\n" +
                        "Card Order: " + cardOrderToReadableString(cardOrder) + "\n\n" +
                        "# of Cards: " + matchSettings.getNumCards() + "\n\n" +
                        "# of Decks: " + matchSettings.getNumDecks() + "\n\n" +
                        "Straight Order: " + (matchSettings.isUseCardOrderStraight() ? "" + (Card.getReadableValue(cardOrder.get(
                        0)) + " to " + Card.getReadableValue(cardOrder.get(cardOrder.size() - 1))) : "A to A"));
                break;
            case INVITE_REAL_TIME:
            case INVITE_TURN_BASED:
                holder.textRules.setText(
                        "Players: " + playerNames.toString());

                holder.buttonLeft.setImageResource(R.drawable.ic_close_white_24dp);
                holder.buttonRight.setImageResource(R.drawable.ic_check_white_24dp);

                holder.buttonLeft.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        controllerMatches.onClickInvitation(matchType, holder.getAdapterPosition(), false);
                    }
                });

                holder.buttonRight.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        controllerMatches.onClickInvitation(matchType, holder.getAdapterPosition(), true);
                    }
                });
                break;

        }


        List<MatchPlayer> players = matchItem.getPlayers();
        if (matchItem.isCreatorCurrentPlayer()) {
            players.remove(new MatchPlayer(matchItem.getCreatorId(), "", null));
            imageManager.loadImage(holder.imagePlayer, players.get(0).getIconUri(), R.drawable.app_icon);
        }
        else {
            Uri creatorImageUri = null;
            for (MatchPlayer player : players) {
                if (player.getId()
                        .equals(matchItem.getCreatorId())) {
                    creatorImageUri = player.getIconUri();
                    break;
                }
            }

            if (creatorImageUri == null) {
                imageManager.loadImage(holder.imagePlayer, matchItem.getPlayers()
                        .get(0)
                        .getIconUri(),
                        R.drawable.app_icon);
            }
            else {
                imageManager.loadImage(holder.imagePlayer, creatorImageUri, R.drawable.app_icon);
            }
        }

    }

    public String cardOrderToReadableString(List<Integer> cardOrder) {

        ArrayList<String> cards = new ArrayList<>(cardOrder.size());
        for (int value : cardOrder) {
            cards.add(Card.getReadableValue(value));
        }

        return cards.toString();
    }

    @Override
    public int getItemCount() {
        return controllerMatches.getMatches(matchType).size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        protected final ImageView imagePlayer;
        protected final TextView textTitle;
        protected final TextView textRules;
        protected final ImageButton buttonLeft;
        protected final ImageButton buttonRight;
        protected final LinearLayout layoutExpand;

        public ViewHolder(View itemView) {
            super(itemView);
            imagePlayer = (ImageView) itemView.findViewById(R.id.image_player);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textRules = (TextView) itemView.findViewById(R.id.text_rules);
            buttonLeft = (ImageButton) itemView.findViewById(R.id.button_left);
            buttonRight = (ImageButton) itemView.findViewById(R.id.button_right);
            layoutExpand = (LinearLayout) itemView.findViewById(R.id.layout_expand);

            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (layoutExpand.getVisibility() == View.VISIBLE) {
                        layoutExpand.setVisibility(View.GONE);
                    }
                    else {
                        layoutExpand.setVisibility(View.VISIBLE);
                    }
                }
            };

            this.itemView.setOnClickListener(clickListener);
            imagePlayer.setOnClickListener(clickListener);
            textTitle.setOnClickListener(clickListener);
        }

    }

}
