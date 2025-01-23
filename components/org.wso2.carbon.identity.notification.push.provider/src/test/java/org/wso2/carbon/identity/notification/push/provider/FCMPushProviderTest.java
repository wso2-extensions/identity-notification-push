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

package org.wso2.carbon.identity.notification.push.provider;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.impl.FCMPushProvider;
import org.wso2.carbon.identity.notification.push.provider.internal.ProviderDataHolder;
import org.wso2.carbon.identity.notification.push.provider.model.PushDeviceData;
import org.wso2.carbon.identity.notification.push.provider.model.PushNotificationData;
import org.wso2.carbon.identity.notification.push.provider.model.PushSenderData;
import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementException;
import org.wso2.carbon.identity.secret.mgt.core.model.ResolvedSecret;
import org.wso2.carbon.identity.secret.mgt.core.model.SecretType;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.notification.push.provider.constant.PushProviderConstants.FCM_SERVICE_ACCOUNT_SECRET;
import static org.wso2.carbon.identity.notification.push.provider.constant.PushProviderConstants.PUSH_PROVIDER_SECRET_TYPE;

/**
 * FCM Push Provider Test.
 */
public class FCMPushProviderTest {

    private static final String ENCODED_SERVICE_ACCOUNT_STRING = "c2VydmljZUFjY291bnRDb250ZW50";
    private FCMPushProvider fcmPushProvider;
    @Mock
    private PushSenderData pushSenderData = Mockito.mock(PushSenderData.class);

    @Mock
    private GoogleCredentials googleCredentials = Mockito.mock(GoogleCredentials.class);

    @BeforeTest
    public void createNewObject() {

        fcmPushProvider = new FCMPushProvider();
    }

    @Test(priority = 1)
    public void testGetName() {

        String name = fcmPushProvider.getName();
        Assert.assertNotNull(name);
    }

    @Test(expectedExceptions = {PushProviderException.class}, priority = 2)
    public void testPreProcessPropertiesFail() throws PushProviderException {

        Map<String, String> properties = new HashMap<>();
        when(pushSenderData.getProperties()).thenReturn(properties);
        fcmPushProvider.preProcessProperties(pushSenderData);
    }

    @Test(priority = 3)
    public void testPreProcessPropertiesSuccess() throws PushProviderException {

        Map<String, String> properties = new HashMap<>();
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, ENCODED_SERVICE_ACCOUNT_STRING);
        when(pushSenderData.getProperties()).thenReturn(properties);
        Map<String, String> processedProperties = fcmPushProvider.preProcessProperties(pushSenderData);
        Assert.assertNotNull(processedProperties);
        Assert.assertEquals(processedProperties.get(FCM_SERVICE_ACCOUNT_SECRET), FCM_SERVICE_ACCOUNT_SECRET);
    }

    @Test(expectedExceptions = {PushProviderException.class}, priority = 4)
    public void testPostProcessPropertiesFail() throws PushProviderException {

        Map<String, String> properties = new HashMap<>();
        when(pushSenderData.getProperties()).thenReturn(properties);
        fcmPushProvider.postProcessProperties(pushSenderData);
    }

    @Test(priority = 5)
    public void testPostProcessPropertiesSuccess() throws PushProviderException {

        Map<String, String> properties = new HashMap<>();
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, FCM_SERVICE_ACCOUNT_SECRET);
        when(pushSenderData.getProperties()).thenReturn(properties);
        Map<String, String> processedProperties = fcmPushProvider.postProcessProperties(pushSenderData);
        Assert.assertNotNull(processedProperties);
        Assert.assertEquals(processedProperties.get(FCM_SERVICE_ACCOUNT_SECRET),
                ENCODED_SERVICE_ACCOUNT_STRING);
    }

    @Test(priority = 6)
    public void testUpdateCredentials() throws PushProviderException {

        try (MockedStatic<FirebaseApp> mockedFirebaseApp = Mockito.mockStatic(FirebaseApp.class)) {

            FirebaseApp mockFirebaseAppToDelete = Mockito.mock(FirebaseApp.class);
            FirebaseApp mockFirebaseAppToKeep = Mockito.mock(FirebaseApp.class);

            when(mockFirebaseAppToDelete.getName()).thenReturn("FirebaseApp-testProvider");
            when(mockFirebaseAppToKeep.getName()).thenReturn("FirebaseApp-otherProvider");

            mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(
                    Arrays.asList(mockFirebaseAppToDelete, mockFirebaseAppToKeep));

            when(pushSenderData.getProviderId()).thenReturn("testProvider");
            fcmPushProvider.updateCredentials(pushSenderData);

            verify(mockFirebaseAppToDelete, times(1)).delete();
            verify(mockFirebaseAppToKeep, never()).delete();
        }
    }

    @Test(priority = 7)
    public void testSendNotification() throws PushProviderException, FirebaseMessagingException {

        try (MockedStatic<GoogleCredentials> mockedCredentials = Mockito.mockStatic(GoogleCredentials.class)) {

            mockedCredentials.when(() -> GoogleCredentials.fromStream(Mockito.any(InputStream.class)))
                    .thenReturn(googleCredentials);

            when(pushSenderData.getProviderId()).thenReturn("testFCMProviderId");
            Map<String, String> properties = new HashMap<>();
            properties.put(FCM_SERVICE_ACCOUNT_SECRET, ENCODED_SERVICE_ACCOUNT_STRING);
            when(pushSenderData.getProperties()).thenReturn(properties);

            try (MockedStatic<FirebaseMessaging> mockedFirebaseMessaging =
                         Mockito.mockStatic(FirebaseMessaging.class)) {

                FirebaseMessaging firebaseMessaging = Mockito.mock(FirebaseMessaging.class);
                mockedFirebaseMessaging.when(() -> FirebaseMessaging.getInstance(Mockito.any(FirebaseApp.class)))
                        .thenReturn(firebaseMessaging);
                when(firebaseMessaging.send(Mockito.any(Message.class))).thenReturn("mockMessageId");

                PushNotificationData pushNotificationData = new PushNotificationData.Builder()
                        .setNotificationTitle("Test Title")
                        .setNotificationBody("Test Body")
                        .setDeviceToken("testDeviceToken")
                        .build();

                fcmPushProvider.sendNotification(pushNotificationData, pushSenderData, "carbon.super");
            }
        }
    }

    @Test(priority = 8)
    public void testRegisterDevice() throws PushProviderException {

        PushDeviceData device = new PushDeviceData("testDeviceToken", "testDeviceHandle", "testProvider");
        PushSenderData pushSenderData = new PushSenderData();
        fcmPushProvider.registerDevice(device, pushSenderData);
    }

    @Test(priority = 9)
    public void testUnregisterDevice() throws PushProviderException {

        PushDeviceData device = new PushDeviceData("testDeviceToken", "testDeviceHandle", "testProvider");
        PushSenderData pushSenderData = new PushSenderData();
        fcmPushProvider.unregisterDevice(device, pushSenderData);
    }

    @Test(priority = 10)
    public void testUpdateDevice() throws PushProviderException {

        PushDeviceData device = new PushDeviceData("testDeviceToken", "testDeviceHandle", "testProvider");
        PushSenderData pushSenderData = new PushSenderData();
        fcmPushProvider.updateDevice(device, pushSenderData);
    }

    @Test(priority = 11, expectedExceptions = {PushProviderException.class})
    public void storePushProviderSecretPropertiesFail() throws PushProviderException {

        Map<String, String> properties = new HashMap<>();
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn("testProviderId");

        fcmPushProvider.storePushProviderSecretProperties(pushSenderData);
    }

    @Test(priority = 12)
    public void testStoreNewPushProviderSecretProperties() throws PushProviderException, SecretManagementException {

        Map<String, String> properties = new HashMap<>();
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, ENCODED_SERVICE_ACCOUNT_STRING);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn("testProviderId");

        try (MockedStatic<ProviderDataHolder> mockedProviderDataHolder = Mockito.mockStatic(ProviderDataHolder.class)) {

            ProviderDataHolder providerDataHolder = Mockito.mock(ProviderDataHolder.class);
            mockedProviderDataHolder.when(ProviderDataHolder::getInstance).thenReturn(providerDataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(providerDataHolder.getSecretManager()).thenReturn(secretManager);

            SecretType secretType = new SecretType();
            secretType.setName(PUSH_PROVIDER_SECRET_TYPE);
            secretType.setId("testSecretTypeId");
            when(secretManager.getSecretType(Mockito.anyString())).thenReturn(secretType);

            Map<String, String> processedProperties = fcmPushProvider.storePushProviderSecretProperties(pushSenderData);
            Assert.assertNotNull(processedProperties);
            Assert.assertEquals(processedProperties.get(FCM_SERVICE_ACCOUNT_SECRET),
                    "FCM-credentials");
        }
    }

    @Test(priority = 13)
    public void testUpdateExistingPushProviderSecretProperties()
            throws PushProviderException, SecretManagementException {

        Map<String, String> properties = new HashMap<>();
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, ENCODED_SERVICE_ACCOUNT_STRING);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn("testProviderId");

        try (MockedStatic<ProviderDataHolder> mockedProviderDataHolder = Mockito.mockStatic(ProviderDataHolder.class)) {

            ProviderDataHolder providerDataHolder = Mockito.mock(ProviderDataHolder.class);
            mockedProviderDataHolder.when(ProviderDataHolder::getInstance).thenReturn(providerDataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(providerDataHolder.getSecretManager()).thenReturn(secretManager);

            when(secretManager.isSecretExist(Mockito.anyString(), Mockito.anyString())).thenReturn(true);

            SecretType secretType = new SecretType();
            secretType.setName(PUSH_PROVIDER_SECRET_TYPE);
            secretType.setId("testSecretTypeId");
            when(secretManager.getSecretType(Mockito.anyString())).thenReturn(secretType);

            Map<String, String> processedProperties = fcmPushProvider.storePushProviderSecretProperties(pushSenderData);
            Assert.assertNotNull(processedProperties);
            Assert.assertEquals(processedProperties.get(FCM_SERVICE_ACCOUNT_SECRET),
                    "FCM-credentials");
        }
    }

    @Test(priority = 14, expectedExceptions = {PushProviderException.class})
    public void testRetrievePushProviderSecretPropertiesFailByServiceAccount() throws PushProviderException {

        Map<String, String> properties = new HashMap<>();
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn("testProviderId");

        fcmPushProvider.retrievePushProviderSecretProperties(pushSenderData);
    }

    @Test(priority = 15)
    public void testRetrievePushProviderSecretProperties() throws PushProviderException, SecretManagementException {

        Map<String, String> properties = new HashMap<>();
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, ENCODED_SERVICE_ACCOUNT_STRING);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn("testProviderId");

        try (MockedStatic<ProviderDataHolder> mockedProviderDataHolder = Mockito.mockStatic(ProviderDataHolder.class)) {

            ProviderDataHolder providerDataHolder = Mockito.mock(ProviderDataHolder.class);
            mockedProviderDataHolder.when(ProviderDataHolder::getInstance).thenReturn(providerDataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(providerDataHolder.getSecretManager()).thenReturn(secretManager);

            SecretResolveManager secretResolveManager = Mockito.mock(SecretResolveManager.class);
            when(providerDataHolder.getSecretResolveManager()).thenReturn(secretResolveManager);

            when(secretManager.isSecretExist(Mockito.anyString(), Mockito.anyString())).thenReturn(true);

            ResolvedSecret resolvedSecret = new ResolvedSecret();
            resolvedSecret.setResolvedSecretValue("ResolvedServiceAccountContent");

            when(secretResolveManager.getResolvedSecret(Mockito.anyString(), Mockito.anyString()))
                    .thenReturn(resolvedSecret);

            Map<String, String> processedProperties =
                    fcmPushProvider.retrievePushProviderSecretProperties(pushSenderData);
            Assert.assertNotNull(processedProperties);
            Assert.assertNotNull(processedProperties.get(FCM_SERVICE_ACCOUNT_SECRET));
        }
    }

    @Test(priority = 15, expectedExceptions = {PushProviderException.class})
    public void testRetrievePushProviderSecretPropertiesFailBySecret() throws PushProviderException {

        Map<String, String> properties = new HashMap<>();
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, ENCODED_SERVICE_ACCOUNT_STRING);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn("testProviderId");

        try (MockedStatic<ProviderDataHolder> mockedProviderDataHolder = Mockito.mockStatic(ProviderDataHolder.class)) {

            ProviderDataHolder providerDataHolder = Mockito.mock(ProviderDataHolder.class);
            mockedProviderDataHolder.when(ProviderDataHolder::getInstance).thenReturn(providerDataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(providerDataHolder.getSecretManager()).thenReturn(secretManager);

            SecretResolveManager secretResolveManager = Mockito.mock(SecretResolveManager.class);
            when(providerDataHolder.getSecretResolveManager()).thenReturn(secretResolveManager);

            fcmPushProvider.retrievePushProviderSecretProperties(pushSenderData);
        }
    }

    @Test(priority = 16)
    public void testDeletePushProviderSecretProperties() throws PushProviderException, SecretManagementException {

        Map<String, String> properties = new HashMap<>();
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, ENCODED_SERVICE_ACCOUNT_STRING);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn("testProviderId");

        try (MockedStatic<ProviderDataHolder> mockedProviderDataHolder = Mockito.mockStatic(ProviderDataHolder.class)) {

            ProviderDataHolder providerDataHolder = Mockito.mock(ProviderDataHolder.class);
            mockedProviderDataHolder.when(ProviderDataHolder::getInstance).thenReturn(providerDataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(providerDataHolder.getSecretManager()).thenReturn(secretManager);

            when(secretManager.isSecretExist(Mockito.anyString(), Mockito.anyString())).thenReturn(true);

            fcmPushProvider.deletePushProviderSecretProperties(pushSenderData);
        }
    }

    @Test(priority = 17)
    public void testDeletePushProviderSecretPropertiesFalsePositive()
            throws PushProviderException, SecretManagementException {

        Map<String, String> properties = new HashMap<>();
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, ENCODED_SERVICE_ACCOUNT_STRING);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn("testProviderId");

        try (MockedStatic<ProviderDataHolder> mockedProviderDataHolder = Mockito.mockStatic(ProviderDataHolder.class)) {

            ProviderDataHolder providerDataHolder = Mockito.mock(ProviderDataHolder.class);
            mockedProviderDataHolder.when(ProviderDataHolder::getInstance).thenReturn(providerDataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(providerDataHolder.getSecretManager()).thenReturn(secretManager);

            when(secretManager.isSecretExist(Mockito.anyString(), Mockito.anyString())).thenReturn(false);

            fcmPushProvider.deletePushProviderSecretProperties(pushSenderData);
        }
    }

    @Test(priority = 18, expectedExceptions = {PushProviderException.class})
    public void testDeletePushProviderSecretPropertiesFail()
            throws PushProviderException, SecretManagementException {

        Map<String, String> properties = new HashMap<>();
        properties.put(FCM_SERVICE_ACCOUNT_SECRET, ENCODED_SERVICE_ACCOUNT_STRING);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn("testProviderId");

        try (MockedStatic<ProviderDataHolder> mockedProviderDataHolder = Mockito.mockStatic(ProviderDataHolder.class)) {

            ProviderDataHolder providerDataHolder = Mockito.mock(ProviderDataHolder.class);
            mockedProviderDataHolder.when(ProviderDataHolder::getInstance).thenReturn(providerDataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(providerDataHolder.getSecretManager()).thenReturn(secretManager);

            when(secretManager.isSecretExist(Mockito.anyString(), Mockito.anyString()))
                    .thenThrow(SecretManagementException.class);

            fcmPushProvider.deletePushProviderSecretProperties(pushSenderData);
        }
    }

    @Test(priority = 19, expectedExceptions = {PushProviderException.class})
    public void testSendNotificationFailWithException() throws PushProviderException, FirebaseMessagingException {

        try (MockedStatic<GoogleCredentials> mockedCredentials = Mockito.mockStatic(GoogleCredentials.class)) {

            mockedCredentials.when(() -> GoogleCredentials.fromStream(Mockito.any(InputStream.class)))
                    .thenReturn(googleCredentials);

            when(pushSenderData.getProviderId()).thenReturn("testFCMProviderId");
            Map<String, String> properties = new HashMap<>();
            properties.put(FCM_SERVICE_ACCOUNT_SECRET, ENCODED_SERVICE_ACCOUNT_STRING);
            when(pushSenderData.getProperties()).thenReturn(properties);

            try (MockedStatic<FirebaseMessaging> mockedFirebaseMessaging =
                         Mockito.mockStatic(FirebaseMessaging.class)) {

                FirebaseMessaging firebaseMessaging = Mockito.mock(FirebaseMessaging.class);
                mockedFirebaseMessaging.when(() -> FirebaseMessaging.getInstance(Mockito.any(FirebaseApp.class)))
                        .thenReturn(firebaseMessaging);
                when(firebaseMessaging.send(Mockito.any(Message.class)))
                        .thenThrow(FirebaseMessagingException.class);

                PushNotificationData pushNotificationData = new PushNotificationData.Builder()
                        .setNotificationTitle("Test Title")
                        .setNotificationBody("Test Body")
                        .setDeviceToken("testDeviceToken")
                        .build();

                fcmPushProvider.sendNotification(pushNotificationData, pushSenderData, "carbon.super");
            }
        }
    }
}
