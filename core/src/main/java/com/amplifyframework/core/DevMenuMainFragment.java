/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.core;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

/**
 * A {@link Fragment} subclass representing the main view for
 * the developer menu.
 */
public final class DevMenuMainFragment extends Fragment {
    // Text displayed in the action bar.
    private TextView titleText;

    /**
     * Required empty public constructor.
     */
    public DevMenuMainFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mainView = inflater.inflate(R.layout.fragment_main, container, false);

        titleText = getActivity().findViewById(R.id.toolbar_title);
        titleText.setText(R.string.menu_title);

        // Set on-click listeners for each button on the main view of the developer menu.
        mainView.findViewById(R.id.env_button).setOnClickListener(view -> hideMainView(view,
                R.id.show_env_info, R.string.env_view_title));
        mainView.findViewById(R.id.device_button).setOnClickListener(view -> hideMainView(view,
                R.id.show_device_info, R.string.device_view_title));
        mainView.findViewById(R.id.logs_button).setOnClickListener(view -> hideMainView(view,
                R.id.show_logs, R.string.logs_view_title));
        mainView.findViewById(R.id.file_issue_button).setOnClickListener(view -> hideMainView(view,
                R.id.show_file_issue, R.string.file_issue_view_title));

        return mainView;
    }

    /**
     * Hides the main developer menu view, displays the view with the given
     * ID, and changes the title text on the action bar.
     * @param view View to navigate away from.
     * @param newViewId ID of the new view to display.
     * @param titleTextId ID of the string resource for the action bar title text.
     */
    private void hideMainView(View view, int newViewId, int titleTextId) {
        Navigation.findNavController(view).navigate(newViewId);
        titleText.setText(titleTextId);
    }
}
