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
