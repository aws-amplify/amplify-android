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

package com.amplifyframework.predictions.aws;

import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.configuration.InterpretTextConfiguration;
import com.amplifyframework.predictions.aws.configuration.TranslateTextConfiguration;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.testutils.Resources;

import com.amazonaws.regions.Region;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public final class AWSPredictionsPluginConfigurationTest {

    /**
     * Test that passing in null configuration causes configuration
     * failure.
     * @throws Exception if configuration fails
     */
    @Test(expected = PredictionsException.class)
    public void testNullConfigurationFails() throws Exception {
        AWSPredictionsPluginConfiguration.fromJson(null);
    }

    /**
     * Test that configuration without "defaultRegion" section causes
     * configuration failure.
     * @throws Exception if configuration fails
     */
    @Test(expected = PredictionsException.class)
    public void testConfigurationFailsWithoutDefaultRegion() throws Exception {
        JSONObject json = Resources.readAsJson("configuration-without-region.json");
        AWSPredictionsPluginConfiguration.fromJson(json);
    }

    /**
     * Test that configuration without any resources section
     * creates plugin-level configuration instance.
     * @throws Exception if configuration fails
     */
    @Test
    public void testConfigurationPassesInitially() throws Exception {
        JSONObject json = Resources.readAsJson("configuration-with-region.json");
        AWSPredictionsPluginConfiguration pluginConfig = AWSPredictionsPluginConfiguration.fromJson(json);

        // Default plugin configuration
        assertEquals(Region.getRegion("us-west-2"), pluginConfig.getDefaultRegion());
        assertEquals(NetworkPolicy.AUTO, pluginConfig.getDefaultNetworkPolicy());
    }

    /**
     * Test that configuration without any resources section
     * creates plugin-level configuration instance without
     * complaining UNTIL a missing configuration is called.
     * @throws Exception if configuration fails
     */
    @Test(expected = PredictionsException.class)
    public void testConfigurationThrowsRetroactivelyForMissingConfiguration() throws Exception {
        JSONObject json = Resources.readAsJson("configuration-with-region.json");
        AWSPredictionsPluginConfiguration pluginConfig = AWSPredictionsPluginConfiguration.fromJson(json);

        // Default plugin configuration
        assertEquals(Region.getRegion("us-west-2"), pluginConfig.getDefaultRegion());
        assertEquals(NetworkPolicy.AUTO, pluginConfig.getDefaultNetworkPolicy());

        // Trying to obtain missing configuration throws
        pluginConfig.getTranslateTextConfiguration();
    }

    /**
     * Test that configuration with explicit "translateText" section creates
     * customized translate configuration instance.
     * @throws Exception if configuration fails
     */
    @Test
    public void testTranslateTextConfiguration() throws Exception {
        JSONObject json = Resources.readAsJson("configuration-with-translate.json");
        AWSPredictionsPluginConfiguration pluginConfig = AWSPredictionsPluginConfiguration.fromJson(json);

        // Custom translate configuration
        TranslateTextConfiguration translateConfig = pluginConfig.getTranslateTextConfiguration();
        assertNotNull(translateConfig);
        assertEquals(LanguageType.ENGLISH, translateConfig.getSourceLanguage());
        assertEquals(LanguageType.KOREAN, translateConfig.getTargetLanguage());
        assertEquals(NetworkPolicy.AUTO, translateConfig.getNetworkPolicy());
    }

    /**
     * Test that configuration with explicit "interpretText" section creates
     * customized interpret configuration instance.
     * @throws Exception if configuration fails
     */
    @Test
    public void testInterpretTextConfiguration() throws Exception {
        JSONObject json = Resources.readAsJson("configuration-with-interpret.json");
        AWSPredictionsPluginConfiguration pluginConfig = AWSPredictionsPluginConfiguration.fromJson(json);

        // Custom interpret configuration
        InterpretTextConfiguration interpretConfig = pluginConfig.getInterpretTextConfiguration();
        assertNotNull(interpretConfig);
        assertEquals(InterpretTextConfiguration.InterpretType.SENTIMENT, interpretConfig.getType());
        assertEquals(NetworkPolicy.OFFLINE, interpretConfig.getNetworkPolicy());
    }
}
