package it.nexbit.cuba.security.userprofile.restapi.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

@CheckPasswordInfo
public class PasswordInfo {

    @NotNull
    @Size(min = 1, max = 255)
    protected String password;

    @JsonIgnore
    public UUID userId;

    public PasswordInfo() {}

    public PasswordInfo(@NotNull String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
