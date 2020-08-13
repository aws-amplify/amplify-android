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
    // The view for this fragment.
    private View logsView;
    // An instance of DeveloperMenu.
    private DeveloperMenu developerMenu;
    // Displays the logs text.
    private TextSwitcher logsText;

    /**
     * Required empty public constructor.
     */
    public DevMenuLogsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        logsView = inflater.inflate(R.layout.dev_menu_fragment_logs, container, false);
        // Display the logs (if any).
        developerMenu = DeveloperMenu.singletonInstance(getContext());
        logsText = logsView.findViewById(R.id.logs_text);
        logsText.setText(developerMenu.getLogs());
        // Search the logs when the search button is pressed.
        logsView.findViewById(R.id.search_logs_button).setOnClickListener(view -> searchLogs());
        return logsView;
    }

    /**
     * Display the logs (if any) that contain the search query.
     */
    private void searchLogs() {
        /*
        // Uncomment this block of code to hide the soft input keyboard
        // when the search button is pressed.
        Context context = getContext();
        if (context != null) {
            // Hide the soft input keyboard.
            InputMethodManager inputMethodManager = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(logsView.getWindowToken(), 0);
        }
         */
        EditText searchText = logsView.findViewById(R.id.search_logs_field);
        searchText.clearFocus();
        logsText.setText(getString(R.string.placeholder_logs));
        logsText.setText(developerMenu.getFilteredLogs(searchText.getText().toString()));
    }
}
