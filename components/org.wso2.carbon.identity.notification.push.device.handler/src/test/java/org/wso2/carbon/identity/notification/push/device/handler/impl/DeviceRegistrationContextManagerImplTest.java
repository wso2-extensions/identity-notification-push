package org.wso2.carbon.identity.notification.push.device.handler.impl;

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.notification.push.device.handler.cache.DeviceRegistrationRequestCache;
import org.wso2.carbon.identity.notification.push.device.handler.cache.DeviceRegistrationRequestCacheEntry;
import org.wso2.carbon.identity.notification.push.device.handler.cache.DeviceRegistrationRequestCacheKey;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationContext;

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
                     = Mockito.mockStatic(DeviceRegistrationRequestCache.class)) {
            DeviceRegistrationRequestCache cache = Mockito.mock(DeviceRegistrationRequestCache.class);
            mockedCache.when(DeviceRegistrationRequestCache::getInstance).thenReturn(cache);

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
                     = Mockito.mockStatic(DeviceRegistrationRequestCache.class)) {
            DeviceRegistrationRequestCache cache = Mockito.mock(DeviceRegistrationRequestCache.class);
            mockedCache.when(DeviceRegistrationRequestCache::getInstance).thenReturn(cache);
            Mockito.when(cache.getValueFromCache(new DeviceRegistrationRequestCacheKey(key), tenantDomain))
                    .thenReturn(new DeviceRegistrationRequestCacheEntry(context));

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
                     = Mockito.mockStatic(DeviceRegistrationRequestCache.class)) {
            DeviceRegistrationRequestCache cache = Mockito.mock(DeviceRegistrationRequestCache.class);
            mockedCache.when(DeviceRegistrationRequestCache::getInstance).thenReturn(cache);
            Mockito.when(cache.getValueFromCache(new DeviceRegistrationRequestCacheKey(key), tenantDomain))
                    .thenReturn(null);

            DeviceRegistrationContext result = deviceRegistrationContextManager.getContext(key, tenantDomain);

            Assert.assertNull(result);
        }
    }

    @Test
    public void testClearContext() {
        String key = "testKey";
        String tenantDomain = "carbon.super";

        try (MockedStatic<DeviceRegistrationRequestCache> mockedCache
                     = Mockito.mockStatic(DeviceRegistrationRequestCache.class)) {
            DeviceRegistrationRequestCache cache = Mockito.mock(DeviceRegistrationRequestCache.class);
            mockedCache.when(DeviceRegistrationRequestCache::getInstance).thenReturn(cache);

            deviceRegistrationContextManager.clearContext(key, tenantDomain);

            Mockito.verify(cache).clearCacheEntry(new DeviceRegistrationRequestCacheKey(key), tenantDomain);
        }
    }
}
