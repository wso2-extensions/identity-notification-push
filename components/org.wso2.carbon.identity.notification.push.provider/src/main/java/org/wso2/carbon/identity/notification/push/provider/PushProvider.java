package org.wso2.carbon.identity.notification.push.provider;

import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.model.PushDeviceData;
import org.wso2.carbon.identity.notification.push.provider.model.PushNotificationData;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.PushSenderDTO;

import java.util.Map;

/**
 * This interface represents a push notification provider.
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
     * @param pushSenderDTO        Sender data required to send the push notification.
     * @param tenantDomain         Tenant domain of the user.
     * @throws PushProviderException If an error occurs while sending the push notification.
     */
    void sendNotification(PushNotificationData pushNotificationData, PushSenderDTO pushSenderDTO, String tenantDomain)
            throws PushProviderException;

    /**
     * Register the device.
     *
     * @param device    Device data.
     * @param pushSender Push sender data.
     * @throws PushProviderException If an error occurs while registering the device.
     */
    void registerDevice(PushDeviceData device, PushSenderDTO pushSender) throws PushProviderException;

    /**
     * Unregister the device.
     *
     * @param device     Device data.
     * @param pushSender Push sender data.
     * @throws PushProviderException If an error occurs while unregistering the device.
     */
    void unregisterDevice(PushDeviceData device, PushSenderDTO pushSender) throws PushProviderException;

    /**
     * Update the device details.
     *
     * @param device     Device data.
     * @param pushSender Push sender data.
     * @throws PushProviderException If an error occurs while updating the device.
     */
    void updateDevice(PushDeviceData device, PushSenderDTO pushSender) throws PushProviderException;

    /**
     * Pre-process the secret properties before storing them in the secret store.
     *
     * @param pushSenderDTO Push sender data.
     * @return Processed secret properties.
     * @throws PushProviderException If an error occurs while processing the secret properties.
     */
    Map<String, String> preProcessProperties(PushSenderDTO pushSenderDTO) throws PushProviderException;

    /**
     * Post-process the secret properties before using them in the provider.
     *
     * @param pushSenderDTO Push sender data.
     * @return Processed secret properties.
     * @throws PushProviderException If an error occurs while processing the secret properties.
     */
    Map<String, String> postProcessProperties(PushSenderDTO pushSenderDTO) throws PushProviderException;

    /**
     * Actions to be performed when updating the credentials of the provider.
     *
     * @param pushSender Push sender data.
     * @throws PushProviderException If an error occurs while updating the credentials.
     */
    void updateCredentials(PushSenderDTO pushSender) throws PushProviderException;

    /**
     * Store the secrets in the secret store.
     *
     * @param pushSender Push sender data.
     * @return Secret properties stored in the secret store.
     * @throws PushProviderException If an error occurs while storing the secrets.
     */
    Map<String, String> storePushProviderSecretProperties(PushSenderDTO pushSender) throws PushProviderException;

    /**
     * Retrieve the secrets from the secret store.
     *
     * @param pushSender Push sender data.
     * @return Secret properties retrieved from the secret store.
     * @throws PushProviderException If an error occurs while retrieving the secrets.
     */
    Map<String, String> retrievePushProviderSecretProperties(PushSenderDTO pushSender) throws PushProviderException;

    /**
     * Delete the secrets from the secret store.
     *
     * @param pushSender Push sender data.
     * @throws PushProviderException If an error occurs while deleting the secrets.
     */
    void deletePushProviderSecretProperties(PushSenderDTO pushSender) throws PushProviderException;
}
