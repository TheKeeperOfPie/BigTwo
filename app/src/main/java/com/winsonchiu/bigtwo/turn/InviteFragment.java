package com.winsonchiu.bigtwo.turn;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.winsonchiu.bigtwo.MatchPlayer;
import com.winsonchiu.bigtwo.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InviteFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link InviteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InviteFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private InviteListAdapter inviteListAdapter;
    private SwipeRefreshLayout inviteRefreshLayout;
    private Activity activity;
    private ControllerInvitedPlayers.InvitedPlayersListener invitedPlayersListener;
    private RecyclerView inviteList;
    private int invitePaddingTop;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment InviteFragment.
     */
    public static InviteFragment newInstance() {
        InviteFragment fragment = new InviteFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public InviteFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_invite, container, false);

        inviteRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.layout_refresh_invite);
        inviteRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mListener.refreshInviteList(true);
            }
        });
        if (inviteListAdapter == null || inviteListAdapter.getItemCount() == 0) {
            inviteRefreshLayout.setRefreshing(true);
        }

        if (inviteListAdapter == null) {
            inviteListAdapter = new InviteListAdapter(activity,
                    mListener.getControllerInvitedPlayers());
        }
        if (invitedPlayersListener == null) {
            invitedPlayersListener = new ControllerInvitedPlayers.InvitedPlayersListener() {
                @Override
                public void notifyInvitedAdded(MatchPlayer player) {
                    inviteListAdapter.notifyChanged(player);
                }

                @Override
                public void notifyInvitedRemoved(MatchPlayer player, int position) {
                    inviteListAdapter.notifyChanged(player);
                }

                @Override
                public void notifyAllPlayersChanged() {
                    inviteListAdapter.notifyDataSetChanged();
                    inviteRefreshLayout.setRefreshing(false);
                }
            };
        }


        inviteList = (RecyclerView) view.findViewById(R.id.recycler_invite);
        inviteList.setHasFixedSize(true);
        inviteList.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        inviteList.setAdapter(inviteListAdapter);
        setPaddingTop(invitePaddingTop);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_invite, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        Drawable searchDrawable = getResources().getDrawable(R.drawable.ic_search_white_24dp);
        int colorFilterInt = getResources().getColor(R.color.ICON_COLOR);
        searchDrawable.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        searchItem.setIcon(searchDrawable);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mListener.getControllerInvitedPlayers().resetFilter();
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mListener.getControllerInvitedPlayers().setFilter(newText);
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
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
        activity = null;
        mListener = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.addListener(invitedPlayersListener);
    }

    @Override
    public void onPause() {
        mListener.removeListener(invitedPlayersListener);
        super.onPause();
    }

    public void setPaddingTop(int padding) {
        invitePaddingTop = padding;
        inviteList.setPadding(inviteList.getPaddingLeft(), padding, inviteList.getPaddingRight(), inviteList.getPaddingBottom());
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
        void refreshInviteList(boolean force);
        void addListener(ControllerInvitedPlayers.InvitedPlayersListener listener);
        void removeListener(ControllerInvitedPlayers.InvitedPlayersListener listener);
        ControllerInvitedPlayers getControllerInvitedPlayers();
    }

}