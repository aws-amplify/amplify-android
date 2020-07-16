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

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;

/**
 * Tests the navigation behavior for {@link DevMenuMainFragment}.
 */
public final class DevMenuMainViewInstrumentationTest {
    // A navigation host controller for testing.
    private static TestNavHostController navHostController;

    /**
     * Initialize the mock NavController.
     */
    @BeforeClass
    public static void initialize() {
        navHostController = new TestNavHostController(ApplicationProvider.getApplicationContext());
        navHostController.setGraph(R.navigation.dev_menu_nav_graph);
    }

    /**
     * Go to the main view of the developer menu.
     */
    @Before
    public void resetView() {
        FragmentScenario<DevMenuMainFragment> mainMenuScenario =
                FragmentScenario.launchInContainer(DevMenuMainFragment.class);
        mainMenuScenario.onFragment(fragment -> Navigation.setViewNavController(fragment.requireView(),
                navHostController));
    }


    /**
     * Tests that the app is navigated to the environment information
     * screen when the view environment information button is pressed.
     */
    @Test
    public void testNavigationToEnvInfoScreen() {
        onView(ViewMatchers.withId(R.id.env_button)).perform(ViewActions.click());
        NavDestination curDestination = navHostController.getCurrentDestination();
        Assert.assertNotNull(curDestination);
        Assert.assertEquals(curDestination.getId(), R.id.environment_fragment);
    }

    /**
     * Tests that the app is navigated to the device information
     * screen when the view device information button is pressed.
     */
    @Test
    public void testNavigationToDeviceInfoScreen() {
        onView(ViewMatchers.withId(R.id.device_button)).perform(ViewActions.click());
        NavDestination curDestination = navHostController.getCurrentDestination();
        Assert.assertNotNull(curDestination);
        Assert.assertEquals(curDestination.getId(), R.id.device_fragment);
    }

    /**
     * Tests that the app is navigated to the logs screen when
     * the view logs button is pressed.
     */
    @Test
    public void testNavigationToLogsScreen() {
        onView(ViewMatchers.withId(R.id.logs_button)).perform(ViewActions.click());
        NavDestination curDestination = navHostController.getCurrentDestination();
        Assert.assertNotNull(curDestination);
        Assert.assertEquals(curDestination.getId(), R.id.logs_fragment);
    }

    /**
     * Tests that the app is navigated to the file issue screen
     * when the file issue button is pressed.
     */
    @Test
    public void testNavigationToFileIssueScreen() {
        onView(ViewMatchers.withId(R.id.file_issue_button)).perform(ViewActions.click());
        NavDestination curDestination = navHostController.getCurrentDestination();
        Assert.assertNotNull(curDestination);
        Assert.assertEquals(curDestination.getId(), R.id.file_issue_fragment);
    }
}
