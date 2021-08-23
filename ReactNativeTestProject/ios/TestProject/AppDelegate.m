#import "AppDelegate.h"

#import <React/RCTBridge.h>
#import <React/RCTBundleURLProvider.h>
#import <React/RCTRootView.h>
#import <UserNotifications/UserNotifications.h>
@import Applozic;

#ifdef FB_SONARKIT_ENABLED
#import <FlipperKit/FlipperClient.h>
#import <FlipperKitLayoutPlugin/FlipperKitLayoutPlugin.h>
#import <FlipperKitUserDefaultsPlugin/FKUserDefaultsPlugin.h>
#import <FlipperKitNetworkPlugin/FlipperKitNetworkPlugin.h>
#import <SKIOSNetworkPlugin/SKIOSNetworkAdapter.h>
#import <FlipperKitReactPlugin/FlipperKitReactPlugin.h>

static void InitializeFlipper(UIApplication *application) {
  FlipperClient *client = [FlipperClient sharedClient];
  SKDescriptorMapper *layoutDescriptorMapper = [[SKDescriptorMapper alloc] initWithDefaults];
  [client addPlugin:[[FlipperKitLayoutPlugin alloc] initWithRootNode:application withDescriptorMapper:layoutDescriptorMapper]];
  [client addPlugin:[[FKUserDefaultsPlugin alloc] initWithSuiteName:nil]];
  [client addPlugin:[FlipperKitReactPlugin new]];
  [client addPlugin:[[FlipperKitNetworkPlugin alloc] initWithNetworkAdapter:[SKIOSNetworkAdapter new]]];
  [client start];
}
#endif

@interface AppDelegate () <UNUserNotificationCenterDelegate>

@end

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
#ifdef FB_SONARKIT_ENABLED
  InitializeFlipper(application);
#endif

  [self registerForNotification];
  // checks wheather app version is updated/changed then makes server call setting VERSION_CODE
  [ALRegisterUserClientService isAppUpdated];

  ALAppLocalNotifications *localNotification = [ALAppLocalNotifications appLocalNotificationHandler];
  [localNotification dataConnectionNotificationHandler];
  ALPushNotificationHandler *pushNotificationHandler = [ALPushNotificationHandler shared];
  [pushNotificationHandler dataConnectionNotificationHandler];

  RCTBridge *bridge = [[RCTBridge alloc] initWithDelegate:self launchOptions:launchOptions];
  RCTRootView *rootView = [[RCTRootView alloc] initWithBridge:bridge
                                                   moduleName:@"TestProject"
                                            initialProperties:nil];

  rootView.backgroundColor = [[UIColor alloc] initWithRed:1.0f green:1.0f blue:1.0f alpha:1];

  self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
  UIViewController *rootViewController = [UIViewController new];
  rootViewController.view = rootView;
  self.window.rootViewController = rootViewController;
  [self.window makeKeyAndVisible];

  if (launchOptions != nil)
  {
    NSDictionary *dictionary = [launchOptions objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey];
    if (dictionary != nil)
    {
      NSLog(@"Launched from push notification: %@", dictionary);
      ALPushNotificationService *pushNotificationService = [[ALPushNotificationService alloc] init];
      BOOL applozicProcessed = [pushNotificationService processPushNotification:dictionary
                                                                       updateUI:[NSNumber numberWithInt:APP_STATE_INACTIVE]];

      if (!applozicProcessed) {
        //Note: notification for app
      }
    }
  }

  return YES;
}

- (NSURL *)sourceURLForBridge:(RCTBridge *)bridge
{
#if DEBUG
  return [[RCTBundleURLProvider sharedSettings] jsBundleURLForBundleRoot:@"index" fallbackResource:nil];
#else
  return [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
#endif
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
  // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
  [application setApplicationIconBadgeNumber:0];
}

-(void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(nonnull void (^)(UIBackgroundFetchResult))completionHandler{

  NSLog(@"RECEIVED_NOTIFICATION_WITH_COMPLETION :: %@", userInfo);
  ALPushNotificationService *pushNotificationService = [[ALPushNotificationService alloc] init];
  [pushNotificationService notificationArrivedToApplication:application withDictionary:userInfo];
  completionHandler(UIBackgroundFetchResultNewData);
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification*)notification withCompletionHandler:(void (^)(UNNotificationPresentationOptions options))completionHandler {

  ALPushNotificationService *pushNotificationService = [[ALPushNotificationService
                                                         alloc] init];
  NSDictionary *userInfo = notification.request.content.userInfo;

  if ([pushNotificationService isApplozicNotification:userInfo]) {
    [pushNotificationService notificationArrivedToApplication:[UIApplication sharedApplication] withDictionary:userInfo];
    completionHandler(UNNotificationPresentationOptionNone);
    return;
  }
  completionHandler(UNNotificationPresentationOptionAlert|UNNotificationPresentationOptionBadge|UNNotificationPresentationOptionSound);
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(nonnull UNNotificationResponse* )response withCompletionHandler:(nonnull void (^)(void))completionHandler {


  ALPushNotificationService *pushNotificationService = [[ALPushNotificationService
                                                         alloc] init];
  NSDictionary *userInfo =  response.notification.request.content.userInfo;
  if ([pushNotificationService isApplozicNotification:userInfo]) {
    [pushNotificationService notificationArrivedToApplication:[UIApplication sharedApplication] withDictionary:userInfo];
    completionHandler();
    return;
  }
  completionHandler();
}


- (void)applicationWillTerminate:(UIApplication *)application {
  // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.

  [[ALDBHandler sharedInstance] saveContext];
}

- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
  NSLog(@"DEVICE_TOKEN :: %@", deviceToken);

  const unsigned *tokenBytes = [deviceToken bytes];
  NSString *hexToken = [NSString stringWithFormat:@"%08x%08x%08x%08x%08x%08x%08x%08x",
                        ntohl(tokenBytes[0]), ntohl(tokenBytes[1]), ntohl(tokenBytes[2]),
                        ntohl(tokenBytes[3]), ntohl(tokenBytes[4]), ntohl(tokenBytes[5]),
                        ntohl(tokenBytes[6]), ntohl(tokenBytes[7])];

  NSString *apnDeviceToken = hexToken;
  NSLog(@"APN_DEVICE_TOKEN :: %@", hexToken);

  if ([[ALUserDefaultsHandler getApnDeviceToken] isEqualToString:apnDeviceToken])
  {
    return;
  }

  ALRegisterUserClientService *registerUserClientService = [[ALRegisterUserClientService alloc] init];
  [registerUserClientService updateApnDeviceTokenWithCompletion:apnDeviceToken withCompletion:^(ALRegistrationResponse *rResponse, NSError *error) {

    if (error)
    {
      NSLog(@"REGISTRATION ERROR :: %@",error.description);
      return;
    }

    NSLog(@"Registration response from server : %@", rResponse);
  }];
}


-(void)registerForNotification {
  UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
  center.delegate = self;
  [center requestAuthorizationWithOptions:(UNAuthorizationOptionSound | UNAuthorizationOptionAlert | UNAuthorizationOptionBadge) completionHandler:^(BOOL granted, NSError * _Nullable error)
   {
    if(!error)
    {
      dispatch_async(dispatch_get_main_queue(), ^ {
        [[UIApplication sharedApplication] registerForRemoteNotifications];  // required to get the app to do anything at all about push notifications
        NSLog(@"Push registration success." );
      });
    }
    else
    {
      NSLog(@"Push registration FAILED" );
      NSLog(@"ERROR: %@ - %@", error.localizedFailureReason, error.localizedDescription );
      NSLog(@"SUGGESTIONS: %@ - %@", error.localizedRecoveryOptions, error.localizedRecoverySuggestion );
    }
  }];
}

@end
