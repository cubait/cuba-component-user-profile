package it.nexbit.cuba.security.userprofile.restapi.services;

import com.haulmont.addon.restapi.api.exception.RestAPIException;
import com.haulmont.addon.restapi.api.service.ServicesControllerManager;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.app.serialization.EntitySerializationAPI;
import com.haulmont.cuba.core.app.serialization.EntitySerializationOption;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.BeanValidation;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.PasswordEncryption;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.global.validation.CustomValidationException;
import com.haulmont.cuba.security.app.UserManagementService;
import com.haulmont.cuba.security.entity.User;
import it.nexbit.cuba.security.userprofile.app.UserProfileService;
import it.nexbit.cuba.security.userprofile.restapi.data.PasswordInfo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

@Component("nexbit_UserProfileControllerManager")
public class UserProfileControllerManager {

    @Inject
    protected UserSessionSource userSessionSource;
    @Inject
    protected UserManagementService userManagementService;
    @Inject
    protected UserProfileService userProfileService;
    @Inject
    protected PasswordEncryption passwordEncryption;
    @Inject
    protected EntitySerializationAPI entitySerializationAPI;
    @Inject
    protected Metadata metadata;
    @Inject
    protected BeanValidation beanValidation;

    @NotNull
    public ServicesControllerManager.ServiceCallResult getUserProfile() {
        User user = userProfileService.getProfile();
        return serializeEntity(user);
    }

    @NotNull
    public ServicesControllerManager.ServiceCallResult updateUserProfile(@NotNull String userJson) {
        throwIfNull(userJson);

        MetaClass metaClass = metadata.getClass("sec$User");
        User updateUser;
        try {
            updateUser = entitySerializationAPI.entityFromJson(userJson, metaClass);
        } catch (Exception e) {
            throw new RestAPIException("Cannot deserialize an entity from JSON", "", HttpStatus.BAD_REQUEST, e);
        }

        User updatedUser = userProfileService.updateProfile(updateUser);
        return serializeEntity(updatedUser);
    }

    public boolean changePassword(@NotNull PasswordInfo passwordInfo) {
        throwIfNull(passwordInfo);

        if (userSessionSource.checkCurrentUserSession()) {
            UUID userId = userSessionSource.getUserSession().getUser().getId();
            passwordInfo.userId = userId; // needed for custom validation
            throwIfInvalid(passwordInfo);

            String passwordHash = passwordEncryption.getPasswordHash(userId, passwordInfo.getPassword());
            userManagementService.changeUserPassword(userId, passwordHash);
            return true;
        }
        return false;
    }

    protected <T extends Entity> ServicesControllerManager.ServiceCallResult serializeEntity(T entity) {
        if (entity != null) {
            String entityJson = entitySerializationAPI.toJson(entity,
                    null,
                    EntitySerializationOption.SERIALIZE_INSTANCE_NAME);
            return new ServicesControllerManager.ServiceCallResult(entityJson, true);
        }
        return new ServicesControllerManager.ServiceCallResult("", false);
    }

    protected <T> void throwIfNull(T requestEntity) {
        if (requestEntity == null) {
            throw new CustomValidationException("empty request body");
        }
    }

    protected <T> void throwIfInvalid(T requestEntity) {
        Validator validator = this.beanValidation.getValidator();
        Set<ConstraintViolation<T>> violations = validator.validate(requestEntity);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
