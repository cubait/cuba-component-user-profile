package it.nexbit.cuba.security.userprofile.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.Default;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;
import com.haulmont.cuba.security.entity.User;
import it.nexbit.cuba.security.userprofile.app.UserProfileService;

public interface UserProfileConfig extends Config {
    /**
     * Get a flag to instruct if hiding the "Change password" button from the Settings screen.
     * Default: {@code false}
     *
     * @return {@code true} to hide the button or {@code false} to retain the default behavior.
     */
    @Property("ext.security.hideChangePasswordInSettings")
    @DefaultBoolean(false)
    @Source(type = SourceType.APP)
    boolean getHideChangePasswordInSettings();

    /**
     * Get the view name used by the {@link UserProfileService#getProfile()} method to select
     * which properties to include in the returned {@code User} entity.
     * Default: "user.profile"
     *
     * @return a {@code String} with the view name
     */
    @Property("ext.security.defaultViewForUserProfile")
    @Default("user.profile")
    @Source(type = SourceType.APP)
    String getDefaultViewForUserProfile();

    /**
     * Get the view name used by the {@link UserProfileService#updateProfile(User)} method to
     * determine which properties will be updated in the {@code User} entity stored in the current
     * {@code UserSession}.
     * Default: "user.profileUpdate"
     *
     * @return a {@code String} with the view name
     */
    @Property("ext.security.defaultViewForUserProfileUpdate")
    @Default("user.profileUpdate")
    @Source(type = SourceType.APP)
    String getDefaultViewForUserProfileUpdate();
}
