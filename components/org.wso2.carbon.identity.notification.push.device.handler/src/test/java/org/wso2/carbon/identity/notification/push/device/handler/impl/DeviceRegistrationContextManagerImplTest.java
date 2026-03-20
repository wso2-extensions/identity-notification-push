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

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.notification.push.device.handler.cache.DeviceRegistrationRequestCache;
import org.wso2.carbon.identity.notification.push.device.handler.cache.DeviceRegistrationRequestCacheEntry;
import org.wso2.carbon.identity.notification.push.device.handler.cache.DeviceRegistrationRequestCacheKey;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationContext;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEFAULT_DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD;

/**
 * Unit tests for DeviceRegistrationContextManagerImpl.
 */
public class DeviceRegistrationContextManagerImplTest {

    private static final String TEST_ID = "id";
    private static final String TEST_USER = "user";
    private static final String TEST_TENANT = "tenant";
    private static final String TEST_KEY = "testKey";
    private static final String TEST_TENANT_DOMAIN = "carbon.super";
    private static final String DEVICE_REGISTRATION_REQUEST_CACHE = "DEVICE_REGISTRATION_REQUEST_CACHE";
    private static final long DEFAULT_VALIDITY_NANOS =
            TimeUnit.SECONDS.toNanos(DEFAULT_DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD);


    private DeviceRegistrationContextManagerImpl deviceRegistrationContextManager;

    @BeforeMethod
    public void setUp() {

        deviceRegistrationContextManager = new DeviceRegistrationContextManagerImpl();
    }

    @Test
    public void testStoreRegistrationContext() {

        DeviceRegistrationContext context = new DeviceRegistrationContext(TEST_ID, TEST_USER, TEST_TENANT, false);
        String key = TEST_KEY;
        String tenantDomain = TEST_TENANT_DOMAIN;

        try (MockedStatic<DeviceRegistrationRequestCache> mockedCache
                     = Mockito.mockStatic(DeviceRegistrationRequestCache.class);
             MockedStatic<SessionDataStore> mockedSessionDataStore = Mockito.mockStatic(SessionDataStore.class)
        ) {
            DeviceRegistrationRequestCache cache = Mockito.mock(DeviceRegistrationRequestCache.class);
            mockedCache.when(DeviceRegistrationRequestCache::getInstance).thenReturn(cache);

            SessionDataStore sessionDataStore = Mockito.mock(SessionDataStore.class);
            mockedSessionDataStore.when(SessionDataStore::getInstance).thenReturn(sessionDataStore);
            doNothing().when(sessionDataStore).storeSessionData(any(), any(), any());

            deviceRegistrationContextManager.storeRegistrationContext(key, context, tenantDomain);

            ArgumentCaptor<DeviceRegistrationRequestCacheKey> keyCaptor
                    = ArgumentCaptor.forClass(DeviceRegistrationRequestCacheKey.class);
            ArgumentCaptor<DeviceRegistrationRequestCacheEntry> entryCaptor
                    = ArgumentCaptor.forClass(DeviceRegistrationRequestCacheEntry.class);
            ArgumentCaptor<String> tenantCaptor = ArgumentCaptor.forClass(String.class);

            // Verify the addToCache invocation
            Mockito.verify(cache).addToCache(keyCaptor.capture(), entryCaptor.capture(), tenantCaptor.capture());

            // Validate the captured arguments
            Assert.assertEquals(keyCaptor.getValue().getDeviceId(), key, "Cache key does not match.");
            Assert.assertEquals(entryCaptor.getValue().getDeviceRegistrationContext(), context,
                    "Cache entry does not match.");
            Assert.assertEquals(tenantCaptor.getValue(), tenantDomain, "Tenant domain does not match.");
        }
    }

    @Test
    public void testGetContext() {

        DeviceRegistrationContext context = new DeviceRegistrationContext(TEST_ID, TEST_USER, TEST_TENANT, false);
        String key = TEST_KEY;
        String tenantDomain = TEST_TENANT_DOMAIN;

        try (MockedStatic<DeviceRegistrationRequestCache> mockedCache
                     = Mockito.mockStatic(DeviceRegistrationRequestCache.class);
             MockedStatic<SessionDataStore> mockedSessionDataStore = Mockito.mockStatic(SessionDataStore.class)
        ) {
            DeviceRegistrationRequestCache cache = Mockito.mock(DeviceRegistrationRequestCache.class);
            mockedCache.when(DeviceRegistrationRequestCache::getInstance).thenReturn(cache);
            Mockito.when(cache.getValueFromCache(new DeviceRegistrationRequestCacheKey(key), tenantDomain))
                    .thenReturn(new DeviceRegistrationRequestCacheEntry(context));

            SessionDataStore sessionDataStore = Mockito.mock(SessionDataStore.class);
            mockedSessionDataStore.when(SessionDataStore::getInstance).thenReturn(sessionDataStore);
            DeviceRegistrationRequestCacheEntry entry = new DeviceRegistrationRequestCacheEntry(null);
            when(sessionDataStore.getSessionData(anyString(), anyString())).thenReturn(entry);

            DeviceRegistrationContext result = deviceRegistrationContextManager.getContext(key, tenantDomain);

            Assert.assertNotNull(result);
            Assert.assertEquals(result, context);
        }
    }

    @Test
    public void testGetContextWithNull() {

        String key = TEST_KEY;
        String tenantDomain = TEST_TENANT_DOMAIN;

        try (MockedStatic<DeviceRegistrationRequestCache> mockedCache
                     = Mockito.mockStatic(DeviceRegistrationRequestCache.class);
             MockedStatic<SessionDataStore> mockedSessionDataStore = Mockito.mockStatic(SessionDataStore.class)
        ) {
            DeviceRegistrationRequestCache cache = Mockito.mock(DeviceRegistrationRequestCache.class);
            mockedCache.when(DeviceRegistrationRequestCache::getInstance).thenReturn(cache);
            Mockito.when(cache.getValueFromCache(new DeviceRegistrationRequestCacheKey(key), tenantDomain))
                    .thenReturn(null);

            SessionDataStore sessionDataStore = Mockito.mock(SessionDataStore.class);
            mockedSessionDataStore.when(SessionDataStore::getInstance).thenReturn(sessionDataStore);
            DeviceRegistrationRequestCacheEntry entry = new DeviceRegistrationRequestCacheEntry(null);
            when(sessionDataStore.getSessionData(anyString(), anyString())).thenReturn(entry);

            DeviceRegistrationContext result = deviceRegistrationContextManager.getContext(key, tenantDomain);

            Assert.assertNull(result);
        }
    }

    @Test
    public void testClearContext() {

        String key = TEST_KEY;
        String tenantDomain = TEST_TENANT_DOMAIN;

        try (MockedStatic<DeviceRegistrationRequestCache> mockedCache =
                     Mockito.mockStatic(DeviceRegistrationRequestCache.class);
             MockedStatic<SessionDataStore> mockedSessionDataStore = Mockito.mockStatic(SessionDataStore.class)
        ) {

            DeviceRegistrationRequestCache cache = Mockito.mock(DeviceRegistrationRequestCache.class);
            mockedCache.when(DeviceRegistrationRequestCache::getInstance).thenReturn(cache);

            SessionDataStore sessionDataStore = Mockito.mock(SessionDataStore.class);
            mockedSessionDataStore.when(SessionDataStore::getInstance).thenReturn(sessionDataStore);
            doNothing().when(sessionDataStore).clearSessionData(key, DEVICE_REGISTRATION_REQUEST_CACHE);

            deviceRegistrationContextManager.clearContext(key, tenantDomain);

            Mockito.verify(cache).clearCacheEntry(new DeviceRegistrationRequestCacheKey(key), tenantDomain);
        }
    }

    /**
     * Test getContext returns null when both the primary cache and the session store miss (return null).
     */
    @Test
    public void testGetContext_ReturnNullWhenBothCacheAndSessionStoreMiss() {

        try (MockedStatic<DeviceRegistrationRequestCache> mockedCache =
                     Mockito.mockStatic(DeviceRegistrationRequestCache.class);
             MockedStatic<SessionDataStore> mockedSessionDataStore = Mockito.mockStatic(SessionDataStore.class)
        ) {
            DeviceRegistrationRequestCache cache = Mockito.mock(DeviceRegistrationRequestCache.class);
            mockedCache.when(DeviceRegistrationRequestCache::getInstance).thenReturn(cache);
            when(cache.getValueFromCache(any(DeviceRegistrationRequestCacheKey.class), anyString()))
                    .thenReturn(null);

            SessionDataStore sessionDataStore = Mockito.mock(SessionDataStore.class);
            mockedSessionDataStore.when(SessionDataStore::getInstance).thenReturn(sessionDataStore);
            when(sessionDataStore.getSessionData(anyString(), anyString())).thenReturn(null);

            DeviceRegistrationContext result = deviceRegistrationContextManager.getContext(TEST_KEY,
                    TEST_TENANT_DOMAIN);

            Assert.assertNull(result, "Expected null when both cache and session store return null.");
        }
    }

    /**
     * Test getContext returns null when the primary cache misses and the session store entry is expired.
     */
    @Test
    public void testGetContext_ReturnNullWhenSessionStoreEntryIsExpired() {

        DeviceRegistrationContext context = new DeviceRegistrationContext(TEST_ID, TEST_USER, TEST_TENANT, false);

        try (MockedStatic<DeviceRegistrationRequestCache> mockedCache =
                     Mockito.mockStatic(DeviceRegistrationRequestCache.class);
             MockedStatic<SessionDataStore> mockedSessionDataStore = Mockito.mockStatic(SessionDataStore.class);
             MockedStatic<FrameworkUtils> mockedFrameworkUtils = Mockito.mockStatic(FrameworkUtils.class);
             MockedStatic<IdentityUtil> mockedIdentityUtil = Mockito.mockStatic(IdentityUtil.class)
        ) {
            // Primary cache misses.
            DeviceRegistrationRequestCache cache = Mockito.mock(DeviceRegistrationRequestCache.class);
            mockedCache.when(DeviceRegistrationRequestCache::getInstance).thenReturn(cache);
            when(cache.getValueFromCache(any(DeviceRegistrationRequestCacheKey.class), anyString())).thenReturn(null);

            mockedIdentityUtil.when(() -> IdentityUtil.getProperty(DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD))
                    .thenReturn(null);
            // Control time: 0L for construction, past validity for getter (expired).
            mockedFrameworkUtils.when(FrameworkUtils::getCurrentStandardNano)
                    .thenReturn(0L, DEFAULT_VALIDITY_NANOS + 1L);
            DeviceRegistrationRequestCacheEntry sessionEntry = new DeviceRegistrationRequestCacheEntry(context);

            SessionDataStore sessionDataStore = Mockito.mock(SessionDataStore.class);
            mockedSessionDataStore.when(SessionDataStore::getInstance).thenReturn(sessionDataStore);
            when(sessionDataStore.getSessionData(anyString(), anyString())).thenReturn(sessionEntry);

            DeviceRegistrationContext result = deviceRegistrationContextManager.getContext(TEST_KEY,
                    TEST_TENANT_DOMAIN);

            Assert.assertNull(result, "Expected null when session store entry is expired.");
        }
    }
}
