/*
 * Copyright (c) 2017 Nexbit di Paolo Furini
 */
package it.nexbit.cuba.security.userprofile.web.screens;

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
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.security.ExternalUserCredentials;
import com.haulmont.cuba.web.sys.WebUserSessionSource;
import com.vaadin.server.VaadinSession;
import it.nexbit.cuba.security.userprofile.app.UserProfileService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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

    @Override
    @SuppressWarnings("unchecked")
    public void init(Map<String, Object> params) {

        UserSession userSession = userSessionSource.getUserSession();
        User user = userSession.getUser();
        changePasswordBtn.setAction(new BaseAction("changePassw")
                .withCaption(getMessage("changePassw"))
                .withHandler(event -> {
                    Window passwordDialog = openWindow("sec$User.changePassword", WindowManager.OpenType.DIALOG,
                            ParamsMap.of("currentPasswordRequired", true));
                    passwordDialog.getFrameOwner()
                            .addAfterCloseListener(afterCloseEvent -> changePasswordBtn.focus());
                }));

        if (!user.equals(userSession.getCurrentOrSubstitutedUser())
                || ExternalUserCredentials.isLoggedInWithExternalAuth(userSession)) {
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

        final TextField<String> firstNameField = (TextField<String>) fieldGroup.getFieldNN("firstName").getComponentNN();
        final TextField<String> lastNameField = (TextField<String>) fieldGroup.getFieldNN("lastName").getComponentNN();
        final TextField<String> nameField = (TextField<String>) fieldGroup.getFieldNN("name").getComponentNN();
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
        validateAdditionalRules(errors);
    }

    protected void commitAndClose() {
        if (getDsContext() != null && getDsContext().isModified()) {
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
        // IMPL NOTE: this code has been extracted (and adapted) from the WebUserSessionSource
        //            class.  The original logic is for reading the current UserSession, the
        //            following one is the same (reversed) logic to force an update.
        if (App.isBound()) {
            VaadinSession.getCurrent().setAttribute(UserSession.class, userSession);
        } else {
            SecurityContext securityContext = AppContext.getSecurityContextNN();
            if (securityContext.getSession() == null) {
                HttpServletRequest request = null;
                RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
                if (requestAttributes instanceof ServletRequestAttributes) {
                    request = ((ServletRequestAttributes) requestAttributes).getRequest();
                }
                if (request != null && request.getAttribute(WebUserSessionSource.REQUEST_ATTR) != null) {
                    request.setAttribute(WebUserSessionSource.REQUEST_ATTR, userSession);
                }
            }
        }

        // replace the current security context (if any) with a new one based on new session
        if (AppContext.getSecurityContext() != null)
            AppContext.setSecurityContext(new SecurityContext(userSession));
    }

    protected void cancel() {
        close(CLOSE_ACTION_ID);
    }

}