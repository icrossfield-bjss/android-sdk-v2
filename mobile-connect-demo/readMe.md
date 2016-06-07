                                 SOFTWARE USE PERMISSION

By downloading and accessing this software and associated documentation files ("Software") you are granted the
unrestricted right to deal in the Software, including, without limitation the right to use, copy, modify, publish,
sublicense and grant such rights to third parties, subject to the following conditions:

The following copyright notice and this permission notice shall be included in all copies, modifications or
substantial portions of this Software: Copyright © 2016 GSM Association.

THE SOFTWARE IS PROVIDED "AS IS," WITHOUT WARRANTY OF ANY KIND, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. YOU
AGREE TO INDEMNIFY AND HOLD HARMLESS THE AUTHORS AND COPYRIGHT HOLDERS FROM AND AGAINST ANY SUCH LIABILITY.


Mobile Connect SDK
------------------
This demo project has been built with Android Studio, it can be built by importing it into Android Studio and
running "gradle build".

The Android SDK is included as an Android Archive Module (AAR)

Customising the Demo App
------------------------
This demo app is configured out of the box to use a set of demonstration application credentials. You will need to replace
these with application credentials created using the GSMA Mobile Connect developer portal (developer.mobileconnect.io)

The following changes need to be made

1/ In the file app/src/main/assets/demo.properties
   - Replace config.applicationURL with the 'Redirect URL' that you will be registering on the GSMA Mobile Connect developer 
     when you provision your application.
   - Replace (only if you want) config.discoveryRedirectURL with a URL that will be used at the end of the discovery process. 
     Note generally what is in this URL is not important so unless you really want to it's recommended to leave this as is.

2/ In the file app/src/main/java/com.gsma.android/xoperatorapidemo/discovery/DiscoveryDeveloperOperatorSettings.java you 
   will see there are two sets of application credentials provisioned. These should be replaced with credentials issued
   by the GSMA Mobile Connect developer portal when you provision your application
   - You will see two sets of 'settings' are coded allowing you to put in both production and development mode credentials.
     These will be displayed when you run the demo app. Initially you should just replace the development mode credentials with
     your own (issued from the developer portal) and comment out the production ones, or use the sandbox details from the dev
     portal.
   - Make sure to have the correct URL for the Discovery Service, and the Client Key and Secret issued by the dev portal 
     to replace the details that are already hard coded
   - You can change the name of the settings to suit your own requirements, this is simply displayed in the demo app 

Build Instructions
------------------

Build under Android Studio or from the command line use 

gradle build

Running Instructions
--------------------

It's recommended to run via Android Studio so that you can see the demo app logging

1/ Run the app
2/ Click settings
2a/ Under developer operator choose a setting you've customised as above
2b/ Under serving operaotor select 'No auto assist'
2c/ Uncheck 'Send MCC/MNC in discovery'
2d/ Check 'Send cookies if using webview'
2e/ Under Discovery Startup Option' choose 'Manually Controlled Discovery'
2f/ Click 'Clear Discovery/ Logo Cache'
2g/ Press the back button
3/ Press discover - and enter your mobile number/ select the operator (this happens in a webview)
4/ When discovery is complete you should see a new button appear 'Connect via operator' - press this and you will be 
   taken throught the Mobile Connect OpenID API calls 

In case of problems
-------------------

Please get in touch with us via https://developer.mobileconnect.io/content/contact-us 
