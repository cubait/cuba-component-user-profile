[![license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

# CUBA Forgot Password Component

This application component gives the following features once added to a CUBA project:

- Provides a `UserProfileService` with methods for getting and updating the details of the current logged in user (what I call the _user profile_)
- Makes possible to call the get and update profile methods even from REST clients (with sanity checks)
- Adds a `userProfile` screen, and corresponding menu item after the Settings one in the Help main menu
- Optionally hides the _Change password_ button from the `Settings` screen (because that button is replicated in the `userProfile` screen)

## Installation

1. Add the following maven repository `https://dl.bintray.com/pfurini/cuba-components` to the build.gradle of your CUBA application:


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

2. Select a version of the add-on which is compatible with the platform version used in your project:

| Platform Version | Add-on Version |
| ---------------- | -------------- |
| 6.6.4            | 0.1.x          |

The latest version is: `0.1.0`

Add custom application component to your project:

* Artifact group: `it.nexbit.cuba.security.userprofile`
* Artifact name: `nxsecup-global`
* Version: *add-on version*

## Supported DBMS engines

_N/A_ - This component does not need any data

## Created tables

_NONE_

## Usage

_TODO_