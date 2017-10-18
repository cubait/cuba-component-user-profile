/*
 * Copyright (c) 2017 Nexbit di Paolo Furini
 */

package it.nexbit.cuba.security.userprofile.web.companions;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.sys.WebUserSessionSource;
import com.vaadin.server.VaadinSession;
import it.nexbit.cuba.security.userprofile.UserEditProfile;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import static com.haulmont.cuba.web.auth.ExternallyAuthenticatedConnection.EXTERNAL_AUTH_USER_SESSION_ATTRIBUTE;

public class UserEditProfileCompanion implements UserEditProfile.Companion {

    @Override
    public Boolean isLoggedInWithExternalAuth() {
        UserSession userSession = AppBeans.get(UserSession.class);
        return Boolean.TRUE.equals(userSession.getAttribute(EXTERNAL_AUTH_USER_SESSION_ATTRIBUTE));
    }

    @Override
    public void pushUserSessionUpdate(UserSession userSession) {
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
    }
}
