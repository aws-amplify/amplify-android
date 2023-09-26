/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.testmodels.lazyinstrumented;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.ModelPath;
import com.amplifyframework.core.model.PropertyPath;

/** This is an auto generated class representing the ModelPath for the Parent type in your schema. */
public final class ParentPath extends ModelPath<Parent> {
  private HasOneChildPath child;
  private HasManyChildPath children;
  ParentPath(@NonNull String name, @NonNull Boolean isCollection, @Nullable PropertyPath parent) {
    super(name, isCollection, parent, Parent.class);
  }
  
  public synchronized HasOneChildPath getChild() {
    if (child == null) {
      child = new HasOneChildPath("child", false, this);
    }
    return child;
  }
  
  public synchronized HasManyChildPath getChildren() {
    if (children == null) {
      children = new HasManyChildPath("children", true, this);
    }
    return children;
  }
}
