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

package org.wso2.carbon.identity.notification.push.device.handler.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.notification.push.device.handler.DeviceRegistrationContextManager;
import org.wso2.carbon.identity.notification.push.device.handler.dao.DeviceDAO;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerClientException;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerException;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerServerException;
import org.wso2.carbon.identity.notification.push.device.handler.internal.PushDeviceHandlerDataHolder;
import org.wso2.carbon.identity.notification.push.device.handler.model.Device;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationContext;
import org.wso2.carbon.identity.notification.push.device.handler.model.RegistrationDiscoveryData;
import org.wso2.carbon.identity.notification.push.device.handler.model.RegistrationRequest;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.impl.FCMPushProvider;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementService;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.PushSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.HASHING_ALGORITHM;

/**
 * Test class for DeviceHandlerServiceImpl.
 */
public class DeviceHandlerServiceImplTest {

    private String publicKey;
    private String signature;
    private String deviceToken;
    private String validJwt;

    private static MockedStatic<CarbonContext> mockedCarbonContext;
    private static MockedStatic<UserCoreUtil> mockedUserCoreUtil;
    private static MockedStatic<MultitenantUtils> mockedMultitenantUtils;
    private static MockedStatic<IdentityUtil> mockedIdentityUtil;
    private static MockedStatic<LoggerUtils> mockedLoggerUtils;

    @Mock
    private DeviceRegistrationContextManager deviceRegistrationContextManager;

    @Mock
    private DeviceDAO deviceDAO;

    @Mock
    private SignedJWT mockSignedJWT;

    @Mock
    private JWTClaimsSet mockClaimsSet;

    @InjectMocks
    DeviceHandlerServiceImpl deviceHandlerService;

    @BeforeClass
    public static void setUpClass() {
        // Mock static dependencies needed by AUDIT_LOGGER before class loading
        System.setProperty("carbon.home", ".");

        mockedCarbonContext = mockStatic(CarbonContext.class);
        mockedUserCoreUtil = mockStatic(UserCoreUtil.class);
        mockedMultitenantUtils = mockStatic(MultitenantUtils.class);
        mockedIdentityUtil = mockStatic(IdentityUtil.class);
        mockedLoggerUtils = mockStatic(LoggerUtils.class);

        CarbonContext carbonContext = mock(CarbonContext.class);
        mockedCarbonContext.when(CarbonContext::getThreadLocalCarbonContext).thenReturn(carbonContext);
        when(carbonContext.getUsername()).thenReturn("testUser");
        when(carbonContext.getTenantDomain()).thenReturn("carbon.super");

        mockedUserCoreUtil.when(() -> UserCoreUtil.addTenantDomainToEntry(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String username = invocation.getArgument(0);
                    String tenantDomain = invocation.getArgument(1);
                    if (username == null) {
                        return null;
                    }
                    return username + "@" + tenantDomain;
                });

        mockedMultitenantUtils.when(() -> MultitenantUtils.getTenantAwareUsername(anyString()))
                .thenAnswer(invocation -> {
                    String username = invocation.getArgument(0);
                    if (username == null) {
                        return null;
                    }
                    return username.contains("@") ? username.split("@")[0] : username;
                });

        mockedMultitenantUtils.when(() -> MultitenantUtils.getTenantDomain(anyString()))
                .thenAnswer(invocation -> {
                    String username = invocation.getArgument(0);
                    if (username == null) {
                        return "carbon.super";
                    }
                    return username.contains("@") ? username.split("@")[1] : "carbon.super";
                });

        mockedIdentityUtil.when(() -> IdentityUtil.getInitiatorId(anyString(), anyString()))
                .thenReturn("initiator-id-test");

        mockedLoggerUtils.when(() -> LoggerUtils.getMaskedContent(anyString())).thenReturn("masked-content");
    }

    @AfterClass
    public static void tearDownClass() {
        if (mockedCarbonContext != null) {
            mockedCarbonContext.close();
        }
        if (mockedUserCoreUtil != null) {
            mockedUserCoreUtil.close();
        }
        if (mockedMultitenantUtils != null) {
            mockedMultitenantUtils.close();
        }
        if (mockedIdentityUtil != null) {
            mockedIdentityUtil.close();
        }
        if (mockedLoggerUtils != null) {
            mockedLoggerUtils.close();
        }
    }

    @BeforeTest
    void setUp() {

        MockitoAnnotations.openMocks(this);
        deviceHandlerService = new DeviceHandlerServiceImpl(deviceRegistrationContextManager, deviceDAO);

        publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwdysF2GMCgENwNmGvdk0c31JlkLHqs3X4cjN2On1/e4GY2ZU+" +
                "Q/ZGkmWPhYxx9e18rHrpR7MYM92Nji8cv9q0MgwIHj0CpfMq73GJh/HKZD2638xpdOAMpgoiFnhGGk/hqtrtfS7GZuIKOsdw" +
                "NYTX8K1CTKGAJvJLu6X9d6NrQJmxl2CVYXuLyxrJ/3/DVPCIcPLxUk6pzz/Bsaye4LsiwGhSDMhEIvYO1ZSVN48L1yzVG/yQz" +
                "GtStEj8bNBQCkHJewU3pMle1lVSdcefwo6j4L54gNHG65QibswFpUyhkoI1oUvVLm2vjlVGXuWK0akC0/DITE/nOhjPUk/9JE" +
                "uNQIDAQAB";
        signature = "qASzypFEdu+qi31KQF9/c0fH1cqbgeCq0O42hc45T4mmECoyeEYmRI6OZobS3lwbYYdoZP5FA7c0iazN+baxcybSIWx" +
                "evm87jcREJa23QnXzJRDfDWydU0HxlTrYAwtq6F9BsvrCHfO5jh3NQm86Qb4r2tkNSDJyM+g19Ml/2eo9GJ6nnTNUSeSGGh/" +
                "+jPr+8GWxEIAWgAsAJfTkJHfZwUQY+vPDyKuDOCkyDqEiWAC3jLS7+D9M5R6oy4HNrmE2S1Or5+oEwN/6dhvtzg1gqv/clRm" +
                "62JeI7jRpf6u9H1/PT8ZKRWnkSaiwVIBxYQgaXty8fGxgl2qoT8eW+co7Qg==";
        deviceToken = "dq1iZmAaTgSiy7RUSsiDbi:APA91bGU-fPcEW0VJ3-CiVn2Gw7Z2pOnspq2ACx7gfK6ZN1bchrFKl_6MMXtFOoEv3O" +
                "otWMxZ_x62zb3etjJNdvATY0jqZilQjnTVTO2-3-zaJegfuFNhFbxLpC4Qr1KVcrw_PifJXQ7";
        validJwt = "eyJhbGciOiJSUzI1NiIsImRpZCI6IjM4NWViMDg1LWQ1NzUtNDU1ZS1iYjdhLTE5MmJmYTE1NTVkMCIsInR5cCI6IkpXV" +
                "CJ9.eyJ0ZCI6ImNhcmJvbi5zdXBlciIsInBpZCI6ImY1YWU2YTBkLTM5MGEtNGVlYS1hMzgwLThiY2M4NmU0YTE0OCIsImNoZ" +
                "yI6ImUwYzNkMDRjLTc1MGItNDMwMS04Zjc2LWUwN2ViZjAyZTUzYSIsInJlcyI6IkFQUFJPVkVEIiwibnVtIjoiMTAiLCJleH" +
                "AiOjE3MzYwNzYxMDUsIm5iZiI6MTczNjA3NTIwNX0.pBis-9330OboGW0fYGQavBY67G-QNfduB4dJQYQj58zfzKbCDjMfpdzj" +
                "WAeIJXp28WuYbxgsDzYVCTjbPHpAKRnghwD_a40K3mFQdeIxwRw3szaPDXK7n-OesSpR6T--1-HFs3n5OzSRUI9SxT-Cr-7cM" +
                "QeyFa_mYvTy1FrdlWUzfjlfeyjtd7bU_APn1jL3dEo6GctiaeTsTwVR9fo-toRJHVMBDMeRBuvGjApczeFC0EEO2Ug7vlms5WF" +
                "kJLABLgNdus9Vqel9rbOebvqyuFVAJh3Tj7uDvvQQRENnvtov7EiVQYDfJ66UfEQAjHQgWE43OzStUZbTvMMopERHiw";

    }

    @Test
    public void testRegisterDevice()
            throws PushDeviceHandlerException, UserStoreException, NotificationSenderManagementException,
            PushProviderException {

        RegistrationRequest registrationRequest = createRegistrationRequest();

        DeviceRegistrationContext deviceRegistrationContext = createDeviceRegistrationContext();

        when(deviceRegistrationContextManager.getContext(anyString(), anyString()))
                .thenReturn(deviceRegistrationContext);

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
        ) {

            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);
            UserRealm userRealm = mock(UserRealm.class);
            mockedIdentityTenantUtil.when(
                    () -> IdentityTenantUtil.getRealm(anyString(), anyString())).thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");

            when(deviceDAO.getDeviceByUserId(anyString(), anyInt())).thenReturn(Optional.empty());

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);
            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            when(notificationSenderManagementService.getPushSender(anyString(), anyBoolean()))
                    .thenReturn(pushSenderDTO);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(anyString())).thenReturn(fcmPushProvider);
            doNothing().when(fcmPushProvider).registerDevice(any(), any());

            doNothing().when(deviceDAO).registerDevice(any(), anyInt());

            Device registeredDevice = deviceHandlerService
                    .registerDevice(registrationRequest, "carbon.super");

            Assert.assertNotNull(registeredDevice);
            Assert.assertTrue(deviceRegistrationContext.isRegistered());
        }
    }

    @Test
    public void testRegisterDeviceWithNoRegistrationContext() {

        RegistrationRequest registrationRequest = new RegistrationRequest();

        when(deviceRegistrationContextManager.getContext(anyString(), anyString())).thenReturn(null);

        Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
            deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
        });
    }

    @Test
    public void testRegisterDeviceWithAlreadyRegisteredContext() {

        RegistrationRequest registrationRequest = createRegistrationRequest();

        DeviceRegistrationContext deviceRegistrationContext = createDeviceRegistrationContext();
        deviceRegistrationContext.setRegistered(true);

        when(deviceRegistrationContextManager.getContext(anyString(), anyString()))
                .thenReturn(deviceRegistrationContext);
        doNothing().when(deviceRegistrationContextManager).clearContext(anyString(), anyString());

        Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
            deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
        });
    }

    @Test
    public void testRegisterDeviceWithSignatureVerificationFailure() throws SignatureException,
            NoSuchAlgorithmException {

        RegistrationRequest registrationRequest = createRegistrationRequest();

        DeviceRegistrationContext deviceRegistrationContext = createDeviceRegistrationContext();

        when(deviceRegistrationContextManager.getContext(anyString(), anyString()))
                .thenReturn(deviceRegistrationContext);

        try (MockedStatic<Signature> mockedSignature = Mockito.mockStatic(Signature.class)) {
            Signature signature = mock(Signature.class);
            mockedSignature.when(() -> Signature.getInstance(HASHING_ALGORITHM)).thenReturn(signature);

            Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
                deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
            });
        }
    }

    @Test
    public void testRegisterDeviceWithUserIdBlank() throws UserStoreException {

        RegistrationRequest registrationRequest = createRegistrationRequest();

        DeviceRegistrationContext deviceRegistrationContext = createDeviceRegistrationContext();

        when(deviceRegistrationContextManager.getContext(anyString(), anyString()))
                .thenReturn(deviceRegistrationContext);

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
        ) {

            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);
            UserRealm userRealm = mock(UserRealm.class);
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn(null);

            Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
                deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
            });
        }
    }

    @Test
    public void testRegisterDeviceWithExistingDevice() throws UserStoreException, PushDeviceHandlerServerException {

        RegistrationRequest registrationRequest = createRegistrationRequest();

        DeviceRegistrationContext deviceRegistrationContext = createDeviceRegistrationContext();

        when(deviceRegistrationContextManager.getContext(anyString(), anyString()))
                .thenReturn(deviceRegistrationContext);

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
        ) {

            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);
            UserRealm userRealm = mock(UserRealm.class);
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");

            Optional<Device> device = Optional.of(new Device());
            when(deviceDAO.getDeviceByUserId(anyString(), anyInt())).thenReturn(device);

            Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
                deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
            });
        }
    }

    @Test
    public void testGetDevice() throws PushDeviceHandlerException {

        Device deviceObj = new Device();
        Optional<Device> device = Optional.of(deviceObj);
        when(deviceDAO.getDevice(anyString())).thenReturn(device);

        Assert.assertNotNull(deviceHandlerService.getDevice(anyString()));
    }

    @Test
    public void testGetDeviceFailure() throws PushDeviceHandlerException {

        Optional<Device> device = Optional.empty();
        when(deviceDAO.getDevice(anyString())).thenReturn(device);

        Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
            deviceHandlerService.getDevice(anyString());
        });
    }

    @Test
    public void testGetPublicKey() throws PushDeviceHandlerException {

        Optional<String> publicKey = Optional.of("publicKey");
        when(deviceDAO.getPublicKey(anyString())).thenReturn(publicKey);
        Assert.assertNotNull(deviceHandlerService.getPublicKey(anyString()));
    }

    @Test
    public void testGetPublicKeyFailure() throws PushDeviceHandlerException {

        Optional<String> publicKey = Optional.empty();
        when(deviceDAO.getPublicKey(anyString())).thenReturn(publicKey);

        Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
            deviceHandlerService.getPublicKey(anyString());
        });
    }

    @Test
    public void testUnregisterDeviceFailure() throws PushDeviceHandlerException {

        Optional<Device> device = Optional.empty();
        when(deviceDAO.getDevice(anyString())).thenReturn(device);

        Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
            deviceHandlerService.unregisterDevice(anyString());
        });
    }

    @Test
    public void testUnregisterDeviceByUserId()
            throws PushDeviceHandlerException, NotificationSenderManagementException, PushProviderException {

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
        ) {

            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);

            Device deviceObj = new Device();
            deviceObj.setDeviceId("1234567890");
            deviceObj.setProvider("FCM");
            deviceObj.setDeviceToken(deviceToken);
            Optional<Device> device = Optional.of(deviceObj);
            when(deviceDAO.getDevice(anyString())).thenReturn(device);
            when(deviceDAO.getDeviceByUserId(anyString(), anyInt())).thenReturn(device);

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(PushDeviceHandlerDataHolder::getInstance)
                    .thenReturn(pushDeviceHandlerDataHolder);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);
            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            when(notificationSenderManagementService.getPushSender(anyString(), anyBoolean()))
                    .thenReturn(pushSenderDTO);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(anyString())).thenReturn(fcmPushProvider);
            doNothing().when(fcmPushProvider).unregisterDevice(any(), any());

            doNothing().when(deviceDAO).unregisterDevice(anyString());

            deviceHandlerService.unregisterDeviceByUserId(anyString(), "carbon.super");
        }
    }

    @Test
    public void testUnregisterDeviceByUserIdFailure() throws PushDeviceHandlerException {

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
        ) {

            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);

            Optional<Device> device = Optional.empty();
            when(deviceDAO.getDevice(anyString())).thenReturn(device);
            when(deviceDAO.getDeviceByUserId(anyString(), anyInt())).thenReturn(device);

            Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
                deviceHandlerService.unregisterDeviceByUserId(anyString(), "carbon.super");
            });
        }
    }

    @Test
    public void testUnregisterDeviceMobile()
            throws PushDeviceHandlerException, NotificationSenderManagementException, PushProviderException,
            ParseException, JOSEException {

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
                MockedStatic<SignedJWT> mockedStatic = Mockito.mockStatic(SignedJWT.class)
        ) {

            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);

            Device deviceObj = new Device();
            deviceObj.setDeviceId("1234567890");
            deviceObj.setProvider("FCM");
            deviceObj.setDeviceToken(deviceToken);
            deviceObj.setPublicKey(publicKey);
            Optional<Device> device = Optional.of(deviceObj);
            when(deviceDAO.getDevice(anyString())).thenReturn(device);
            when(deviceDAO.getDeviceByUserId(anyString(), anyInt())).thenReturn(device);

            mockedStatic.when(() -> SignedJWT.parse(validJwt)).thenReturn(mockSignedJWT);

            when(mockSignedJWT.getJWTClaimsSet()).thenReturn(mockClaimsSet);
            when(mockClaimsSet.getExpirationTime()).thenReturn(new Date(System.currentTimeMillis() + 3000000));
            when(mockClaimsSet.getNotBeforeTime()).thenReturn(new Date(System.currentTimeMillis() - 3000000));
            when(mockSignedJWT.verify(any())).thenReturn(true);

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(PushDeviceHandlerDataHolder::getInstance)
                    .thenReturn(pushDeviceHandlerDataHolder);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);
            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            when(notificationSenderManagementService.getPushSender(anyString(), anyBoolean()))
                    .thenReturn(pushSenderDTO);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(anyString())).thenReturn(fcmPushProvider);
            doNothing().when(fcmPushProvider).unregisterDevice(any(), any());

            doNothing().when(deviceDAO).unregisterDevice(anyString());

            deviceHandlerService.unregisterDeviceMobile(anyString(), validJwt);
        }
    }

    @Test
    public void testUnregisterDeviceMobileFailure() throws PushDeviceHandlerException {

        Optional<Device> device = Optional.empty();
        when(deviceDAO.getDevice(anyString())).thenReturn(device);

        Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
            deviceHandlerService.unregisterDeviceMobile(anyString(), validJwt);
        });
    }

    @Test
    public void testEditDevice()
            throws PushDeviceHandlerException, NotificationSenderManagementException, PushProviderException {

        try (
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
        ) {

            Device deviceObj = new Device();
            deviceObj.setDeviceId("1234567890");
            deviceObj.setProvider("FCM");
            deviceObj.setDeviceToken(deviceToken);
            deviceObj.setPublicKey(publicKey);
            Optional<Device> device = Optional.of(deviceObj);
            when(deviceDAO.getDevice(anyString())).thenReturn(device);

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(PushDeviceHandlerDataHolder::getInstance)
                    .thenReturn(pushDeviceHandlerDataHolder);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);
            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            when(notificationSenderManagementService.getPushSender(anyString(), anyBoolean()))
                    .thenReturn(pushSenderDTO);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(anyString())).thenReturn(fcmPushProvider);
            doNothing().when(fcmPushProvider).updateDevice(any(), any());

            doNothing().when(deviceDAO).editDevice(anyString(), any());

            deviceHandlerService.editDevice(anyString(), "/device-token", "sampleToken");
            deviceHandlerService.editDevice(anyString(), "/device-name", "sampleName");
        }
    }

    @Test
    public void testEditDeviceFailureWithInvalidPath() throws PushDeviceHandlerException {

        Device deviceObj = new Device();
        deviceObj.setDeviceId("1234567890");
        deviceObj.setProvider("FCM");
        deviceObj.setDeviceToken(deviceToken);
        deviceObj.setPublicKey(publicKey);
        Optional<Device> device = Optional.of(deviceObj);
        when(deviceDAO.getDevice(anyString())).thenReturn(device);

        Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
            deviceHandlerService.editDevice(anyString(), "/invalid-path", "sampleName");
        });
    }

    @Test
    public void testEditDeviceFailureWithNoDevice() throws PushDeviceHandlerException {

        Optional<Device> device = Optional.empty();
        when(deviceDAO.getDevice(anyString())).thenReturn(device);

        Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
            deviceHandlerService.editDevice(anyString(), "/device-token", "sampleName");
        });
    }

    @Test
    public void testGetRegistrationDiscoveryData() throws PushDeviceHandlerException {

        try (
                MockedStatic<OrganizationManagementUtil> mockedOrganizationManagementUtil =
                        Mockito.mockStatic(OrganizationManagementUtil.class);
        ) {
            mockedMultitenantUtils.when(
                    () -> MultitenantUtils.getTenantAwareUsername(anyString())).thenReturn("testUser");
            mockedOrganizationManagementUtil.when(
                    () -> OrganizationManagementUtil.isOrganization(anyString())).thenReturn(false);
            mockedIdentityUtil.when(
                    () -> IdentityUtil.getServerURL(null, false, false)).thenReturn("https://localhost:9443");

            RegistrationDiscoveryData data =
                    deviceHandlerService.getRegistrationDiscoveryData("testUser@carbon.super", "carbon.super");
            Assert.assertNotNull(data);
            Assert.assertNull(data.getOrganizationId());
        }
    }

    @Test
    public void testGetRegistrationDiscoveryDataWithOrganization()
            throws PushDeviceHandlerException, OrganizationManagementException {

        try (
                MockedStatic<OrganizationManagementUtil> mockedOrganizationManagementUtil =
                        Mockito.mockStatic(OrganizationManagementUtil.class);
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
        ) {
            mockedMultitenantUtils.when(
                    () -> MultitenantUtils.getTenantAwareUsername(anyString())).thenReturn("testUser");
            mockedOrganizationManagementUtil.when(
                    () -> OrganizationManagementUtil.isOrganization(anyString())).thenReturn(true);

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(PushDeviceHandlerDataHolder::getInstance)
                    .thenReturn(pushDeviceHandlerDataHolder);
            OrganizationManager organizationManager = mock(OrganizationManager.class);
            when(pushDeviceHandlerDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveOrganizationId(anyString())).thenReturn("SubOrgId");
            when(organizationManager.getOrganizationNameById(anyString())).thenReturn("SubOrg");
            when(organizationManager.getPrimaryOrganizationId(anyString())).thenReturn("PrimaryOrgId");
            when(organizationManager.resolveTenantDomain(anyString())).thenReturn("carbon.super");

            mockedIdentityUtil.when(
                    () -> IdentityUtil.getServerURL(null, false, false))
                    .thenReturn("https://localhost:9443");

            RegistrationDiscoveryData data =
                    deviceHandlerService.getRegistrationDiscoveryData("testUser@carbon.super", "carbon.super");
            Assert.assertNotNull(data);
            Assert.assertNotNull(data.getOrganizationId());
        }
    }

    private RegistrationRequest createRegistrationRequest() {

        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setDeviceId("1234567890");
        registrationRequest.setDeviceName("deviceName");
        registrationRequest.setDeviceModel("deviceModel");
        registrationRequest.setDeviceToken(deviceToken);
        registrationRequest.setSignature(signature);
        registrationRequest.setPublicKey(publicKey);
        return registrationRequest;
    }

    private DeviceRegistrationContext createDeviceRegistrationContext() {

        return new DeviceRegistrationContext(
                "bfd5e5cf-47e2-4214-9994-f9217ef5b344",
                "testUser",
                "carbon.super",
                false
        );
    }
}
