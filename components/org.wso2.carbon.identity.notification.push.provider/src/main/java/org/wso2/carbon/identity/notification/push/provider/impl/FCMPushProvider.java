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

package org.wso2.carbon.identity.notification.push.provider.impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.push.provider.constant.PushProviderConstants;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.internal.ProviderDataHolder;
import org.wso2.carbon.identity.notification.push.provider.model.PushDeviceData;
import org.wso2.carbon.identity.notification.push.provider.model.PushNotificationData;
import org.wso2.carbon.identity.notification.push.provider.model.PushSenderData;
import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementException;
import org.wso2.carbon.identity.secret.mgt.core.model.ResolvedSecret;
import org.wso2.carbon.identity.secret.mgt.core.model.Secret;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.google.firebase.messaging.MessagingErrorCode.INVALID_ARGUMENT;
import static com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED;
import static org.wso2.carbon.identity.notification.push.provider.constant.PushProviderConstants.FCM_SERVICE_ACCOUNT_SECRET;
import static org.wso2.carbon.identity.notification.push.provider.constant.PushProviderConstants.PUSH_PROVIDER_SECRET_TYPE;

/**
 * Push provider implementation for Firebase Cloud Messaging (FCM).
 */
public class FCMPushProvider implements PushProvider {

    private static final Log log = LogFactory.getLog(FCMPushProvider.class);
    private static final String FCM_PROVIDER_NAME = "FCM";
    private static final String FCM_APP_PREFIX = "FirebaseApp-";
    private static final String FCM_SECRET_REFERENCE = "FCM-credentials";

    @Override
    public String getName() {

        return FCM_PROVIDER_NAME;
    }

    @Override
    public void sendNotification(PushNotificationData pushNotificationData, PushSenderData pushSenderData,
                                 String tenantDomain) throws PushProviderException {

        log.debug("Initiating push notification sending process for FCM provider.");
        String appName = generateFirebaseAppName(tenantDomain, pushSenderData.getProviderId());

        if (FirebaseApp.getApps().stream().noneMatch(app -> app.getName().equals(appName))) {
            log.debug("Firebase app instance not found. Initializing new Firebase app.");

            Map<String, String> processedProperties = this.preProcessProperties(pushSenderData);
            String serviceAccountString = processedProperties.get(FCM_SERVICE_ACCOUNT_SECRET);
            if (StringUtils.isBlank(serviceAccountString)) {
                log.debug("Service account credentials are missing.");
                throw new PushProviderException("Service account credentials are not provided for FCM push provider.");
            }

            log.debug("Processing service account credentials for Firebase authentication.");
            GoogleCredentials credentials;
            try {
                ByteArrayInputStream jsonInputStream = new ByteArrayInputStream(
                        serviceAccountString.getBytes(StandardCharsets.UTF_8));
                credentials = GoogleCredentials.fromStream(jsonInputStream);
                log.debug("Successfully loaded Google credentials from service account.");
            } catch (IOException e) {
                log.debug("Failed to read service account credentials.");
                throw new PushProviderException("Error occurred while reading the service account credentials.", e);
            }

            FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
            FirebaseApp.initializeApp(options, appName);
            log.debug("Successfully initialized the firebase app.");
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Using existing Firebase app instance.");
            }
        }

        FirebaseApp firebaseApp = FirebaseApp.getInstance(appName);

        // Build the content for the pop-up notification.
        Notification notification = Notification.builder()
                .setTitle(pushNotificationData.getNotificationTitle())
                .setBody(pushNotificationData.getNotificationBody())
                .build();

        // Build the push notification message.
        log.debug("Building push notification message with device token and additional data.");
        Message message = Message.builder()
                .setToken(pushNotificationData.getDeviceToken())
                .setNotification(notification)
                .putAllData(pushNotificationData.getAdditionalData())
                .build();

        try {
            log.debug("Sending push notification via Firebase Messaging.");
            String response = FirebaseMessaging.getInstance(firebaseApp).send(message);
            if (log.isDebugEnabled()) {
                log.debug("Successfully sent message: " + response);
            }
        } catch (FirebaseMessagingException e) {
            log.debug("Error while sending the push notification.", e);
            throw handleFirebaseMessagingException(e);
        }
    }

    @Override
    public void registerDevice(PushDeviceData device, PushSenderData pushSenderData) throws PushProviderException {

        // FCM does not require any registration to be done on its side. Hence, the method is not implemented.
    }

    @Override
    public void unregisterDevice(PushDeviceData device, PushSenderData pushSenderData) throws PushProviderException {

        // FCM does not require any unregistration to be done on its side. Hence, the method is not implemented.
    }

    @Override
    public void updateDevice(PushDeviceData device, PushSenderData pushSenderData) throws PushProviderException {

        // FCM does not require any update to be done on its side. Hence, the method is not implemented.
    }

    @Override
    public Map<String, String> preProcessProperties(PushSenderData pushSenderData) throws PushProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Pre-processing properties for providerId: " + pushSenderData.getProviderId());
        }
        Map<String, String> properties = new HashMap<>(pushSenderData.getProperties());
        String serviceAccountString = properties.get(FCM_SERVICE_ACCOUNT_SECRET);
        if (StringUtils.isBlank(serviceAccountString)) {
            log.debug("Service account credentials are missing in properties.");
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_REQUIRED_PROPERTY_MISSING;
            throw new PushProviderException(error.getCode(), error.getMessage() + FCM_SERVICE_ACCOUNT_SECRET);
        }
        log.debug("Decoding Base64 encoded service account credentials.");
        String decodedServiceAccountString = new String(Base64.getDecoder().decode(serviceAccountString),
                StandardCharsets.UTF_8);
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, decodedServiceAccountString);
        return properties;
    }

    @Override
    public Map<String, String> postProcessProperties(PushSenderData pushSenderData) throws PushProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Post-processing properties for providerId: " + pushSenderData.getProviderId());
        }
        Map<String, String> properties = new HashMap<>(pushSenderData.getProperties());
        String serviceAccountString = properties.get(FCM_SERVICE_ACCOUNT_SECRET);
        if (StringUtils.isBlank(serviceAccountString)) {
            log.debug("Service account credentials are missing in properties.");
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_REQUIRED_PROPERTY_MISSING;
            throw new PushProviderException(error.getCode(), error.getMessage() + FCM_SERVICE_ACCOUNT_SECRET);
        }
        log.debug("Encoding service account credentials to Base64.");
        String encodedServiceAccountString = Base64.getEncoder()
                .encodeToString(serviceAccountString.getBytes(StandardCharsets.UTF_8));
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, encodedServiceAccountString);
        return properties;
    }

    @Override
    public void updateCredentials(PushSenderData pushSenderData, String tenantDomain) throws PushProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Updating credentials for tenant: " + tenantDomain + ", providerId: " +
                    pushSenderData.getProviderId());
        }
        String appName = generateFirebaseAppName(tenantDomain, pushSenderData.getProviderId());
        FirebaseApp.getApps().stream().filter(app -> app.getName().equals(appName)).forEach(FirebaseApp::delete);
    }

    @Override
    public Map<String, String> storePushProviderSecretProperties(PushSenderData pushSenderData)
            throws PushProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Storing push provider secret properties for providerId: " + pushSenderData.getProviderId());
        }
        try {
            Map<String, String> properties = new HashMap<>(pushSenderData.getProperties());
            String serviceAccountContent = properties.get(FCM_SERVICE_ACCOUNT_SECRET);
            if (StringUtils.isBlank(serviceAccountContent)) {
                log.debug("Service account credentials are missing in properties.");
                PushProviderConstants.ErrorMessages error =
                        PushProviderConstants.ErrorMessages.ERROR_REQUIRED_PROPERTY_MISSING;
                throw new PushProviderException(error.getCode(), error.getMessage() + FCM_SERVICE_ACCOUNT_SECRET);
            }
            SecretManager secretManager = ProviderDataHolder.getInstance().getSecretManager();
            if (secretManager.isSecretExist(PUSH_PROVIDER_SECRET_TYPE, FCM_SECRET_REFERENCE)) {
                log.debug("Updating existing secret in secret manager.");
                // Update the existing secret.
                secretManager.updateSecretValue(PUSH_PROVIDER_SECRET_TYPE, FCM_SECRET_REFERENCE, serviceAccountContent);
            } else {
                log.debug("Adding new secret to secret manager.");
                // Add the new secret.
                Secret newSecret = new Secret();
                newSecret.setSecretType(PUSH_PROVIDER_SECRET_TYPE);
                newSecret.setSecretName(FCM_SECRET_REFERENCE);
                newSecret.setSecretValue(serviceAccountContent);
                secretManager.addSecret(PUSH_PROVIDER_SECRET_TYPE, newSecret);
            }
            properties.put(FCM_SERVICE_ACCOUNT_SECRET, FCM_SECRET_REFERENCE);
            return properties;
        } catch (SecretManagementException e) {
            log.debug("Error occurred while storing secrets.");
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_WHILE_STORING_SECRETS_OF_PUSH_PROVIDER;
            throw new PushProviderException(error.getCode(), error.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> retrievePushProviderSecretProperties(PushSenderData pushSenderData)
            throws PushProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Retrieving push provider secret properties for providerId: " + pushSenderData.getProviderId());
        }
        try {
            Map<String, String> properties = new HashMap<>(pushSenderData.getProperties());
            String serviceAccountContent = properties.get(FCM_SERVICE_ACCOUNT_SECRET);
            if (StringUtils.isBlank(serviceAccountContent)) {
                log.debug("Service account credentials are missing in properties.");
                PushProviderConstants.ErrorMessages error =
                        PushProviderConstants.ErrorMessages.ERROR_REQUIRED_PROPERTY_MISSING;
                throw new PushProviderException(error.getCode(), error.getMessage() + FCM_SERVICE_ACCOUNT_SECRET);
            }

            SecretManager secretManager = ProviderDataHolder.getInstance().getSecretManager();
            SecretResolveManager secretResolveManager = ProviderDataHolder.getInstance().getSecretResolveManager();
            if (secretManager.isSecretExist(PUSH_PROVIDER_SECRET_TYPE, FCM_SECRET_REFERENCE)) {
                log.debug("Secret exists. Resolving secret value.");
                ResolvedSecret resolvedSecret =
                        secretResolveManager.getResolvedSecret(PUSH_PROVIDER_SECRET_TYPE, FCM_SECRET_REFERENCE);
                properties.put(FCM_SERVICE_ACCOUNT_SECRET, resolvedSecret.getResolvedSecretValue());
                return properties;
            } else {
                log.debug("Secret does not exist in secret manager.");
                PushProviderConstants.ErrorMessages error =
                        PushProviderConstants.ErrorMessages.ERROR_WHILE_RETRIEVING_SECRETS_OF_PUSH_PROVIDER;
                throw new PushProviderException(error.getCode(), error.getMessage());
            }
        } catch (SecretManagementException e) {
            log.debug("Error occurred while retrieving secrets.");
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_WHILE_RETRIEVING_SECRETS_OF_PUSH_PROVIDER;
            throw new PushProviderException(error.getCode(), error.getMessage(), e);
        }
    }

    @Override
    public void deletePushProviderSecretProperties(PushSenderData pushSenderData)
            throws PushProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Deleting push provider secret properties for providerId: " + pushSenderData.getProviderId());
        }
        try {
            SecretManager secretManager = ProviderDataHolder.getInstance().getSecretManager();
            if (secretManager.isSecretExist(PUSH_PROVIDER_SECRET_TYPE, FCM_SECRET_REFERENCE)) {
                log.debug("Secret exists. Deleting secret from secret manager.");
                secretManager.deleteSecret(PUSH_PROVIDER_SECRET_TYPE, FCM_SECRET_REFERENCE);
            } else {
                log.debug("Secret does not exist in secret manager. No deletion required.");
            }
        } catch (SecretManagementException e) {
            log.debug("Error occurred while deleting secrets.");
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_WHILE_DELETING_SECRETS_OF_PUSH_PROVIDER;
            throw new PushProviderException(error.getCode(), error.getMessage(), e);
        }
    }

    /**
     * Handle FirebaseMessagingException and throw PushProviderException with the appropriate error message.
     *
     * @param e FirebaseMessagingException
     * @throws PushProviderException PushProviderException
     */
    private PushProviderException handleFirebaseMessagingException(FirebaseMessagingException e)
            throws PushProviderException {

        MessagingErrorCode errorCodeFromProvider = e.getMessagingErrorCode();
        PushProviderConstants.ErrorMessages error;
        if (errorCodeFromProvider == INVALID_ARGUMENT) {
            error = PushProviderConstants.ErrorMessages.ERROR_INVALID_DEVICE_HANDLE_FOR_CONFIGURED_PROVIDER;
        } else if (errorCodeFromProvider == UNREGISTERED) {
            error = PushProviderConstants.ErrorMessages.ERROR_DEVICE_HANDLE_EXPIRED_OR_NEW_REGISTRATION_REQUIRED;
        } else {
            error = PushProviderConstants.ErrorMessages.ERROR_PUSH_NOTIFICATION_SENDING_FAILED;
        }
        throw new PushProviderException(error.getCode(), error.getMessage(), e);
    }

    private String generateFirebaseAppName(String tenantDomain, String providerId) {

        return FCM_APP_PREFIX + tenantDomain + "-" + providerId;
    }
}
