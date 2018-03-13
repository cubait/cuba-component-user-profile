/*
 * Copyright (c) 2017 Nexbit di Paolo Furini
 */

package it.nexbit.cuba.security.userprofile.core;

import com.google.common.collect.Sets;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.TransactionParams;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.global.validation.EntityValidationException;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.security.app.UserSessionsAPI;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.validation.*;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;

@Component
public class UserProfileHelper {
    public final static Set<String> FILTERED_USER_PROPERTIES = Sets.newHashSet(
            "version", "createTs", "createdBy", "updateTs", "updatedBy", "deleteTs", "deletedBy"
    );

    @Inject
    protected UserSessionSource userSessionSource;

    @Inject
    protected GlobalConfig globalConfig;

    @Inject
    protected UserSessionsAPI userSessionsAPI;

    @Inject
    protected Persistence persistence;

    @Inject
    protected Metadata metadata;

    @Inject
    protected BeanValidation beanValidation;

    /**
     * Get the User entity associated with the current logged-in user.  In case of user
     * substitution, it always returns the real logged-in user, not the substituted one.
     * The properties to include in the returned entity are determined by the passed
     * {@code viewName}.
     * <p>
     *     <b>NOTE:</b> It always reload the {@code User} entity from db, it does not simply
     *     return the entity stored in the {@code UserSession}.
     * </p>
     *
     * @param viewName  the view name to load the User entity with
     * @return the User entity of the logged-in user, or {@code null} if this is a System session
     *      or the anonymous user.
     */
    public User getProfile(@NotNull String viewName) {
        checkNotNullArgument(viewName, "viewName must not be null");
        final UserSession userSession = getUserSession();

        if (userSession != null) {
            // does NOT use the User entity inside the session, because it can be stale..
            // instead, reload the user from the database using the id stored in the session
            User user;
            try (Transaction tx = persistence.createTransaction(new TransactionParams().setReadOnly(true))) {
                EntityManager em = persistence.getEntityManager();
                View view = metadata.getViewRepository().getView(User.class, viewName);
                view.setLoadPartialEntities(true);
                user = em.find(User.class, userSession.getUser().getId(), view);
                tx.commit();
            }
            return user;
        }
        return null;
    }

    /**
     * Update the {@code user} entity in the database, and then set it in the active
     * {@code UserSession}, if any.  Push the updated session in the entire
     * middleware cluster and recreate the {@code SecurityContext}, if any.
     * NOTE: It uses getTransaction()
     *
     * @param user      a {@code User} entity to set in the current {@code UserSession}
     * @param viewName
     */
    public void updateProfile(@NotNull User user, @NotNull String viewName) {
        checkNotNullArgument(user, "user must not be null");
        checkNotNullArgument(viewName, "viewName must not be null");
        final UserSession userSession = getUserSession();
        if (userSession != null) {
            final User sessionUser = userSession.getUser();
            final Set<String> viewProperties = new HashSet<>();

            // set the id on the passed User entity to be the same as user's id in session
            user.setId(sessionUser.getId());

            final View updateView = metadata.getViewRepository().getView(User.class, viewName);
            //view.setLoadPartialEntities(true);
            for (ViewProperty prop : updateView.getProperties()) {
                if (!FILTERED_USER_PROPERTIES.contains(prop.getName())) {
                    viewProperties.add(prop.getName());
                }
            }

            // validates the User entity, but exclude validation errors for excluded properties
            Validator validator = beanValidation.getValidator();
            Set<ConstraintViolation<User>> violations = validator.validate(user, Default.class);
            violations = violations.stream().filter(violation -> {
                for (Path.Node node : violation.getPropertyPath()) {
                    if (node.getKind() == ElementKind.PROPERTY &&
                            viewProperties.contains(node.getName())) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toSet());
            if (!violations.isEmpty()) {
                throw new EntityValidationException(violations);
            }

            // update the record on db
            try (Transaction tx = persistence.getTransaction()) {
                EntityManager em = persistence.getEntityManager();

                // load existing user from db
                User originalUser = em.find(User.class, user.getId(), updateView);

                // copy only permitted properties from the passed in user to the originalUser
                for (String propName : viewProperties) {
                    originalUser.setValue(propName, user.getValue(propName));
                }

                // flush the changes and reload the changed entity
                em.flush();
                user = em.reload(originalUser, View.LOCAL);
                tx.commit();
            }
            updateUserSession(userSession, user);
        }
    }

    /**
     * Set the {@code user} entity in the provided {@code session}, and then update
     * the current {@code SecurityContext} with the updated session, and push it in
     * the entire middleware cluster.
     *
     * @param session  an active {@code UserSession}
     * @param user     a detached {@code User} entity
     */
    public void updateUserSession(@NotNull UserSession session, @NotNull User user) {
        checkNotNullArgument(session, "session must not be null");
        checkNotNullArgument(user, "user must not be null");
        // replace the current security context (if any) with a new one based on new session
        final SecurityContext securityContext = AppContext.getSecurityContext();
        if (securityContext != null) {
            if (securityContext.getSessionId().equals(AppContext.NO_USER_CONTEXT.getSessionId())) {
                return;
            }
            AppContext.setSecurityContext(new SecurityContext(session));
        }
        // update the session object and propagate to the cluster
        session.setUser(user);
        userSessionsAPI.propagate(session.getId());
    }

    /**
     * Get the current user session, if it is not the system user, or the anonymous user,  or the
     * special "server" one used in the login mechanism. In these latter cases, it returns
     * {@code null}.
     * It uses the UserSessionSource bean, that by default prolongs the session's lifetime in
     * the cache (it calls {@link UserSessionsAPI#getAndRefresh(UUID)}).
     *
     * @return
     */
    protected UserSession getUserSession() {
        if (userSessionSource.checkCurrentUserSession()) {
            final UserSession userSession = userSessionSource.getUserSession();
            if (!userSession.isSystem()
                    && !userSession.getId().equals(globalConfig.getAnonymousSessionId())
                    && !userSession.getId().equals(AppContext.NO_USER_CONTEXT.getSessionId())) {
                return userSession;
            }
        }
        return null;
    }
}
