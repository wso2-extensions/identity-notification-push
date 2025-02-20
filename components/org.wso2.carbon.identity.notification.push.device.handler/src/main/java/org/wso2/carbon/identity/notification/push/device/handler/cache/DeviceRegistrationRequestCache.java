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

package org.wso2.carbon.identity.notification.push.device.handler.cache;

import org.wso2.carbon.identity.core.cache.BaseCache;
import org.wso2.carbon.utils.CarbonUtils;

import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEVICE_REGISTRATION_REQUEST_CACHE;

/**
 * Device registration request cache.
 */
public class DeviceRegistrationRequestCache extends BaseCache<DeviceRegistrationRequestCacheKey,
        DeviceRegistrationRequestCacheEntry> {

    private static volatile DeviceRegistrationRequestCache instance;

    /**
     * Private constructor which initializes the device registration request cache.
     */
    private DeviceRegistrationRequestCache() {

        super(DEVICE_REGISTRATION_REQUEST_CACHE, true);
    }

    /**
     * Get device registration request cache by type instance.
     *
     * @return Device registration request cache by type instance.
     */
    public static DeviceRegistrationRequestCache getInstance() {

        CarbonUtils.checkSecurity();
        if (instance == null) {
            synchronized (DeviceRegistrationRequestCache.class) {
                if (instance == null) {
                    instance = new DeviceRegistrationRequestCache();
                }
            }
        }
        return instance;
    }
}
