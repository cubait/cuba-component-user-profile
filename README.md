[![license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Generic badge](https://img.shields.io/badge/API%20docs-HERE-orange.svg)][2]
[![Run in Postman](https://run.pstmn.io/button.svg)][1]

# CUBA User Profile Add-on

This add-on gives the following features once added to a CUBA project:

- Provides a `UserProfileService` with methods for getting and updating the details of the current logged in user (what I call the _user profile_)
- Exposes a new REST API endpoint (_Richardson Maturity Model - Level 2_ compliant), called `/rest/nxsecup/v1/userProfile` supporting GET (`getProfile`)
and PUT (`updateProfile`) requests, and a `/rest/nxsecup/v1/userProfile/password` endpoint supporting POST
requests for updating the user's password
- Adds a `userProfile` screen, and corresponding menu item after the Settings one in the Help main menu
- Optionally hides the _Change password_ button from the `Settings` screen (because that button is replicated in the `userProfile` screen)

**UPDATE FOR CUBA >= 7.1**: This add-on adds a dependency to the [REST API add-on](https://www.cuba-platform.com/marketplace/rest-api/)
once added to your project.

## Installation

**NOTE**: This add-on's repository is officially linked to the main CUBA repository.

*You can jump straight to step 2. for instructions on how to add it to your project*, or *optionally*
you can choose to add my repository to your project's repositories.  

1. [*OPTIONAL*] Add the following maven repository `https://dl.bintray.com/pfurini/cuba-components` to the build.gradle of your CUBA application:

```
buildscript {
    
    //...
    
    repositories {
    
        // ...
    
        maven {
            url  "https://dl.bintray.com/pfurini/cuba-components"
        }
    }
    
    // ...
}
```

2. Select a version of the add-on which is compatible with the platform version used in your project:

| Platform Version | Add-on Version | Coordinates
| ---------------- | -------------- | ------------
| 6.6.*            | 0.2.2          | it.nexbit.cuba.security.userprofile:nxsecup-global:0.2.2
| 6.7.*            | *N/A*          | *not compatible*
| 6.8.*            | 0.3.0          | it.nexbit.cuba.security.userprofile:nxsecup-global:0.3.0
| 6.9.*            | 1.0.0          | it.nexbit.cuba.security.userprofile:nxsecup-global:1.0.0
| 6.10.*           | 1.1.0          | it.nexbit.cuba.security.userprofile:nxsecup-global:1.1.0
| 7.0.*            | 2.0.0          | it.nexbit.cuba.security.userprofile:nxsecup-global:2.0.0
| 7.1.*            | 3.0.0          | it.nexbit.cuba.security.userprofile:nxsecup-global:3.0.0

The latest stable version is: `3.0.0`

3. Install the correct add-on version in your project using **CUBA Studio**, or manually by editing your `build.gradle` file.

**PLEASE NOTE** that new features are developed only in the latest releases, while only critical bug fixes
are back-ported to older ones. Please update your CUBA version as soon as possible, if you are in need of
some features available only in a recent release (see the [CHANGELOG](./CHANGELOG.md) for reference).


## Supported DBMS engines

_N/A_ - This component does not need any data

## Created tables

_NONE_

## Usage

### Configuration

The component behavior can be altered by means of configuration properties, and custom views.

#### Application properties

The following properties can be set in `*.properties` files (typically in your `web-app.properties` file).
For programmatic access, use to the `UserProfileConfig` interface. 

| Property                                       | Default Value                       | Description                                              |
| ---------------------------------------------- | ----------------------------------- | -------------------------------------------------------- |
| ext.security.hideChangePasswordInSettings      | `false`                             | Set to `true` to hide the *Change password* button in the *Settings* screen   
| ext.security.defaultViewForUserProfile         | user.profile                        | The view used by the `UserProfileService#getProfile()` method to select which properties to include in the returned `User` entity
| ext.security.defaultViewForUserProfileUpdate   | user.profileUpdate                  | The view used by the `UserProfileService#updateProfile(User)` method to determine which properties will be updated in the `User` entity stored in the current `UserSession`

#### Default views

The following views are the ones used by default in the `defaultView*` application properties.
Extend or replace them if the default properties are not suitable for your app.

```xml
<view class="com.haulmont.cuba.security.entity.User"
      extends="_minimal"
      name="user.profile">
    <property name="loginLowerCase"/>
    <property name="firstName"/>
    <property name="lastName"/>
    <property name="middleName"/>
    <property name="position"/>
    <property name="email"/>
    <property name="language"/>
    <property name="timeZone"/>
    <property name="timeZoneAuto"/>
    <property name="changePasswordAtNextLogon"/>
    <property fetch="JOIN"
              name="group"
              view="_minimal"/>
    <property fetch="JOIN"
              name="userRoles">
        <property fetch="JOIN"
                  name="role"
                  view="_minimal"/>
    </property>
</view>
<view class="com.haulmont.cuba.security.entity.User"
      name="user.profileUpdate">
    <property name="firstName"/>
    <property name="lastName"/>
    <property name="middleName"/>
    <property name="position"/>
    <property name="email"/>
    <property name="language"/>
    <property name="timeZone"/>
    <property name="timeZoneAuto"/>
    <property name="name"/>
</view>
```

### REST API

If you use **Postman** (and if you don't, you should), then click the following button to import
a collection with all the requests

[![Run in Postman](https://run.pstmn.io/button.svg)][1]

And here is the public documentation URL: [REST API Docs][2]

Every request makes use of the following variables:

| Variable name | Description
| ------------- | --------------
| {{baseurl}}   | The base URL for the requests, for example `http://localhost:8080/app/rest`
| {{bearer}}    | An auth token obtained by calling the `/rest/v2/oauth/token` endpoint

**HINT**: you can paste the following script in the `Tests` tab of the `/rest/v2/oauth/token` request
to automatically set the `bearer` variable after a successful auth

```
var jsonData = JSON.parse(responseBody)
pm.environment.set("bearer", jsonData.access_token);
```

For your convenience, a *Get Access Token* request is already included in the Postman collection. Only make
sure to update the *sec-user-profile TEST* environment to reflect your app URL (by default the `baseurl`
variable is set to `http://localhost:8080/app/rest`)

#### extsec_UserProfileService Methods

The following are the methods exposed by the `extsec_UserProfileService`, through the standard
REST API endpoint (`/rest/v2/services`).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<services xmlns="http://schemas.haulmont.com/cuba/rest-services-v2.xsd">
    <service name="extsec_UserProfileService">
        <method name="getProfile"/>
        <method name="updateProfile">
            <param name="user"/>
        </method>
    </service>
</services>
```

#### Alternative REST endpoint

The component exposes an alternative REST endpoint (`/rest/nxsecup/v1`) that aligns best to 
*Level 2* of the *Richardson Maturity Model*. It is more resource oriented than the service based
approach of the official REST API, and it makes use of HTTP verbs correctly.

Here is a brief list of the supported endpoints (please use the *Postman* collection above to play with
the actual requests in your project):


| Endpoint                                     | Verb | Description
| -------------------------------------------- | ---- | -----------
| {{baseurl}}/nxsecup/v1/userProfile           | GET  | Get the JSON representation of the `User` entity in the current `UserSession` (filtered by the `ext.security.defaultViewForUserProfile` view)
| {{baseurl}}/nxsecup/v1/userProfile           | PUT  | Update the `User` entity associated with the current `UserSession` (actual fields updated are filtered by the `ext.security.defaultViewForUserProfileUpdate` view)
| {{baseurl}}/nxsecup/v1/userProfile/password  | GET  | Update the password for the current user, with a `GET` operation (append `?password=newPasswordToSet`)
| {{baseurl}}/nxsecup/v1/userProfile/password  | POST | Update the password for the current user, with a `POST` operation (use the JSON `{"password": "newPasswordToSet"}`)


### Customisation

The `user-edit-profile` screen can be extended in the usual CUBA way, so that you can add/remove/alter
the `User` entity fields exposed to the user.

[1]: https://app.getpostman.com/run-collection/da701a9750c75da9ab02#?env%5Bsec-user-profile%20TEST%5D=W3sia2V5IjoiYmFzZXVybCIsInZhbHVlIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL2FwcC9yZXN0IiwiZW5hYmxlZCI6dHJ1ZSwidHlwZSI6InRleHQifV0=
[2]: https://documenter.getpostman.com/view/48162/collection/RW1VqMDm