package com.amplifyframework.auth.cognito


import com.amplifyframework.auth.cognito.helpers.SmithyMod


import org.junit.Test


class NewTesting {

    @Test
    fun runAll() {
        val testingList = SmithyMod().getTest() //get list of tests
        for (value in testingList) { //iterate through list of tests
            val testingCurrent = AWSCognitoAuthPluginFeatureTest(value) //init test runner
            testingCurrent.api_feature_test() // run the test
        }


    }


    @Test
    fun getDeleteUser() {
        val list = SmithyMod().runApi("deleteUser")
        for (value in list) { //iterate through list of tests
            val testingCurrent = AWSCognitoAuthPluginFeatureTest(value) //init test runner
            testingCurrent.api_feature_test() // run the test
        }

    }

    @Test
    fun initializeDeserialized() {
        val listNeedConvert = listOf("fetchDevices", "fetchUserAttributes", "forgetDevice", "rememberDevice", "resetPassword", "signUp")
        for (value in listNeedConvert) {

            SmithyMod().convertAPI(value)

        }
    }
}
//converts test information to a serialized json representing the direct smithy instance




