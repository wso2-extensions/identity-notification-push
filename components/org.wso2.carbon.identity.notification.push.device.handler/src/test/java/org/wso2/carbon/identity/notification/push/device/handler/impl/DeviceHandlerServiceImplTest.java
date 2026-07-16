/*
 * Copyright (c) 2025-2026, WSO2 LLC. (http://www.wso2.com).
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.core.context.IdentityContext;
import org.wso2.carbon.identity.core.context.model.Header;
import org.wso2.carbon.identity.core.context.model.Request;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
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
import org.wso2.carbon.identity.notification.push.device.handler.model.RegistrationRequestProviderData;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderClientException;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderServerException;
import org.wso2.carbon.identity.notification.push.provider.impl.FCMPushProvider;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementService;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.PushSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEFAULT_PUSH_PROVIDER;
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

    @BeforeMethod
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
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);
            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(anyString())).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
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
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
        ) {

            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);
            UserRealm userRealm = mock(UserRealm.class);
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn(null);

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

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
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
        ) {

            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);
            UserRealm userRealm = mock(UserRealm.class);
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            when(deviceDAO.getDevicesByUserId(anyString(), anyInt()))
                    .thenReturn(java.util.Collections.singletonList(new Device()));

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
    public void testGetDeviceByUserId() throws PushDeviceHandlerException {

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
        ) {
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);

            Device deviceObj = new Device();
            deviceObj.setDeviceId("1234567890");
            Optional<Device> device = Optional.of(deviceObj);
            when(deviceDAO.getDeviceByUserId(anyString(), anyInt())).thenReturn(device);

            Device result = deviceHandlerService.getDeviceByUserId("testUserId", "carbon.super");
            Assert.assertNotNull(result);
            Assert.assertEquals(result.getDeviceId(), "1234567890");
        }
    }

    @Test
    public void testGetDeviceByUserIdFailure() throws PushDeviceHandlerException {

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
        ) {
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);

            Optional<Device> device = Optional.empty();
            when(deviceDAO.getDeviceByUserId(anyString(), anyInt())).thenReturn(device);

            Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
                deviceHandlerService.getDeviceByUserId("testUserId", "carbon.super");
            });
        }
    }

    @Test
    public void testGetDevicesByUserId() throws PushDeviceHandlerException {

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
        ) {
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);

            Device deviceObj = new Device();
            deviceObj.setDeviceId("1234567890");
            List<Device> deviceList = new ArrayList<>();
            deviceList.add(deviceObj);
            when(deviceDAO.getDevicesByUserId(anyString(), anyInt())).thenReturn(deviceList);

            List<Device> result = deviceHandlerService.getDevicesByUserId("testUserId", "carbon.super");
            Assert.assertNotNull(result);
            Assert.assertEquals(result.size(), 1);
        }
    }

    @Test
    public void testGetDevicesByUserIdWhenNoDevices() throws PushDeviceHandlerException {

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
        ) {
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);
            when(deviceDAO.getDevicesByUserId(anyString(), anyInt())).thenReturn(new ArrayList<>());

            // No registered devices is a valid result and must return an empty list, not throw.
            List<Device> result = deviceHandlerService.getDevicesByUserId("testUserId", "carbon.super");
            Assert.assertNotNull(result);
            Assert.assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testUnregisterDevice()
            throws PushDeviceHandlerException, NotificationSenderManagementException, PushProviderException {

        try (
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
        ) {
            Device deviceObj = new Device();
            deviceObj.setDeviceId("1234567890");
            deviceObj.setProvider("FCM");
            deviceObj.setDeviceToken(deviceToken);
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
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(anyString())).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
            doNothing().when(fcmPushProvider).unregisterDevice(any(), any());

            doNothing().when(deviceDAO).unregisterDevice(anyString());

            deviceHandlerService.unregisterDevice("1234567890");
        }
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
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
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
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
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
    public void testUnregisterDeviceWhenNotificationSenderServiceFails()
            throws PushDeviceHandlerException, NotificationSenderManagementException, PushProviderException {

        try (
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
        ) {
            Device deviceObj = new Device();
            deviceObj.setDeviceId("1234567890");
            deviceObj.setProvider("FCM");
            deviceObj.setDeviceToken(deviceToken);
            Optional<Device> device = Optional.of(deviceObj);
            when(deviceDAO.getDevice(anyString())).thenReturn(device);

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(PushDeviceHandlerDataHolder::getInstance)
                    .thenReturn(pushDeviceHandlerDataHolder);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);

            // Mock PushProvider to return successfully
            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");

            // Simulate NotificationSenderManagementService failing
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenThrow(new NotificationSenderManagementException(
                            NotificationSenderManagementConstants.ErrorMessage
                                    .ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDERS_BY_TYPE,
                            "push"));

            Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
                deviceHandlerService.unregisterDevice("1234567890");
            });
        }
    }

    @Test
    public void testUnregisterDeviceWhenPushProviderClientExceptionThrown()
            throws PushDeviceHandlerException, NotificationSenderManagementException, PushProviderException {

        try (
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
        ) {
            Device deviceObj = new Device();
            deviceObj.setDeviceId("1234567890");
            deviceObj.setProvider("FCM");
            deviceObj.setDeviceToken(deviceToken);
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
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            // Mock PushProvider to throw client exception
            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(anyString())).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
            org.wso2.carbon.identity.notification.push.provider.exception.PushProviderClientException ppce =
                    new org.wso2.carbon.identity.notification.push.provider.exception.PushProviderClientException(
                            "ERR_DEVICE_UNREGISTER", "Failed to unregister device from provider");
            org.mockito.Mockito.doThrow(ppce).when(fcmPushProvider).unregisterDevice(any(), any());

            Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
                deviceHandlerService.unregisterDevice("1234567890");
            });
        }
    }

    @Test
    public void testUnregisterDeviceWhenPushProviderServerExceptionThrown()
            throws PushDeviceHandlerException, NotificationSenderManagementException, PushProviderException {

        try (
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
        ) {
            Device deviceObj = new Device();
            deviceObj.setDeviceId("1234567890");
            deviceObj.setProvider("FCM");
            deviceObj.setDeviceToken(deviceToken);
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
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            // Mock PushProvider to throw server exception
            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(anyString())).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
            PushProviderServerException ppse =
                    new PushProviderServerException(
                            "ERR_PROVIDER_SERVER", "Provider service unavailable");
            doThrow(ppse).when(fcmPushProvider).unregisterDevice(any(), any());

            Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
                deviceHandlerService.unregisterDevice("1234567890");
            });
        }
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
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
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
    public void testEditDeviceWhenNotificationSenderServiceFails()
            throws PushDeviceHandlerException, NotificationSenderManagementException {

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

            // Mock PushProvider to return successfully
            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");

            // Simulate NotificationSenderManagementService failing
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenThrow(new NotificationSenderManagementException(
                            NotificationSenderManagementConstants.ErrorMessage
                                    .ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDERS_BY_TYPE,
                            "push"));

            Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
                deviceHandlerService.editDevice(anyString(), "/device-token", "sampleToken");
            });
        }
    }

    @Test
    public void testEditDeviceWhenPushProviderClientExceptionThrown()
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
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            // Mock PushProvider to throw client exception
            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
            PushProviderClientException ppce = new PushProviderClientException(
                            "ERR_DEVICE_UPDATE", "Failed to update device at provider");
            org.mockito.Mockito.doThrow(ppce).when(fcmPushProvider).updateDevice(any(), any());

            Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
                deviceHandlerService.editDevice(anyString(), "/device-token", "sampleToken");
            });
        }
    }

    @Test
    public void testEditDeviceWhenPushProviderServerExceptionThrown()
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
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            // Mock PushProvider to throw server exception
            org.wso2.carbon.identity.notification.push.provider.PushProvider pushProvider =
                    mock(org.wso2.carbon.identity.notification.push.provider.PushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(pushProvider);
            when(pushProvider.getName()).thenReturn("FCM");
            org.wso2.carbon.identity.notification.push.provider.exception.PushProviderServerException ppse =
                    new org.wso2.carbon.identity.notification.push.provider.exception.PushProviderServerException(
                            "ERR_PROVIDER_SERVER", "Provider service temporarily unavailable");
            org.mockito.Mockito.doThrow(ppse).when(pushProvider).updateDevice(any(), any());

            Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
                deviceHandlerService.editDevice(anyString(), "/device-token", "sampleToken");
            });
        }
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
                    () -> IdentityUtil.getServerURL(null, false, false))
                    .thenReturn("https://localhost:9443");

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

    @Test
    public void testRegisterDevicePushProviderClientException()
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
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);
            Map<String, String> configs = new HashMap<>();
            configs.put(DEFAULT_PUSH_PROVIDER, "FCM");
            when(notificationSenderManagementService.getNotificationSenderConfigurations(anyString(), anyBoolean()))
                    .thenReturn(configs);

            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            // Mock PushProvider to throw client exception
            org.wso2.carbon.identity.notification.push.provider.PushProvider pushProvider =
                    mock(org.wso2.carbon.identity.notification.push.provider.PushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(pushProvider);
            when(pushProvider.getName()).thenReturn("FCM");
            PushProviderClientException ppce =
                    new PushProviderClientException("ERR", "provider client error");
            org.mockito.Mockito.doThrow(ppce).when(pushProvider).registerDevice(any(), any());

            Assert.assertThrows(PushDeviceHandlerClientException.class, () -> {
                deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
            });
        }
    }

    @Test
    public void testRegisterDeviceWithAlreadyRegisteredDeviceId()
            throws Exception {

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
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            // Multiple device enrollment disabled, so a user that already has a registered device
            // must not be allowed to register another one.
            org.wso2.carbon.identity.configuration.mgt.core.model.Resource resource =
                    new org.wso2.carbon.identity.configuration.mgt.core.model.Resource();
            List<org.wso2.carbon.identity.configuration.mgt.core.model.Attribute> attributes = new ArrayList<>();
            attributes.add(new org.wso2.carbon.identity.configuration.mgt.core.model.Attribute(
                    "enableMultipleDeviceEnrollment", "false"));
            attributes.add(new org.wso2.carbon.identity.configuration.mgt.core.model.Attribute(
                    "maximumDeviceLimit", "1"));
            resource.setAttributes(attributes);
            when(configManager.getResource(anyString(), anyString(), anyBoolean())).thenReturn(resource);

            when(deviceDAO.getDevicesByUserId(anyString(), anyInt()))
                    .thenReturn(java.util.Collections.singletonList(new Device()));

            Assert.assertThrows(PushDeviceHandlerClientException.class,
                    () -> deviceHandlerService.registerDevice(registrationRequest, "carbon.super"));
        }
    }

    @Test
    public void testRegisterDeviceWhenMultiDeviceEnabledAndLimitReached()
            throws Exception {

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
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            org.wso2.carbon.identity.configuration.mgt.core.model.Resource resource =
                    new org.wso2.carbon.identity.configuration.mgt.core.model.Resource();
            List<org.wso2.carbon.identity.configuration.mgt.core.model.Attribute> attributes = new ArrayList<>();
            attributes.add(new org.wso2.carbon.identity.configuration.mgt.core.model.Attribute(
                    "enableMultipleDeviceEnrollment", "true"));
            attributes.add(new org.wso2.carbon.identity.configuration.mgt.core.model.Attribute(
                    "maximumDeviceLimit", "2"));
            resource.setAttributes(attributes);
            when(configManager.getResource(anyString(), anyString(), anyBoolean())).thenReturn(resource);

            when(deviceDAO.getDevicesByUserId(anyString(), anyInt()))
                    .thenReturn(java.util.Arrays.asList(new Device(), new Device()));

            Assert.assertThrows(PushDeviceHandlerClientException.class,
                    () -> deviceHandlerService.registerDevice(registrationRequest, "carbon.super"));
        }
    }

    @Test
    public void testRegisterDeviceWhenMultiDeviceEnabledAndUnderLimit()
            throws Exception {

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
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            org.wso2.carbon.identity.configuration.mgt.core.model.Resource resource =
                    new org.wso2.carbon.identity.configuration.mgt.core.model.Resource();
            List<org.wso2.carbon.identity.configuration.mgt.core.model.Attribute> attributes = new ArrayList<>();
            attributes.add(new org.wso2.carbon.identity.configuration.mgt.core.model.Attribute(
                    "enableMultipleDeviceEnrollment", "true"));
            attributes.add(new org.wso2.carbon.identity.configuration.mgt.core.model.Attribute(
                    "maximumDeviceLimit", "5"));
            resource.setAttributes(attributes);
            when(configManager.getResource(anyString(), anyString(), anyBoolean())).thenReturn(resource);

            when(deviceDAO.getDevicesByUserId(anyString(), anyInt()))
                    .thenReturn(java.util.Collections.singletonList(new Device()));

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);
            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(anyString())).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
            doNothing().when(fcmPushProvider).registerDevice(any(), any());
            doNothing().when(deviceDAO).registerDevice(any(), anyInt());

            Device registeredDevice = deviceHandlerService.registerDevice(registrationRequest, "carbon.super");

            Assert.assertNotNull(registeredDevice);
            Assert.assertTrue(deviceRegistrationContext.isRegistered());
        }
    }

    private DeviceRegistrationContext createDeviceRegistrationContext() {

        return new DeviceRegistrationContext(
                "bfd5e5cf-47e2-4214-9994-f9217ef5b344",
                "testUser",
                "carbon.super",
                false
        );
    }

    // ==================== Multi-Provider Support Tests ====================

    /**
     * Test device registration with explicit provider data.
     * This tests the primary use case where the client specifies which provider to use.
     */
    @Test
    public void testRegisterDeviceWithExplicitProviderData()
            throws PushDeviceHandlerException, UserStoreException, NotificationSenderManagementException,
            PushProviderException {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        // Explicitly set provider data
        Map<String, String> metadata = new HashMap<>();
        RegistrationRequestProviderData providerData = new RegistrationRequestProviderData("FCM", metadata);
        registrationRequest.setProvider(providerData);

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
            when(deviceDAO.getDevicesByUserId(anyString(), anyInt())).thenReturn(new ArrayList<>());

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);
            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
            doNothing().when(fcmPushProvider).registerDevice(any(), any());

            doNothing().when(deviceDAO).registerDevice(any(), anyInt());

            Device registeredDevice = deviceHandlerService
                    .registerDevice(registrationRequest, "carbon.super");

            Assert.assertNotNull(registeredDevice);
            Assert.assertEquals(registeredDevice.getProvider(), "FCM");
            Assert.assertTrue(deviceRegistrationContext.isRegistered());
            verify(fcmPushProvider, times(1)).registerDevice(any(), any());
        }
    }

    @Test
    public void testRegisterDeviceWhenNotificationSenderServiceFails() throws Exception {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        RegistrationRequestProviderData providerData = new RegistrationRequestProviderData("FCM", new HashMap<>());
        registrationRequest.setProvider(providerData);

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
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");

            when(deviceDAO.getDeviceByUserId(anyString(), anyInt())).thenReturn(Optional.empty());

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(PushDeviceHandlerDataHolder::getInstance)
                    .thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);

            // Mock PushProvider returned normally
            org.wso2.carbon.identity.notification.push.provider.PushProvider pushProvider =
                    mock(org.wso2.carbon.identity.notification.push.provider.PushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(pushProvider);
            when(pushProvider.getName()).thenReturn("FCM");

            // Simulate NotificationSenderManagementService failing when retrieving PushSender
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenThrow(new NotificationSenderManagementException(
                            NotificationSenderManagementConstants.ErrorMessage
                                    .ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDERS_BY_TYPE,
                            "push"));

            Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
                deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
            });
        }
    }

    /**
     * Test device registration when no push sender is found for the specified provider.
     * This covers the case where getPushSenderForProvider returns no matching sender.
     */
    @Test
    public void testRegisterDeviceWhenNoPushSenderFoundForProvider() throws Exception {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        RegistrationRequestProviderData providerData = new RegistrationRequestProviderData("FCM", new HashMap<>());
        registrationRequest.setProvider(providerData);

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
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");

            when(deviceDAO.getDeviceByUserId(anyString(), anyInt())).thenReturn(Optional.empty());

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(PushDeviceHandlerDataHolder::getInstance)
                    .thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);

            // Mock PushProvider returned normally
            org.wso2.carbon.identity.notification.push.provider.PushProvider pushProvider =
                    mock(org.wso2.carbon.identity.notification.push.provider.PushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(pushProvider);
            when(pushProvider.getName()).thenReturn("FCM");

            // Return empty list of push senders (no sender found for the provider)
            List<PushSenderDTO> emptyPushSenders = new ArrayList<>();
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(emptyPushSenders);

            Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
                deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
            });
        }
    }

    /**
     * Test device registration when push senders exist but none match the specified provider.
     * This covers the case where there are configured senders but not for the requested provider.
     */
    @Test
    public void testRegisterDeviceWhenPushSenderDoesNotMatchProvider() throws Exception {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        RegistrationRequestProviderData providerData = new RegistrationRequestProviderData("FCM", new HashMap<>());
        registrationRequest.setProvider(providerData);

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
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");

            when(deviceDAO.getDeviceByUserId(anyString(), anyInt())).thenReturn(Optional.empty());

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(PushDeviceHandlerDataHolder::getInstance)
                    .thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);

            // Mock PushProvider returned normally
            org.wso2.carbon.identity.notification.push.provider.PushProvider pushProvider =
                    mock(org.wso2.carbon.identity.notification.push.provider.PushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(pushProvider);
            when(pushProvider.getName()).thenReturn("FCM");

            // Return a list with a different provider (AmazonSNS instead of FCM)
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            PushSenderDTO amazonSNSjPushSender = new PushSenderDTO();
            amazonSNSjPushSender.setProvider("AmazonSNS");
            amazonSNSjPushSender.setName("AmazonSNS_PushPublisher");
            amazonSNSjPushSender.setProviderId("amazon-sns-provider-id");
            amazonSNSjPushSender.setProperties(new HashMap<>());
            pushSenders.add(amazonSNSjPushSender);

            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
                deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
            });
        }
    }

    /**
     * Test device registration without provider data (default provider fallback).
     * This tests the backward compatibility where no provider is specified.
     */
    @Test
    public void testRegisterDeviceWithoutProviderData()
            throws PushDeviceHandlerException, UserStoreException, NotificationSenderManagementException,
            PushProviderException {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        // Set provider data to null to test default provider fallback
        registrationRequest.setProvider(null);

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
            when(deviceDAO.getDevicesByUserId(anyString(), anyInt())).thenReturn(new ArrayList<>());

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);

            // Mock default provider configuration
            Map<String, String> configs = new HashMap<>();
            configs.put(DEFAULT_PUSH_PROVIDER, "FCM");
            when(notificationSenderManagementService.getNotificationSenderConfigurations(anyString(), anyBoolean()))
                    .thenReturn(configs);

            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
            doNothing().when(fcmPushProvider).registerDevice(any(), any());

            doNothing().when(deviceDAO).registerDevice(any(), anyInt());

            Device registeredDevice = deviceHandlerService
                    .registerDevice(registrationRequest, "carbon.super");

            Assert.assertNotNull(registeredDevice);
            Assert.assertEquals(registeredDevice.getProvider(), "FCM");
            Assert.assertTrue(deviceRegistrationContext.isRegistered());
            verify(notificationSenderManagementService, times(1))
                    .getNotificationSenderConfigurations(anyString(), anyBoolean());
        }
    }

    /**
     * Test device registration fails when no provider data and no default configured.
     * This ensures proper error handling when provider configuration is missing.
     */
    @Test
    public void testRegisterDeviceFailsWithNoProviderAndNoDefault() throws UserStoreException,
            PushDeviceHandlerException, NotificationSenderManagementException {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        registrationRequest.setProvider(null);

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
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);

            // Return empty config to simulate no default provider
            when(notificationSenderManagementService.getNotificationSenderConfigurations(anyString(), anyBoolean()))
                    .thenReturn(new HashMap<>());

            // Return empty list of push senders so it falls through to exception
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(new ArrayList<>());

            Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
                deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
            });
        }
    }

    /**
     * Test device registration fails when default provider retrieval fails.
     * This tests error handling for configuration retrieval failures.
     */
    @Test
    public void testRegisterDeviceFailsWhenDefaultProviderRetrievalFails() throws UserStoreException,
            NotificationSenderManagementException, PushDeviceHandlerException {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        registrationRequest.setProvider(null);

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
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);

            // Simulate exception during config retrieval
            when(notificationSenderManagementService.getNotificationSenderConfigurations(anyString(), anyBoolean()))
                    .thenThrow(new NotificationSenderManagementException(
                            NotificationSenderManagementConstants.ErrorMessage
                                    .ERROR_CODE_ERROR_GETTING_NOTIFICATION_SENDER,
                            "Config retrieval failed"));

            Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
                deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
            });
        }
    }

    /**
     * Test device registration falls back to default provider when provider data has a blank name.
     * Covers the branch where providerData is non-null but providerData.getName() is blank.
     */
    @Test
    public void testRegisterDeviceWithBlankProviderNameFallsBackToDefault()
            throws PushDeviceHandlerException, UserStoreException, NotificationSenderManagementException,
            PushProviderException {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        registrationRequest.setProvider(new RegistrationRequestProviderData("", null));

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
            when(deviceDAO.getDevicesByUserId(anyString(), anyInt())).thenReturn(new ArrayList<>());

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);

            // Mock default provider configuration
            Map<String, String> configs = new HashMap<>();
            configs.put(DEFAULT_PUSH_PROVIDER, "FCM");
            when(notificationSenderManagementService.getNotificationSenderConfigurations(anyString(), anyBoolean()))
                    .thenReturn(configs);

            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
            doNothing().when(fcmPushProvider).registerDevice(any(), any());

            doNothing().when(deviceDAO).registerDevice(any(), anyInt());

            Device registeredDevice = deviceHandlerService
                    .registerDevice(registrationRequest, "carbon.super");

            Assert.assertNotNull(registeredDevice);
            Assert.assertEquals(registeredDevice.getProvider(), "FCM");
        }
    }

    /**
     * Test device registration falls back to push sender check when config map exists but does not
     * contain the DEFAULT_PUSH_PROVIDER key.
     * Covers the branch where configs is non-null but containsKey returns false.
     */
    @Test
    public void testRegisterDeviceWithConfigMissingDefaultKeyFallsBackToSenderCheck()
            throws UserStoreException, PushDeviceHandlerException, NotificationSenderManagementException {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        registrationRequest.setProvider(null);

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
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);

            // Return config map that exists but does NOT contain DEFAULT_PUSH_PROVIDER key
            Map<String, String> configs = new HashMap<>();
            configs.put("someOtherKey", "someValue");
            when(notificationSenderManagementService.getNotificationSenderConfigurations(anyString(), anyBoolean()))
                    .thenReturn(configs);

            // Return empty list so it falls through to exception
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(new ArrayList<>());

            Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
                deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
            });
        }
    }

    /**
     * Test device registration auto-detects single available push sender as default when no provider
     * is specified and no default provider is configured.
     */
    @Test
    public void testRegisterDeviceWithNoProviderAndNoDefaultSetsOnlyAvailableSenderAsDefault()
            throws PushDeviceHandlerException, UserStoreException, NotificationSenderManagementException,
            PushProviderException {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        registrationRequest.setProvider(null);

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
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);

            // Return empty config to simulate no default provider configured
            when(notificationSenderManagementService.getNotificationSenderConfigurations(anyString(), anyBoolean()))
                    .thenReturn(new HashMap<>());
            when(deviceDAO.getDevicesByUserId(anyString(), anyInt())).thenReturn(new ArrayList<>());

            // Mock single push sender available
            PushSenderDTO pushSenderDTO = new PushSenderDTO();
            pushSenderDTO.setName("FCM_PushPublisher");
            pushSenderDTO.setProvider("FCM");
            pushSenderDTO.setProviderId("fcm-provider-id");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
            when(pushDeviceHandlerDataHolder.getPushProvider(eq("FCM"))).thenReturn(fcmPushProvider);
            when(fcmPushProvider.getName()).thenReturn("FCM");
            doNothing().when(fcmPushProvider).registerDevice(any(), any());

            doNothing().when(deviceDAO).registerDevice(any(), anyInt());

            Device registeredDevice = deviceHandlerService
                    .registerDevice(registrationRequest, "carbon.super");

            Assert.assertNotNull(registeredDevice);
            Assert.assertEquals(registeredDevice.getProvider(), "FCM");
            Assert.assertTrue(deviceRegistrationContext.isRegistered());
        }
    }

    /**
     * Test device registration fails when no provider is specified, no default is configured,
     * and no push senders are available.
     */
    @Test
    public void testRegisterDeviceWithNoProviderAndNoDefaultFailsWhenNoPushSendersAvailable()
            throws UserStoreException, PushDeviceHandlerException, NotificationSenderManagementException {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        registrationRequest.setProvider(null);

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
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);

            // Return empty config to simulate no default provider configured
            when(notificationSenderManagementService.getNotificationSenderConfigurations(anyString(), anyBoolean()))
                    .thenReturn(new HashMap<>());
            when(deviceDAO.getDevicesByUserId(anyString(), anyInt())).thenReturn(new ArrayList<>());

            // Return empty list of push senders
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(new ArrayList<>());

            Assert.assertThrows(PushDeviceHandlerServerException.class, () -> {
                deviceHandlerService.registerDevice(registrationRequest, "carbon.super");
            });
        }
    }

    /**
     * Test device registration fails when no provider is specified, no default is configured,
     * and multiple push senders are available.
     */
    @Test
    public void testRegisterDeviceWithNoProviderAndNoDefaultFailsWhenMultiplePushSendersAvailable()
            throws UserStoreException, PushDeviceHandlerException, NotificationSenderManagementException {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        registrationRequest.setProvider(null);

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
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);

            NotificationSenderManagementService notificationSenderManagementService =
                    mock(NotificationSenderManagementService.class);
            when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                    .thenReturn(notificationSenderManagementService);

            // Return empty config to simulate no default provider configured
            when(notificationSenderManagementService.getNotificationSenderConfigurations(anyString(), anyBoolean()))
                    .thenReturn(new HashMap<>());
            when(deviceDAO.getDevicesByUserId(anyString(), anyInt())).thenReturn(new ArrayList<>());

            // Return multiple push senders
            PushSenderDTO pushSenderDTO1 = new PushSenderDTO();
            pushSenderDTO1.setName("FCM_PushPublisher");
            pushSenderDTO1.setProvider("FCM");
            PushSenderDTO pushSenderDTO2 = new PushSenderDTO();
            pushSenderDTO2.setName("AmazonSNS_PushPublisher");
            pushSenderDTO2.setProvider("SNS");
            List<PushSenderDTO> pushSenders = new ArrayList<>();
            pushSenders.add(pushSenderDTO1);
            pushSenders.add(pushSenderDTO2);
            when(notificationSenderManagementService.getPushSenders(anyBoolean()))
                    .thenReturn(pushSenders);

            Assert.assertThrows(PushDeviceHandlerServerException.class,
                    () -> deviceHandlerService.registerDevice(registrationRequest, "carbon.super"));
        }
    }

    @Test
    public void testRegisterDeviceTriggersRegistrationNotificationsWhenEnabled() throws Exception {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        DeviceRegistrationContext deviceRegistrationContext = createDeviceRegistrationContext();
        when(deviceRegistrationContextManager.getContext(anyString(), anyString()))
                .thenReturn(deviceRegistrationContext);

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
                MockedStatic<IdentityContext> mockedIdentityContext =
                        Mockito.mockStatic(IdentityContext.class);
        ) {
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);
            UserRealm userRealm = mock(UserRealm.class);
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");

            User user = mock(User.class);
            when(user.getUserID()).thenReturn("testUserId");
            when(user.getUsername()).thenReturn("testUser");
            when(user.getUserStoreDomain()).thenReturn("PRIMARY");
            Map<String, String> userAttributes = new HashMap<>();
            userAttributes.put(NotificationChannels.EMAIL_CHANNEL.getClaimUri(), "testuser@wso2.com");
            when(user.getAttributes()).thenReturn(userAttributes);
            when(abstractUserStoreManager.getUserWithID(anyString(), any(String[].class), anyString()))
                    .thenReturn(user);

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);
            when(configManager.getResource(anyString(), anyString(), anyBoolean()))
                    .thenReturn(createDeviceMgtConfigResource("true", "5", "true", "EMAIL,PUSH_NOTIFICATION"));

            Device existingDevice = new Device();
            existingDevice.setDeviceId("existing-device-id");
            existingDevice.setProvider("FCM");
            existingDevice.setDeviceToken("existing-device-token");
            existingDevice.setDeviceHandle("existing-device-handle");
            when(deviceDAO.getDevicesByUserId(anyString(), anyInt()))
                    .thenReturn(java.util.Collections.singletonList(existingDevice));

            mockPushSenderAndProvider(pushDeviceHandlerDataHolder);
            doNothing().when(deviceDAO).registerDevice(any(), anyInt());

            IdentityEventService identityEventService = mock(IdentityEventService.class);
            when(pushDeviceHandlerDataHolder.getIdentityEventService()).thenReturn(identityEventService);
            doNothing().when(identityEventService).handleEvent(any());

            IdentityContext identityContext = mock(IdentityContext.class);
            mockedIdentityContext.when(IdentityContext::getThreadLocalIdentityContext)
                    .thenReturn(identityContext);
            Request request = mock(Request.class);
            List<Header> headers = new ArrayList<>();
            headers.add(new Header("User-Agent", java.util.Collections.singletonList("test-agent")));
            headers.add(new Header("X-Forwarded-For",
                    java.util.Collections.singletonList("10.0.0.1, 192.168.1.1")));
            when(request.getHeaders()).thenReturn(headers);
            when(identityContext.getRequest()).thenReturn(request);

            Device registeredDevice = deviceHandlerService.registerDevice(registrationRequest, "carbon.super");

            Assert.assertNotNull(registeredDevice);
            Assert.assertTrue(deviceRegistrationContext.isRegistered());
            // One email notification plus one push notification to the existing device.
            verify(identityEventService, times(2)).handleEvent(any());
        }
    }

    @Test
    public void testRegisterDeviceSkipsNotificationsWhenEmailClaimNotAvailable() throws Exception {

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
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");
            // The resolved user has no email claim, so the email notification must be
            // skipped and registration must still succeed.
            User user = mock(User.class);
            when(user.getUserID()).thenReturn("testUserId");
            when(user.getUsername()).thenReturn("testUser");
            when(user.getUserStoreDomain()).thenReturn("PRIMARY");
            when(user.getAttributes()).thenReturn(new HashMap<>());
            when(abstractUserStoreManager.getUserWithID(anyString(), any(String[].class), anyString()))
                    .thenReturn(user);

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);
            when(configManager.getResource(anyString(), anyString(), anyBoolean()))
                    .thenReturn(createDeviceMgtConfigResource("true", "5", "true", "EMAIL,PUSH_NOTIFICATION"));

            // No previously registered devices, so the push notification is skipped too.
            when(deviceDAO.getDevicesByUserId(anyString(), anyInt())).thenReturn(new ArrayList<>());

            mockPushSenderAndProvider(pushDeviceHandlerDataHolder);
            doNothing().when(deviceDAO).registerDevice(any(), anyInt());

            IdentityEventService identityEventService = mock(IdentityEventService.class);
            when(pushDeviceHandlerDataHolder.getIdentityEventService()).thenReturn(identityEventService);

            Device registeredDevice = deviceHandlerService.registerDevice(registrationRequest, "carbon.super");

            Assert.assertNotNull(registeredDevice);
            Assert.assertTrue(deviceRegistrationContext.isRegistered());
            verify(identityEventService, times(0)).handleEvent(any());
        }
    }

    @Test
    public void testRegisterDeviceSucceedsWhenNotificationDeliveryFails() throws Exception {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        DeviceRegistrationContext deviceRegistrationContext = createDeviceRegistrationContext();
        when(deviceRegistrationContextManager.getContext(anyString(), anyString()))
                .thenReturn(deviceRegistrationContext);

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
                MockedStatic<IdentityContext> mockedIdentityContext =
                        Mockito.mockStatic(IdentityContext.class);
        ) {
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);
            UserRealm userRealm = mock(UserRealm.class);
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");

            User user = mock(User.class);
            when(user.getUserID()).thenReturn("testUserId");
            when(user.getUsername()).thenReturn("testUser");
            when(user.getUserStoreDomain()).thenReturn("PRIMARY");
            Map<String, String> userAttributes = new HashMap<>();
            userAttributes.put(NotificationChannels.EMAIL_CHANNEL.getClaimUri(), "testuser@wso2.com");
            when(user.getAttributes()).thenReturn(userAttributes);
            when(abstractUserStoreManager.getUserWithID(anyString(), any(String[].class), anyString()))
                    .thenReturn(user);

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);
            when(configManager.getResource(anyString(), anyString(), anyBoolean()))
                    .thenReturn(createDeviceMgtConfigResource("true", "5", "true", "EMAIL,PUSH_NOTIFICATION"));

            Device existingDevice = new Device();
            existingDevice.setDeviceId("existing-device-id");
            when(deviceDAO.getDevicesByUserId(anyString(), anyInt()))
                    .thenReturn(java.util.Collections.singletonList(existingDevice));

            mockPushSenderAndProvider(pushDeviceHandlerDataHolder);
            doNothing().when(deviceDAO).registerDevice(any(), anyInt());

            IdentityEventService identityEventService = mock(IdentityEventService.class);
            when(pushDeviceHandlerDataHolder.getIdentityEventService()).thenReturn(identityEventService);
            doThrow(new IdentityEventException("Notification delivery failed."))
                    .when(identityEventService).handleEvent(any());

            // No inbound request captured in the identity context; the IP address
            // cannot be resolved and the notification is sent without it.
            IdentityContext identityContext = mock(IdentityContext.class);
            mockedIdentityContext.when(IdentityContext::getThreadLocalIdentityContext)
                    .thenReturn(identityContext);
            when(identityContext.getRequest()).thenReturn(null);

            Device registeredDevice = deviceHandlerService.registerDevice(registrationRequest, "carbon.super");

            Assert.assertNotNull(registeredDevice);
            Assert.assertTrue(deviceRegistrationContext.isRegistered());
            verify(identityEventService, times(2)).handleEvent(any());
        }
    }

    @Test
    public void testRegisterDeviceEmitsDiagnosticLogsWhenNotificationDeliveryFails() throws Exception {

        RegistrationRequest registrationRequest = createRegistrationRequest();
        DeviceRegistrationContext deviceRegistrationContext = createDeviceRegistrationContext();
        when(deviceRegistrationContextManager.getContext(anyString(), anyString()))
                .thenReturn(deviceRegistrationContext);

        try (
                MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil =
                        Mockito.mockStatic(IdentityTenantUtil.class);
                MockedStatic<PushDeviceHandlerDataHolder> mockedPushDeviceHandlerDataHolder =
                        Mockito.mockStatic(PushDeviceHandlerDataHolder.class);
                MockedStatic<IdentityContext> mockedIdentityContext =
                        Mockito.mockStatic(IdentityContext.class);
        ) {
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(-1234);
            UserRealm userRealm = mock(UserRealm.class);
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getRealm(anyString(), anyString()))
                    .thenReturn(userRealm);
            AbstractUserStoreManager abstractUserStoreManager = mock(AbstractUserStoreManager.class);
            when(userRealm.getUserStoreManager()).thenReturn(abstractUserStoreManager);
            when(abstractUserStoreManager.getUserIDFromUserName(anyString())).thenReturn("testUserId");

            // Enable diagnostic logs so the notification failure paths emit diagnostic log events.
            mockedLoggerUtils.when(LoggerUtils::isDiagnosticLogsEnabled).thenReturn(true);

            User user = mock(User.class);
            when(user.getUserID()).thenReturn("testUserId");
            when(user.getUsername()).thenReturn("testUser");
            when(user.getUserStoreDomain()).thenReturn("PRIMARY");
            Map<String, String> userAttributes = new HashMap<>();
            userAttributes.put(NotificationChannels.EMAIL_CHANNEL.getClaimUri(), "testuser@wso2.com");
            when(user.getAttributes()).thenReturn(userAttributes);
            when(abstractUserStoreManager.getUserWithID(anyString(), any(String[].class), anyString()))
                    .thenReturn(user);

            PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder = mock(PushDeviceHandlerDataHolder.class);
            mockedPushDeviceHandlerDataHolder.when(
                    PushDeviceHandlerDataHolder::getInstance).thenReturn(pushDeviceHandlerDataHolder);
            ConfigurationManager configManager = mock(ConfigurationManager.class);
            when(pushDeviceHandlerDataHolder.getConfigurationManager()).thenReturn(configManager);
            when(configManager.getResource(anyString(), anyString(), anyBoolean()))
                    .thenReturn(createDeviceMgtConfigResource("true", "5", "true", "EMAIL,PUSH_NOTIFICATION"));

            Device existingDevice = new Device();
            existingDevice.setDeviceId("existing-device-id");
            when(deviceDAO.getDevicesByUserId(anyString(), anyInt()))
                    .thenReturn(java.util.Collections.singletonList(existingDevice));

            mockPushSenderAndProvider(pushDeviceHandlerDataHolder);
            doNothing().when(deviceDAO).registerDevice(any(), anyInt());

            IdentityEventService identityEventService = mock(IdentityEventService.class);
            when(pushDeviceHandlerDataHolder.getIdentityEventService()).thenReturn(identityEventService);
            doThrow(new IdentityEventException("Notification delivery failed."))
                    .when(identityEventService).handleEvent(any());

            // An inbound request is captured in the identity context so the IP address
            // input parameter is included in the emitted diagnostic logs.
            IdentityContext identityContext = mock(IdentityContext.class);
            mockedIdentityContext.when(IdentityContext::getThreadLocalIdentityContext)
                    .thenReturn(identityContext);
            org.wso2.carbon.identity.core.context.model.Request request =
                    mock(org.wso2.carbon.identity.core.context.model.Request.class);
            when(request.getIpAddress()).thenReturn("10.0.0.1");
            when(identityContext.getRequest()).thenReturn(request);

            Device registeredDevice = deviceHandlerService.registerDevice(registrationRequest, "carbon.super");

            Assert.assertNotNull(registeredDevice);
            Assert.assertTrue(deviceRegistrationContext.isRegistered());
            verify(identityEventService, times(2)).handleEvent(any());
        }
    }

    private void mockPushSenderAndProvider(PushDeviceHandlerDataHolder pushDeviceHandlerDataHolder)
            throws NotificationSenderManagementException, PushProviderException {

        NotificationSenderManagementService notificationSenderManagementService =
                mock(NotificationSenderManagementService.class);
        when(pushDeviceHandlerDataHolder.getNotificationSenderManagementService())
                .thenReturn(notificationSenderManagementService);
        PushSenderDTO pushSenderDTO = new PushSenderDTO();
        pushSenderDTO.setName("FCM_PushPublisher");
        pushSenderDTO.setProvider("FCM");
        pushSenderDTO.setProviderId("fcm-provider-id");
        List<PushSenderDTO> pushSenders = new ArrayList<>();
        pushSenders.add(pushSenderDTO);
        when(notificationSenderManagementService.getPushSenders(anyBoolean())).thenReturn(pushSenders);

        FCMPushProvider fcmPushProvider = mock(FCMPushProvider.class);
        when(pushDeviceHandlerDataHolder.getPushProvider(anyString())).thenReturn(fcmPushProvider);
        when(fcmPushProvider.getName()).thenReturn("FCM");
        doNothing().when(fcmPushProvider).registerDevice(any(), any());
    }

    private org.wso2.carbon.identity.configuration.mgt.core.model.Resource createDeviceMgtConfigResource(
            String enableMultipleDeviceEnrollment, String maximumDeviceLimit,
            String enableNotifications, String notificationChannels) {

        org.wso2.carbon.identity.configuration.mgt.core.model.Resource resource =
                new org.wso2.carbon.identity.configuration.mgt.core.model.Resource();
        List<org.wso2.carbon.identity.configuration.mgt.core.model.Attribute> attributes = new ArrayList<>();
        attributes.add(new org.wso2.carbon.identity.configuration.mgt.core.model.Attribute(
                "enableMultipleDeviceEnrollment", enableMultipleDeviceEnrollment));
        attributes.add(new org.wso2.carbon.identity.configuration.mgt.core.model.Attribute(
                "maximumDeviceLimit", maximumDeviceLimit));
        attributes.add(new org.wso2.carbon.identity.configuration.mgt.core.model.Attribute(
                "enableDeviceRegistrationNotifications", enableNotifications));
        attributes.add(new org.wso2.carbon.identity.configuration.mgt.core.model.Attribute(
                "deviceRegistrationNotificationChannels", notificationChannels));
        resource.setAttributes(attributes);
        return resource;
    }

    private RegistrationRequest createRegistrationRequest() {

        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setDeviceId("1234567890");
        registrationRequest.setDeviceName("deviceName");
        registrationRequest.setDeviceModel("deviceModel");
        registrationRequest.setDeviceToken(deviceToken);
        registrationRequest.setSignature(signature);
        registrationRequest.setPublicKey(publicKey);
        // Add provider data for multi-provider support
        RegistrationRequestProviderData providerData = new RegistrationRequestProviderData("FCM", null);
        registrationRequest.setProvider(providerData);
        return registrationRequest;
    }
}
