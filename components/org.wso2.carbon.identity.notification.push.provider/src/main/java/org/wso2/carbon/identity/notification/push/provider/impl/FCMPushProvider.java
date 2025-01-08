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
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.PushSenderDTO;
import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementException;
import org.wso2.carbon.identity.secret.mgt.core.model.ResolvedSecret;
import org.wso2.carbon.identity.secret.mgt.core.model.Secret;
import org.wso2.carbon.identity.secret.mgt.core.model.SecretType;

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

    @Override
    public String getName() {

        return FCM_PROVIDER_NAME;
    }

    @Override
    public void sendNotification(PushNotificationData pushNotificationData, PushSenderDTO pushSenderDTO,
                                 String tenantDomain) throws PushProviderException {

        String appName = FCM_APP_PREFIX + pushSenderDTO.getProviderId();

        if (FirebaseApp.getApps().stream().noneMatch(app -> app.getName().equals(appName))) {

            Map<String, String> processedProperties = this.preProcessProperties(pushSenderDTO);
            String serviceAccountString = processedProperties.get(FCM_SERVICE_ACCOUNT_SECRET);
            if (StringUtils.isBlank(serviceAccountString)) {
                throw new PushProviderException("Service account credentials are not provided for FCM push provider.");
            }

            GoogleCredentials credentials;
            try {
                ByteArrayInputStream jsonInputStream = new ByteArrayInputStream(
                        serviceAccountString.getBytes(StandardCharsets.UTF_8));
                credentials = GoogleCredentials.fromStream(jsonInputStream);
            } catch (IOException e) {
                throw new PushProviderException("Error occurred while reading the service account credentials.", e);
            }

            FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
            FirebaseApp.initializeApp(options, appName);
        }

        FirebaseApp firebaseApp = FirebaseApp.getInstance(appName);

        // Build the content for the pop-up notification.
        Notification notification = Notification.builder()
                .setTitle(pushNotificationData.getNotificationTitle())
                .setBody(pushNotificationData.getNotificationBody())
                .build();

        // Build the push notification message.
        Message message = Message.builder()
                .setToken(pushNotificationData.getDeviceToken())
                .setNotification(notification)
                .putAllData(pushNotificationData.getAdditionalData())
                .build();

        try {
            String response = FirebaseMessaging.getInstance(firebaseApp).send(message);
            if (log.isDebugEnabled()) {
                log.debug("Successfully sent message: " + response);
            }
        } catch (FirebaseMessagingException e) {
            throw handleFirebaseMessagingException(e);
        }
    }

    @Override
    public void registerDevice(PushDeviceData device, PushSenderDTO pushSender) throws PushProviderException {

        // FCM does not require any registration to be done on its side. Hence, the method is not implemented.
    }

    @Override
    public void unregisterDevice(PushDeviceData device, PushSenderDTO pushSender) throws PushProviderException {

        // FCM does not require any unregistration to be done on its side. Hence, the method is not implemented.
    }

    @Override
    public void updateDevice(PushDeviceData device, PushSenderDTO pushSender) throws PushProviderException {

        // FCM does not require any update to be done on its side. Hence, the method is not implemented.
    }

    @Override
    public Map<String, String> preProcessProperties(PushSenderDTO pushSenderDTO) throws PushProviderException {

        Map<String, String> properties = new HashMap<>(pushSenderDTO.getProperties());
        String serviceAccountString = properties.get(FCM_SERVICE_ACCOUNT_SECRET);
        if (StringUtils.isBlank(serviceAccountString)) {
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_REQUIRED_PROPERTY_MISSING;
            throw new PushProviderException(error.getCode(), error.getMessage() + FCM_SERVICE_ACCOUNT_SECRET);
        }
        String decodedServiceAccountString = new String(Base64.getDecoder().decode(serviceAccountString),
                StandardCharsets.UTF_8);
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, decodedServiceAccountString);
        return properties;
    }

    @Override
    public Map<String, String> postProcessProperties(PushSenderDTO pushSenderDTO) throws PushProviderException {

        Map<String, String> properties = new HashMap<>(pushSenderDTO.getProperties());
        String serviceAccountString = properties.get(FCM_SERVICE_ACCOUNT_SECRET);
        if (StringUtils.isBlank(serviceAccountString)) {
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_REQUIRED_PROPERTY_MISSING;
            throw new PushProviderException(error.getCode(), error.getMessage() + FCM_SERVICE_ACCOUNT_SECRET);
        }
        String encodedServiceAccountString = Base64.getEncoder()
                .encodeToString(serviceAccountString.getBytes(StandardCharsets.UTF_8));
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, encodedServiceAccountString);
        return properties;
    }

    @Override
    public void updateCredentials(PushSenderDTO pushSender) throws PushProviderException {

        String appName = FCM_APP_PREFIX + pushSender.getProviderId();
        FirebaseApp.getApps().stream().filter(app -> app.getName().equals(appName)).forEach(FirebaseApp::delete);
    }

    @Override
    public Map<String, String> storePushProviderSecretProperties(PushSenderDTO pushSender)
            throws PushProviderException {

        try {
            Map<String, String> properties = new HashMap<>(pushSender.getProperties());
            String serviceAccountContent = properties.get(FCM_SERVICE_ACCOUNT_SECRET);
            if (StringUtils.isBlank(serviceAccountContent)) {
                PushProviderConstants.ErrorMessages error =
                        PushProviderConstants.ErrorMessages.ERROR_REQUIRED_PROPERTY_MISSING;
                throw new PushProviderException(error.getCode(), error.getMessage() + FCM_SERVICE_ACCOUNT_SECRET);
            }
            SecretManager secretManager = ProviderDataHolder.getInstance().getSecretManager();
            String secretName = constructSecretName(pushSender.getProviderId(), FCM_SERVICE_ACCOUNT_SECRET);
            if (secretManager.isSecretExist(PUSH_PROVIDER_SECRET_TYPE, secretName)) {
                // Update the existing secret.
                secretManager.updateSecretValue(PUSH_PROVIDER_SECRET_TYPE, secretName, serviceAccountContent);
            } else {
                // Add the new secret.
                Secret newSecret = new Secret();
                newSecret.setSecretType(PUSH_PROVIDER_SECRET_TYPE);
                newSecret.setSecretName(secretName);
                newSecret.setSecretValue(serviceAccountContent);
                secretManager.addSecret(PUSH_PROVIDER_SECRET_TYPE, newSecret);
            }
            SecretType secretType = secretManager.getSecretType(PUSH_PROVIDER_SECRET_TYPE);
            String secretReference = constructSecretReference(secretType.getId(), secretName);
            properties.put(FCM_SERVICE_ACCOUNT_SECRET, secretReference);
            return properties;
        } catch (SecretManagementException e) {
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_WHILE_STORING_SECRETS_OF_PUSH_PROVIDER;
            throw new PushProviderException(error.getCode(), error.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> retrievePushProviderSecretProperties(PushSenderDTO pushSender)
            throws PushProviderException {

        try {
            Map<String, String> properties = new HashMap<>(pushSender.getProperties());
            String serviceAccountContent = properties.get(FCM_SERVICE_ACCOUNT_SECRET);
            if (StringUtils.isBlank(serviceAccountContent)) {
                PushProviderConstants.ErrorMessages error =
                        PushProviderConstants.ErrorMessages.ERROR_REQUIRED_PROPERTY_MISSING;
                throw new PushProviderException(error.getCode(), error.getMessage() + FCM_SERVICE_ACCOUNT_SECRET);
            }

            SecretManager secretManager = ProviderDataHolder.getInstance().getSecretManager();
            SecretResolveManager secretResolveManager = ProviderDataHolder.getInstance().getSecretResolveManager();
            String secretName = constructSecretName(pushSender.getProviderId(), FCM_SERVICE_ACCOUNT_SECRET);
            if (secretManager.isSecretExist(PUSH_PROVIDER_SECRET_TYPE, secretName)) {
                ResolvedSecret resolvedSecret =
                        secretResolveManager.getResolvedSecret(PUSH_PROVIDER_SECRET_TYPE, secretName);
                properties.put(FCM_SERVICE_ACCOUNT_SECRET, resolvedSecret.getResolvedSecretValue());
                return properties;
            } else {
                PushProviderConstants.ErrorMessages error =
                        PushProviderConstants.ErrorMessages.ERROR_WHILE_RETRIEVING_SECRETS_OF_PUSH_PROVIDER;
                throw new PushProviderException(error.getCode(), error.getMessage());
            }
        } catch (SecretManagementException e) {
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_WHILE_RETRIEVING_SECRETS_OF_PUSH_PROVIDER;
            throw new PushProviderException(error.getCode(), error.getMessage(), e);
        }
    }

    @Override
    public void deletePushProviderSecretProperties(PushSenderDTO pushSender)
            throws PushProviderException {

        try {
            SecretManager secretManager = ProviderDataHolder.getInstance().getSecretManager();
            String secretName = constructSecretName(pushSender.getProviderId(), FCM_SERVICE_ACCOUNT_SECRET);
            if (secretManager.isSecretExist(PUSH_PROVIDER_SECRET_TYPE, secretName)) {
                secretManager.deleteSecret(PUSH_PROVIDER_SECRET_TYPE, secretName);
            }
        } catch (SecretManagementException e) {
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_WHILE_DELETING_SECRETS_OF_PUSH_PROVIDER;
            throw new PushProviderException(error.getCode(), error.getMessage(), e);
        }
    }

    private String constructSecretName(String providerId, String secretAttributeName) {

        return providerId + ":" + secretAttributeName;
    }

    private String constructSecretReference(String secretTypeId, String secretName) {

        return secretTypeId + ":" + secretName;
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
}
