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

package org.wso2.carbon.identity.notification.push.device.handler.dao;

import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerServerException;
import org.wso2.carbon.identity.notification.push.device.handler.model.Device;

import java.util.Optional;

/**
 * DeviceDAO interface.
 */
public interface DeviceDAO {

    /**
     * Register a device.
     *
     * @param device Device to be registered.
     * @param tenantId Tenant ID.
     * @throws PushDeviceHandlerServerException PushDeviceHandlerServerException.
     */
    void registerDevice(Device device, int tenantId) throws PushDeviceHandlerServerException;

    /**
     * Unregister a device.
     *
     * @param deviceId Device ID.
     * @throws PushDeviceHandlerServerException PushDeviceHandlerServerException.
     */
    void unregisterDevice(String deviceId) throws PushDeviceHandlerServerException;

    /**
     * Edit a device.
     *
     * @param deviceId Device ID.
     * @param updatedDevice Updated device.
     * @throws PushDeviceHandlerServerException PushDeviceHandlerServerException.
     */
    void editDevice(String deviceId, Device updatedDevice) throws PushDeviceHandlerServerException;

    /**
     * Get a device.
     *
     * @param deviceId Device ID.
     * @return Device.
     * @throws PushDeviceHandlerServerException PushDeviceHandlerServerException.
     */
    Optional<Device> getDevice(String deviceId) throws PushDeviceHandlerServerException;

    /**
     * Get a device by the user ID.
     *
     * @param userId User ID.
     * @return Device.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    Optional<Device> getDeviceByUserId(String userId, int tenantId) throws PushDeviceHandlerServerException;

    /**
     * Get the public key of a device.
     *
     * @param deviceId Device ID.
     * @return Public key string.
     * @throws PushDeviceHandlerServerException PushDeviceHandlerServerException.
     */
    Optional<String> getPublicKey(String deviceId) throws PushDeviceHandlerServerException;
}
