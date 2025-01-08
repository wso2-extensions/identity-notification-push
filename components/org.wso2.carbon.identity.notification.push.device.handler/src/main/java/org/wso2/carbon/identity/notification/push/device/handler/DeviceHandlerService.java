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

package org.wso2.carbon.identity.notification.push.device.handler;

import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerException;
import org.wso2.carbon.identity.notification.push.device.handler.model.Device;
import org.wso2.carbon.identity.notification.push.device.handler.model.RegistrationDiscoveryData;
import org.wso2.carbon.identity.notification.push.device.handler.model.RegistrationRequest;

/**
 * Device Handler interface.
 */
public interface DeviceHandlerService {

    /**
     * Register a device.
     *
     * @param registrationRequest Registration request.
     * @param tenantDomain        Tenant Domain.
     * @return Registered device.
     * @throws PushDeviceHandlerException Push Device Handler Exception.
     */
    Device registerDevice(RegistrationRequest registrationRequest, String tenantDomain)
            throws PushDeviceHandlerException;

    /**
     * Unregister a device.
     *
     * @param deviceId Device ID.
     * @throws PushDeviceHandlerException Push Device Handler Exception.
     */
    void unregisterDevice(String deviceId) throws PushDeviceHandlerException;

    /**
     * Unregister a device from mobile.
     *
     * @param deviceId Device ID.
     * @param token Token.
     * @throws PushDeviceHandlerException Push Device Handler Exception.
     */
    void unregisterDeviceMobile(String deviceId, String token) throws PushDeviceHandlerException;

    /**
     * Remove all devices of a registered user.
     *
     * @param userId User ID.
     * @throws PushDeviceHandlerException Push Device Handler Exception.
     */
    void unregisterDeviceByUserId(String userId, String tenantDomain) throws PushDeviceHandlerException;

    /**
     * Get a device by the device ID.
     *
     * @param deviceId Device ID.
     * @return Device.
     * @throws PushDeviceHandlerException Push Device Handler Exception.
     */
    Device getDevice(String deviceId) throws PushDeviceHandlerException;

    /**
     * Get a device by the user ID.
     *
     * @param userId User ID.
     * @return Device.
     * @throws PushDeviceHandlerException Push Device Handler Exception.
     */
    Device getDeviceByUserId(String userId, String tenantDomain) throws PushDeviceHandlerException;

    /**
     * Edit the name of a registered device.
     *
     * @param deviceId Device ID.
     * @param path     Path.
     * @param value    Value.
     * @throws PushDeviceHandlerException Push Device Handler Exception.
     */
    void editDevice(String deviceId, String path, String value) throws PushDeviceHandlerException;

    /**
     * Get registration discovery data.
     *
     * @param username     Username.
     * @param tenantDomain Tenant Domain.
     * @return Registration discovery data.
     * @throws PushDeviceHandlerException Push Device Handler Exception.
     */
    RegistrationDiscoveryData getRegistrationDiscoveryData(String username, String tenantDomain)
            throws PushDeviceHandlerException;

    /**
     * Get the public key for a specific device from the database.
     *
     * @param deviceId Unique ID to identify the device
     * @return Public Key string
     * @throws PushDeviceHandlerException Push Device Handler Exception.
     */
    String getPublicKey(String deviceId) throws PushDeviceHandlerException;

}
