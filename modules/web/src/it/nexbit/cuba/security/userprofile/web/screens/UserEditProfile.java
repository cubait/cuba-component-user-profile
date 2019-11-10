package it.nexbit.cuba.security.userprofile.web.screens;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.ValidationErrors;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
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

@UiController("userProfile")
@UiDescriptor("user-edit-profile.xml")
public class UserEditProfile extends Screen {

    @Inject
    protected Button changePasswordBtn;

    @Inject
    protected Button okBtn;

    @Inject
    protected Button cancelBtn;

    @Inject
    protected TextField<String> firstNameField;
    @Inject
    protected TextField<String> lastNameField;
    @Inject
    protected TextField<String> nameField;

    @Inject
    protected UserSessionSource userSessionSource;
    @Inject
    protected Messages messages;
    @Inject
    protected ClientConfig clientConfig;
    @Inject
    protected UserProfileService userProfile;
    @Inject
    protected ScreenBuilders screenBuilders;
    @Inject
    protected Notifications notifications;
    @Inject
    protected ScreenValidation screenValidation;
    @Inject
    protected InstanceContainer<User> userDc;

    protected boolean modifiedAfterOpen = false;

    @Subscribe
    protected void onInit(InitEvent event) {

        UserSession userSession = userSessionSource.getUserSession();
        User user = userSession.getUser();
        changePasswordBtn.setAction(new BaseAction("changePassw")
                .withCaption(messages.getMainMessage("changePassw"))
                .withHandler(e -> {
                    Screen passwordDialog = screenBuilders.screen(this)
                            .withScreenId("sec$User.changePassword")
                            .withOpenMode(OpenMode.DIALOG)
                            .withOptions(new MapScreenOptions(ParamsMap.of("currentPasswordRequired", true)))
                            .show();
                    passwordDialog.addAfterCloseListener(closeEvent -> changePasswordBtn.focus());
                }));

        if (!user.equals(userSession.getCurrentOrSubstitutedUser())
                || ExternalUserCredentials.isLoggedInWithExternalAuth(userSession)) {
            changePasswordBtn.setEnabled(false);
        }

        Action commitAction = new BaseAction("commit")
                .withCaption(messages.getMainMessage("actions.Ok"))
                .withShortcut(clientConfig.getCommitShortcut())
                .withHandler(e -> commitAndClose());
        getWindow().addAction(commitAction);
        okBtn.setAction(commitAction);

        userDc.setItem(getScreenData().getDataContext().merge(userProfile.getProfile()));

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

    @Subscribe
    protected void onAfterShow(AfterShowEvent event) {
        DataContext dataContext = getScreenData().getDataContext();
        if (dataContext != null) {
            dataContext.addChangeListener(e -> modifiedAfterOpen = true);
            dataContext.addPostCommitListener(e -> modifiedAfterOpen = false);
        }
    }

    protected void commitAndClose() {
        if (getScreenData().getDataContext().hasChanges() || modifiedAfterOpen) {
            if (!validateScreen()) return;

            User newUser;
            try {
                newUser = userProfile.updateProfile(userDc.getItem());
            } catch (ValidationException e) {
                notifications.create(Notifications.NotificationType.ERROR)
                        .withCaption(e.getLocalizedMessage()).show();
                return;
            }

            UserSession userSession = userSessionSource.getUserSession();
            userSession.setUser(newUser);
            pushUserSessionUpdate(userSession);

            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption(messages.getMainMessage("profile.commitSuccess"))
                    .show();
        }
        close(WINDOW_COMMIT_AND_CLOSE_ACTION);
    }

    protected boolean validateScreen() {
        ValidationErrors validationErrors = screenValidation.validateUiComponents(this.getWindow());
        this.validateAdditionalRules(validationErrors);
        if (!validationErrors.isEmpty()) {
            screenValidation.showValidationErrors(this, validationErrors);
            return false;
        }
        return true;
    }

    protected void validateAdditionalRules(ValidationErrors errors) {
        if (errors.isEmpty()) {
            ValidationErrors validationErrors = screenValidation.validateCrossFieldRules(this, userDc.getItem());
            errors.addAll(validationErrors);
        }
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

    @Subscribe("cancelBtn")
    protected void onCancelBtnClick(Button.ClickEvent event) {
        closeWithDefaultAction();
    }

}