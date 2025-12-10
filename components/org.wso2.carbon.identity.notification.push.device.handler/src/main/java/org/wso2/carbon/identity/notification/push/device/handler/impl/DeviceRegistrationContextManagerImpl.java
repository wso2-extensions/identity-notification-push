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

import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.core.util.IdentityCacheUtil;
import org.wso2.carbon.identity.notification.push.device.handler.DeviceRegistrationContextManager;
import org.wso2.carbon.identity.notification.push.device.handler.cache.DeviceRegistrationRequestCache;
import org.wso2.carbon.identity.notification.push.device.handler.cache.DeviceRegistrationRequestCacheEntry;
import org.wso2.carbon.identity.notification.push.device.handler.cache.DeviceRegistrationRequestCacheKey;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationContext;

import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEVICE_REGISTRATION_REQUEST_CACHE;

/**
 * Device registration cache manager implementation.
 */
public class DeviceRegistrationContextManagerImpl implements DeviceRegistrationContextManager {

    @Override
    public void storeRegistrationContext(String key, DeviceRegistrationContext context, String tenantDomain) {

        DeviceRegistrationRequestCache.getInstance().addToCache(
                new DeviceRegistrationRequestCacheKey(key),
                new DeviceRegistrationRequestCacheEntry(context),
                tenantDomain);
        storeToSessionStore(key, new DeviceRegistrationRequestCacheEntry(context));
    }

    @Override
    public DeviceRegistrationContext getContext(String key, String tenantDomain) {

        DeviceRegistrationRequestCacheEntry cacheEntry = DeviceRegistrationRequestCache.getInstance().getValueFromCache(
                new DeviceRegistrationRequestCacheKey(key), tenantDomain);
        if (cacheEntry != null) {
            return cacheEntry.getDeviceRegistrationContext();
        } else {
            cacheEntry = getFromSessionStore(key);
            if (IdentityCacheUtil.isCacheEntryExpired(cacheEntry)) {
                return cacheEntry.getDeviceRegistrationContext();
            }
        }
        return null;
    }

    @Override
    public void clearContext(String key, String tenantDomain) {

        DeviceRegistrationRequestCache.getInstance().clearCacheEntry(
                new DeviceRegistrationRequestCacheKey(key), tenantDomain);
        clearFromSessionStore(key);
    }

    /**
     * Store device registration context in session store.
     *
     * @param id            Unique key for identifying the device registration context for the session.
     * @param entry         Device registration context cache entry.
     */
    private void storeToSessionStore(String id, DeviceRegistrationRequestCacheEntry entry) {

        SessionDataStore.getInstance().storeSessionData(id, DEVICE_REGISTRATION_REQUEST_CACHE, entry);
    }

    /**
     * Get device registration context from session store.
     *
     * @param id            Unique key for identifying the device registration context for the session.
     * @return              Device registration context cache entry.
     */
    private DeviceRegistrationRequestCacheEntry getFromSessionStore(String id) {

        return (DeviceRegistrationRequestCacheEntry) SessionDataStore.getInstance().getSessionData(id,
                DEVICE_REGISTRATION_REQUEST_CACHE);
    }

    /**
     * Clear device registration context from session store.
     *
     * @param id            Unique key for identifying the device registration context for the session.
     */
    private void clearFromSessionStore(String id) {

        SessionDataStore.getInstance().clearSessionData(id, DEVICE_REGISTRATION_REQUEST_CACHE);
    }
}
