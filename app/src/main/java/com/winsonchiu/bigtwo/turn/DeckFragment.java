package com.winsonchiu.bigtwo.turn;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.winsonchiu.bigtwo.Card;
import com.winsonchiu.bigtwo.MatchSettings;
import com.winsonchiu.bigtwo.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeckFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DeckFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DeckFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private EditText numCardsEditText;
    private EditText numDecksEditText;
    private int numDecks = 1;
    private int numCards = 26;
    private View parentContainer;
    private int paddingTop;
    private boolean useCardOrderStraight;
    private boolean isRealTime;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DeckFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DeckFragment newInstance() {
        DeckFragment fragment = new DeckFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public DeckFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        parentContainer = inflater.inflate(R.layout.fragment_deck, container, false);

        numCardsEditText = (EditText) parentContainer.findViewById(R.id.edit_text_num_cards);
        numCardsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString()
                        .length() > 0) {
                    numCards = Integer.parseInt(s.toString());
                    int maxCards = numDecks * Card.DECK_SIZE / mListener.getNumPlayers();
                    if (numCards > maxCards) {
                        numCardsEditText.setText("" + maxCards);
                    }
                }
            }
        });

        parentContainer.findViewById(R.id.image_num_cards_increment).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int maxCards = numDecks * Card.DECK_SIZE / mListener.getNumPlayers();
                        if (numCards < maxCards) {
                            numCardsEditText.setText("" + ++numCards);
                        }
                    }
                });

        parentContainer.findViewById(R.id.image_num_cards_decrement).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (numCards > 1) {
                            numCardsEditText.setText("" + --numCards);
                        }
                    }
                });

        numDecksEditText = (EditText) parentContainer.findViewById(R.id.edit_text_num_decks);
        numDecksEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString()
                        .length() > 0) {
                    numDecks = Integer.parseInt(s.toString());
                    int maxCards = numDecks * Card.DECK_SIZE / mListener.getNumPlayers();
                    if (numCards > maxCards) {
                        numCardsEditText.setText("" + maxCards);
                    }
                }
            }
        });

        parentContainer.findViewById(R.id.image_num_decks_increment).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        numDecksEditText.setText("" + (numDecks + 1));
                    }
                });

        parentContainer.findViewById(R.id.image_num_decks_decrement).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (numDecks > 1) {
                            numDecksEditText.setText("" + --numDecks);
                        }
                    }
                });

        ((CheckBox) parentContainer.findViewById(R.id.check_box_card_order_straight)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        useCardOrderStraight = isChecked;
                    }
                });

        ((CheckBox) parentContainer.findViewById(R.id.check_box_real_time)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        isRealTime = isChecked;
                    }
                });

        return parentContainer;
    }

    public void setPaddingTop(int padding) {
        paddingTop = padding;
        if (parentContainer != null) {
            parentContainer.setPadding(parentContainer.getPaddingLeft(), paddingTop,
                    parentContainer.getPaddingRight(), parentContainer.getPaddingBottom());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                                         + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        onPlayersChanged();
    }

    public void onPlayersChanged() {

        numCards = numDecks * Card.DECK_SIZE / (mListener.getNumPlayers() == 2 ? 3 : mListener.getNumPlayers());

        numCardsEditText.setText("" + numCards);
        numDecksEditText.setText("" + numDecks);
    }

    public int getNumDecks() {
        return numDecks;
    }

    public int getNumCards() {
        return numCards;
    }

    public boolean useCardOrderStraight() {
        return useCardOrderStraight;
    }

    public boolean isRealTime() {
        return isRealTime;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        int getNumPlayers();

        MatchSettings getMatchSettings();
    }

}