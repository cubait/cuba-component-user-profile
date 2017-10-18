/*
 * Copyright (c) 2017 Nexbit di Paolo Furini
 */

package it.nexbit.cuba.security.userprofile.core;

import com.haulmont.cuba.security.entity.User;
import it.nexbit.cuba.security.userprofile.app.UserProfileService;
import it.nexbit.cuba.security.userprofile.config.UserProfileConfig;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.xml.bind.ValidationException;

@Service(UserProfileService.NAME)
public class UserProfileServiceBean implements UserProfileService {
    @Inject
    protected UserProfileHelper userProfileHelper;

    @Inject
    protected UserProfileConfig userProfileConfig;

    /**
     * Get the {@code User} entity associated with the current logged-in user.  In case of user
     * substitution, it always returns the real logged-in user, not the substituted one.
     * It uses the view returned by {@link UserProfileConfig#getDefaultViewForUserProfile()}
     * to select which properties to include in the returned entity.
     * <p>
     *     <b>NOTE:</b> It always reload the {@code User} entity from db, it does not simply
     *     return the entity stored in the {@code UserSession}.
     * </p>
     *
     * @return  a {@code User} entity with the current user's details, or {@code null} if no
     *      active profile
     */
    @Override
    public User getProfile() {
        return userProfileHelper.getProfile(userProfileConfig.getDefaultViewForUserProfile());
    }

    /**
     * Get the {@code User} entity associated with the current logged-in user.  In case of user
     * substitution, it always returns the real logged-in user, not the substituted one.
     * The properties to include in the returned entity are determined by the passed
     * {@code viewName}.
     * <p>
     *     <b>NOTE:</b> It always reload the {@code User} entity from db, it does not simply
     *     return the entity stored in the {@code UserSession}.
     * </p>
     *
     * @param viewName  the view name used to load the User entity with
     * @return  a {@code User} entity with the current user's details, or {@code null} if no
     *      active profile
     */
    @Override
    public User getProfile(@NotNull String viewName) {
        return userProfileHelper.getProfile(viewName);
    }

    /**
     * Update the user entity in the database, and then set it in the active
     * {@code UserSession}, if any.  Push the updated session in the entire
     * middleware cluster and recreate the {@code SecurityContext}, if any.
     * Only the properties included in the view returned by
     * {@link UserProfileConfig#getDefaultViewForUserProfileUpdate()} will be
     * updated, and the others will be simply discarded.
     * <h3>Preconditions</h3>
     * <ul>
     *     <li>The {@code user}'s id should be omitted, or otherwise must be equal
     *     to {@code UserSession} user's id</li>
     *     <li>The {@code user} entity instance will be validated using Bean Validation
     *     (with {@code Default} constraint group). Only properties included in the
     *     {@link UserProfileConfig#getDefaultViewForUserProfileUpdate()} will
     *     be validated.</li>
     * </ul>
     *
     * @param user  a {@code User} entity to set in the current {@code UserSession}
     * @throws ValidationException  if bean validation fails for {@code user}
     */
    @Override
    public void updateProfile(@NotNull User user) throws ValidationException {
        userProfileHelper.updateProfile(user, userProfileConfig.getDefaultViewForUserProfileUpdate());
    }

    /**
     * Update the user entity in the database, and then set it in the active
     * {@code UserSession}, if any.  Push the updated session in the entire
     * middleware cluster and recreate the {@code SecurityContext}, if any.
     * <h3>Preconditions</h3>
     * <ul>
     *     <li>The {@code user}'s id should be omitted, or otherwise must be equal
     *     to {@code UserSession} user's id</li>
     *     <li>The {@code user} entity instance will be validated using Bean Validation
     *     (with {@code Default} constraint group). Only properties included in
     *     {@code viewName} will be validated.</li>
     * </ul>
     * <p>
     *     <b>NOTE:</b> It uses getTransaction()
     * </p>
     *
     * @param user  a {@code User} entity to set in the current {@code UserSession}
     * @param viewName  the view used to update the user entity. Only the properties included
     *                  in this view will be updated in the original entity.
     */
    @Override
    public void updateProfile(@NotNull User user, @NotNull String viewName) throws ValidationException {
        userProfileHelper.updateProfile(user, viewName);
    }
}
