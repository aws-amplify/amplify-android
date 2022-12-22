package com.amplifyframework.datastore.generated.model;

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

public final class AmplifyModelProvider implements ModelProvider {
  private static final String AMPLIFY_MODEL_VERSION = "81a5f19e244652e316120083dc47c4c0";
  private static AmplifyModelProvider amplifyGeneratedModelInstance;
  private AmplifyModelProvider() {
    
  }
  
  public static AmplifyModelProvider getInstance() {
    if (amplifyGeneratedModelInstance == null) {
      amplifyGeneratedModelInstance = new AmplifyModelProvider();
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
          Arrays.<Class<? extends Model>>asList(CreatorPlus.class, VFXCategoryLocale.class, TextTemplateLocale.class, TransitionVFXCategoryLocale.class, TransitionVFXLocale.class, VideoFilterCategoryLocale.class, VideoFilterLocale.class, TextTemplateCategoryLocale.class, ClipAnimLocale.class, ClipAnimCategoryLocale.class, OverlayMediaCategoryLocale.class, VFXLocale.class, ClipAnim.class, ClipAnimCategory.class, VideoFilter.class, VideoFilterCategory.class, Recommend.class, RecommendCategory.class, OverlayMediaCategory.class, OverlayMedia.class, TextTemplateCategory.class, TextTemplate.class, Font2.class, TransitionVFX.class, TransitionVFXCategory.class, FilterVFX.class, FilterVFXCategory.class, FontVFXCategory.class, FontVFX.class, VFXCategory.class, VFX.class, AudioCategory.class, Audio.class, TextAnim.class, TextAnimLocale.class, TextAnimCategory.class, TextAnimCategoryLocale.class)
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
