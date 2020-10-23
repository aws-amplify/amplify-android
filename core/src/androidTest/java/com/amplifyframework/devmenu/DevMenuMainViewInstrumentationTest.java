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

import androidx.annotation.RawRes;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;

import com.amplifyframework.core.R;

import org.junit.Before;
import org.junit.Ignore;

import static androidx.test.espresso.Espresso.onView;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the navigation behavior for {@link DevMenuMainFragment}.
 */
@Ignore("All of these tests timeout frequently with an error about the root of the view hierarchy never taking focus")
public final class DevMenuMainViewInstrumentationTest {
    // A navigation host controller for testing.
    private TestNavHostController navHostController;

    /**
     * Go to the main screen of the developer menu.
     */
    @Before
    public void resetView() {
        navHostController = new TestNavHostController(ApplicationProvider.getApplicationContext());
        navHostController.setGraph(R.navigation.dev_menu_nav_graph);
        FragmentScenario<DevMenuMainFragment> mainMenuScenario =
                FragmentScenario.launchInContainer(DevMenuMainFragment.class);
        mainMenuScenario.onFragment(fragment ->
                Navigation.setViewNavController(fragment.requireView(), navHostController));
    }


    /**
     * Tests that the app is navigated to the environment information
     * screen when the view environment information button is pressed.
     */
    @Ignore("DeviceFarm does not appear to recognize ignore at the class level.")
    public void testNavigationToEnvInfoScreen() {
        testNavigationOnButtonPress(R.id.env_button, R.id.environment_fragment);
    }

    /**
     * Tests that the app is navigated to the device information
     * screen when the view device information button is pressed.
     */
    @Ignore("DeviceFarm does not appear to recognize ignore at the class level.")
    public void testNavigationToDeviceInfoScreen() {
        testNavigationOnButtonPress(R.id.device_button, R.id.device_fragment);
    }

    /**
     * Tests that the app is navigated to the logs screen when
     * the view logs button is pressed.
     */
    @Ignore("DeviceFarm does not appear to recognize ignore at the class level.")
    public void testNavigationToLogsScreen() {
        testNavigationOnButtonPress(R.id.logs_button, R.id.logs_fragment);
    }

    /**
     * Tests that the app is navigated to the file issue screen
     * when the file issue button is pressed.
     */
    @Ignore("DeviceFarm does not appear to recognize ignore at the class level.")
    public void testNavigationToFileIssueScreen() {
        testNavigationOnButtonPress(R.id.file_issue_button, R.id.file_issue_fragment);
    }

    /**
     * Test that the app is navigated to the destination with the given ID when
     * the button with the given ID is pressed.
     * @param buttonId ID of the button pressed
     * @param destinationId ID of the navigation destination
     */
    private void testNavigationOnButtonPress(@RawRes int buttonId, @RawRes int destinationId) {
        onView(ViewMatchers.withId(buttonId)).perform(ViewActions.click());
        NavDestination curDestination = navHostController.getCurrentDestination();
        assertNotNull(curDestination);
        assertEquals(curDestination.getId(), destinationId);
    }
}
