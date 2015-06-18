package com.winsonchiu.bigtwo;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;


public class LaunchFragment extends Fragment {

    private static final String TAG = LaunchFragment.class.getCanonicalName();
    private static final int SCROLL_ANIMATION_TIME = 150;
    private static final int EXPAND_ANIMATION_TIME = 150;
    private static final int CIRCLE_ANIMATION_TIME = 350;
    private static final int NUM_PAGES = 3;

    private OnFragmentInteractionListener listener;
    private Activity activity;
    private ViewPager viewPager;
    private RecyclerView recyclerMyTurn;
    private RecyclerView recyclerTheirTurn;
    private RecyclerView recyclerFinished;
    private AdapterMatch adapterMyTurn;
    private AdapterMatch adapterTheirTurn;
    private AdapterMatch adapterFinished;
    private ControllerMatches.EventListener listenerMyTurn;
    private ControllerMatches.EventListener listenerTheirTurn;
    private ControllerMatches.EventListener listenerFinished;
    private ImageView newMatchButton;
    private LaunchPagerAdapter viewPagerAdapter;
    private SwipeRefreshLayout layoutRefreshMyTurn;
    private SwipeRefreshLayout layoutRefreshTheirTurn;
    private SwipeRefreshLayout layoutRefreshFinished;
    private TextView emptyText;
    private ImageView newMatchBackground;
    private LinearLayoutManager layoutManagerMyTurn;
    private LinearLayoutManager layoutManagerTheirTurn;
    private LinearLayoutManager layoutManagerFinished;

    public LaunchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_launch, container, false);

        newMatchBackground = (ImageView) view.findViewById(R.id.floating_button_background);
        newMatchButton = (ImageView) view.findViewById(R.id.floating_button);
        newMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final GradientDrawable circleDrawable = (GradientDrawable) getResources().getDrawable(
                        R.drawable.floating_button_accent);

                if (circleDrawable == null) {
                    listener.onNewGameClicked();
                    return;
                }

                final float scale = (float) ((Math.hypot(newMatchBackground.getX(),
                        newMatchBackground.getY()) + newMatchBackground.getWidth()) / newMatchBackground.getWidth() * 2);
                Animation animation = new Animation() {

                    private float pivot;

                    @Override
                    public void initialize(int width,
                                           int height,
                                           int parentWidth,
                                           int parentHeight) {
                        super.initialize(width, height, parentWidth, parentHeight);

                        pivot = resolveSize(RELATIVE_TO_SELF, 0.5f, width, parentWidth);
                    }

                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        float scaleFactor = 1.0f + scale * interpolatedTime;
                        t.getMatrix()
                                .setScale(scaleFactor, scaleFactor, pivot, pivot);
                    }

                    @Override
                    public boolean willChangeBounds() {
                        return true;
                    }

                };

                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        circleDrawable.setColor(getResources().getColor(R.color.COLOR_ACCENT));
                        newMatchBackground.setImageDrawable(circleDrawable);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        listener.onNewGameClicked();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }

                });

                ValueAnimator buttonColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                        getResources().getColor(R.color.COLOR_ACCENT),
                        getResources().getColor(R.color.DARK_THEME_BACKGROUND));
                buttonColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        circleDrawable.setColor((Integer) animation.getAnimatedValue());
                        newMatchBackground.setImageDrawable(circleDrawable);
                    }

                });

                DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();

                animation.setDuration(CIRCLE_ANIMATION_TIME);
                animation.setFillAfter(true);
                buttonColorAnimation.setDuration((long) (CIRCLE_ANIMATION_TIME * 0.9));
                buttonColorAnimation.setInterpolator(decelerateInterpolator);
                animation.setInterpolator(decelerateInterpolator);

                newMatchButton.setVisibility(View.GONE);
                buttonColorAnimation.start();
                newMatchBackground.startAnimation(animation);
            }
        });

        adapterMyTurn = new AdapterMatch(activity, listener.getControllerMatches(), MatchType.MATCH_MY_TURN);
        adapterTheirTurn = new AdapterMatch(activity, listener.getControllerMatches(), MatchType.MATCH_THEIR_TURN);
        adapterFinished = new AdapterMatch(activity, listener.getControllerMatches(), MatchType.MATCH_FINISHED);

        layoutManagerMyTurn = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        recyclerMyTurn = (RecyclerView) view.findViewById(R.id.recycler_my_turn);
        recyclerMyTurn.setHasFixedSize(true);
        recyclerMyTurn.setLayoutManager(layoutManagerMyTurn);
        recyclerMyTurn.setAdapter(adapterMyTurn);
        recyclerMyTurn.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                layoutRefreshMyTurn.setEnabled(
                        layoutManagerMyTurn.findFirstCompletelyVisibleItemPosition() == 0);
            }
        });

        layoutManagerTheirTurn = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        recyclerTheirTurn = (RecyclerView) view.findViewById(R.id.recycler_their_turn);
        recyclerTheirTurn.setHasFixedSize(true);
        recyclerTheirTurn.setLayoutManager(layoutManagerTheirTurn);
        recyclerTheirTurn.setAdapter(adapterTheirTurn);
        recyclerTheirTurn.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                layoutRefreshTheirTurn.setEnabled(
                        layoutManagerTheirTurn.findFirstCompletelyVisibleItemPosition() == 0);
            }
        });

        layoutManagerFinished = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        recyclerFinished = (RecyclerView) view.findViewById(R.id.recycler_finished);
        recyclerFinished.setHasFixedSize(true);
        recyclerFinished.setLayoutManager(layoutManagerFinished);
        recyclerFinished.setAdapter(adapterFinished);
        recyclerFinished.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                layoutRefreshFinished.setEnabled(
                        layoutManagerFinished.findFirstCompletelyVisibleItemPosition() == 0);
            }
        });

        listenerMyTurn = new ControllerMatches.EventListener() {
            @Override
            public MatchType getMatchType() {
                return MatchType.MATCH_MY_TURN;
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                layoutRefreshMyTurn.setRefreshing(refreshing);
            }

            @Override
            public AdapterMatch getAdapter() {
                return adapterMyTurn;
            }

            @Override
            public void updateEmptyText() {
                LaunchFragment.this.updateEmptyText();
            }
        };

        listenerTheirTurn = new ControllerMatches.EventListener() {
            @Override
            public MatchType getMatchType() {
                return MatchType.MATCH_THEIR_TURN;
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                layoutRefreshTheirTurn.setRefreshing(refreshing);
            }

            @Override
            public AdapterMatch getAdapter() {
                return adapterTheirTurn;
            }

            @Override
            public void updateEmptyText() {
                LaunchFragment.this.updateEmptyText();
            }
        };

        listenerFinished = new ControllerMatches.EventListener() {
            @Override
            public MatchType getMatchType() {
                return MatchType.MATCH_FINISHED;
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                layoutRefreshFinished.setRefreshing(refreshing);
            }

            @Override
            public AdapterMatch getAdapter() {
                return adapterFinished;
            }

            @Override
            public void updateEmptyText() {
                LaunchFragment.this.updateEmptyText();
            }
        };

        emptyText = (TextView) view.findViewById(R.id.empty_text);

        if (viewPagerAdapter == null) {
            viewPagerAdapter = new LaunchPagerAdapter();
        }

        viewPager = (ViewPager) view.findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(NUM_PAGES - 1);
        viewPager.setAdapter(viewPagerAdapter);

        PagerSlidingTabStrip tabStrip = (PagerSlidingTabStrip) view.findViewById(R.id.pager_tab_strip);
        tabStrip.setViewPager(viewPager);
        tabStrip.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position,
                                       float positionOffset,
                                       int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                updateEmptyText();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        SwipeRefreshLayout.OnRefreshListener refreshListener
                = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                listener.getControllerMatches().loadMatches();
            }
        };

        layoutRefreshMyTurn = (SwipeRefreshLayout) view.findViewById(R.id.layout_refresh_my_turn);
        layoutRefreshMyTurn.setOnRefreshListener(refreshListener);
        layoutRefreshMyTurn.getViewTreeObserver().addOnScrollChangedListener(
                new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {

                        boolean canScrollUp = false;

                        switch (viewPager.getCurrentItem()) {
                            case 0:
                                canScrollUp = recyclerMyTurn.canScrollVertically(-1);
                                break;
                            case 1:
                                canScrollUp = recyclerTheirTurn.canScrollVertically(-1);
                                break;
                            case 2:
                                canScrollUp = recyclerFinished.canScrollVertically(-1);
                                break;
                        }

                        if (canScrollUp) {
                            layoutRefreshMyTurn.setEnabled(false);
                        }
                        else {
                            layoutRefreshMyTurn.setEnabled(true);
                        }

                    }
                });

        layoutRefreshTheirTurn = (SwipeRefreshLayout) view.findViewById(R.id.layout_refresh_their_turn);
        layoutRefreshTheirTurn.setOnRefreshListener(refreshListener);
        layoutRefreshTheirTurn.getViewTreeObserver().addOnScrollChangedListener(
                new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {

                        boolean canScrollUp = false;

                        switch (viewPager.getCurrentItem()) {
                            case 0:
                                canScrollUp = recyclerMyTurn.canScrollVertically(-1);
                                break;
                            case 1:
                                canScrollUp = recyclerTheirTurn.canScrollVertically(-1);
                                break;
                            case 2:
                                canScrollUp = recyclerFinished.canScrollVertically(-1);
                                break;
                        }

                        if (canScrollUp) {
                            layoutRefreshTheirTurn.setEnabled(false);
                        }
                        else {
                            layoutRefreshMyTurn.setEnabled(true);
                        }

                    }
                });

        layoutRefreshFinished = (SwipeRefreshLayout) view.findViewById(R.id.layout_refresh_finished);
        layoutRefreshFinished.setOnRefreshListener(refreshListener);
        layoutRefreshFinished.getViewTreeObserver().addOnScrollChangedListener(
                new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {

                        boolean canScrollUp = false;

                        switch (viewPager.getCurrentItem()) {
                            case 0:
                                canScrollUp = recyclerMyTurn.canScrollVertically(-1);
                                break;
                            case 1:
                                canScrollUp = recyclerTheirTurn.canScrollVertically(-1);
                                break;
                            case 2:
                                canScrollUp = recyclerFinished.canScrollVertically(-1);
                                break;
                        }

                        if (canScrollUp) {
                            layoutRefreshFinished.setEnabled(false);
                        }
                        else {
                            layoutRefreshMyTurn.setEnabled(true);
                        }

                    }
                });
        viewPager.setCurrentItem(0);

        return view;
    }

    private void updateEmptyText() {

        MatchType matchType = null;
        int textId = -1;
        switch (viewPager.getCurrentItem()) {
            case 0:
                matchType  = MatchType.MATCH_MY_TURN;
                textId = R.string.empty_my_turn_text;
                break;
            case 1:
                matchType  = MatchType.MATCH_THEIR_TURN;
                textId = R.string.empty_their_turn_text;
                break;
            case 2:
                matchType  = MatchType.MATCH_FINISHED;
                textId = R.string.empty_finished_text;
                break;
        }

        if (listener.getControllerMatches().getMatches(matchType).isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(textId);
        }
        else {
            emptyText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;

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
        super.onDetach();
        listener = null;
        activity = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        GradientDrawable circleDrawable = (GradientDrawable) getResources().getDrawable(
                R.drawable.floating_button_accent);
        newMatchBackground.setScaleX(1.0f);
        newMatchBackground.setScaleY(1.0f);
        newMatchBackground.clearAnimation();
        if (circleDrawable != null) {
            circleDrawable.setColor(
                    getResources().getColor(R.color.COLOR_ACCENT));
        }
        newMatchBackground.setImageDrawable(circleDrawable);
        newMatchButton.setVisibility(View.VISIBLE);

        listener.getControllerMatches().addListener(listenerMyTurn);
        listener.getControllerMatches().addListener(listenerTheirTurn);
        listener.getControllerMatches().addListener(listenerFinished);

        listener.getControllerMatches().loadMatches();
    }

    @Override
    public void onPause() {
        listener.getControllerMatches().removeListener(listenerMyTurn);
        listener.getControllerMatches().removeListener(listenerTheirTurn);
        listener.getControllerMatches().removeListener(listenerFinished);
        super.onPause();
    }

    private class LaunchPagerAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            switch (position) {
                case 0:
                    return layoutRefreshMyTurn;
                case 1:
                    return layoutRefreshTheirTurn;
                case 2:
                    return layoutRefreshFinished;
            }

            return super.instantiateItem(container, position);
        }

        @Override
        public CharSequence getPageTitle(int position) {

            switch (position) {
                case 0:
                    return getString(R.string.tab_my_turn);
                case 1:
                    return getString(R.string.tab_their_turn);
                case 2:
                    return getString(R.string.tab_finished);
            }

            return super.getPageTitle(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
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

        void onNewGameClicked();
        ControllerMatches getControllerMatches();
    }

}
