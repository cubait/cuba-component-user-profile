/*
 * Copyright (c) 2017 Nexbit di Paolo Furini
 */
package it.nexbit.cuba.security.userprofile.web.screens;

import com.haulmont.cuba.web.app.ui.core.settings.SettingsWindow;
import it.nexbit.cuba.security.userprofile.config.UserProfileConfig;

import javax.inject.Inject;
import java.util.Map;

/**
 * @author Paolo Furini
 */
public class NexbitSettingsWindow extends SettingsWindow {

    @Inject
    private UserProfileConfig userProfileConfig;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
        if (userProfileConfig.getHideChangePasswordInSettings()) {
            changePasswordBtn.setVisible(false);
        }
    }
}