/*
 * Copyright (c) 2023-2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.notification.push.device.handler.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * This class represents the push device management configuration.
 */
public class PushDeviceMgtConfigData {

    private Boolean enableMultipleDeviceEnrollment;
    private Integer maximumDeviceLimit;
    private Boolean enableDeviceRegistrationNotifications;
    private Set<DeviceRegistrationNotificationChannelEnum> deviceRegistrationNotificationChannelEnums;

    /**
     * Get whether multiple device enrollment is enabled.
     *
     * @return Whether multiple device enrollment is enabled.
     */
    public Boolean getEnableMultipleDeviceEnrollment() {

        return enableMultipleDeviceEnrollment;
    }

    /**
     * Set whether multiple device enrollment is enabled.
     *
     * @param enableMultipleDeviceEnrollment Whether multiple device enrollment
     *                                       is enabled.
     */
    public void setEnableMultipleDeviceEnrollment(Boolean enableMultipleDeviceEnrollment) {

        this.enableMultipleDeviceEnrollment = enableMultipleDeviceEnrollment;
    }

    /**
     * Get the maximum device limit.
     *
     * @return Maximum device limit.
     */
    public Integer getMaximumDeviceLimit() {

        return maximumDeviceLimit;
    }

    /**
     * Set the maximum device limit.
     *
     * @param maximumDeviceLimit Maximum device limit.
     */
    public void setMaximumDeviceLimit(Integer maximumDeviceLimit) {

        this.maximumDeviceLimit = maximumDeviceLimit;
    }

    /**
     * Get whether device registration notifications are enabled.
     *
     * @return Whether device registration notifications are enabled.
     */
    public Boolean getEnableDeviceRegistrationNotifications() {

        return enableDeviceRegistrationNotifications;
    }

    /**
     * Set whether device registration notifications are enabled.
     *
     * @param enableDeviceRegistrationNotifications Whether device registration
     *                                              notifications are enabled.
     */
    public void setEnableDeviceRegistrationNotifications(Boolean enableDeviceRegistrationNotifications) {

        this.enableDeviceRegistrationNotifications = enableDeviceRegistrationNotifications;
    }

    /**
     * Get the channels used to deliver device registration notifications.
     *
     * @return Unmodifiable view of the configured channels, or {@code null}
     *         when the channels have not been set.
     */
    public Set<DeviceRegistrationNotificationChannelEnum> getDeviceRegistrationNotificationChannels() {

        if (deviceRegistrationNotificationChannelEnums == null) {
            return null;
        }
        return Collections.unmodifiableSet(deviceRegistrationNotificationChannelEnums);
    }

    /**
     * Set the channels used to deliver device registration notifications.
     *
     * @param deviceRegistrationNotificationChannelEnums Notification channels.
     */
    public void setDeviceRegistrationNotificationChannels(
            Set<DeviceRegistrationNotificationChannelEnum> deviceRegistrationNotificationChannelEnums) {

        if (deviceRegistrationNotificationChannelEnums == null) {
            this.deviceRegistrationNotificationChannelEnums = null;
            return;
        }
        this.deviceRegistrationNotificationChannelEnums = deviceRegistrationNotificationChannelEnums.isEmpty()
                ? EnumSet.noneOf(DeviceRegistrationNotificationChannelEnum.class)
                : EnumSet.copyOf(deviceRegistrationNotificationChannelEnums);
    }
}
