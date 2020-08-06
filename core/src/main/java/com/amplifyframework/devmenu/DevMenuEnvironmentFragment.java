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

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.amplifyframework.core.R;

/**
 * A {@link Fragment} subclass representing the view
 * to display the environment information on the developer menu.
 */
public final class DevMenuEnvironmentFragment extends Fragment {

    /**
     * Required empty public constructor.
     */
    public DevMenuEnvironmentFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View envInfoView = inflater.inflate(R.layout.dev_menu_fragment_environment, container, false);
        displayEnvironmentInfo((TextView) envInfoView.findViewById(R.id.env_info_text));
        return envInfoView;
    }

    /**
     * Displays the environment information on the given TextView.
     * @param envInfoTextView the TextView that will contain the environment information text
     */
    private void displayEnvironmentInfo(TextView envInfoTextView) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        SpannableString pluginInfoTitle = new SpannableString("Amplify Plugins Information");
        pluginInfoTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, pluginInfoTitle.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.append(pluginInfoTitle);
        stringBuilder.append("\nPlugin versions will be added here...\n\n");
        SpannableString devEnvInfoTitle = new SpannableString("Developer Environment Information");
        devEnvInfoTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, devEnvInfoTitle.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.append(devEnvInfoTitle);
        stringBuilder.append("\nDeveloper environment information will be added here...");
        envInfoTextView.setText(stringBuilder);
    }
}
