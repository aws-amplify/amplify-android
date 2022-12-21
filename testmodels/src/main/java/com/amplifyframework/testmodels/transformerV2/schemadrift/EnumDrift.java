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

package com.amplifyframework.testmodels.transformerV2.schemadrift;

/**
* This schema has been manually modified to create a schema drift scenario.
* One of the enum cases in EnumDrift has been removed. This allows tests to
* decode data that contains the missing value to further observe the state of the system.
* Data that contains the missing value needs to be persisted with API directly
* using a custom GraphQL document/variables since model objects cannot be created with the
* commented out enum case.
**/

/** Auto generated enum from GraphQL schema. */
@SuppressWarnings("all")
public enum EnumDrift {
  ONE,
  TWO //,
  // THREE
}
