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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.R;
import com.amplifyframework.logging.Logger;

/**
 * A {@link Fragment} subclass representing the view
 * to display the environment information on the developer menu.
 */
public final class DevMenuEnvironmentFragment extends Fragment {
    private static final Logger LOG = Amplify.Logging.logger("amplify:devmenu");

    /**
     * Required empty public constructor.
     */
    public DevMenuEnvironmentFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View envInfoView = inflater.inflate(R.layout.dev_menu_fragment_environment, container, false);
        ((TextView) envInfoView.findViewById(R.id.env_info_text)).setText(getEnvironmentInfo());
        return envInfoView;
    }

    /**
     * Returns the environment information to be displayed.
     * @return a SpannableStringBuilder containing the formatted environment information
     */
    private SpannableStringBuilder getEnvironmentInfo() {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        EnvironmentInfo envInfo = new EnvironmentInfo();
        // Append the amplify plugin versions to stringBuilder
        stringBuilder.append(setBold("Amplify Plugins Information"));
        String pluginVersions = "\n" + envInfo.getPluginVersions() + "\n";
        stringBuilder.append(pluginVersions);

        // Append the developer environment information to stringBuilder
        stringBuilder.append(setBold("Developer Environment Information"));
        String devEnvInfo = "";
        try {
            devEnvInfo = envInfo.getDeveloperEnvironmentInfo(requireContext());
        } catch (AmplifyException error) {
            LOG.warn("Error reading developer environment information.");
        }
        if (devEnvInfo.isEmpty()) {
            stringBuilder.append("\nUnable to retrieve developer environment information.");
        } else {
            devEnvInfo = "\n" + devEnvInfo;
            stringBuilder.append(devEnvInfo);
        }
        return stringBuilder;
    }

    /**
     * Returns the given text in bold.
     * @param text the text to make bold
     * @return a SpannableString representing bold text
     */
    private SpannableString setBold(String text) {
        SpannableString boldText = new SpannableString(text);
        boldText.setSpan(new StyleSpan(Typeface.BOLD), 0, boldText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return boldText;
    }
}
