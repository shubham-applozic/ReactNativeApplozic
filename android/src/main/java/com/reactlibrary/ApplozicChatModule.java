package com.reactlibrary;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.applozic.mobicomkit.Applozic;
import com.applozic.mobicomkit.ApplozicClient;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.MessageBuilder;
import com.applozic.mobicomkit.exception.ApplozicException;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.listners.MediaUploadProgressHandler;
import com.applozic.mobicomkit.uiwidgets.ApplozicSetting;
import com.applozic.mobicomkit.api.account.register.RegistrationResponse;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.User;
import com.applozic.mobicomkit.api.account.user.PushNotificationTask;
import com.applozic.mobicomkit.api.conversation.database.MessageDatabaseService;
import com.applozic.mobicomkit.api.people.ChannelInfo;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.uiwidgets.async.AlGroupInformationAsyncTask;
import com.applozic.mobicomkit.uiwidgets.async.ApplozicChannelAddMemberTask;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.ConversationActivity;
import com.applozic.mobicommons.file.FileUtils;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.applozic.mobicomkit.feed.AlResponse;
import com.applozic.mobicomkit.uiwidgets.async.ApplozicChannelRemoveMemberTask;
import com.applozic.mobicommons.people.contact.Contact;
import com.applozic.mobicommons.task.AlTask;
import com.applozic.mobicomkit.listners.AlLoginHandler;
import com.applozic.mobicomkit.listners.AlLogoutHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ApplozicChatModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    public ApplozicChatModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "ApplozicChat";
    }

    @ReactMethod
    public void login(final ReadableMap config, final Callback callback) {
        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke("Error", "Activity doesn't exist.");
            return;
        }

        MobiComUserPreference.getInstance(currentActivity).setUrl("https://staging.applozic.com");

        User user = (User) GsonUtils.getObjectFromJson(GsonUtils.getJsonFromObject(config.toHashMap(), HashMap.class), User.class);
        Applozic.connectUser(currentActivity, user, new AlLoginHandler() {
            @Override
            public void onSuccess(RegistrationResponse registrationResponse, Context context) {
                if (MobiComUserPreference.getInstance(currentActivity).isRegistered()) {
                    String json = GsonUtils.getJsonFromObject(registrationResponse, RegistrationResponse.class);
                    callback.invoke(null, json);

                    PushNotificationTask pushNotificationTask = null;

                    PushNotificationTask.TaskListener listener = new PushNotificationTask.TaskListener() {
                        public void onSuccess(RegistrationResponse registrationResponse) {

                        }

                        @Override
                        public void onFailure(RegistrationResponse registrationResponse, Exception exception) {
                        }
                    };
                    String registrationId = Applozic.getInstance(context).getDeviceRegistrationId();
                    pushNotificationTask = new PushNotificationTask(registrationId, listener, currentActivity);
                    AlTask.execute(pushNotificationTask);
                } else {
                    String json = GsonUtils.getJsonFromObject(registrationResponse, RegistrationResponse.class);
                    callback.invoke(json, null);
                }

            }

            @Override
            public void onFailure(RegistrationResponse registrationResponse, Exception exception) {
                //If any failure in registration the callback  will come here
                callback.invoke(exception != null ? exception.toString() : "error", registrationResponse != null ? GsonUtils.getJsonFromObject(registrationResponse, RegistrationResponse.class) : "Unknown error occurred");
            }
        });
    }

    @ReactMethod
    public void openChat() {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.i("Error", "Activity doesn't exist.");
            return;
        }

        Intent intent = new Intent(currentActivity, ConversationActivity.class);
        currentActivity.startActivity(intent);
    }

    @ReactMethod
    public void openChatWithUser(String userId) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.i("Error", "Activity doesn't exist.");
            return;
        }

        Intent intent = new Intent(currentActivity, ConversationActivity.class);

        if (userId != null) {

            intent.putExtra(ConversationUIService.USER_ID, userId);
            intent.putExtra(ConversationUIService.TAKE_ORDER, true);

        }
        currentActivity.startActivity(intent);
    }

    @ReactMethod
    public void openChatWithGroup(Integer groupId, final Callback callback) {
        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke("Error", "Activity doesn't exist.");
            return;
        }

        if (groupId != null) {

            AlGroupInformationAsyncTask.GroupMemberListener taskListener = new AlGroupInformationAsyncTask.GroupMemberListener() {
                @Override
                public void onSuccess(Channel channel, Context context) {
                    Intent chatIntent = new Intent(context, ConversationActivity.class);
                    chatIntent.putExtra(ConversationUIService.GROUP_ID, channel.getKey());
                    chatIntent.putExtra(ConversationUIService.GROUP_NAME, channel.getName());
                    chatIntent.putExtra(ConversationUIService.TAKE_ORDER, true);
                    context.startActivity(chatIntent);
                    callback.invoke(null, "success");
                }

                @Override
                public void onFailure(Channel channel, Exception e, Context context) {
                    callback.invoke("Failed to launch group chat", null);
                }
            };
            AlGroupInformationAsyncTask groupInfoTask = new AlGroupInformationAsyncTask(currentActivity, groupId, taskListener);
            AlTask.execute(groupInfoTask);

        } else {
            callback.invoke("unable to launch group chat, check your groupId/ClientGroupId", null);
        }

    }

    @ReactMethod
    public void openChatWithClientGroupId(String clientGroupId, final Callback callback) {
        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke("Error", "Activity doesn't exist.");
            return;
        }

        if (TextUtils.isEmpty(clientGroupId)) {
            callback.invoke("unable to launch group chat, check your groupId/ClientGroupId", null);
        } else {

            AlGroupInformationAsyncTask.GroupMemberListener taskListener = new AlGroupInformationAsyncTask.GroupMemberListener() {
                @Override
                public void onSuccess(Channel channel, Context context) {
                    Intent chatIntent = new Intent(context, ConversationActivity.class);
                    chatIntent.putExtra(ConversationUIService.GROUP_ID, channel.getKey());
                    chatIntent.putExtra(ConversationUIService.GROUP_NAME, channel.getName());
                    chatIntent.putExtra(ConversationUIService.TAKE_ORDER, true);
                    context.startActivity(chatIntent);
                    callback.invoke(null, "success");
                }

                @Override
                public void onFailure(Channel channel, Exception e, Context context) {
                    callback.invoke("Failed to launch group chat", null);
                }
            };
            AlGroupInformationAsyncTask groupInfoTask = new AlGroupInformationAsyncTask(currentActivity, clientGroupId, taskListener);
            AlTask.execute(groupInfoTask);
        }
    }

    @ReactMethod
    public void logoutUser(final Callback callback) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke("Error", "Activity doesn't exist.");
            return;
        }

        Applozic.logoutUser(currentActivity, new AlLogoutHandler() {
            @Override
            public void onSuccess(Context context) {
                callback.invoke(null, "success");
            }

            @Override
            public void onFailure(Exception exception) {
                callback.invoke("Some internal error occurred", null);

            }
        });
    }

    //============================================ Group Method ==============================================

    /***
     *
     * @param config
     * @param callback
     */
    @ReactMethod
    public void createGroup(final ReadableMap config, final Callback callback) {

        final Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            callback.invoke("Error", "Activity doesn't exist.");
            return;

        }

        if (TextUtils.isEmpty(config.getString("groupName"))) {

            callback.invoke("Group name must be passed", null);
            return;
        }

        List<String> channelMembersList = (List<String>) (Object) (config.getArray("groupMemberList").toArrayList());

        final ChannelInfo channelInfo = new ChannelInfo(config.getString("groupName"), channelMembersList);

        if (!TextUtils.isEmpty(config.getString("clientGroupId"))) {
            channelInfo.setClientGroupId(config.getString("clientGroupId"));
        }
        if (config.hasKey("type")) {
            channelInfo.setType(config.getInt("type")); //group type
        } else {
            channelInfo.setType(Channel.GroupType.PUBLIC.getValue().intValue()); //group type
        }
        channelInfo.setImageUrl(config.getString("imageUrl")); //pass group image link URL
        Map<String, String> metadata = (HashMap<String, String>) (Object) (config.getMap("metadata").toHashMap());
        channelInfo.setMetadata(metadata);

        new Thread(new Runnable() {
            @Override
            public void run() {

                AlResponse alResponse = ChannelService.getInstance(currentActivity).createChannel(channelInfo);
                Channel channel = null;
                if (alResponse.isSuccess()) {
                    channel = (Channel) alResponse.getResponse();
                }
                if (channel != null && channel.getKey() != null) {
                    callback.invoke(null, channel.getKey());
                } else {
                    if (alResponse.getResponse() != null) {
                        callback.invoke(GsonUtils.getJsonFromObject(alResponse.getResponse(), List.class), null);
                    } else if (alResponse.getException() != null) {
                        callback.invoke(alResponse.getException().getMessage(), null);
                    }
                }
            }
        }).start();
    }

    /**
     * @param config
     * @param callback
     */
    @ReactMethod
    public void addMemberToGroup(final ReadableMap config, final Callback callback) {

        final Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            callback.invoke("Error", "Activity doesn't exist.");
            return;

        }

        Integer channelKey = null;
        String userId = config.getString("userId");

        if (!TextUtils.isEmpty(config.getString("clientGroupId"))) {
            Channel channel = ChannelService.getInstance(currentActivity).getChannelByClientGroupId(config.getString("clientGroupId"));
            channelKey = channel != null ? channel.getKey() : null;

        } else if (!TextUtils.isEmpty(config.getString("groupId"))) {
            channelKey = Integer.parseInt(config.getString("groupId"));
        }

        if (channelKey == null) {
            callback.invoke("groupId/clientGroupId not passed", null);
            return;
        }

        ApplozicChannelAddMemberTask.ChannelAddMemberListener channelAddMemberListener = new ApplozicChannelAddMemberTask.ChannelAddMemberListener() {
            @Override
            public void onSuccess(String response, Context context) {
                //Response will be "success" if user is added successfully
                Log.i("ApplozicChannelMember", "Add Response:" + response);
                callback.invoke(null, response);
            }

            @Override
            public void onFailure(String response, Exception e, Context context) {
                callback.invoke(response, null);

            }
        };

        ApplozicChannelAddMemberTask applozicChannelAddMemberTask = new ApplozicChannelAddMemberTask(currentActivity, channelKey, userId, channelAddMemberListener);//pass channel key and userId whom you want to add to channel
        AlTask.execute(applozicChannelAddMemberTask);
    }


    /**
     * @param config
     * @param callback
     */
    @ReactMethod
    public void removeUserFromGroup(final ReadableMap config, final Callback callback) {

        final Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            callback.invoke("Error", "Activity doesn't exist.");
            return;

        }

        Integer channelKey = null;
        String userId = config.getString("userId");

        if (!TextUtils.isEmpty(config.getString("clientGroupId"))) {
            Channel channel = ChannelService.getInstance(currentActivity).getChannelByClientGroupId(config.getString("clientGroupId"));
            channelKey = channel != null ? channel.getKey() : null;

        } else if (!TextUtils.isEmpty(config.getString("groupId"))) {
            channelKey = Integer.parseInt(config.getString("groupId"));
        }

        if (channelKey == null) {
            callback.invoke("groupId/clientGroupId not passed", null);
            return;
        }

        ApplozicChannelRemoveMemberTask.ChannelRemoveMemberListener channelRemoveMemberListener = new ApplozicChannelRemoveMemberTask.ChannelRemoveMemberListener() {
            @Override
            public void onSuccess(String response, Context context) {
                callback.invoke(null, response);
                //Response will be "success" if user is removed successfully
                Log.i("ApplozicChannel", "remove member response:" + response);
            }

            @Override
            public void onFailure(String response, Exception e, Context context) {
                callback.invoke(response, null);

            }
        };

        ApplozicChannelRemoveMemberTask applozicChannelRemoveMemberTask = new ApplozicChannelRemoveMemberTask(currentActivity, channelKey, userId, channelRemoveMemberListener);//pass channelKey and userId whom you want to remove from channel
        AlTask.execute(applozicChannelRemoveMemberTask);
    }
    //======================================================================================================

    @ReactMethod
    public void getUnreadCountForUser(String userId, final Callback callback) {

        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            callback.invoke("Error", "Activity doesn't exist.");
            return;
        }

        int contactUnreadCount = new MessageDatabaseService(getCurrentActivity()).getUnreadMessageCountForContact(userId);
        callback.invoke(null, contactUnreadCount);

    }

    @ReactMethod
    public void getUnreadCountForChannel(ReadableMap config, final Callback callback) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            callback.invoke("Error", "Activity doesn't exist.");
            return;
        }

        AlGroupInformationAsyncTask.GroupMemberListener listener = new AlGroupInformationAsyncTask.GroupMemberListener() {
            @Override
            public void onSuccess(Channel channel, Context context) {
                if (channel == null) {
                    callback.invoke("Channel dose not exist", null);
                } else {
                    callback.invoke(null, new MessageDatabaseService(context).getUnreadMessageCountForChannel(channel.getKey()));
                }
            }

            @Override
            public void onFailure(Channel channel, Exception e, Context context) {
                callback.invoke("Some error occurred : " + (e != null ? e.getMessage() : ""));
            }
        };

        if (config != null && config.hasKey("clientGroupId")) {
            AlGroupInformationAsyncTask groupInformationAsyncTask = new AlGroupInformationAsyncTask(currentActivity, config.getString("clientGroupId"), listener);
            AlTask.execute(groupInformationAsyncTask);
        } else if (config != null && config.hasKey("groupId")) {
            AlGroupInformationAsyncTask groupInformationAsyncTask = new AlGroupInformationAsyncTask(currentActivity, config.getInt("groupId"), listener);
            AlTask.execute(groupInformationAsyncTask);
        } else {
            callback.invoke("Invalid data sent");
        }
    }

    @ReactMethod
    public void setContactsGroupNameList(ReadableMap config) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.i("Error", "Activity doesn't exist.");
            return;
        }
        List<String> contactGroupIdList = Arrays.asList((String[]) GsonUtils.getObjectFromJson(config.getString("contactGroupNameList"), String[].class));
        Set<String> contactGroupIdsSet = new HashSet<String>(contactGroupIdList);
        MobiComUserPreference.getInstance(currentActivity).setIsContactGroupNameList(true);
        MobiComUserPreference.getInstance(currentActivity).setContactGroupIdList(contactGroupIdsSet);
    }

    @ReactMethod
    public void totalUnreadCount(final Callback callback) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            callback.invoke("Error", "Activity doesn't exist.");
            Log.i("Error", "Activity doesn't exist.");
            return;
        }

        int totalUnreadCount = new MessageDatabaseService(currentActivity).getTotalUnreadCount();
        callback.invoke(null, totalUnreadCount);

    }

    @ReactMethod
    public void isUserLogIn(final Callback successCallback) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.i("Error", "Activity doesn't exist.");
            return;
        }

        MobiComUserPreference mobiComUserPreference = MobiComUserPreference.getInstance(currentActivity);
        successCallback.invoke(mobiComUserPreference.isLoggedIn());
    }

    @ReactMethod
    public void sendMessage(final String messageJson, final Callback callback) {
        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke("Error", "Activity doesn't exist.");
            return;
        }

        final Message message = (Message) GsonUtils.getObjectFromJson(messageJson, Message.class);

        if (message == null) {
            callback.invoke("error", "Unable to parse data to Applozic Message");
            return;
        }

        new MessageBuilder(currentActivity).setMessageObject(message).send(new MediaUploadProgressHandler() {
            @Override
            public void onUploadStarted(ApplozicException e, String oldMessageKey) {

            }

            @Override
            public void onProgressUpdate(int percentage, ApplozicException e, String oldMessageKey) {

            }

            @Override
            public void onCancelled(ApplozicException e, String oldMessageKey) {

            }

            @Override
            public void onCompleted(ApplozicException e, String oldMessageKey) {

            }

            @Override
            public void onSent(Message message, String oldMessageKey) {
                callback.invoke("Message sent", oldMessageKey, GsonUtils.getJsonFromObject(message, Message.class));
            }
        });
    }

    @ReactMethod
    public void addContacts(String contactJson, Callback callback) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke("Error", "Activity doesn't exist.");
            return;
        }
        try {
            if (!TextUtils.isEmpty(contactJson)) {
                AppContactService appContactService = new AppContactService(currentActivity);
                Contact[] contactList = (Contact[]) GsonUtils.getObjectFromJson(contactJson, Contact[].class);
                for (Contact contact : contactList) {
                    appContactService.upsert(contact);
                }
                callback.invoke("Success", "Contacts inserted");
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.invoke("Error", e.getMessage());
            }
        }
    }

    @ReactMethod
    public void openChatWithUserName(String userId, String userName) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.i("Error", "Activity doesn't exist.");
            return;
        }

        Intent intent = new Intent(currentActivity, ConversationActivity.class);

        if (userId != null) {

            intent.putExtra(ConversationUIService.USER_ID, userId);
            intent.putExtra(ConversationUIService.TAKE_ORDER, true);

        }
        if (userName != null && userName != "") {
            intent.putExtra(ConversationUIService.DISPLAY_NAME, userName);
        }
        currentActivity.startActivity(intent);
    }

    @ReactMethod
    public void hideCreateGroupIcon(boolean hide) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.i("Error", "Activity doesn't exist.");
            return;
        }

        if (hide) {
            ApplozicSetting.getInstance(currentActivity).hideStartNewGroupButton();
        } else {
            ApplozicSetting.getInstance(currentActivity).showStartNewGroupButton();
        }
    }

    @ReactMethod
    public void showOnlyMyContacts(boolean showOnlyMyContacts) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.i("Error", "Activity doesn't exist.");
            return;
        }

        if (showOnlyMyContacts) {
            ApplozicClient.getInstance(currentActivity).enableShowMyContacts();
        } else {
            ApplozicClient.getInstance(currentActivity).disableShowMyContacts();
        }
    }

    @ReactMethod
    public void hideChatListOnNotification() {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.i("Error", "Activity doesn't exist.");
            return;
        }

        ApplozicClient.getInstance(currentActivity).hideChatListOnNotification();
    }

    @ReactMethod
    public void hideGroupSubtitle() {

    }

    @ReactMethod
    public void setAttachmentType(ReadableMap config) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.i("Error", "Activity doesn't exist.");
            return;
        }

        Map<FileUtils.GalleryFilterOptions, Boolean> options = new HashMap<>();

        if (config.hasKey("allFiles")) {
            options.put(FileUtils.GalleryFilterOptions.ALL_FILES, config.getBoolean("allFiles"));
        }

        if (config.hasKey("imageVideo")) {
            options.put(FileUtils.GalleryFilterOptions.IMAGE_VIDEO, config.getBoolean("imageVideo"));
        }

        if (config.hasKey("image")) {
            options.put(FileUtils.GalleryFilterOptions.IMAGE_ONLY, config.getBoolean("image"));
        }

        if (config.hasKey("audio")) {
            options.put(FileUtils.GalleryFilterOptions.AUDIO_ONLY, config.getBoolean("audio"));
        }

        if (config.hasKey("video")) {
            options.put(FileUtils.GalleryFilterOptions.VIDEO_ONLY, config.getBoolean("video"));
        }

        ApplozicSetting.getInstance(currentActivity).setGalleryFilterOptions(options);
    }

    @ReactMethod
    public void setAttachmentOptions(ReadableMap config) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            Log.i("Error", "Activity doesn't exist.");
            return;
        }

        Map<String, Boolean> options = new HashMap<>();

        for (Map.Entry<String, Object> item : config.toHashMap().entrySet()) {
            options.put(item.getKey(), (Boolean) item.getValue());
        }
        ApplozicSetting.getInstance(currentActivity).setAttachmentOptions(options);
    }

    @Override
    public void onActivityResult(Activity activity, final int requestCode, final int resultCode, final Intent intent) {
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

}
