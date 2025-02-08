/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.notification.push.provider.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class for push notification data using Builder pattern.
 */
public class PushNotificationData {

    private final String notificationTitle;
    private final String notificationBody;
    private final String deviceId;
    private final String deviceToken;
    private final String username;
    private final String tenantDomain;
    private final String organizationId;
    private final String organizationName;
    private final String userStoreDomain;
    private final String applicationName;
    private final String notificationScenario;
    private final String pushId;
    private final String challenge;
    private final String numberChallenge;
    private final String ipAddress;
    private final String deviceOS;
    private final String browser;

    private PushNotificationData(Builder builder) {

        this.notificationTitle = builder.notificationTitle;
        this.notificationBody = builder.notificationBody;
        this.username = builder.username;
        this.tenantDomain = builder.tenantDomain;
        this.organizationId = builder.organizationId;
        this.organizationName = builder.organizationName;
        this.userStoreDomain = builder.userStoreDomain;
        this.applicationName = builder.applicationName;
        this.notificationScenario = builder.notificationScenario;
        this.pushId = builder.pushId;
        this.deviceToken = builder.deviceToken;
        this.deviceId = builder.deviceId;
        this.challenge = builder.challenge;
        this.numberChallenge = builder.numberChallenge;
        this.ipAddress = builder.ipAddress;
        this.deviceOS = builder.deviceOS;
        this.browser = builder.browser;
    }

    public String getNotificationTitle() {

        return notificationTitle;
    }

    public String getNotificationBody() {

        return notificationBody;
    }

    public String getUsername() {

        return username;
    }

    public String getTenantDomain() {

        return tenantDomain;
    }

    public String getOrganizationId() {

        return organizationId;
    }

    public String getOrganizationName() {

        return organizationName;
    }

    public String getUserStoreDomain() {

        return userStoreDomain;
    }

    public String getApplicationName() {

        return applicationName;
    }

    public String getNotificationScenario() {

        return notificationScenario;
    }

    public String getPushId() {

        return pushId;
    }

    public String getDeviceToken() {

        return deviceToken;
    }

    public String getDeviceId() {

        return deviceId;
    }

    public String getChallenge() {

        return challenge;
    }

    public String getNumberChallenge() {

        return numberChallenge;
    }

    public String getIpAddress() {

        return ipAddress;
    }

    public String getDeviceOS() {

        return deviceOS;
    }

    public Map<String, String> getAdditionalData() {

        Map<String, String> additionalData = new HashMap<>();
        boolean isOrganizationUser = organizationId != null && organizationName != null;
        if (username != null) {
            additionalData.put("username", username);
        }
        if (isOrganizationUser) {
            additionalData.put("organizationId", organizationId);
            additionalData.put("organizationName", organizationName);
        }
        if (tenantDomain != null && !isOrganizationUser) {
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
        if (deviceId != null) {
            additionalData.put("deviceId", deviceId);
        }
        return additionalData;
    }

    /**
     * Builder class for PushNotificationData.
     */
    public static class Builder {

        private String notificationTitle;
        private String notificationBody;
        private String deviceToken;
        private String deviceId;
        private String username;
        private String tenantDomain;
        private String organizationId;
        private String organizationName;
        private String userStoreDomain;
        private String applicationName;
        private String notificationScenario;
        private String pushId;
        private String challenge;
        private String numberChallenge;
        private String ipAddress;
        private String deviceOS;
        private String browser;

        public Builder setNotificationTitle(String notificationTitle) {

            this.notificationTitle = notificationTitle;
            return this;
        }

        public Builder setNotificationBody(String notificationBody) {

            this.notificationBody = notificationBody;
            return this;
        }

        public Builder setUsername(String username) {

            this.username = username;
            return this;
        }

        public Builder setTenantDomain(String tenantDomain) {

            this.tenantDomain = tenantDomain;
            return this;
        }

        public Builder setOrganizationId(String organizationId) {

            this.organizationId = organizationId;
            return this;
        }

        public Builder setOrganizationName(String organizationName) {

            this.organizationName = organizationName;
            return this;
        }

        public Builder setUserStoreDomain(String userStoreDomain) {

            this.userStoreDomain = userStoreDomain;
            return this;
        }

        public Builder setApplicationName(String applicationName) {

            this.applicationName = applicationName;
            return this;
        }

        public Builder setNotificationScenario(String notificationScenario) {

            this.notificationScenario = notificationScenario;
            return this;
        }

        public Builder setPushId(String pushId) {

            this.pushId = pushId;
            return this;
        }

        public Builder setDeviceToken(String deviceToken) {

            this.deviceToken = deviceToken;
            return this;
        }

        public Builder setDeviceId(String deviceId) {

            this.deviceId = deviceId;
            return this;
        }

        public Builder setChallenge(String challenge) {

            this.challenge = challenge;
            return this;
        }

        public Builder setNumberChallenge(String numberChallenge) {

            this.numberChallenge = numberChallenge;
            return this;
        }

        public Builder setIpAddress(String ipAddress) {

            this.ipAddress = ipAddress;
            return this;
        }

        public Builder setDeviceOS(String deviceOS) {

            this.deviceOS = deviceOS;
            return this;
        }

        public Builder setBrowser(String browser) {

            this.browser = browser;
            return this;
        }

        public PushNotificationData build() {

            return new PushNotificationData(this);
        }
    }
}
