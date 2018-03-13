/*
 * Copyright (c) 2017 Nexbit di Paolo Furini
 */
package it.nexbit.cuba.security.userprofile;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import it.nexbit.cuba.security.userprofile.app.UserProfileService;

import javax.inject.Inject;
import javax.validation.ValidationException;
import java.util.Map;

/**
 * @author Paolo Furini
 */
public class UserEditProfile extends AbstractWindow {
    @Inject
    protected Button changePasswordBtn;

    @Inject
    protected Button okBtn;

    @Inject
    protected Button cancelBtn;

    @Inject
    protected FieldGroup fieldGroup;

    @Inject
    protected Datasource<User> userDs;

    @Inject
    protected UserSessionSource userSessionSource;
    @Inject
    protected Messages messages;
    @Inject
    protected ClientConfig clientConfig;
    @Inject
    protected UserProfileService userProfile;

    @Inject
    protected Companion companion;

    protected EditorWindowDelegate delegate;

    @Override
    public void init(Map<String, Object> params) {
        // get companion (defined in web block only, so provide some defaults for desktop)
        if (companion == null) {
            companion = new Companion() {
                @Override
                public Boolean isLoggedInWithExternalAuth() {
                    return false;
                }

                @Override
                public void pushUserSessionUpdate(UserSession userSession) {

                }
            };
        }

        UserSession userSession = userSessionSource.getUserSession();
        User user = userSession.getUser();
        changePasswordBtn.setAction(new BaseAction("changePassw")
                .withCaption(getMessage("changePassw"))
                .withHandler(event -> {
                    Window passwordDialog = openWindow("sec$User.changePassword", WindowManager.OpenType.DIALOG,
                            ParamsMap.of("currentPasswordRequired", true));
                    passwordDialog.addCloseListener(actionId -> {
                        // move focus back to window
                        changePasswordBtn.requestFocus();
                    });
                }));

        if (!user.equals(userSession.getCurrentOrSubstitutedUser())
                || companion.isLoggedInWithExternalAuth()) {
            changePasswordBtn.setEnabled(false);
        }

        Action commitAction = new BaseAction("commit")
                .withCaption(messages.getMainMessage("actions.Ok"))
                .withShortcut(clientConfig.getCommitShortcut())
                .withHandler(event ->
                        commitAndClose()
                );
        addAction(commitAction);
        okBtn.setAction(commitAction);

        cancelBtn.setAction(new BaseAction("cancel")
                .withCaption(messages.getMainMessage("actions.Cancel"))
                .withHandler(event ->
                        cancel()
                ));

        userDs.setItem(userProfile.getProfile());

        delegate = new EditorWindowDelegate(this);

        final TextField firstNameField = (TextField) fieldGroup.getFieldNN("firstName").getComponentNN();
        final TextField lastNameField = (TextField) fieldGroup.getFieldNN("lastName").getComponentNN();
        final TextField nameField = (TextField) fieldGroup.getFieldNN("name").getComponentNN();
        firstNameField.addValueChangeListener(e -> {
            String prevValue = (e.getPrevValue() != null ? e.getPrevValue() + " " : "");
            if (e.getValue() != null) {
                if (nameField.getValue() == null ||
                        nameField.getRawValue().equals(prevValue + lastNameField.getValue())) {
                    if (lastNameField.getValue() != null) {
                        nameField.setValue(e.getValue() + " " + lastNameField.getValue());
                    } else {
                        nameField.setValue(e.getValue());
                    }
                }
            } else {
                if (lastNameField.getValue() == null)
                    prevValue = prevValue.trim();
                if (nameField.getRawValue().equals(prevValue + lastNameField.getRawValue())) {
                    nameField.setValue(lastNameField.getValue());
                }
            }
        });
        lastNameField.addValueChangeListener(e -> {
            String prevValue = (e.getPrevValue() != null ? " " + e.getPrevValue() : "");
            if (e.getValue() != null) {
                if (nameField.getValue() == null ||
                        nameField.getRawValue().equals(firstNameField.getValue() + prevValue)) {
                    if (firstNameField.getValue() != null) {
                        nameField.setValue(firstNameField.getValue() + " " + e.getValue());
                    } else {
                        nameField.setValue(e.getValue());
                    }
                }
            } else {
                if (firstNameField.getValue() == null)
                    prevValue = prevValue.trim();
                if (nameField.getRawValue().equals(firstNameField.getRawValue() + prevValue)) {
                    nameField.setValue(firstNameField.getValue());
                }
            }
        });
    }

    @Override
    protected void postValidate(ValidationErrors errors) {
        delegate.validateAdditionalRules(errors);
    }

    protected void commitAndClose() {
        if (delegate.isModified()) {
            if (!validateAll()) return;

            try {
                userProfile.updateProfile(userDs.getItem());
            } catch (ValidationException e) {
                showNotification(e.getLocalizedMessage(), NotificationType.ERROR);
                return;
            }
            User newUser = userProfile.getProfile();
            userDs.setItem(newUser);

            UserSession userSession = userSessionSource.getUserSession();
            userSession.setUser(newUser);
            pushUserSessionUpdate(userSession);

            showNotification(getMessage("profile.commitSuccess"), NotificationType.HUMANIZED);
        }
        close(COMMIT_ACTION_ID);
    }

    protected void pushUserSessionUpdate(UserSession userSession) {
        companion.pushUserSessionUpdate(userSession);

        // replace the current security context (if any) with a new one based on new session
        if (AppContext.getSecurityContext() != null)
            AppContext.setSecurityContext(new SecurityContext(userSession));
    }

    protected void cancel() {
        close(CLOSE_ACTION_ID);
    }

    public interface Companion {
        /**
         * Check if the user has been authenticated with a user/password login mechanism (based
         * on {@code LoginPasswordCredentials} credentials), or with an external one.
         * Only in the former case the password for the user can be changed by the web client.
         *
         * @return {@code true} if the current UserSession was obtained from an external
         *      authentication provider, {@code false} otherwise
         */
        Boolean isLoggedInWithExternalAuth();

        /**
         * Set the updated {@code userSession} in the current {@code VaadinSession} instance, so
         * that subsequent invocations of {@link UserSessionSource#getUserSession()} will get the
         * new instance.
         *
         * @param userSession the new {@code UserSession} object to set in the current
         *                    {@code VaadinSession} and propagated in the middleware
         */
        void pushUserSessionUpdate(UserSession userSession);
    }
}