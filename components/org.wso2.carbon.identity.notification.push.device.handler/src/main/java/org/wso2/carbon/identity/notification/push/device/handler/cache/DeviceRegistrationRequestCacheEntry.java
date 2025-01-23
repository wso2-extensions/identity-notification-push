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

import org.wso2.carbon.identity.core.cache.CacheEntry;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationContext;

/**
 * Cache entry for Device Registration Request.
 */
public class DeviceRegistrationRequestCacheEntry extends CacheEntry {

    private static final long serialVersionUID = -4976259795783529978L;
    private final DeviceRegistrationContext deviceRegistrationContext;

    /**
     * Constructor for DeviceRegistrationRequestCacheEntry.
     *
     * @param deviceRegistrationContext DeviceRegistrationContext
     */
    public DeviceRegistrationRequestCacheEntry(DeviceRegistrationContext deviceRegistrationContext) {

        this.deviceRegistrationContext = deviceRegistrationContext;
    }

    /**
     * Get the device registration context.
     *
     * @return DeviceRegistrationContext
     */
    public DeviceRegistrationContext getDeviceRegistrationContext() {

        return deviceRegistrationContext;
    }
}
