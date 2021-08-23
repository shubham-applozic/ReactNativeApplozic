# Applozic-React-Native-Chat-Messaging-SDK


Applozic powers real time messaging across any device, any platform & anywhere in the world. Integrate our simple SDK to engage your users with image, file, location sharing and audio/video conversations.

Signup at https://www.applozic.com/signup.html to get the App Id.

## Installation

install plugin in root directory of react-native project.

```
npm install react-native-applozic-chat --save
```

### Android

#### Add applozic-chat in gradle

1. Open project's android/app/build.gradle file add below dependency

```
dependencies {
    ...
    compile project(':react-native-applozic-chat')
}
```

2. Open android/settings.gradle and include below

```
include ':react-native-applozic-chat'
project(':react-native-applozic-chat').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-applozic-chat/android')
```

3. Add ApplozicChatPackage in MainApplication.java

```
 @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
          new MainReactPackage(),new ApplozicChatPackage()
      );
    }
```

### iOS

Setup your Podfile located at /ios/Podfile and add below pod dependency.
>Note: Applozic requires min ios platform version 10 and uses dynamic frameworks. Make sure you have the below settings at the top of your Podfile:

```
platform :ios, '10.0'
use_frameworks!
```
Navigate to YourApp/ios directory from terminal and run the below command:

```
pod install
```

If you have not yet using pod dependency, check out how you can add pod in your react-native [here](https://guides.cocoapods.org/using/getting-started.html])

#### Add Bridge Files

1) Copy applozic folder from [here](https://github.com/AppLozic/Applozic-React-Native-Chat-Messaging-SDK/tree/master/ios/applozic) to /ios/YourApp folder to your project.

2) Open project from .xcworkspace file from /ios/ folder in Xcode.

**NOTE :** Please make sure to use .xcworkspace, not .xcproject after that.

 3) Add all .h and .m files to your project from applozic folder in step (1)

#### Add permission

Add permissions for Camera, Photo Library, Microphone, Contacts and Location usage. </br>

Note: We won't be asking the users for these permissions unless they use the respective feature. Due to Apple's requirement, we have to add these permissions if we are using any of their APIs related to Camera, Microphone etc.

Open `Info.plist` from `/ios/YOUR_PROJECT_NAME/Info.plist` file and paste these permissions anywhere inside the `<dict>` tag.
```
<key>NSCameraUsageDescription</key>
<string>Allow Camera</string>
<key>NSContactsUsageDescription</key>
<string>Allow Contacts</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>Allow location sharing!!</string>
<key>NSMicrophoneUsageDescription</key>
<string>Allow MicroPhone</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>Allow Photos</string>
<key>NSPhotoLibraryAddUsageDescription</key>
<string>Allow write access</string>
```
## Setup AppId

This will guide to setup your APP-ID in your project.
Your App ID can be found here [App ID] [install](https://console.applozic.com/settings/install)

### Android
In your app android manifest file inside `android/app/src/main/AndroidManifest.xml` add the below metadata and put your APP-ID in value

```
<meta-data android:name="com.applozic.application.key"
           android:value="<YOUR_APPLOZIC_APP_ID>" />
```

### iOS

Open the `ALChatManager.h` and Replace "applozic-sample-app" with your APP-ID.

```
#define APPLICATION_ID @"<YOUR_APPLOZIC_APP_ID>"
```

 ### Push Notification

 ### iOS

   Open AppDelegate.m file under /ios/YOUR_PROJECT/

   Add code as mentioned in the following documentation link [here](https://docs.applozic.com/docs/ios-push-notification#section-setting-up-apns-certificates)

 ### Android

 #### gradle changes
 1. Open android/app/build.gradle and add below at bottom

 ```
 apply plugin: 'com.google.gms.google-services'
 ```
 2. open project android/build.gradle and add google service dependency.

 ```
 dependencies {
        ......
        classpath 'com.google.gms:google-services:4.0.1'
    }
 ```
 #### FCM setup
 Follow below documentation for FCM setup link [here](https://docs.applozic.com/docs/android-push-notification)

 ## Integration

Import NativeModules
```js
 import {
   NativeModules
} from 'react-native';
```

Define native module for accessing methods.

 ```js
 var ApplozicChat = NativeModules.ApplozicChat;
```

 ### User Authentication

 ```
 ApplozicChat.login({
                    'userId': UNIQUE_ID, //Please Note: +,*,/ is not allowed in userIds and it needs be string.
                    'email': EMAIL,
                    'contactNumber': PHONE NO,
                    'password': PASSWORD,
                     authenticationTypeId: 1,
                    'displayName': DISPLAY NAME OF USER
                }, (error, response) => {
                  if(error){
                      console.log(error)
                  }else{
                    // Login success
                    console.log(response);
                  }
                })

 ```
 ### Chat List

 ```
 ApplozicChat.openChat();
 ```

 ### One to one Chat

 ```
 ApplozicChat.openChatWithUser(userId);
 ```

 ### Group Chat

 1. With Applozic generated GroupId
 ```
 ApplozicChat.openChatWithGroup(groupId , (error,response) =>{
                if(error){
                  //Group launch error
                  console.log(error);
                }else{
                  //group launch successful
                  console.log(response)
                }
              });
 ```
 **NOTE**: groupId must be Numeric.

 2. With your assigned groupId (Client group Id)

```
ApplozicChat.openChatWithClientGroupId(clientGroupId, (error,response) =>{
            if(error){
              //Group launch error
              console.log(error);
            }else{
              //group launch successful
              console.log(response)
            }
          });
```
 **NOTE**: groupId must be String

 ### Unread Count

 1) Individual user

  To get the user's chat unread count with any user, use the below code:

  ```
    ApplozicChat.getUnreadCountForUser( <user Id>, (error, count) => {
              console.log("count for userId:" + count);
            });
  ```
 2) Individual Group

To get the user's chat unread count in any group, use the below code:

```
  var requestData = {
                'clientGroupId':'recatNativeCGI',
                'groupId': GROUP_ID // pass either channelKey or clientGroupId
            };

          ApplozicChat.getUnreadCountForChannel(requestData, (error, count) => {
            if(error){
              console.log("error ::" + error);
            }else{
              console.log("count for requestData ::" + count);
            }
          });

```
 3) Total count

 To get the user's total unread count i.e Users + Groups, use the below code:

 ```
 ApplozicChat.totalUnreadCount((error, totalUnreadCount) => {
              console.log("totalUnreadCount for logged-in user:" + totalUnreadCount);

            });

 ```
 ### Create Group

 ```
 var groupDetails = {
                'groupName':'React Test2',
                'clientGroupId':'recatNativeCGI',
                'groupMemberList': ['ak101', 'ak102', 'ak103'], // Pass list of user Ids in groupMemberList
                'imageUrl': 'https://www.applozic.com/favicon.ico',
                'type' : 2,    //'type' : 1, //(required) 1:private, 2:public, 5:broadcast,7:GroupofTwo
                'metadata' : {
                    'key1' : 'value1',
                    'key2' : 'value2'
                }
            };
            ApplozicChat.createGroup(groupDetails, (error, response) => {
                if(error){
                    console.log(error)
                }else{
                  console.log(response);
                }
              });
 ```
 ### Add member to group


 ```
   var requestData = {
              'clientGroupId':'recatNativeCGI',
              'userId': 'ak111', // Pass list of user Ids in groupMemberList
          };

          ApplozicChat.addMemberToGroup(requestData, (error, response) => {
               if(error){
                   console.log(error)
               }else{
                 console.log(response);
               }
             });
 ```
 ### Remove member from group

 ```
 var requestData = {
             'clientGroupId':'recatNativeCGI',
             'userId': 'ak104', // Pass list of user Ids in groupMemberList
         };

          ApplozicChat.removeUserFromGroup(requestData, (error, response) => {
              if(error){
                  console.log(error)
              }else{
                console.log(response);
              }
            });
 ```

 ### Logout

 ```
   ApplozicChat.logoutUser((error, response) => {
              if(error){
                console.log("error" + error);
              }else{
                console.log(response);
              }

            });
 ```
# ReactNativeApplozic
