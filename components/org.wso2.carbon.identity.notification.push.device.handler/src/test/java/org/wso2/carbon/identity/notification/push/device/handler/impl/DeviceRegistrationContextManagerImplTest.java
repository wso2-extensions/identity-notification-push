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

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.notification.push.device.handler.cache.DeviceRegistrationRequestCache;
import org.wso2.carbon.identity.notification.push.device.handler.cache.DeviceRegistrationRequestCacheEntry;
import org.wso2.carbon.identity.notification.push.device.handler.cache.DeviceRegistrationRequestCacheKey;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DeviceRegistrationContextManagerImpl.
 */
public class DeviceRegistrationContextManagerImplTest {

    private DeviceRegistrationContextManagerImpl deviceRegistrationContextManager;

    @BeforeMethod
    public void setUp() {

        deviceRegistrationContextManager = new DeviceRegistrationContextManagerImpl();
    }

    @Test
    public void testStoreRegistrationContext() {

        DeviceRegistrationContext context = new DeviceRegistrationContext("id", "user", "tenant", false);
        String key = "testKey";
        String tenantDomain = "carbon.super";

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

        DeviceRegistrationContext context = new DeviceRegistrationContext("id", "user", "tenant", false);
        String key = "testKey";
        String tenantDomain = "carbon.super";

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

        String key = "testKey";
        String tenantDomain = "carbon.super";

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

        String key = "testKey";
        String tenantDomain = "carbon.super";

        try (MockedStatic<DeviceRegistrationRequestCache> mockedCache =
                     Mockito.mockStatic(DeviceRegistrationRequestCache.class);
            MockedStatic<SessionDataStore> mockedSessionDataStore = Mockito.mockStatic(SessionDataStore.class)
        ) {

            DeviceRegistrationRequestCache cache = Mockito.mock(DeviceRegistrationRequestCache.class);
            mockedCache.when(DeviceRegistrationRequestCache::getInstance).thenReturn(cache);

            SessionDataStore sessionDataStore = Mockito.mock(SessionDataStore.class);
            mockedSessionDataStore.when(SessionDataStore::getInstance).thenReturn(sessionDataStore);
            doNothing().when(sessionDataStore).clearSessionData(key, "DEVICE_REGISTRATION_REQUEST_CACHE");

            deviceRegistrationContextManager.clearContext(key, tenantDomain);

            Mockito.verify(cache).clearCacheEntry(new DeviceRegistrationRequestCacheKey(key), tenantDomain);
        }
    }
}
