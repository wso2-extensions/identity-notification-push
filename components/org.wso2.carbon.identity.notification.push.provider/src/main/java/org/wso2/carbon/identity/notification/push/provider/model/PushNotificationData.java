package org.wso2.carbon.identity.notification.push.provider.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class for push notification data.
 */
public class PushNotificationData {

    private String notificationTitle;
    private String notificationBody;
    private String username;
    private String tenantDomain;
    private String userStoreDomain;
    private String applicationName;
    private String notificationScenario;
    private String pushId;
    private String deviceToken;
    private String challenge;
    private String numberChallenge;
    private String ipAddress;
    private String deviceOS;
    private String browser;

    public PushNotificationData() {

    }

    public String getNotificationTitle() {

        return notificationTitle;
    }

    public void setNotificationTitle(String notificationTitle) {

        this.notificationTitle = notificationTitle;
    }

    public String getNotificationBody() {

        return notificationBody;
    }

    public void setNotificationBody(String notificationBody) {

        this.notificationBody = notificationBody;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public String getTenantDomain() {

        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {

        this.tenantDomain = tenantDomain;
    }

    public String getUserStoreDomain() {

        return userStoreDomain;
    }

    public void setUserStoreDomain(String userStoreDomain) {

        this.userStoreDomain = userStoreDomain;
    }

    public String getApplicationName() {

        return applicationName;
    }

    public void setApplicationName(String applicationName) {

        this.applicationName = applicationName;
    }

    public String getNotificationScenario() {

        return notificationScenario;
    }

    public void setNotificationScenario(String notificationScenario) {

        this.notificationScenario = notificationScenario;
    }

    public String getPushId() {

        return pushId;
    }

    public void setPushId(String pushId) {

        this.pushId = pushId;
    }

    public String getDeviceToken() {

        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {

        this.deviceToken = deviceToken;
    }

    public String getChallenge() {

        return challenge;
    }

    public void setChallenge(String challenge) {

        this.challenge = challenge;
    }

    public String getNumberChallenge() {

        return numberChallenge;
    }

    public void setNumberChallenge(String numberChallenge) {

        this.numberChallenge = numberChallenge;
    }

    public String getIpAddress() {

        return ipAddress;
    }

    public void setIpAddress(String hostName) {

        this.ipAddress = hostName;
    }

    public String getDeviceOS() {

        return deviceOS;
    }

    public void setDeviceOS(String deviceOS) {

        this.deviceOS = deviceOS;
    }

    public String getBrowser() {

        return browser;
    }

    public void setBrowser(String browser) {

        this.browser = browser;
    }

    public Map<String, String> getAdditionalData() {

        Map<String, String> additionalData = new HashMap<>();
        if (username != null) {
            additionalData.put("username", username);
        }
        if (tenantDomain != null) {
            additionalData.put("tenantDomain", tenantDomain);
        }
        if (userStoreDomain != null) {
            additionalData.put("userStoreDomain", userStoreDomain);
        }
        if (applicationName != null) {
            additionalData.put("applicationName", applicationName);
        }
        if (notificationScenario != null) {
            additionalData.put("notificationScenario", notificationScenario);
        }
        if (pushId != null) {
            additionalData.put("pushId", pushId);
        }
        if (challenge != null) {
            additionalData.put("challenge", challenge);
        }
        if (numberChallenge != null) {
            additionalData.put("numberChallenge", numberChallenge);
        }
        if (ipAddress != null) {
            additionalData.put("ipAddress", ipAddress);
        }
        if (deviceOS != null) {
            additionalData.put("deviceOS", deviceOS);
        }
        if (browser != null) {
            additionalData.put("browser", browser);
        }
        return additionalData;
    }
}
