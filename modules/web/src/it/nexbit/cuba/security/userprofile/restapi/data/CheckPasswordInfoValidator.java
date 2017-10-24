package it.nexbit.cuba.security.userprofile.restapi.data;

import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.PasswordEncryption;
import com.haulmont.cuba.security.app.UserManagementService;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CheckPasswordInfoValidator implements ConstraintValidator<CheckPasswordInfo, PasswordInfo> {
    private UserManagementService userManagementService;
    private PasswordEncryption passwordEncryption;
    private ClientConfig clientConfig;

    public void initialize(CheckPasswordInfo constraint) {
        userManagementService = AppBeans.get(UserManagementService.NAME);
        passwordEncryption = AppBeans.get(PasswordEncryption.NAME);
        clientConfig = AppBeans.get(Configuration.class).getConfig(ClientConfig.class);
    }

    public boolean isValid(PasswordInfo passwordInfo, ConstraintValidatorContext context) {
        if (passwordInfo == null || passwordInfo.userId == null || passwordInfo.getPassword() == null) {
            return true;
        }

        if (userManagementService.checkPassword(passwordInfo.userId, passwordEncryption.getPlainHash(passwordInfo.getPassword()))) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "msg://it.nexbit.cuba.security.userprofile.web/CheckPasswordInfo.sameAsOldPassword"
            ).addPropertyNode("password").addConstraintViolation();

            return false;
        }
        if (clientConfig.getPasswordPolicyEnabled()) {
            String regExp = clientConfig.getPasswordPolicyRegExp();
            if (!passwordInfo.getPassword().matches(regExp)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "msg://it.nexbit.cuba.security.userprofile.web/CheckPasswordInfo.policyViolation"
                ).addPropertyNode("password").addConstraintViolation();

                return false;
            }
        }
        return true;
    }
}
