## Mobile Connect Android SDK

## Motivation

Mobile Connect Android SDK is designed to help developers quickly bootstrap their own solutions by seeing how it works and what is required.

-----------------

## Usage

Any updates on the Android SDK will require re-building by performing `gradlew assemble` on the Android SDK. This will create an `AAR` within the `build\outputs\aar` directory for each `Build Type`. For the Demo Application to get updated with the latest changed from the SDK, simply copy any of the `AAR`'s in the mentioned directory and paste it in `app\libs` within the Demo Application.

Run/Debug the Demo (The demo application is a sub-project of this Git Repository) application on an Android Studio Emulator which is running API Level 15 or higher. Third party emulators will work providing they are fully compatible with Android's SDK.

## Support

Any issues, please send us a message here: https://developer.mobileconnect.io/content/contact-us

Enjoy using Mobile Connect!

