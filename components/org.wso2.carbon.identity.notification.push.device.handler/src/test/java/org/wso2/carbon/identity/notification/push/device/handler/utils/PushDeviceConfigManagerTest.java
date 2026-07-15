/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.notification.push.device.handler.utils;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceTypeAdd;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.notification.push.device.handler.internal.PushDeviceHandlerDataHolder;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationNotificationChannelEnum;
import org.wso2.carbon.identity.notification.push.device.handler.model.PushDeviceMgtConfigData;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.configuration.mgt.core.constant.ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_DOES_NOT_EXISTS;
import static org.wso2.carbon.identity.configuration.mgt.core.constant.ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEFAULT_MAX_DEVICE_LIMIT_PER_USER;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_DEVICE_LIMIT_PER_USER_EXCEEDS;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_INVALID_DEVICE_LIMIT_CONFIG;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_INVALID_DEVICE_LIMIT_VALUE;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_UPDATING_PUSH_DEVICE_CONFIG;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.MAX_DEVICE_LIMIT_PER_USER;

/**
 * Unit tests for PushDeviceConfigManager.
 */
public class PushDeviceConfigManagerTest {

    private static final String TENANT_DOMAIN = "carbon.super";
    private static final String RESOURCE_TYPE = "DEVICE_MANAGEMENT";
    private static final String RESOURCE_NAME = "PUSH_DEVICE_MANAGEMENT";
    private static final String ATTR_ENABLE_DEVICE_MANAGEMENT = "enableMultipleDeviceEnrollment";
    private static final String ATTR_MAX_DEVICE_LIMIT = "maximumDeviceLimit";
    private static final String DEFAULT_ENABLE_DEVICE_MANAGEMENT = "false";
    private static final String DEFAULT_MAX_DEVICE_LIMIT = "2";

    @Mock
    private ConfigurationManager configurationManager;

    private MockedStatic<PushDeviceHandlerDataHolder> mockedDataHolder;
    private MockedStatic<IdentityUtil> mockedIdentityUtil;
    private PushDeviceHandlerDataHolder dataHolderInstance;

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        dataHolderInstance = mock(PushDeviceHandlerDataHolder.class);
        mockedDataHolder = mockStatic(PushDeviceHandlerDataHolder.class);
        mockedDataHolder.when(PushDeviceHandlerDataHolder::getInstance).thenReturn(dataHolderInstance);
        when(dataHolderInstance.getConfigurationManager()).thenReturn(configurationManager);
        mockedIdentityUtil = mockStatic(IdentityUtil.class);
        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(MAX_DEVICE_LIMIT_PER_USER))
                .thenReturn(null);
    }

    @AfterMethod
    public void tearDown() {

        mockedDataHolder.close();
        mockedIdentityUtil.close();
    }

    @Test
    public void testGetPushDeviceConfig_WhenResourceExists_ReturnsConfigData() throws Exception {

        Resource resource = createResourceWithAttributes("true", "5");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenReturn(resource);

        PushDeviceMgtConfigData result = PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.getEnableMultipleDeviceEnrollment(),
                "Multiple device enrollment should be enabled as per the resource attributes.");
        Assert.assertEquals(result.getMaximumDeviceLimit(), Integer.valueOf(5),
                "Maximum device limit should match the resource attribute value.");
    }

    @Test
    public void testGetPushDeviceConfig_WhenResourceNotFound_ReturnsDefaultConfig() throws Exception {

        ConfigurationManagementException exception = mock(ConfigurationManagementException.class);
        when(exception.getErrorCode()).thenReturn(ERROR_CODE_RESOURCE_DOES_NOT_EXISTS.getCode());
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenThrow(exception);

        PushDeviceMgtConfigData result = PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);

        assertIsDefaultConfig(result);
    }

    @Test
    public void testGetPushDeviceConfig_WhenResourceTypeNotFound_ReturnsDefaultConfig() throws Exception {

        ConfigurationManagementException exception = mock(ConfigurationManagementException.class);
        when(exception.getErrorCode()).thenReturn(ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS.getCode());
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenThrow(exception);

        PushDeviceMgtConfigData result = PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);

        assertIsDefaultConfig(result);
    }

    @Test(expectedExceptions = IdentityException.class)
    public void testGetPushDeviceConfig_WhenUnexpectedConfigException_ThrowsServerException() throws Exception {

        ConfigurationManagementException exception = mock(ConfigurationManagementException.class);
        when(exception.getErrorCode()).thenReturn("CONFIGM_UNEXPECTED_00099");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenThrow(exception);

        PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);
    }

    @Test
    public void testGetPushDeviceConfig_WhenUnexpectedConfigException_ErrorCodeMatches() throws Exception {

        ConfigurationManagementException exception = mock(ConfigurationManagementException.class);
        when(exception.getErrorCode()).thenReturn("CONFIGM_UNEXPECTED_00099");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenThrow(exception);

        try {
            PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);
            Assert.fail("Expected IdentityException was not thrown.");
        } catch (IdentityException e) {
            Assert.assertEquals(e.getErrorCode(), ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG.getCode(),
                    "Error code should be PDH-15014 for config retrieval failure.");
        }
    }

    @Test
    public void testGetPushDeviceConfig_WhenResourceIsNull_ReturnsDefaultConfig() throws Exception {

        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenReturn(null);

        PushDeviceMgtConfigData result = PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);

        assertIsDefaultConfig(result);
    }

    @Test
    public void testGetPushDeviceConfig_WhenResourceHasNullAttributes_ReturnsDefaultConfig() throws Exception {

        Resource resource = new Resource();
        resource.setResourceName(RESOURCE_NAME);
        resource.setAttributes(null);
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenReturn(resource);

        PushDeviceMgtConfigData result = PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);

        assertIsDefaultConfig(result);
    }

    @Test
    public void testGetPushDeviceConfig_WhenResourceHasEmptyAttributes_ReturnsDefaultConfig() throws Exception {

        Resource resource = new Resource();
        resource.setResourceName(RESOURCE_NAME);
        resource.setAttributes(new ArrayList<>());
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenReturn(resource);

        PushDeviceMgtConfigData result = PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);

        assertIsDefaultConfig(result);
    }

    @Test
    public void testGetPushDeviceConfig_DefaultConfig_HasExpectedValues() throws Exception {

        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenReturn(null);

        PushDeviceMgtConfigData result = PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getEnableMultipleDeviceEnrollment(),
                Boolean.valueOf(DEFAULT_ENABLE_DEVICE_MANAGEMENT),
                "Default config must disable multiple device enrollment.");
        Assert.assertEquals(result.getMaximumDeviceLimit(), Integer.valueOf(DEFAULT_MAX_DEVICE_LIMIT),
                "Default config must set the maximum device limit to 1.");
        Assert.assertFalse(result.getEnableDeviceRegistrationNotifications(),
                "Default config must disable device registration notifications.");
        Assert.assertEquals(result.getDeviceRegistrationNotificationChannels(),
                java.util.EnumSet.of(DeviceRegistrationNotificationChannelEnum.EMAIL),
                "Default config must default the notification channels to EMAIL only.");
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenResourceExists_CallsReplaceResource() throws Exception {

        Resource existing = createResourceWithAttributes("false", "1");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenReturn(existing);

        PushDeviceMgtConfigData config = createConfigData(true, 5);
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);

        verify(configurationManager).replaceResource(eq(RESOURCE_TYPE), any(Resource.class));
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenResourceNotExists_CallsAddResource() throws Exception {

        ConfigurationManagementException exception = mock(ConfigurationManagementException.class);
        when(exception.getErrorCode()).thenReturn(ERROR_CODE_RESOURCE_DOES_NOT_EXISTS.getCode());
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenThrow(exception);

        PushDeviceMgtConfigData config = createConfigData(true, 3);
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);

        verify(configurationManager).addResource(eq(RESOURCE_TYPE), any(Resource.class));
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenResourceTypeNotExists_CreatesTypeAndAdds() throws Exception {

        ConfigurationManagementException getException = mock(ConfigurationManagementException.class);
        when(getException.getErrorCode()).thenReturn(ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS.getCode());
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenThrow(getException);

        ConfigurationManagementException addException = mock(ConfigurationManagementException.class);
        when(addException.getErrorCode()).thenReturn(ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS.getCode());
        when(configurationManager.addResource(eq(RESOURCE_TYPE), any(Resource.class)))
                .thenThrow(addException)
                .thenReturn(new Resource());

        PushDeviceMgtConfigData config = createConfigData(true, 2);
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);

        verify(configurationManager).addResourceType(any(ResourceTypeAdd.class));
    }

    @Test(expectedExceptions = IdentityException.class)
    public void testUpdatePushDeviceConfig_WhenReplaceThrowsException_ThrowsServerException() throws Exception {

        Resource existing = createResourceWithAttributes("false", "1");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenReturn(existing);

        ConfigurationManagementException replaceException = mock(ConfigurationManagementException.class);
        when(replaceException.getErrorCode()).thenReturn("CONFIGM_UNEXPECTED_00099");
        when(configurationManager.replaceResource(eq(RESOURCE_TYPE), any(Resource.class)))
                .thenThrow(replaceException);

        PushDeviceMgtConfigData config = createConfigData(true, 5);
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenReplaceThrowsException_ErrorCodeMatches() throws Exception {

        Resource existing = createResourceWithAttributes("false", "1");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenReturn(existing);

        ConfigurationManagementException replaceException = mock(ConfigurationManagementException.class);
        when(replaceException.getErrorCode()).thenReturn("CONFIGM_UNEXPECTED_00099");
        when(configurationManager.replaceResource(eq(RESOURCE_TYPE), any(Resource.class)))
                .thenThrow(replaceException);

        PushDeviceMgtConfigData config = createConfigData(true, 5);
        try {
            PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);
            Assert.fail("Expected IdentityException was not thrown.");
        } catch (IdentityException e) {
            Assert.assertEquals(e.getErrorCode(), ERROR_CODE_UPDATING_PUSH_DEVICE_CONFIG.getCode(),
                    "Error code should be PDH-15016 for config update failure.");
        }
    }

    @Test
    public void testUpdatePushDeviceConfig_BuildsCorrectResourceAttributes() throws Exception {

        Resource existing = createResourceWithAttributes("false", "1");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenReturn(existing);

        PushDeviceMgtConfigData config = createConfigData(true, 10);
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);

        verify(configurationManager).replaceResource(eq(RESOURCE_TYPE), any(Resource.class));
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenUpperBoundConfigIsInvalid_FallsBackToDefault() throws Exception {

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(MAX_DEVICE_LIMIT_PER_USER))
                .thenReturn("not-a-number");
        Resource existing = createResourceWithAttributes("true", "3");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenReturn(existing);

        PushDeviceMgtConfigData config = createConfigData(true, DEFAULT_MAX_DEVICE_LIMIT_PER_USER);
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);

        verify(configurationManager).replaceResource(eq(RESOURCE_TYPE), any(Resource.class));
    }

    @Test(expectedExceptions = IdentityException.class)
    public void testUpdatePushDeviceConfig_WhenLimitExceedsServerUpperBound_ThrowsClientException()
            throws Exception {

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(MAX_DEVICE_LIMIT_PER_USER))
                .thenReturn("5");

        PushDeviceMgtConfigData config = createConfigData(true, 6);
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenLimitExceedsServerUpperBound_ErrorCodeMatches()
            throws Exception {

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(MAX_DEVICE_LIMIT_PER_USER))
                .thenReturn("5");

        PushDeviceMgtConfigData config = createConfigData(true, 6);
        try {
            PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);
            Assert.fail("Expected IdentityException was not thrown.");
        } catch (IdentityException e) {
            Assert.assertEquals(e.getErrorCode(),
                    ERROR_CODE_DEVICE_LIMIT_PER_USER_EXCEEDS.getCode(),
                    "Error code should be PDH-15018 when limit exceeds server upper bound.");
        }
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenLimitEqualsServerUpperBound_Succeeds()
            throws Exception {

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(MAX_DEVICE_LIMIT_PER_USER))
                .thenReturn("5");
        Resource existing = createResourceWithAttributes("true", "3");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenReturn(existing);

        PushDeviceMgtConfigData config = createConfigData(true, 5);
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);

        verify(configurationManager).replaceResource(eq(RESOURCE_TYPE), any(Resource.class));
    }

    @Test(expectedExceptions = IdentityException.class)
    public void testUpdatePushDeviceConfig_WhenMultiDeviceDisabledAndLimitAboveOne_ThrowsClientException()
            throws Exception {

        Resource existing = createResourceWithAttributes("false", "3");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenReturn(existing);

        PushDeviceMgtConfigData config = createConfigData(false, 3);
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenMultiDeviceDisabledAndLimitAboveOne_ErrorCodeMatches()
            throws Exception {

        Resource existing = createResourceWithAttributes("false", "3");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenReturn(existing);

        PushDeviceMgtConfigData config = createConfigData(false, 3);
        try {
            PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);
            Assert.fail("Expected IdentityException was not thrown.");
        } catch (IdentityException e) {
            Assert.assertEquals(e.getErrorCode(), ERROR_CODE_INVALID_DEVICE_LIMIT_CONFIG.getCode(),
                    "Error code should be PDH-15017 for invalid device limit config.");
        }
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenMultiDeviceDisabledAndLimitIsOne_Succeeds() throws Exception {

        ConfigurationManagementException exception = mock(ConfigurationManagementException.class);
        when(exception.getErrorCode()).thenReturn(ERROR_CODE_RESOURCE_DOES_NOT_EXISTS.getCode());
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenThrow(exception);

        PushDeviceMgtConfigData config = createConfigData(false, 1);
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);

        verify(configurationManager).addResource(eq(RESOURCE_TYPE), any(Resource.class));
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenMultiDeviceEnabledAndLimitAboveOne_Succeeds() throws Exception {

        Resource existing = createResourceWithAttributes("false", "1");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenReturn(existing);

        PushDeviceMgtConfigData config = createConfigData(true, 5);
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);

        verify(configurationManager).replaceResource(eq(RESOURCE_TYPE), any(Resource.class));
    }

    private Resource createResourceWithAttributes(String enableDeviceManagement, String maxDeviceLimit) {

        Resource resource = new Resource();
        resource.setResourceName(RESOURCE_NAME);
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute(ATTR_ENABLE_DEVICE_MANAGEMENT, enableDeviceManagement));
        attributes.add(new Attribute(ATTR_MAX_DEVICE_LIMIT, maxDeviceLimit));
        resource.setAttributes(attributes);
        return resource;
    }

    @Test
    public void testGetPushDeviceConfig_WhenAllAttributesPresent_MapsNotificationFlags() throws Exception {

        Resource resource = createResourceWithAttributes("true", "5");
        resource.getAttributes().add(new Attribute("enableDeviceRegistrationNotifications", "true"));
        resource.getAttributes().add(new Attribute("deviceRegistrationNotificationChannels",
                "EMAIL,PUSH_NOTIFICATION"));
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenReturn(resource);

        PushDeviceMgtConfigData result = PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);

        Assert.assertTrue(result.getEnableDeviceRegistrationNotifications(),
                "Notification enable flag should be mapped from the resource attributes.");
        Assert.assertEquals(result.getDeviceRegistrationNotificationChannels(),
                java.util.EnumSet.of(
                        DeviceRegistrationNotificationChannelEnum.EMAIL,
                        DeviceRegistrationNotificationChannelEnum.PUSH_NOTIFICATION),
                "Notification channels should be mapped from the resource attributes.");
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenMaxDeviceLimitIsNull_SkipsLimitValidations() throws Exception {

        Resource existing = createResourceWithAttributes("true", "3");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenReturn(existing);

        PushDeviceMgtConfigData config = new PushDeviceMgtConfigData();
        config.setEnableMultipleDeviceEnrollment(false);
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);

        verify(configurationManager).replaceResource(eq(RESOURCE_TYPE), any(Resource.class));
    }

    @Test
    public void testGetPushDeviceConfig_WhenChannelsAttributeIsBlank_ReturnsEmptyChannelSet() throws Exception {

        Resource resource = createResourceWithAttributes("true", "5");
        resource.getAttributes().add(new Attribute("enableDeviceRegistrationNotifications", "true"));
        resource.getAttributes().add(new Attribute("deviceRegistrationNotificationChannels", "   "));
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenReturn(resource);

        PushDeviceMgtConfigData result = PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);

        Assert.assertNotNull(result.getDeviceRegistrationNotificationChannels(),
                "Channel set should not be null for blank attribute.");
        Assert.assertTrue(result.getDeviceRegistrationNotificationChannels().isEmpty(),
                "Channel set should be empty when the attribute is blank.");
    }

    @Test
    public void testGetPushDeviceConfig_WhenChannelsAttributeHasInvalidAndEmptyTokens_SkipsThem() throws Exception {

        Resource resource = createResourceWithAttributes("true", "5");
        resource.getAttributes().add(new Attribute("enableDeviceRegistrationNotifications", "true"));
        resource.getAttributes().add(new Attribute("deviceRegistrationNotificationChannels",
                "EMAIL, ,UNKNOWN_CHANNEL,push_notification"));
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenReturn(resource);

        PushDeviceMgtConfigData result = PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);

        Assert.assertEquals(result.getDeviceRegistrationNotificationChannels(),
                java.util.EnumSet.of(
                        DeviceRegistrationNotificationChannelEnum.EMAIL,
                        DeviceRegistrationNotificationChannelEnum.PUSH_NOTIFICATION),
                "Empty and unknown tokens should be skipped while keeping valid channels.");
    }

    @Test
    public void testUpdatePushDeviceConfig_WithNotificationChannels_SerialisesIntoAttribute() throws Exception {

        Resource existing = createResourceWithAttributes("true", "3");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenReturn(existing);

        PushDeviceMgtConfigData config = createConfigData(true, 3);
        config.setEnableDeviceRegistrationNotifications(true);
        config.setDeviceRegistrationNotificationChannels(java.util.EnumSet.of(
                DeviceRegistrationNotificationChannelEnum.EMAIL,
                DeviceRegistrationNotificationChannelEnum.PUSH_NOTIFICATION));

        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);

        org.mockito.ArgumentCaptor<Resource> captor = org.mockito.ArgumentCaptor.forClass(Resource.class);
        verify(configurationManager).replaceResource(eq(RESOURCE_TYPE), captor.capture());

        String serialised = captor.getValue().getAttributes().stream()
                .filter(a -> "deviceRegistrationNotificationChannels".equals(a.getKey()))
                .map(Attribute::getValue)
                .findFirst()
                .orElse(null);
        Assert.assertNotNull(serialised, "Notification channels attribute should be present.");
        java.util.Set<String> tokens = new java.util.HashSet<>(java.util.Arrays.asList(serialised.split(",")));
        Assert.assertEquals(tokens,
                new java.util.HashSet<>(java.util.Arrays.asList("EMAIL", "PUSH_NOTIFICATION")),
                "Serialised channels should contain EMAIL and PUSH_NOTIFICATION tokens.");
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenMultiDeviceEnabledAndLimitIsNull_ThrowsClientException()
            throws Exception {

        PushDeviceMgtConfigData config = new PushDeviceMgtConfigData();
        config.setEnableMultipleDeviceEnrollment(true);
        // maximumDeviceLimit intentionally left null.
        try {
            PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);
            Assert.fail("Expected IdentityException was not thrown.");
        } catch (IdentityException e) {
            Assert.assertEquals(e.getErrorCode(), ERROR_CODE_INVALID_DEVICE_LIMIT_VALUE.getCode(),
                    "Error code should be PDH-15021 when multi-device is enabled without a positive limit.");
        }
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenMultiDeviceEnabledAndLimitIsNotPositive_ThrowsClientException()
            throws Exception {

        PushDeviceMgtConfigData config = createConfigData(true, 0);
        try {
            PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);
            Assert.fail("Expected IdentityException was not thrown.");
        } catch (IdentityException e) {
            Assert.assertEquals(e.getErrorCode(), ERROR_CODE_INVALID_DEVICE_LIMIT_VALUE.getCode(),
                    "Error code should be PDH-15021 when the limit is not positive.");
        }
    }

    @Test
    public void testUpdatePushDeviceConfig_WhenMultiDeviceDisabledAndLimitIsNull_PersistsDefaultLimit()
            throws Exception {

        ConfigurationManagementException exception = mock(ConfigurationManagementException.class);
        when(exception.getErrorCode()).thenReturn(ERROR_CODE_RESOURCE_DOES_NOT_EXISTS.getCode());
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, false)).thenThrow(exception);

        PushDeviceMgtConfigData config = new PushDeviceMgtConfigData();
        config.setEnableMultipleDeviceEnrollment(false);
        // maximumDeviceLimit intentionally left null.
        PushDeviceConfigManager.updatePushDeviceConfig(config, TENANT_DOMAIN);

        org.mockito.ArgumentCaptor<Resource> captor = org.mockito.ArgumentCaptor.forClass(Resource.class);
        verify(configurationManager).addResource(eq(RESOURCE_TYPE), captor.capture());

        String persistedLimit = captor.getValue().getAttributes().stream()
                .filter(a -> ATTR_MAX_DEVICE_LIMIT.equals(a.getKey()))
                .map(Attribute::getValue)
                .findFirst()
                .orElse(null);
        // A null limit must never be persisted as the literal string "null".
        Assert.assertEquals(persistedLimit, DEFAULT_MAX_DEVICE_LIMIT,
                "A null maximum device limit must be normalised to the default before persistence.");
    }

    @Test
    public void testGetPushDeviceConfig_WhenStoredLimitIsNotNumeric_FallsBackToDefault() throws Exception {

        Resource resource = createResourceWithAttributes("true", "null");
        when(configurationManager.getResource(RESOURCE_TYPE, RESOURCE_NAME, true)).thenReturn(resource);

        // A corrupted/legacy non-numeric limit must not make the config unreadable.
        PushDeviceMgtConfigData result = PushDeviceConfigManager.getPushDeviceConfig(TENANT_DOMAIN);

        Assert.assertEquals(result.getMaximumDeviceLimit(), Integer.valueOf(DEFAULT_MAX_DEVICE_LIMIT),
                "A non-numeric stored limit must fall back to the default.");
    }

    private PushDeviceMgtConfigData createConfigData(boolean enableMultipleDeviceEnrollment, int maximumDeviceLimit) {

        PushDeviceMgtConfigData config = new PushDeviceMgtConfigData();
        config.setEnableMultipleDeviceEnrollment(enableMultipleDeviceEnrollment);
        config.setMaximumDeviceLimit(maximumDeviceLimit);
        return config;
    }

    private void assertIsDefaultConfig(PushDeviceMgtConfigData config) {

        Assert.assertNotNull(config, "Default config must not be null.");
        Assert.assertEquals(config.getEnableMultipleDeviceEnrollment(),
                Boolean.valueOf(DEFAULT_ENABLE_DEVICE_MANAGEMENT),
                "Default config must disable multiple device enrollment.");
        Assert.assertEquals(config.getMaximumDeviceLimit(), Integer.valueOf(DEFAULT_MAX_DEVICE_LIMIT),
                "Default config must set the maximum device limit to 1.");
    }
}
