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

import org.wso2.carbon.identity.core.cache.CacheKey;

/**
 * Cache key for device registration request cache.
 */
public class DeviceRegistrationRequestCacheKey extends CacheKey {

    private static final long serialVersionUID = -2422942978801837265L;
    private final String deviceId;

    /**
     * Constructor for DeviceRegistrationRequestCacheKey.
     *
     * @param deviceId Device ID
     */
    public DeviceRegistrationRequestCacheKey(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Get the device ID.
     *
     * @return Device ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof DeviceRegistrationRequestCacheKey)) {
            return false;
        }
        return this.deviceId.equals(((DeviceRegistrationRequestCacheKey) o).getDeviceId());
    }

    @Override
    public int hashCode() {

        return deviceId.hashCode();
    }

}
