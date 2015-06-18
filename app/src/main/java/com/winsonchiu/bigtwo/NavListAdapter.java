/*
 * Copyright (C) Winson Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.winsonchiu.bigtwo;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class NavListAdapter extends BaseAdapter {

    private static LayoutInflater inflater = null;

    private ArrayList<String> fragmentList;

    private int[] iconImages = new int[]{
            R.drawable.app_icon,
            R.drawable.ic_settings_white_24dp,
            R.drawable.ic_people_white_24dp
    };

    private int colorFilterInt;

    public NavListAdapter(Activity activity, String[] nameArray) {
        fragmentList = new ArrayList<>();
        Collections.addAll(fragmentList, nameArray);
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        colorFilterInt = activity.getResources().getColor(R.color.ICON_COLOR);
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }

    @Override
    public Object getItem(int position) {
        return fragmentList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ImageView fragmentImage;
        TextView fragmentTitle;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_nav, parent, false);

            fragmentImage = (ImageView) convertView.findViewById(R.id.fragment_image);
            fragmentTitle = (TextView) convertView.findViewById(R.id.fragment_title);

            fragmentTitle.setTextColor(colorFilterInt);

            convertView.setTag(new ViewHolder(fragmentImage, fragmentTitle));
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        fragmentImage = viewHolder.fragmentImage;
        fragmentTitle = viewHolder.fragmentTitle;

        Drawable iconDrawable = parent.getContext().getResources().getDrawable(iconImages[position])
                .mutate();
        if (position != 0) {
            iconDrawable.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        }
        fragmentImage.setImageDrawable(iconDrawable);

        fragmentTitle.setText(fragmentList.get(position));

        return convertView;
    }

    private class ViewHolder {

        public final ImageView fragmentImage;

        public final TextView fragmentTitle;

        public ViewHolder(ImageView fragmentImage, TextView fragmentTitle) {
            this.fragmentImage = fragmentImage;
            this.fragmentTitle = fragmentTitle;
        }

    }

}
