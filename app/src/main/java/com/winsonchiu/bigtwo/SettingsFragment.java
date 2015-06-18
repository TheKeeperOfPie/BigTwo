package com.winsonchiu.bigtwo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsFragment.class.getCanonicalName();

    private static final int BACKGROUND_IMAGE_REQUEST_CODE = 1;
    private static final int CARD_IMAGE_REQUEST_CODE = 2;

    private OnFragmentInteractionListener mListener;

    private Context context;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_settings);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
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
        context = null;
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case BACKGROUND_IMAGE_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    File file = new File(getPathFromURI(data.getData()));
                    AppSettings.setBackgroundImage(file);
                    mListener.onChangeBackgroundImage();
                }
                else {
                    ((SwitchPreference) findPreference("use_background_image")).setChecked(false);
                }
                break;

            case CARD_IMAGE_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    File file = new File(getPathFromURI(data.getData()));
                    AppSettings.setCardImage(file);
                    mListener.onChangeCardImage();
                }
                else {
                    ((SwitchPreference) findPreference("use_card_image")).setChecked(false);
                }
                break;
        }

    }

    private String getPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(context, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "use_background_image":
                if (sharedPreferences.getBoolean(key, false)) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("file/*");
                        startActivityForResult(intent, BACKGROUND_IMAGE_REQUEST_CODE);
                    }
                    catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Activity not found to get image", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case "use_card_image":
                if (sharedPreferences.getBoolean(key, false)) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("file/*");
                        startActivityForResult(intent, CARD_IMAGE_REQUEST_CODE);
                    }
                    catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Activity not found to get image", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
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

        void onChangeBackgroundImage();

        void onChangeCardImage();
    }

}
