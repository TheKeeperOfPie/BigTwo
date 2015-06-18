package com.winsonchiu.bigtwo.turn;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.GridLayout;
import android.widget.ImageView;

import com.winsonchiu.bigtwo.MatchSettings;
import com.winsonchiu.bigtwo.R;
import com.winsonchiu.bigtwo.Suit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.winsonchiu.bigtwo.turn.SuitOrderFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link com.winsonchiu.bigtwo.turn.SuitOrderFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SuitOrderFragment extends Fragment implements View.OnTouchListener {

    private static final String TAG = SuitOrderFragment.class.getCanonicalName();

    private OnFragmentInteractionListener listener;
    private GridLayout suitLayout;
    private Context context;
    private int downIndex = -1;
    private List<Suit> suitOrder;
    private View parentContainer;
    private int paddingTop;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NewMatchFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SuitOrderFragment newInstance() {
        SuitOrderFragment fragment = new SuitOrderFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SuitOrderFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        suitOrder = new ArrayList<>();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        parentContainer = inflater.inflate(R.layout.fragment_suit_order, container, false);

        parentContainer.setPadding(parentContainer.getPaddingLeft(), paddingTop, parentContainer.getPaddingRight(), parentContainer.getPaddingBottom());

        suitLayout = (GridLayout) parentContainer.findViewById(R.id.suit_layout);

        List<Suit> suits = Arrays.asList(Suit.values());

        for (Suit suit : suits) {

            View cardView = inflater.inflate(R.layout.card_view, suitLayout, false);
            ImageView cardImage = (ImageView) cardView.findViewById(R.id.card_image);

            cardView.setTag(suit);
            cardImage.setImageResource(suit.getDrawable());

            suitLayout.addView(cardView);
        }

        suitLayout.setOnTouchListener(this);
        suitLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {

                        suitLayout.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);

                        int width = suitLayout.getWidth() / 4;
                        int height = (int) (width * 3.5 / 2.5);

                        for (int index = 0; index < suitLayout.getChildCount(); index++) {
                            GridLayout.LayoutParams layoutParams
                                    = (GridLayout.LayoutParams) suitLayout.getChildAt(index)
                                    .getLayoutParams();
                            layoutParams.width = width;
                            layoutParams.height = height;
                            suitLayout.getChildAt(index)
                                    .setLayoutParams(layoutParams);
                        }

                        Log.i(TAG, "card width: " + width + " card height: " + height);
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

//        if (suitLayout.getClipBounds().contains((int) event.getX(), (int) event.getY())) {
            onTouchLayout(suitLayout, event);
//        }

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
                suitOrder.clear();
                for (int index = 0; index < layout.getChildCount(); index++) {
                    suitOrder.add((Suit) layout.getChildAt(index).getTag());
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

    public List<Suit> getSuitOrder() {

        if (suitOrder == null) {
            suitOrder = new ArrayList<>();
        }

        if (isVisible()) {
            suitOrder.clear();
            for (int index = 0; index < suitLayout.getChildCount(); index++) {
                suitOrder.add((Suit) suitLayout.getChildAt(index).getTag());
            }
        }

        if (suitOrder.size() == 0) {
            suitOrder = Arrays.asList(Suit.values());
        }

        return suitOrder;
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