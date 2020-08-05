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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import androidx.fragment.app.Fragment;

import com.amplifyframework.core.R;

/**
 * A {@link Fragment} subclass corresponding to the
 * file issue view for the developer menu.
 */
public final class DevMenuFileIssueFragment extends Fragment {
    // Minimum number of characters required for the issue description.
    private static final int MIN_DESCRIPTION_LENGTH = 140;
    // Error message to display when the description does not satisfy the
    // minimum character count requirement.
    private static final String DESCRIPTION_LENGTH_ERROR = "Issue description must be at least "
            + MIN_DESCRIPTION_LENGTH + " characters.";
    // The base URL for filing a new issue.
    private static final String NEW_ISSUE_URL = "https://github.com/aws-amplify/amplify-android/issues/new";
    // The view for this fragment.
    private View fileIssueView;
    // An instance of DeveloperMenu.
    private DeveloperMenu developerMenu;

    /**
     * Required empty public constructor.
     */
    public DevMenuFileIssueFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fileIssueView = inflater.inflate(R.layout.dev_menu_fragment_file_issue, container, false);
        developerMenu = DeveloperMenu.singletonInstance(getContext());
        fileIssueView.findViewById(R.id.file_issue).setOnClickListener(view -> fileIssue());
        fileIssueView.findViewById(R.id.copy_issue).setOnClickListener(view -> {
            String issueBody = getIssueBody();
            if (!issueBody.isEmpty()) {
                developerMenu.copyToClipboard(issueBody);
            }
        });
        return fileIssueView;
    }

    /**
     * Returns the issue body, or an empty string if the issue description did not
     * contain at least MIN_DESCRIPTION_LENGTH characters.
     * @return the issue body
     */
    private String getIssueBody() {
        EditText issueDescription = (EditText) fileIssueView.findViewById(R.id.issue_description);
        String description = issueDescription.getText().toString();
        if (description.length() < MIN_DESCRIPTION_LENGTH) {
            issueDescription.setError(DESCRIPTION_LENGTH_ERROR);
            return "";
        } else {
            Switch logsSwitch = (Switch) fileIssueView.findViewById(R.id.logs_switch);
            return developerMenu.createIssueBody(description, logsSwitch.isChecked());
        }
    }

    /**
     * Opens the file new issue page and sets the issue body.
     */
    private void fileIssue() {
        String issueBody = getIssueBody();
        if (!issueBody.isEmpty()) {
            Uri fileIssueLink = Uri.parse(NEW_ISSUE_URL).buildUpon().appendQueryParameter("title", "")
                    .appendQueryParameter("body", issueBody).build();
            Intent openNewIssue = new Intent(Intent.ACTION_VIEW, fileIssueLink);
            startActivity(openNewIssue);
        }
    }
}
