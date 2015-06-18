package com.winsonchiu.bigtwo.turn;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.GridLayout;
import android.widget.ImageView;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.winsonchiu.bigtwo.Card;
import com.winsonchiu.bigtwo.MatchSettings;
import com.winsonchiu.bigtwo.R;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CardOrderFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CardOrderFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CardOrderFragment extends Fragment implements View.OnTouchListener {

    private static final String TAG = CardOrderFragment.class.getCanonicalName();

    private OnFragmentInteractionListener listener;
    private GridLayout cardLayout;
    private Context context;
    private int downIndex = -1;
    private TurnBasedMatch match;
    private TurnBasedMatchConfig config;
    private MatchSettings matchSettings;
    private List<Integer> cardOrder;
    private View parentContainer;
    private int paddingTop;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NewMatchFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CardOrderFragment newInstance() {
        CardOrderFragment fragment = new CardOrderFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public CardOrderFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cardOrder = new ArrayList<>(Card.NUM_CARDS);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        parentContainer = inflater.inflate(R.layout.fragment_card_order, container, false);
        parentContainer.setPadding(parentContainer.getPaddingLeft(), paddingTop, parentContainer.getPaddingRight(), parentContainer.getPaddingBottom());

        cardLayout = (GridLayout) parentContainer.findViewById(R.id.card_layout);

        int[] drawableIds = new int[]{
                R.drawable.s00,
                R.drawable.s01,
                R.drawable.s02,
                R.drawable.s03,
                R.drawable.s04,
                R.drawable.s05,
                R.drawable.s06,
                R.drawable.s07,
                R.drawable.s08,
                R.drawable.s09,
                R.drawable.s10,
                R.drawable.s11,
                R.drawable.s12,
        };

        ArrayList<Integer> indexes = new ArrayList<>();
        if (matchSettings == null) {
            for (int index = 2; index < drawableIds.length + 2; index++) {
                indexes.add(index % drawableIds.length);
            }
        }
        else {
            indexes.addAll(matchSettings.getCardOrder());
        }

        for (int index : indexes) {
            View cardView = inflater.inflate(R.layout.card_view, cardLayout, false);
            ImageView cardImage = (ImageView) cardView.findViewById(R.id.card_image);

            cardView.setTag(index);
            cardImage.setImageResource(drawableIds[index]);
            cardLayout.addView(cardView);
        }

        cardLayout.setOnTouchListener(this);
        cardLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        cardLayout.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);

                        int width = cardLayout.getWidth() / 5;
                        int height = cardLayout.getHeight() / 3;

                        for (int index = 0; index < cardLayout.getChildCount(); index++) {
                            GridLayout.LayoutParams layoutParams
                                    = (GridLayout.LayoutParams) cardLayout.getChildAt(index)
                                    .getLayoutParams();
                            layoutParams.width = width;
                            layoutParams.height = height;
                            cardLayout.getChildAt(index)
                                    .setLayoutParams(layoutParams);
                        }

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
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        try {
            listener = (OnFragmentInteractionListener) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                                         + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        context = null;
        super.onDetach();
        listener = null;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        onTouchLayout(cardLayout, event);
        return true;
    }

    private void onTouchLayout(ViewGroup layout, MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downIndex = calculateSuitListTouchIndex(layout, event);
                listener.requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                int newIndex = calculateSuitListTouchIndex(layout, event);
                if (newIndex != downIndex && downIndex >= 0 && newIndex >= 0) {

                    int largerIndex = newIndex > downIndex ? newIndex : downIndex;
                    int smallerIndex = newIndex < downIndex ? newIndex : downIndex;

                    ArrayList<View> childViews = new ArrayList<>();

                    for (int index = 0; index < layout.getChildCount(); index++) {
                        childViews.add(layout.getChildAt(index));
                    }

                    View tempView = childViews.get(largerIndex);
                    childViews.set(largerIndex, childViews.get(smallerIndex));
                    childViews.set(smallerIndex, tempView);

                    layout.removeAllViews();

                    for (View child : childViews) {
                        layout.addView(child);
                    }

                    downIndex = newIndex;
                }
                break;
            case MotionEvent.ACTION_UP:
                downIndex = -1;
                cardOrder.clear();
                for (int index = 0; index < layout.getChildCount(); index++) {
                    cardOrder.add((Integer) layout.getChildAt(index).getTag());
                }
                listener.requestDisallowInterceptTouchEvent(false);
                break;
        }
    }

    private int calculateSuitListTouchIndex(ViewGroup layout, MotionEvent event) {
        Rect boundRect = new Rect();
        int childCount = layout.getChildCount();
        int[] listViewCoords = new int[2];
        layout.getLocationOnScreen(listViewCoords);
        int x = (int) event.getRawX() - listViewCoords[0];
        int y = (int) event.getRawY() - listViewCoords[1];
        View child;
        for (int i = 0; i < childCount; i++) {
            child = layout.getChildAt(i);
            child.getHitRect(boundRect);
            if (boundRect.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    public List<Integer> getCardOrder() {

        if (cardOrder == null) {
            cardOrder = new ArrayList<>(Card.NUM_CARDS);
        }

        if (isVisible()) {
            cardOrder.clear();
            for (int index = 0; index < cardLayout.getChildCount(); index++) {
                cardOrder.add((Integer) cardLayout.getChildAt(index).getTag());
            }
        }

        if (cardOrder.size() == 0) {
            cardOrder.add(2);
            cardOrder.add(3);
            cardOrder.add(4);
            cardOrder.add(5);
            cardOrder.add(6);
            cardOrder.add(7);
            cardOrder.add(8);
            cardOrder.add(9);
            cardOrder.add(10);
            cardOrder.add(11);
            cardOrder.add(12);
            cardOrder.add(0);
            cardOrder.add(1);
        }

        return cardOrder;
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
        MatchSettings getMatchSettings();
        void requestDisallowInterceptTouchEvent(boolean disallow);
    }
}