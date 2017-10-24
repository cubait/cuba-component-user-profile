/*
 * Copyright (c) 2017 Nexbit di Paolo Furini
 */

package it.nexbit.cuba.security.userprofile.app;

import com.haulmont.cuba.security.entity.User;
import it.nexbit.cuba.security.userprofile.config.UserProfileConfig;
import org.springframework.validation.annotation.Validated;

import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;

/**
 * Provides methods for getting and updating the user's profile (that is, the {@code User}
 * entity associated with the active {@code UserSession}).
 */
public interface UserProfileService {
    String NAME = "extsec_UserProfileService";

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
    User getProfile();

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
    @Validated
    User getProfile(@NotNull String viewName);

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
     * @return the updated user, if any (may return null)
     */
    @Validated
    User updateProfile(@NotNull User user);

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
     *
     * @param user  a {@code User} entity to set in the current {@code UserSession}
     * @param viewName  the view used to update the user entity. Only the properties included
     *                  in this view will be updated in the original entity.
     * @throws ValidationException  if bean validation fails for {@code user}
     * @return the updated user, if any (may return null)
     */
    @Validated
    User updateProfile(@NotNull User user, @NotNull String viewName);
}
