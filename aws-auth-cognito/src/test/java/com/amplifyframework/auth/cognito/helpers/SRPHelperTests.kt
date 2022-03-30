/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.helpers

import java.math.BigInteger
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class SRPHelperTests {
    private val privateA =
        BigInteger(
            "9488887383906780169920975115798309747280340386237614989425790548298032630501412070884132" +
                "7474383776867490835991544401757353458851423548817030650959502170715563858472246798478938383992111" +
                "8641214763439688576190818458131929816508697212646134511379438491061305971630419332754538817571764" +
                "16122005373573809300830865"
        )
    private val publicA =
        BigInteger(
            "11192474286840334471722978684986209783175488578992296589467453128630265626913161504063468" +
                "64106062669911158129314950857465603171803287021003588123791821185916072587217955369271623510429537" +
                "32551155633054210178493778939640028675261283516115373388764339335220509247187335763466688437428792" +
                "65362566787484080412385977638882831105761333994602485939233731111266073804596815964749387213765155" +
                "93329462662848506719165950175083044905231704580656419611364744204398179729110438190571587555259293" +
                "01338606512963194813501894270468944932239610289366203865675340496336217232074486190696250297871427" +
                "64141360057837172190037590510990635716226816042121117916066383179076658278882993585798860002749973" +
                "30740606430758361551878153579452183428496577734529102684413238959372501144449784518943588392083353" +
                "49485274657157338737638401847919818879364724164793787007156859929896097113574756120642188086454670" +
                "7291574811962872976502777643906968756980611812218024"
        )

    private val srpB = BigInteger(
        "e05d0716c6f6bf2b96e4b08354015be28024b4bc9a022c3c7553b86a5feffe8ee0ae87ae8ebb970b9457ed56ef0f5249fad4b9" +
            "9235912c2445a803ff9c9412cb39bd860bcca12868c8e813404435f3621a2fa033270f053e15b7d2ec0617e40cafff3fef4d" +
            "5be716821b389e5338c45ac2d65b292a4739dec8d05ae7feaca6a46d56624c753b705bbc78071ef5c47152c46d7c79b52e4f" +
            "1de3a15ed68923b0f8b08cd4d6b9d358282e219ccfff37ac143b9c7283b859187f57ca445129aeef0517c394a363ed83ca65" +
            "b11a7ede22b96b7e0ff43dd5f58248dbf16c47fad319a23c15ed27c47c765841cb6474eda7a8d44a65c7242d9ff7afc6fc37" +
            "d03b43c9c6279d88248120908c4ecdfb84a6e42344cf1a57f976ef036a1eb7a64067eff195855735d4010ca33831bded8b40" +
            "b5e60a61f19900f928353203adacab71948ef3bcd3540bf926fee5578a00f039bb5c60d10e446a55c9d9620257e05418523a" +
            "fe02e1bc3f3f00c8a43841248094c8806d38f4207679170126f449a62006b42bf0",
        16
    )
    private val uExpected = BigInteger("55210528188551933294168660600273656944094602294020021251326310004904436019280")
    private val xExpected = BigInteger("35002972281322559225995282163712515452164094971035196098842264315967202596678")
    private val sExpected =
        BigInteger(
            "511345472632077986735986885089071610653969805450786791939125627692790536134000082759348192" +
                "804027153930785691781398398132811986321140892635801915236191818486116863406417546280555943754594296" +
                "366022880249134505094437862370793412787279510964331987195596031068122410211871808565000179838077627" +
                "754092739677312926751171096246264606961497070785421287049130487235010052533171533527878789361026328" +
                "422145458126624755578428525996944079160163469092402312621245864486741224829541296219466452986925814" +
                "020957094270379277789718029731629690713273892568540137695963255567455428179953126762557298477262512" +
                "777361474850226276341413469250282102093072059011915728649991171817255890625341524286426644190898728" +
                "507227357192685939148414555873540207018785644757307191090287082832329442711271294412736445837547417" +
                "799530142252738269462892779146865658440480077734916784666366589585336992336314695955831714303221343" +
                "6496073302424592796663351090753841872905806"
        )
    private val keyExpected = "a0z8GrRyu6bvL/ABfYRAuw=="
    private val secretBlock =
        "5ZmS3XIpUD0zsSdbIfEuyp6Bh/Gj1/1PoxqfZQxR9NoG8w/tX2COvK/U/Y9N736doE8QOYGKsmYGMtrRrQ7frLmn2kjE9vBByMRqyD8pmLL" +
            "3Z2p9OwaAdDzkMH8X/9YubQNlkLR2dtu6DrvDv9DC6o6sq6IOT3WITjpVaIgEJ660nZAcqCM4lsZQF+he1ZqhVmqcOnpQVwGelN" +
            "F9YhSOhHPGFZ9HrGJcADxMdtSVMVr8x5HEmpatsA986HpDMbAmG15GEFo93h7iGH4hwl584I076DyvzKyIGpqhVlJeor/MqvdCE" +
            "G3/61Bku1es+LVhNrx8zPsLbfDitdqqLavjxZAU+UNUlD+Mqr9Ng3JXNVB3eQ76TazE1lHPpMI5mpkLmsJ9XP2zOI7kN+1UokLL" +
            "Vp7tRHQKJeGbARfSJFJBKneYQOJujmr6CixnYCb7XAOk0jRFOMDbv4Lbh4J70iuixe16WFF9UqtBfUzZobbKTuI4kvLYH+F8b3W" +
            "tFBvMI9PIRXWhRYuej7SAMu+BOPO35bnURelp5GLCJwOs+GbYkHDWjZGBMNg5DxbdvZXqE/16Rnigw9JGi9Q63brcnFB1jlYagU" +
            "qbv2ihAm7R0HIib0CEvW7AqK1yxL+0rjkVAI68AYY7eCl2gXjfXNmKLzmGCmExRpWO5DEslDtBcuEpAPVAwYNKdARVVSsXP5HcL" +
            "xa0YzqjbSmOoSESYIE/1KvmncYcHsctVgR/+wmZziXPYtUYc3mwPDdXxXTkAzro8aAEMTeuMwEdBOh1Vji0xFhhdY/P/m2dIDX1" +
            "Hct2O+TUKXOM/X0XM+9p0kaheIHg117lG1DIZT7VeD4x2qQaNR95NR7SzkyoGDZKTDXxS9NwVP8ad8qi5hPUhe0JlFq/aqHyjto" +
            "IS4TGUgl1ZnGl7dO03KQtara72Ivf6RnGMVN1MCiQavgHHIecyReSdIurpopSVA0DRFKJYK0/HDxynj04OaeuW7oyO0kV2trX5e" +
            "bxrdtaCuZofHAb/qqEFUnyac8EWMONA2xZkh8JmmOL9n/qTmD3y5zR45LexAnDxLInNHIbSpqh7Tikre7DpqVpwrtGmrHmW1QAE" +
            "fP95m/YbAdJ/3iMfnB6SEoeK7aTAtRoLKlPFlJjU+bk2387mYOvvwhBEnb50xMM1BW9ZNItj18KIC5E0/e/ePKDGp1HCpX09wO+" +
            "QFabTJuabxc2j1W3p42y/InyW27R+Yhnqh/hWSN48w+ASNMaJIiixWQbrIlX4zkPuEFKZGYnMY3fMROzRrsNfSf7qJzYQYVw0y4G" +
            "Ert5uOpsaeShEvdBLAw3iVQV/VmfF23p6DT0i80ML+xyQTAJ+aDf6Y7SFKXFeCkkylNL9aDldgQ2qrsEPVicL2WMDEyU56QJD8Mq" +
            "aBlo+0qvQWAxT2ruvngXo3kBRyoOrxjS9hcFuzNoCol5Cvu5grD8tkF+mwnvq2W3l4NKnx5YK5Fi9viEub6hU6fDpgSHIGD/8gXF" +
            "XNpBTXHtd1XuhCfLZlg7so7/k64CQNGiWlVn2rE+1c3BbT9fNW7Kz9VgLvL7RimjQqIDcynDxvqw6mTpvgQHdHJYrPUOyuCWmY03" +
            "TbzCUUIt25oBfPVA5IjfwcgXCLczT3CfETJGOi2w3O78uiup3VRxg4vvNFPnNolWlyxtekcxF46Ovlm+qLkVgwENDJTDUlbOjZ+/e" +
            "kGtTev3PtdYjT+dqNm3rMGU"
    private val m1Expected = "QG7a57h+ndPBVasvx/OkmsJdy5uoMEVRshboEd4S+j8="

    private lateinit var helper: SRPHelper

    @Before
    fun setUp() {
        helper = SRPHelper("username", "Password123")
        helper.setAValues(privateA, publicA)
        helper.setUserPoolParams("username", "us-east-2_KO6fcefgd")
    }

    @Test
    fun testValidPublicA() {
        val testHelper = SRPHelper("", "")
        val bigA = BigInteger(testHelper.getPublicA(), 16)
        assertNotEquals(BigInteger.ZERO, testHelper.modN(bigA))
    }

    @Test
    fun testComputeU() {
        val uActual = helper.computeU(srpB)
        assertEquals(uExpected, uActual)
    }

    @Test
    fun testComputeX() {
        val salt = BigInteger("e7dc204cebbfda6b62b8493e932f7f4c", 16)
        val xActual = helper.computeX(salt)
        assertEquals(xExpected, xActual)
    }

    @Test
    fun testComputeS() {
        val sActual = helper.computeS(uExpected, xExpected, srpB)
        assertEquals(sExpected, sActual)
    }

    @Test
    fun testComputePAK() {
        val keyActual = helper.computePasswordAuthenticationKey(sExpected, uExpected)
        assertEquals(keyExpected, String(Base64.getEncoder().encode(keyActual)))
    }

    @Test
    fun testGenerateM1() {
        helper.dateString = "Wed Sep 29 06:40:48 UTC 2021"
        val m1Actual = helper.generateM1Signature(keyExpected.toByteArray(), secretBlock)
        assertEquals(m1Expected, String(Base64.getEncoder().encode(m1Actual)))
    }
}
