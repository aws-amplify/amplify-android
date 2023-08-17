package com.amplifyframework.auth.cognito.helpers

//import aws.smithy.kotlin.runtime.content.Document

import generated.model.Amplify





import generated.model.AmplifyError
import generated.model.ApiCall
import generated.model.AwsService
import generated.model.Cognito
import generated.model.CognitoType
import generated.model.ExpectationShape
import generated.model.MockedResponse
import generated.model.Preconditions
import generated.model.Response
import generated.model.ShapeType
import generated.model.StateMachine
import generated.model.UnitTest
import generated.model.TypeResponse

import generated.model.Validation
import io.mockk.InternalPlatformDsl.toStr
import org.json.JSONObject

import aws.smithy.kotlin.runtime.content.Document
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import deserializeWrapper


import serializeWrapper

//import testwrapper
import java.io.File

class SmithyMod {

    private fun getJSONIterative(directory: File) : MutableList<UnitTest> {
        //iterates through directory, gets serialized representations
        val jsonFiles = mutableListOf<UnitTest>()
        for (subdirectory in directory.listFiles()) {
            for (jsonFile in subdirectory.listFiles()) {
                jsonFiles += deserializeWrapper(JSONObject(jsonFile.readText()))
                //deserializes the json into smithy test directly

            }

        }
        return jsonFiles
    }


    fun getTest(): List<UnitTest> {
        //gets all tests
        val listOfTests = mutableListOf<UnitTest>()
        val directory = File("src/test/resources/feature-test/testsuites")
        listOfTests += getJSONIterative(directory)
        return listOfTests

    }


    fun runApi(apiName: String) : List<UnitTest> {
        //gets tests only for API specified
        val listOfTests = mutableListOf<UnitTest>()
        val directory = File("src/test/resources/feature-test/testsuites/${apiName}")
        for (subdirectory in directory.listFiles()) {
            listOfTests += deserializeWrapper(JSONObject(subdirectory.readText()))

        }
        return listOfTests

    }
    //The Lines below are only for adding additional APIs
    //(1)Get all the information from generator, (2)store to directory, (3)then run convertAPI to serialize to smithy acceptable format






    fun convertAPI() : List<String> {
        //Converts instances of old generator to new one
        val listOfTests = mutableListOf<String>()
        val directory = File("src/test/java/testframework/")
        for (subdirectory in directory.listFiles()) {
            val gson = GsonBuilder().setPrettyPrinting().create()

            val jsonString = convertJSONToSerializeStructure(JSONObject(subdirectory.readText())).toStr()
            val jsonElement = JsonParser.parseString(jsonString)
            val prettyJson = gson.toJson(jsonElement)


            subdirectory.writeText(prettyJson)



        }
        return listOfTests


    }

    fun convertJSONToSerializeStructure(generated : JSONObject) : String {
        //returns serialized representation of Smithy object
        return serializeWrapper(generatorInformationToSmithy(generated))


    }
    private fun apiNameToCognitoType(input_in : String) : CognitoType {
        if (input_in == "getId" || input_in == "getCredentialsForIdentity") {
            return CognitoType.Cognitoidentity
        }
        return CognitoType.Cognitoidentityprovider
    }

    private fun obtainMockListSuccess(expected: JSONObject) : List<MockedResponse> {
        //helper function for old generator to new generator conversion
        var mockList : List<MockedResponse> = mutableListOf()

        val preConditions = expected.getJSONObject("preConditions")
        var mockedResponses = preConditions.getJSONArray("mockedResponses")
        for (i in 0 until mockedResponses.length()) {

            val cur = MockedResponse {
                type = AwsService.CognitoUserPools
                apiName = JSONObject(mockedResponses[i].toStr())["apiName"].toStr()
                responseType = TypeResponse.Success

                val currentResponse = mockedResponses.getJSONObject(i)
                var responseType = currentResponse.getJSONObject("response")


                val success = Document(responseType.toStr())

                response = Response.Success(success)



            }
            mockList = mockList + cur



        }

        return mockList

    }
    private fun obtainMockListError(expected_fail: JSONObject) : List<MockedResponse> {
        //helper function for old generator to new generator conversion
        var mockList : List<MockedResponse> = mutableListOf()

        val preConditions = expected_fail.getJSONObject("preConditions")
        var mockedResponses = preConditions.getJSONArray("mockedResponses")
        for (i in 0 until mockedResponses.length()) {

            val cur = MockedResponse {
                type = AwsService.CognitoUserPools
                apiName = JSONObject(mockedResponses[i].toStr())["apiName"].toStr()//expected_fail.getJSONObject("api")["name"].toStr()
                responseType = TypeResponse.Error



                val currentResponse = mockedResponses.getJSONObject(i)
                val responseType = currentResponse.getJSONObject("response")
                val codeValue = responseType["errorType"]

                val error = AmplifyError {
                    errorType = codeValue.toStr()


                }
                response = Response.Error(error)


            }
            mockList = mockList + cur



        }

        return mockList
    }






    private fun generatorInformationToSmithy(generated : JSONObject) : UnitTest {
        //main helper function for old generator to new generator conversion
        //builds new Smithy instance, then populates it with old generator info
        //returns the smithy instance at the end
        val testing = UnitTest {
            preConditions = Preconditions {
                val mockedResponseList = JSONObject(generated["preConditions"].toStr()).getJSONArray("mockedResponses")
                if (mockedResponseList.length() == 0) {
                    val response = MockedResponse {
                        this.apiName = generated.getJSONObject("api")["name"].toStr()
                        this.responseType = TypeResponse.Complete
                    }
                    mockedResponses = listOf(response)
                }
                else if (JSONObject(mockedResponseList[0].toStr())["responseType"] == "failure") {
                    mockedResponses = obtainMockListError(generated)
                }
                else {
                    mockedResponses = obtainMockListSuccess(generated)
                }
                state = JSONObject(generated.get("preConditions").toStr())["state"].toStr()
                amplifyconfiguration = JSONObject(generated.get("preConditions").toStr())["amplify-configuration"].toStr()

            }
            api = ApiCall {
                name = generated.getJSONObject("api")["name"].toStr()

                val parameterMap = mutableMapOf<String, Document?>()

                val keyParameter = JSONObject(generated.getJSONObject("api")["params"].toStr())
                val keysList = keyParameter.keys()
                for (key in keysList) {

                    val parameterValue = keyParameter[key]
                    parameterMap[key.toStr()] = Document.String(parameterValue.toStr())

                }
                params = Document.Map(parameterMap)

                val optionMap = mutableMapOf<String, Document?>()

                val keyOptionJson = JSONObject(generated.getJSONObject("api")["options"].toStr())
                val keysOption = keyOptionJson.keys()
                for (key in keysOption) {

                    val optionValue = keyOptionJson[key]
                    optionMap[key.toStr()] = Document.String(optionValue.toStr())

                }

                options = Document.Map(optionMap)
            }
            val listValidation = generated.getJSONArray("validations")

            var smithyListValidation: List<Validation> = mutableListOf()



            for (i in 0 until listValidation.length()) {

                val validation = Validation {

                    if (JSONObject(listValidation[i].toStr()).has("response")) {
                        shapetype = ShapeType.Amplify

                        val amplifyBuild = Amplify {

                            this.response = Document(JSONObject(listValidation[i].toStr()).get("response").toStr())
                            this.apiName = JSONObject(listValidation[i].toStr())["apiName"].toStr()

                        }

                        shape = ExpectationShape.Amplify(amplifyBuild)


                    }
                    else if (JSONObject(listValidation[i].toStr()).has("request")) {
                        shapetype = ShapeType.Cognito
                        val cognitoBuild = Cognito {

                            val currentValidation = JSONObject(listValidation[i].toStr())
                            val requestMap = mutableMapOf<String, Document?>()


                            val keysValidation = currentValidation.keys()
                            for (key in keysValidation) {

                                val validationValue = currentValidation[key]
                                requestMap[key.toStr()] = Document.String(validationValue.toStr())

                            }
                            request = Document.Map(requestMap)

                            apiName = api!!.name

                            type = apiNameToCognitoType(apiName!!)
                        }
                        shape = ExpectationShape.Cognito(cognitoBuild)
                    }
                    else {

                        shapetype = ShapeType.Statemachine
                        val stateMachineBuild = StateMachine {

                            expectedState = JSONObject(listValidation[i].toStr()).get("expectedState").toStr()
                        }
                        shape = ExpectationShape.StateMachine(stateMachineBuild)

                    }



                }
                smithyListValidation = smithyListValidation + validation

            }
            validations = smithyListValidation


        }
        return testing

    }


}
