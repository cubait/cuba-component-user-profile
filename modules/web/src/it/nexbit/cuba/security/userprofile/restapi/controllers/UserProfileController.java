package it.nexbit.cuba.security.userprofile.restapi.controllers;

import com.haulmont.restapi.service.ServicesControllerManager;
import it.nexbit.cuba.security.userprofile.restapi.data.PasswordInfo;
import it.nexbit.cuba.security.userprofile.restapi.services.UserProfileControllerManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

/**
 * REST controller that is used for getting and updating the data of the current logged in user.
 */
@RestController
@RequestMapping("/nxsecup/v1/userProfile")
public class UserProfileController {

    protected static String TEXT_PLAIN_UTF8_VALUE = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8";

    @Inject
    protected UserProfileControllerManager userProfileControllerManager;

    /**
     * Get the logged in user's profile
     *
     * @return 200 (application/json) - the current user's profile<br/>
     *      204 (text/plain) - no active user profile (for example for the anonymous user)
     *
     */
    @GetMapping
    public ResponseEntity<String> getUserProfile() {
        ServicesControllerManager.ServiceCallResult result = userProfileControllerManager.getUserProfile();
        return buildUserProfileResponse(result);
    }

    /**
     * Update the logged in user's profile, and return the updated profile
     *
     * @param userJson  the profile data to update, as JSON string
     * @return the updated profile as JSON
     */
    @PutMapping
    public ResponseEntity<String> updateUserProfile(@RequestBody(required = false) String userJson) {
        ServicesControllerManager.ServiceCallResult result =  userProfileControllerManager.updateUserProfile(userJson);
        return buildUserProfileResponse(result);
    }

    /**
     * Update the logged in user's password
     *
     * @param password  the new password to set for the current user
     * @return  204 (empty body) - if password set successfully<br>
     *     403 - if cannot change password because there is no valid user session
     */
    @GetMapping("/password")
    public ResponseEntity changePassword(@RequestParam(required = false) String password) {
        return changePasswordInternal(new PasswordInfo(password));
    }

    /**
     * Update the logged in user's password
     *
     * @param passwordInfo  an object containing the new password
     * @return  204 (empty body) - if password set successfully<br>
     *     403 - if cannot change password because there is no valid user session
     */
    @PostMapping("/password")
    public ResponseEntity changePassword(@RequestBody(required = false) PasswordInfo passwordInfo) {
        return changePasswordInternal(passwordInfo);
    }

    protected ResponseEntity changePasswordInternal(PasswordInfo passwordInfo) {
        if (userProfileControllerManager.changePassword(passwordInfo)) {
            return ResponseEntity.noContent()
                    .header("Content-Type", TEXT_PLAIN_UTF8_VALUE).build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header("Content-Type", TEXT_PLAIN_UTF8_VALUE).build();
    }

    protected ResponseEntity<String> buildUserProfileResponse(ServicesControllerManager.ServiceCallResult result) {
        HttpStatus status;
        String contentType;
        if (result.isValidJson()) {
            status = HttpStatus.OK;
            contentType = MediaType.APPLICATION_JSON_UTF8_VALUE;
        } else {
            status = HttpStatus.NO_CONTENT;
            contentType = TEXT_PLAIN_UTF8_VALUE;
        }
        return ResponseEntity.status(status).header("Content-Type", contentType).body(result.getStringValue());
    }
}
