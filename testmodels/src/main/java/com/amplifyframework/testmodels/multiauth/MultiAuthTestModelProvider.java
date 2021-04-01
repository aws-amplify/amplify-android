package com.amplifyframework.testmodels.multiauth;

import com.amplifyframework.util.Immutable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
/** 
 *  Contains the set of model classes that implement {@link Model}
 * interface.
 */

public final class MultiAuthTestModelProvider implements ModelProvider {
  private static final String AMPLIFY_MODEL_VERSION = "52428e395c1e7bccd45653eae49c294d";
  private static MultiAuthTestModelProvider amplifyGeneratedModelInstance;
  private MultiAuthTestModelProvider() {
    
  }
  
  public static MultiAuthTestModelProvider getInstance() {
    if (amplifyGeneratedModelInstance == null) {
      amplifyGeneratedModelInstance = new MultiAuthTestModelProvider();
    }
    return amplifyGeneratedModelInstance;
  }
  
  /** 
   * Get a set of the model classes.
   * 
   * @return a set of the model classes.
   */
  @Override
   public Set<Class<? extends Model>> models() {
    final Set<Class<? extends Model>> modifiableSet = new HashSet<>(
          Arrays.<Class<? extends Model>>asList(Post.class)
        );
    
        return Immutable.of(modifiableSet);
        
  }
  
  /** 
   * Get the version of the models.
   * 
   * @return the version string of the models.
   */
  @Override
   public String version() {
    return AMPLIFY_MODEL_VERSION;
  }
}
