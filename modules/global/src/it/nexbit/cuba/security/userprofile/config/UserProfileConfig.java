package it.nexbit.cuba.security.userprofile.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.Default;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;

public interface UserProfileConfig extends Config {
    @Property("ext.security.hideChangePasswordInSettings")
    @DefaultBoolean(false)
    @Source(type = SourceType.APP)
    boolean getHideChangePasswordInSettings();

    @Property("ext.security.defaultViewForUserProfile")
    @Default("user.profile")
    @Source(type = SourceType.APP)
    String getDefaultViewForUserProfile();

    @Property("ext.security.defaultViewForUserProfileUpdate")
    @Default("user.profileUpdate")
    @Source(type = SourceType.APP)
    String getDefaultViewForUserProfileUpdate();
}
