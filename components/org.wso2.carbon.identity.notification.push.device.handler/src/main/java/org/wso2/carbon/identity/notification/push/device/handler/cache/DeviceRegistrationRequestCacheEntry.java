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

package org.wso2.carbon.identity.notification.push.device.handler.cache;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.core.cache.CacheEntry;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationContext;

import java.util.concurrent.TimeUnit;

import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEFAULT_DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD;

/**
 * Cache entry for Device Registration Request.
 */
public class DeviceRegistrationRequestCacheEntry extends CacheEntry {

    private static final long serialVersionUID = -4976259795783529978L;
    private static final Log LOG = LogFactory.getLog(DeviceRegistrationRequestCacheEntry.class);

    private final DeviceRegistrationContext deviceRegistrationContext;
    private final long expiresAt;

    /**
     * Constructor for DeviceRegistrationRequestCacheEntry.
     *
     * @param deviceRegistrationContext DeviceRegistrationContext
     */
    public DeviceRegistrationRequestCacheEntry(DeviceRegistrationContext deviceRegistrationContext) {

        this.deviceRegistrationContext = deviceRegistrationContext;
        long validityPeriodInNanos = getValidityPeriodInNanos();
        this.expiresAt = getCurrentTimeInNanos() + validityPeriodInNanos;
        // This will be used in the session store to set the cache entry expiry time.
        this.setValidityPeriod(validityPeriodInNanos);
    }

    /**
     * Get the device registration context.
     *
     * @return DeviceRegistrationContext if the cache entry has not expired, null otherwise.
     */
    public DeviceRegistrationContext getDeviceRegistrationContext() {

        if (getCurrentTimeInNanos() > expiresAt) {
            LOG.debug("Push device registration request cache entry has expired.");
            return null;
        }
        return deviceRegistrationContext;
    }

    private long getValidityPeriodInNanos() {

        int expirySeconds = DEFAULT_DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD;
        String configuredExpiry = IdentityUtil.getProperty(DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD);
        if (StringUtils.isNotBlank(configuredExpiry)) {
            try {
                expirySeconds = Integer.parseInt(configuredExpiry.trim());
            } catch (NumberFormatException e) {
                LOG.warn("Invalid value configured for '" + DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD
                        + "': " + configuredExpiry + ". Using default value: " + expirySeconds + " seconds.", e);
            }
        }

        return TimeUnit.SECONDS.toNanos(expirySeconds);
    }

    private long getCurrentTimeInNanos() {

        return FrameworkUtils.getCurrentStandardNano();
    }
}
