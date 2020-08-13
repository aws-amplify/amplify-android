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
import android.widget.EditText;
import android.widget.TextSwitcher;
import androidx.fragment.app.Fragment;

import com.amplifyframework.core.R;

/**
 * A {@link Fragment} subclass representing the view
 * to display the logs on the developer menu.
 */
public final class DevMenuLogsFragment extends Fragment {

    /**
     * Required empty public constructor.
     */
    public DevMenuLogsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View logsView = inflater.inflate(R.layout.dev_menu_fragment_logs, container, false);
        // Display the logs (if any).
        TextSwitcher logsText = logsView.findViewById(R.id.logs_text);
        DeveloperMenu developerMenu = DeveloperMenu.singletonInstance(getContext());
        logsText.setText(developerMenu.getLogs());
        // Search the logs when the search button is pressed.
        logsView.findViewById(R.id.search_logs_button).setOnClickListener(view -> {
            EditText searchText = logsView.findViewById(R.id.search_logs_field);
            searchText.clearFocus();
            logsText.setText(getString(R.string.placeholder_logs));
            logsText.setText(developerMenu.getFilteredLogs(searchText.getText().toString()));
        });
        return logsView;
    }
}
