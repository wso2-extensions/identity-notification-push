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

package org.wso2.carbon.identity.notification.push.provider;

import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.model.PushDeviceData;
import org.wso2.carbon.identity.notification.push.provider.model.PushNotificationData;
import org.wso2.carbon.identity.notification.push.provider.model.PushSenderData;

import java.util.Map;

/**
 * Functions such as sending push notifications, registering devices, unregistering devices, updating devices, and
 * managing secrets are defined in this interface. Different push providers can implement this interface to provide
 * their own implementations.
 */
public interface PushProvider {

    /**
     * Returns the unique name of the provider. This name will be used to identify the provider and send the necessary
     * data using the provider metadata.
     *
     * @return Name of the provider.
     */
    String getName();

    /**
     * Sends a push notification to the device using the provided data.
     *
     * @param pushNotificationData Data required to send the push notification.
     * @param pushSenderData        Sender data required to send the push notification.
     * @param tenantDomain         Tenant domain of the user.
     * @throws PushProviderException If an error occurs while sending the push notification.
     */
    void sendNotification(PushNotificationData pushNotificationData, PushSenderData pushSenderData, String tenantDomain)
            throws PushProviderException;

    /**
     * Register the device.
     *
     * @param device    Device data.
     * @param pushSenderData Push sender data.
     * @throws PushProviderException If an error occurs while registering the device.
     */
    void registerDevice(PushDeviceData device, PushSenderData pushSenderData) throws PushProviderException;

    /**
     * Unregister the device.
     *
     * @param device     Device data.
     * @param pushSenderData Push sender data.
     * @throws PushProviderException If an error occurs while unregistering the device.
     */
    void unregisterDevice(PushDeviceData device, PushSenderData pushSenderData) throws PushProviderException;

    /**
     * Update the device details.
     *
     * @param device     Device data.
     * @param pushSenderData Push sender data.
     * @throws PushProviderException If an error occurs while updating the device.
     */
    void updateDevice(PushDeviceData device, PushSenderData pushSenderData) throws PushProviderException;

    /**
     * Actions to be performed on the properties of the specific provider.
     *
     * @param pushSenderData Push sender data.
     * @return Processed secret properties.
     * @throws PushProviderException If an error occurs while processing the secret properties.
     */
    Map<String, String> preProcessProperties(PushSenderData pushSenderData) throws PushProviderException;

    /**
     * Actions to be performed on the properties of the specific provider.
     *
     * @param pushSenderData Push sender data.
     * @return Processed secret properties.
     * @throws PushProviderException If an error occurs while processing the secret properties.
     */
    Map<String, String> postProcessProperties(PushSenderData pushSenderData) throws PushProviderException;

    /**
     * Actions to be performed when updating the credentials of the provider.
     *
     * @param pushSenderData Push sender data.
     * @throws PushProviderException If an error occurs while updating the credentials.
     */
    void updateCredentials(PushSenderData pushSenderData, String tenantDomain) throws PushProviderException;

    /**
     * Store the secrets in the secret store.
     *
     * @param pushSenderData Push sender data.
     * @return Secret properties stored in the secret store.
     * @throws PushProviderException If an error occurs while storing the secrets.
     */
    Map<String, String> storePushProviderSecretProperties(PushSenderData pushSenderData) throws PushProviderException;

    /**
     * Retrieve the secrets from the secret store.
     *
     * @param pushSenderData Push sender data.
     * @return Secret properties retrieved from the secret store.
     * @throws PushProviderException If an error occurs while retrieving the secrets.
     */
    Map<String, String> retrievePushProviderSecretProperties(PushSenderData pushSenderData)
            throws PushProviderException;

    /**
     * Delete the secrets from the secret store.
     *
     * @param pushSenderData Push sender data.
     * @throws PushProviderException If an error occurs while deleting the secrets.
     */
    void deletePushProviderSecretProperties(PushSenderData pushSenderData) throws PushProviderException;
}
