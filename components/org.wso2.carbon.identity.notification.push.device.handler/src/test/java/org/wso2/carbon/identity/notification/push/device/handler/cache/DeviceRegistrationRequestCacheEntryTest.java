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

package org.wso2.carbon.identity.notification.push.device.handler.cache;

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationContext;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mockStatic;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEFAULT_DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD;

/**
 * Unit tests for DeviceRegistrationRequestCacheEntry.
 */
public class DeviceRegistrationRequestCacheEntryTest {

    private static final String TEST_CHALLENGE = "test-challenge";
    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_TENANT_DOMAIN = "test.com";
    private static final long DEFAULT_VALIDITY_NANOS =
            TimeUnit.SECONDS.toNanos(DEFAULT_DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD);

    private MockedStatic<IdentityUtil> mockedIdentityUtil;
    private MockedStatic<FrameworkUtils> mockedFrameworkUtils;

    @BeforeMethod
    public void setUp() {

        mockedIdentityUtil = mockStatic(IdentityUtil.class);
        mockedFrameworkUtils = mockStatic(FrameworkUtils.class);
    }

    @AfterMethod
    public void tearDown() {

        mockedIdentityUtil.close();
        mockedFrameworkUtils.close();
    }

    @Test
    public void testGetDeviceRegistrationContext_ReturnsContext_WhenNotExpired() {

        DeviceRegistrationContext context =
                new DeviceRegistrationContext(TEST_CHALLENGE, TEST_USERNAME, TEST_TENANT_DOMAIN, false);

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD))
                .thenReturn(null);
        mockedFrameworkUtils.when(FrameworkUtils::getCurrentStandardNano).thenCallRealMethod();

        DeviceRegistrationRequestCacheEntry entry = new DeviceRegistrationRequestCacheEntry(context);
        DeviceRegistrationContext result = entry.getDeviceRegistrationContext();

        Assert.assertNotNull(result, "Expected context to be returned when not expired.");
        Assert.assertEquals(result, context, "Expected the same context object to be returned.");
    }

    @Test
    public void testGetDeviceRegistrationContext_ReturnsNull_WhenExpired() {

        DeviceRegistrationContext context =
                new DeviceRegistrationContext(TEST_CHALLENGE, TEST_USERNAME, TEST_TENANT_DOMAIN, false);

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD))
                .thenReturn(null);
        // Constructor at time 0, getter past default validity
        long expiredQueryTime = DEFAULT_VALIDITY_NANOS + 1L;
        mockedFrameworkUtils.when(FrameworkUtils::getCurrentStandardNano).thenReturn(0L, expiredQueryTime);

        DeviceRegistrationRequestCacheEntry entry = new DeviceRegistrationRequestCacheEntry(context);
        DeviceRegistrationContext result = entry.getDeviceRegistrationContext();

        Assert.assertNull(result, "Expected null when the cache entry has expired.");
    }

    @Test
    public void testGetDeviceRegistrationContext_ReturnsContext_AtExactExpiry() {

        DeviceRegistrationContext context =
                new DeviceRegistrationContext(TEST_CHALLENGE, TEST_USERNAME, TEST_TENANT_DOMAIN, false);

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD))
                .thenReturn(null);
        mockedFrameworkUtils.when(FrameworkUtils::getCurrentStandardNano).thenReturn(0L, DEFAULT_VALIDITY_NANOS);

        DeviceRegistrationRequestCacheEntry entry = new DeviceRegistrationRequestCacheEntry(context);
        DeviceRegistrationContext result = entry.getDeviceRegistrationContext();

        Assert.assertNotNull(result,
                "Expected context at exact expiry boundary (expiry check uses strict > not >=).");
    }

    @Test
    public void testGetDeviceRegistrationContext_ReturnsNull_WhenContextIsNull() {

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD))
                .thenReturn(null);
        mockedFrameworkUtils.when(FrameworkUtils::getCurrentStandardNano).thenReturn(0L, 0L);

        DeviceRegistrationRequestCacheEntry entry = new DeviceRegistrationRequestCacheEntry(null);
        DeviceRegistrationContext result = entry.getDeviceRegistrationContext();

        Assert.assertNull(result, "Expected null when the stored context itself is null.");
    }

    @Test
    public void testValidityPeriod_UsesDefault_WhenConfigIsNull() {

        DeviceRegistrationContext context =
                new DeviceRegistrationContext(TEST_CHALLENGE, TEST_USERNAME, TEST_TENANT_DOMAIN, false);

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD))
                .thenReturn(null);
        // Constructor: 0L | 1st getter: at exact boundary (still valid) | 2nd getter: past boundary (expired)
        mockedFrameworkUtils.when(FrameworkUtils::getCurrentStandardNano)
                .thenReturn(0L, DEFAULT_VALIDITY_NANOS, DEFAULT_VALIDITY_NANOS + 1L);

        DeviceRegistrationRequestCacheEntry entry = new DeviceRegistrationRequestCacheEntry(context);

        Assert.assertNotNull(entry.getDeviceRegistrationContext(),
                "Context should be valid at the exact default expiry boundary.");
        Assert.assertNull(entry.getDeviceRegistrationContext(),
                "Context should be null after the default 180s validity period.");
    }

    @Test
    public void testValidityPeriod_UsesDefault_WhenConfigIsBlank() {

        DeviceRegistrationContext context =
                new DeviceRegistrationContext(TEST_CHALLENGE, TEST_USERNAME, TEST_TENANT_DOMAIN, false);

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD))
                .thenReturn("   ");
        // Constructor: 0L | 1st getter: at exact boundary (still valid) | 2nd getter: past boundary (expired)
        mockedFrameworkUtils.when(FrameworkUtils::getCurrentStandardNano)
                .thenReturn(0L, DEFAULT_VALIDITY_NANOS, DEFAULT_VALIDITY_NANOS + 1L);

        DeviceRegistrationRequestCacheEntry entry = new DeviceRegistrationRequestCacheEntry(context);

        Assert.assertNotNull(entry.getDeviceRegistrationContext(),
                "Context should be valid at the exact default expiry boundary when config is blank.");
        Assert.assertNull(entry.getDeviceRegistrationContext(),
                "Context should be null after default validity when config is blank.");
    }

    @Test
    public void testValidityPeriod_UsesCustomValue_WhenConfigIsValid() {

        DeviceRegistrationContext context =
                new DeviceRegistrationContext(TEST_CHALLENGE, TEST_USERNAME, TEST_TENANT_DOMAIN, false);
        int customSeconds = 300;
        long customValidityNanos = TimeUnit.SECONDS.toNanos(customSeconds);

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD))
                .thenReturn(String.valueOf(customSeconds));
        // Constructor: 0L
        // 1st getter: past default 180s but within custom 300s (still valid)
        // 2nd getter: past custom 300s (expired)
        mockedFrameworkUtils.when(FrameworkUtils::getCurrentStandardNano)
                .thenReturn(0L, DEFAULT_VALIDITY_NANOS + 1L, customValidityNanos + 1L);

        DeviceRegistrationRequestCacheEntry entry = new DeviceRegistrationRequestCacheEntry(context);

        Assert.assertNotNull(entry.getDeviceRegistrationContext(),
                "Context should be valid past default 180s when custom 300s period is configured.");
        Assert.assertNull(entry.getDeviceRegistrationContext(),
                "Context should be null after the custom 300s validity period.");
    }

    @Test
    public void testValidityPeriod_UsesDefault_WhenConfigIsInvalid() {

        DeviceRegistrationContext context =
                new DeviceRegistrationContext(TEST_CHALLENGE, TEST_USERNAME, TEST_TENANT_DOMAIN, false);

        mockedIdentityUtil.when(() -> IdentityUtil.getProperty(DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD))
                .thenReturn("notAnInt");
        mockedFrameworkUtils.when(FrameworkUtils::getCurrentStandardNano).thenCallRealMethod();

        DeviceRegistrationRequestCacheEntry entry = new DeviceRegistrationRequestCacheEntry(context);

        Assert.assertNotNull(entry.getDeviceRegistrationContext(),
                "Context should be valid at exact default expiry when config value causes NumberFormatException.");
        Assert.assertEquals(entry.getValidityPeriod(), DEFAULT_VALIDITY_NANOS);
    }
}
