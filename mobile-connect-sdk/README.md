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

Any updates on the Android SDK will require re-building by performing `gradlew assemble` on the Android SDK. This will create an `AAR` within the `build\outputs\aar` directory for each `Build Type`. For this Demo Application to get updated with the latest changes from the SDK, simply copy any of the `AAR`'s in the mentioned directory and paste it in this Demo's `app\libs` directory.

Run/Debug the Demo on an Android Studio Emulator which is running API Level 15 or higher. Third party emulators will work providing they are fully compatible with Android's SDK.

## Support

Any issues, please send us a message here: https://developer.mobileconnect.io/content/contact-us

Enjoy using Mobile Connect!

