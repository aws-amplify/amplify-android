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

package com.amplifyframework.devmenu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.amplifyframework.core.R;

/**
 * A {@link Fragment} subclass representing the main view for
 * the developer menu.
 */
public final class DevMenuMainFragment extends Fragment {

    /**
     * Required empty public constructor.
     */
    public DevMenuMainFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mainView = inflater.inflate(R.layout.dev_menu_fragment_main, container, false);

        // Set on-click listeners for each button on the main view of the developer menu.
        mainView.findViewById(R.id.env_button).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.show_env_info));
        mainView.findViewById(R.id.device_button).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.show_device_info));
        mainView.findViewById(R.id.logs_button).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.show_logs));
        mainView.findViewById(R.id.file_issue_button).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.show_file_issue));

        return mainView;
    }
}
