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

package com.amplifyframework.api.graphql;

/**
 * Location mapping to a particular line and column in the request.
 */
public final class GraphQLLocation {
    private int line;
    private int column;

    public GraphQLLocation(int line, int column) {
        this.line = line;
        this.column = column;
    }

    /**
     * Returns line number corresponding to position in the request where an error occurred.
     *
     * @return line
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns column number corresponding to position in the request where an error occurred.
     *
     * @return column
     */
    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        GraphQLLocation location = (GraphQLLocation) thatObject;

        return line == location.line && column == location.column;
    }

    @Override
    public int hashCode() {
        int result = line;
        result = 31 * result + column;
        return result;
    }

    @Override
    public String toString() {
        return "GraphQLLocation{" +
                "line=\'" + line + "\'" +
                ", column=\'" + column + "\'" +
                '}';
    }
}
