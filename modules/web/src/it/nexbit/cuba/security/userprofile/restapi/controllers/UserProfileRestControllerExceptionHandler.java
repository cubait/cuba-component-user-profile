package it.nexbit.cuba.security.userprofile.restapi.controllers;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.restapi.controllers.RestControllerExceptionHandler;
import com.haulmont.restapi.exception.ConstraintViolationInfo;
import it.nexbit.cuba.security.userprofile.restapi.data.NavigableConstraint;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Set;

@ControllerAdvice("it.nexbit.cuba.security.userprofile.restapi.controllers")
public class UserProfileRestControllerExceptionHandler extends RestControllerExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(UserProfileRestControllerExceptionHandler.class);

    protected MessageTools messageTools;

    public MessageTools getMessageTools() {
        if (messageTools == null) {
            messageTools = AppBeans.get(MessageTools.NAME);
        }
        return messageTools;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public ResponseEntity<List<ConstraintViolationInfo>> handleConstraintViolation(ConstraintViolationException e) {
        log.debug("ConstraintViolationException: {}, violations:\n{}", e.getMessage(), e.getConstraintViolations());

        List<ConstraintViolationInfo> violationInfos = getLocalizedConstraintViolationInfos(e.getConstraintViolations());

        return new ResponseEntity<>(violationInfos, HttpStatus.BAD_REQUEST);
    }

    protected List<ConstraintViolationInfo> getLocalizedConstraintViolationInfos(Set<ConstraintViolation<?>> violations) {
        List<ConstraintViolationInfo> violationInfos = new ArrayList<>();

        for (ConstraintViolation<?> violation : violations) {
            ConstraintViolationInfo info = new ConstraintViolationInfo();

            String template = violation.getMessageTemplate();
            info.setMessageTemplate(template);

            Object invalidValue = violation.getInvalidValue();

            if (violation.getConstraintDescriptor().getAnnotation().annotationType().isAnnotationPresent(NavigableConstraint.class)) {
                String propName = violation.getPropertyPath().toString();
                try {
                    invalidValue = PropertyUtils.getNestedProperty(violation.getRootBean(), propName);
                } catch (Exception e) {
                    invalidValue = null;
                }
            }

            if (invalidValue != null) {
                Class<?> invalidValueClass = invalidValue.getClass();
                boolean serializable = false;
                for (Class serializableType : serializableInvalidValueTypes) {
                    //noinspection unchecked
                    if (serializableType.isAssignableFrom(invalidValueClass)) {
                        serializable = true;
                        break;
                    }
                }
                if (serializable) {
                    info.setInvalidValue(invalidValue);
                } else {
                    info.setInvalidValue(null);
                }
            }

            if (template.startsWith("msg://")) {
                String message;
                try {
                    message = String.format(getMessageTools().loadString(template), info.getInvalidValue());
                } catch (IllegalFormatException e) {
                    message = violation.getMessage();
                }
                info.setMessage(message);
            } else {
                info.setMessage(violation.getMessage());
            }

            if (violation.getPropertyPath() != null) {
                info.setPath(violation.getPropertyPath().toString());
            }

            violationInfos.add(info);
        }
        return violationInfos;
    }

}
