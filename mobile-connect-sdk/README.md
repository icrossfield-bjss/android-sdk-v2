## Mobile Connect Android SDK

This README supercedes any others in this project tree.

## Motivation

Mobile Connect Android SDK is designed to help developers quickly bootstrap their own solutions by seeing how it works and what is required.

## Installation

Recommended Setup

-----------------

Android Studio

Mac - Java Development Kit (JDK) 6

Windows - Java Development Kit (JDK) 8

Linux - Java Development Kit (JDK) 8

-----------------

## Usage

Any updates on the Android SDK will require re-building by performing `gradlew assemble` on the Android SDK. This will create an `AAR` within the `build\outputs\aar` directory for each `Build Type`. The generated `AAR` can then be used to import within the [Demo Application](https://github.com/Mobile-Connect/android-sdk-v2/tree/master/mobile-connect-demo).

This Android SDK relies on the [Java SDK] (https://github.com/Mobile-Connect/java-sdk-v1). The generated `JAR` file from the JAVA SDK is placed within the Android SDK's `app\libs` directory. The following statement is required within the SDK's `app` `build.gradle` file:

    compile fileTree(dir: 'libs', include: ['*.jar'])

Though you may wish to swap this with:

    compile files('libs/mobile-connect-sdk-2.0.0-SNAPSHOT.jar')

If you wish to `compile` only one specific `JAR`. 

## Support

Any issues, please send us a message here: https://developer.mobileconnect.io/content/contact-us

Enjoy using Mobile Connect!

