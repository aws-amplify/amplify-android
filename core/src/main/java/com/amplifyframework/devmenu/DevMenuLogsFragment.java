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
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.fragment.app.Fragment;

import com.amplifyframework.core.R;
import com.amplifyframework.logging.LogLevel;

/**
 * A {@link Fragment} subclass representing the view
 * to display the logs on the developer menu.
 */
public final class DevMenuLogsFragment extends Fragment {
    // An instance of DeveloperMenu.
    private DeveloperMenu developerMenu;
    // The query entered in the search box.
    private String searchQuery;
    // LogLevel currently selected in the log level menu.
    private LogLevel selectedLogLevel;
    // Resource ID of the log level menu item selected.
    @RawRes
    private int logLevelMenuId;
    // The TextView where the logs are displayed.
    private TextView logsText;

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
        developerMenu = DeveloperMenu.singletonInstance(getContext());
        logsText = logsView.findViewById(R.id.logs_text);
        logsText.setText(developerMenu.getLogs());
        // Search the logs when a search query is entered.
        SearchView searchLogsView = logsView.findViewById(R.id.search_logs_field);
        searchLogsView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String searchText) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String searchText) {
                searchQuery = searchText;
                displayFilteredLogs();
                return true;
            }
        });
        logLevelMenuId = R.id.all_logs;
        Button filterLogs = logsView.findViewById(R.id.filter_logs);
        registerForContextMenu(filterLogs);
        filterLogs.setOnClickListener(view -> requireActivity().openContextMenu(view));
        return logsView;
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View view,
                                    @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        requireActivity().getMenuInflater().inflate(R.menu.dev_menu_logs_menu, menu);
        menu.findItem(logLevelMenuId).setChecked(true);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.all_logs) {
            selectedLogLevel = null;
        } else if (itemId == R.id.error_logs) {
            selectedLogLevel = LogLevel.ERROR;
        } else if (itemId == R.id.warn_logs) {
            selectedLogLevel = LogLevel.WARN;
        } else if (itemId == R.id.info_logs) {
            selectedLogLevel = LogLevel.INFO;
        } else if (itemId == R.id.debug_logs) {
            selectedLogLevel = LogLevel.DEBUG;
        } else if (itemId == R.id.verbose_logs) {
            selectedLogLevel = LogLevel.VERBOSE;
        } else {
            return super.onContextItemSelected(item);
        }
        logLevelMenuId = itemId;
        displayFilteredLogs();
        return true;
    }

    /**
     * Display the logs that contain the provided search query and
     * whose log level matches the selected log level.
     */
    private void displayFilteredLogs() {
        logsText.setText(R.string.placeholder_logs);
        logsText.setText(developerMenu.getFilteredLogs(searchQuery, selectedLogLevel));
    }
}
