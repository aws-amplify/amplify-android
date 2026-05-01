package com.amplifyframework.datastore.generated.model;

import com.amplifyframework.testmodels.lazycpk.Blog;
import com.amplifyframework.testmodels.lazycpk.Comment;
import com.amplifyframework.testmodels.lazycpk.HasManyChild;
import com.amplifyframework.testmodels.lazycpk.HasOneChild;
import com.amplifyframework.testmodels.lazycpk.Parent;
import com.amplifyframework.testmodels.lazycpk.Post;
import com.amplifyframework.testmodels.lazycpk.Project;
import com.amplifyframework.testmodels.lazycpk.Team;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.util.Immutable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Bridge ModelProvider at the package location expected by ModelProviderLocator.
 * Registers the lazy-loading codegen models used by instrumentation tests.
 */
public final class AmplifyModelProvider implements ModelProvider {
    private static final String AMPLIFY_MODEL_VERSION = "lazy-test";
    private static AmplifyModelProvider instance;

    private AmplifyModelProvider() { }

    public static synchronized AmplifyModelProvider getInstance() {
        if (instance == null) {
            instance = new AmplifyModelProvider();
        }
        return instance;
    }

    @Override
    public Set<Class<? extends Model>> models() {
        final Set<Class<? extends Model>> modifiableSet = new HashSet<>(
            Arrays.<Class<? extends Model>>asList(
                Parent.class,
                HasOneChild.class,
                HasManyChild.class,
                Project.class,
                Team.class,
                Blog.class,
                Post.class,
                Comment.class
            )
        );
        return Immutable.of(modifiableSet);
    }

    @Override
    public String version() {
        return AMPLIFY_MODEL_VERSION;
    }
}
