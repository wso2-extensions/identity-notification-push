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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.notification.push.common.PushChallengeValidator;
import org.wso2.carbon.identity.notification.push.common.exception.PushTokenValidationException;
import org.wso2.carbon.identity.notification.push.device.handler.DeviceHandlerService;
import org.wso2.carbon.identity.notification.push.device.handler.DeviceRegistrationContextManager;
import org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants;
import org.wso2.carbon.identity.notification.push.device.handler.dao.DeviceDAO;
import org.wso2.carbon.identity.notification.push.device.handler.dao.DeviceDAOImpl;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerClientException;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerException;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerServerException;
import org.wso2.carbon.identity.notification.push.device.handler.internal.PushDeviceHandlerDataHolder;
import org.wso2.carbon.identity.notification.push.device.handler.model.Device;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationContext;
import org.wso2.carbon.identity.notification.push.device.handler.model.RegistrationDiscoveryData;
import org.wso2.carbon.identity.notification.push.device.handler.model.RegistrationRequest;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.model.PushDeviceData;
import org.wso2.carbon.identity.notification.sender.tenant.config.dto.PushSenderDTO;
import org.wso2.carbon.identity.notification.sender.tenant.config.exception.NotificationSenderManagementException;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_DEVICE_ALREADY_REGISTERED;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_DEVICE_NOT_FOUND;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_DEVICE_NOT_FOUND_FOR_USER_ID;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_DEVICE_REGISTRATION_FAILED;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_FAILED_TO_GET_USER_ID;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_INVALID_EDIT_DEVICE_SCENARIO;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_INVALID_SIGNATURE;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_PUBLIC_KEY_NOT_FOUND;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_REGISTRATION_CONTEXT_ALREADY_USED;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_REGISTRATION_CONTEXT_NOT_FOUND;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_SIGNATURE_VERIFICATION_FAILED;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_TOKEN_CLAIM_VERIFICATION_FAILED;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.HASHING_ALGORITHM;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.PushEndpointPaths.PUSH_AUTH_PATH;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.PushEndpointPaths.PUSH_DEVICE_MGT_BASE_PATH;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.PushEndpointPaths.PUSH_DEVICE_REMOVE_PATH;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.SIGNATURE_ALGORITHM;

/**
 * Device handler service implementation.
 */
@SuppressFBWarnings
public class DeviceHandlerServiceImpl implements DeviceHandlerService {

    private static final Log LOG = LogFactory.getLog(DeviceHandlerServiceImpl.class);
    private static DeviceDAO deviceDAO = new DeviceDAOImpl();
    private static DeviceRegistrationContextManager deviceRegistrationContextManager
            = new DeviceRegistrationContextManagerImpl();

    @Override
    public Device registerDevice(RegistrationRequest registrationRequest, String tenantDomain)
            throws PushDeviceHandlerException {

        String deviceId = registrationRequest.getDeviceId();
        DeviceRegistrationContext context = deviceRegistrationContextManager.getContext(deviceId, tenantDomain);
        if (context == null) {
            throw handleDeviceHandlerClientException(ERROR_CODE_REGISTRATION_CONTEXT_NOT_FOUND, deviceId, null);
        }

        Device device;
        if (!context.isRegistered()) {
            handleSignatureVerification(registrationRequest, context);
            device = handleDeviceRegistration(registrationRequest, context);
            if (context.isRegistered()) {
                deviceRegistrationContextManager.clearContext(registrationRequest.getDeviceId(), tenantDomain);
            } else {
                throw handleDeviceHandlerClientException(ERROR_CODE_DEVICE_REGISTRATION_FAILED, deviceId, null);
            }
        } else {
            deviceRegistrationContextManager.clearContext(registrationRequest.getDeviceId(), tenantDomain);
            throw handleDeviceHandlerClientException(ERROR_CODE_REGISTRATION_CONTEXT_ALREADY_USED, deviceId, null);
        }

        return device;
    }

    @Override
    public void unregisterDevice(String deviceId) throws PushDeviceHandlerException {

        Optional<Device> device = deviceDAO.getDevice(deviceId);
        if (device.isPresent()) {
            handleDeleteDeviceForProvider(device.get());
            deviceDAO.unregisterDevice(deviceId);
        } else {
            throw handleDeviceHandlerClientException(ERROR_CODE_DEVICE_NOT_FOUND, deviceId, null);
        }
    }

    @Override
    public void unregisterDeviceMobile(String deviceId, String token) throws PushDeviceHandlerException {

        Device device;
        Optional<Device> deviceOption = deviceDAO.getDevice(deviceId);
        if (deviceOption.isPresent()) {
            device = deviceOption.get();
        } else {
            throw handleDeviceHandlerClientException(ERROR_CODE_DEVICE_NOT_FOUND, deviceId, null);
        }
        try {
            PushChallengeValidator.getValidatedClaimSet(token, device.getPublicKey());
        } catch (PushTokenValidationException e) {
            throw handleDeviceHandlerClientException(ERROR_CODE_TOKEN_CLAIM_VERIFICATION_FAILED, deviceId, e);
        }
        handleDeleteDeviceForProvider(device);
        deviceDAO.unregisterDevice(deviceId);
    }

    @Override
    public void unregisterDeviceByUserId(String userId, String tenantDomain) throws PushDeviceHandlerException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        Optional<Device> device = deviceDAO.getDeviceByUserId(userId, tenantId);
        if (device.isPresent()) {
            handleDeleteDeviceForProvider(device.get());
            String deviceId = device.get().getDeviceId();
            deviceDAO.unregisterDevice(deviceId);
        } else {
            throw handleDeviceHandlerClientException(ERROR_CODE_DEVICE_NOT_FOUND_FOR_USER_ID, userId, null);
        }
    }

    @Override
    public Device getDevice(String deviceId) throws PushDeviceHandlerException {

        Optional<Device> device = deviceDAO.getDevice(deviceId);
        if (device.isPresent()) {
            return device.get();
        } else {
            String errorMessage = String.format(ERROR_CODE_DEVICE_NOT_FOUND.getMessage(), deviceId);
            throw new PushDeviceHandlerClientException(ERROR_CODE_DEVICE_NOT_FOUND.getCode(), errorMessage);
        }
    }

    @Override
    public Device getDeviceByUserId(String userId, String tenantDomain) throws PushDeviceHandlerException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        Optional<Device> device = deviceDAO.getDeviceByUserId(userId, tenantId);
        if (device.isPresent()) {
            return device.get();
        } else {
            throw handleDeviceHandlerClientException(ERROR_CODE_DEVICE_NOT_FOUND_FOR_USER_ID, userId, null);
        }
    }

    @Override
    public void editDevice(String deviceId, String path, String value) throws PushDeviceHandlerException {

        Device device = getDevice(deviceId);
        handleEditDevice(device, path, value);
    }

    @Override
    public RegistrationDiscoveryData getRegistrationDiscoveryData(String username, String tenantDomain)
            throws PushDeviceHandlerException {

        RegistrationDiscoveryData registrationDiscoveryData = new RegistrationDiscoveryData();

        registrationDiscoveryData.setUsername(MultitenantUtils.getTenantAwareUsername(username));

        // Generate device ID.
        String deviceId = UUID.randomUUID().toString();
        registrationDiscoveryData.setDeviceId(deviceId);

        // Generate registration challenge.
        String challenge = UUID.randomUUID().toString();
        registrationDiscoveryData.setChallenge(challenge);

        //Set organization ID and organization name if the user is associated with an organization.
        try {
            resolveTenantAndOrganizationInfo(registrationDiscoveryData, tenantDomain);
        } catch (OrganizationManagementException e) {
            throw new PushDeviceHandlerServerException("Error occurred while resolving the tenant and organization " +
                    "information.", e);
        }

        // Setup push notification feature related endpoints.
        setupEndpoints(registrationDiscoveryData);

        // Store to cache.
        DeviceRegistrationContext deviceRegistrationContext = new DeviceRegistrationContext(
                challenge, username, tenantDomain, false, false);
        deviceRegistrationContextManager.storeRegistrationContext(deviceId, deviceRegistrationContext, tenantDomain);

        return registrationDiscoveryData;
    }

    @Override
    public String getPublicKey(String deviceId) throws PushDeviceHandlerException {

        Optional<String> publicKey = deviceDAO.getPublicKey(deviceId);
        if (publicKey.isPresent()) {
            return publicKey.get();
        } else {
            throw handleDeviceHandlerClientException(ERROR_CODE_PUBLIC_KEY_NOT_FOUND, deviceId, null);
        }
    }

    /**
     * Resolve the tenant and organization information.
     *
     * @param registrationDiscoveryData Registration discovery data.
     * @param tenantDomain              Tenant domain.
     * @throws OrganizationManagementException Organization Management Exception.
     */
    private void resolveTenantAndOrganizationInfo(RegistrationDiscoveryData registrationDiscoveryData,
                                                  String tenantDomain) throws OrganizationManagementException {

        if (OrganizationManagementUtil.isOrganization(tenantDomain)) {
            OrganizationManager organizationManager = PushDeviceHandlerDataHolder.getInstance()
                    .getOrganizationManager();
            String orgId = organizationManager.resolveOrganizationId(tenantDomain);
            String organizationName = organizationManager.getOrganizationNameById(orgId);
            String primaryOrgId = organizationManager.getPrimaryOrganizationId(orgId);
            String primaryTenantDomain = organizationManager.resolveTenantDomain(primaryOrgId);

            registrationDiscoveryData.setTenantDomain(primaryTenantDomain);
            registrationDiscoveryData.setOrganizationId(orgId);
            registrationDiscoveryData.setOrganizationName(organizationName);
        } else {
            registrationDiscoveryData.setTenantDomain(tenantDomain);
        }
    }

    /**
     * Handle the device handler client exception.
     *
     * @param error       Error message.
     * @param stringValue String value.
     * @param cause       Throwable cause.
     * @return PushDeviceHandlerClientException.
     */
    private PushDeviceHandlerClientException handleDeviceHandlerClientException(
            PushDeviceHandlerConstants.ErrorMessages error, String stringValue, Throwable cause) {

        if (cause == null) {
            return new PushDeviceHandlerClientException(error.getCode(),
                    String.format(error.getMessage(), stringValue));
        }
        return new PushDeviceHandlerClientException(error.getCode(),
                String.format(error.getMessage(), stringValue), cause);
    }

    /**
     * Handle the signature verification and exceptions.
     *
     * @param registrationRequest Registration request.
     * @param context            Device registration context.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    private void handleSignatureVerification(RegistrationRequest registrationRequest, DeviceRegistrationContext context)
            throws PushDeviceHandlerServerException, PushDeviceHandlerClientException {

        try {
            boolean isSignatureVerified = verifySignature(registrationRequest, context);
            if (!isSignatureVerified) {
                String errorMessage = String.format(ERROR_CODE_INVALID_SIGNATURE.toString(),
                        registrationRequest.getDeviceId());
                throw new PushDeviceHandlerClientException(ERROR_CODE_INVALID_SIGNATURE.getCode(), errorMessage);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
            String errorMessage = String.format(ERROR_CODE_SIGNATURE_VERIFICATION_FAILED.toString(),
                    registrationRequest.getDeviceId());
            throw new PushDeviceHandlerServerException(errorMessage, e);
        }
    }

    /**
     * Verify the signature using the public key for the registered device.
     *
     * @param registrationRequest Registration request.
     * @param context            Device registration context.
     * @return boolean verification.
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException.
     * @throws InvalidKeySpecException InvalidKeySpecException.
     * @throws InvalidKeyException InvalidKeyException.
     * @throws SignatureException SignatureException.
     */
    private boolean verifySignature(RegistrationRequest registrationRequest, DeviceRegistrationContext context)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {

        String signature = registrationRequest.getSignature();
        String deviceToken = registrationRequest.getDeviceToken();
        String publicKey = registrationRequest.getPublicKey();

        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        Signature sign = Signature.getInstance(HASHING_ALGORITHM);
        byte[] publicKeyData = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyData);
        KeyFactory kf = KeyFactory.getInstance(SIGNATURE_ALGORITHM);
        PublicKey pubKey = kf.generatePublic(spec);
        sign.initVerify(pubKey);
        sign.update((context.getChallenge() + "." + deviceToken).getBytes(StandardCharsets.UTF_8));
        return sign.verify(signatureBytes);
    }

    /**
     * Handle the device registration and exceptions.
     *
     * @param registrationRequest Registration request.
     * @param context            Device registration context.
     * @return Device.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    private Device handleDeviceRegistration(RegistrationRequest registrationRequest,
                                            DeviceRegistrationContext context) throws PushDeviceHandlerException {

        String userId;
        String username = context.getUsername();
        String tenantDomain = context.getTenantDomain();
        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        try {
            UserRealm userRealm = IdentityTenantUtil.getRealm(tenantDomain, username);
            AbstractUserStoreManager userStoreManager = (AbstractUserStoreManager) userRealm.getUserStoreManager();
            userId = userStoreManager.getUserIDFromUserName(MultitenantUtils.getTenantAwareUsername(username));
            if (StringUtils.isBlank(userId)) {
                String errorMessage = String.format(ERROR_CODE_FAILED_TO_GET_USER_ID.toString(), username);
                throw new PushDeviceHandlerServerException(errorMessage);
            }
        } catch (UserStoreException | IdentityException e) {
            String errorMessage = String.format(ERROR_CODE_FAILED_TO_GET_USER_ID.toString(), username);
            throw new PushDeviceHandlerServerException(errorMessage, e);
        }

        /*
        * Currently, we are only allowing the user to register one device instance at a time. There won't be a scenario
        * where the user can register multiple devices.
        * When the push notification provider configuration is changed by the admin, an existing user with a registered
        * device won't be able to receive push notifications because the device token generated is bound to the
        * notification provider. So, the user has to re-register the device to receive push notifications. Hence, the
        * existing device will be removed and the new device will be registered.
        * We analyse the error response from the push notification provider and notify the user to re-register the
        * device if it is a scenario of device token expiry or token is not relevant to the provider.
        * */
        try {
            Device existingDevice = getDeviceByUserId(userId, tenantDomain);
            // If the device is already registered, we will check whether the force registration is enabled.
            if (!context.isForceRegistration()) {
                String errorMessage = String.format(ERROR_CODE_DEVICE_ALREADY_REGISTERED.toString(),
                        registrationRequest.getDeviceId());
                throw new PushDeviceHandlerClientException(ERROR_CODE_DEVICE_ALREADY_REGISTERED.getCode(),
                        errorMessage);
            }
            // If the force registration is enabled, we will remove the existing device and register the new device.
            if (LOG.isDebugEnabled()) {
                String message = String.format("Device already registered for the user: %s. Hence, removing the" +
                        "existing device", userId);
                LOG.debug(message);
            }
            unregisterDevice(existingDevice.getDeviceId());
        } catch (PushDeviceHandlerClientException e) {
            // This means there is no existing device registered for the user.
            if ((ERROR_CODE_DEVICE_NOT_FOUND_FOR_USER_ID.getCode()).equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    String message = String.format("No existing device registered for the user: %s", userId);
                    LOG.debug(message);
                }
            } else {
                throw e;
            }
        }

        /*
         * We will store the device token independent of the push notification provider.
         * Additional device identifiers specific for each provider will be stored as the device handler.
         * The provider type and device identifier will be assigned to the device object while registering to external
         * notification provider.
         */
        Device device = new Device(
                userId, registrationRequest.getDeviceId(), registrationRequest.getDeviceName(),
                registrationRequest.getDeviceModel(), registrationRequest.getDeviceToken(), null,
                null, null, registrationRequest.getPublicKey()
        );

        // Register the device with the push notification providers.
        handleDeviceRegistrationForProvider(device);

        try {
            deviceDAO.registerDevice(device, tenantId);
            context.setRegistered(true);
        } catch (PushDeviceHandlerServerException e) {
            String errorMessage = String.format(ERROR_CODE_DEVICE_REGISTRATION_FAILED.toString(),
                    registrationRequest.getDeviceId());
            throw new PushDeviceHandlerServerException(errorMessage, e);
        }

        return device;
    }

    /**
     * Handle the device registration for the push notification providers.
     *
     * @param device Device.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    private void handleDeviceRegistrationForProvider(Device device) throws PushDeviceHandlerServerException {

        try {
            PushDeviceData pushDeviceData = buildPushDeviceDataFromDevice(device);
            List<PushSenderDTO> pushSenders = PushDeviceHandlerDataHolder.getInstance()
                    .getNotificationSenderManagementService().getPushSenders(true);
            for (PushSenderDTO pushSender : pushSenders) {
                String pushProviderName = pushSender.getProvider();
                PushProvider pushProvider = PushDeviceHandlerDataHolder.getInstance().getPushProvider(pushProviderName);
                pushProvider.registerDevice(pushDeviceData, pushSender);
                device.setProvider(pushProviderName);
                device.setProviderId(pushSender.getProviderId());
            }
        } catch (NotificationSenderManagementException e) {
            throw new PushDeviceHandlerServerException(
                    "Error occurred while retrieving the push notification senders.", e);
        } catch (PushProviderException e) {
            throw new PushDeviceHandlerServerException("Error occurred while registering the device.", e);
        }
    }

    /**
     * Handle the device deletion for the push notification providers.
     *
     * @param device Device.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    private void handleDeleteDeviceForProvider(Device device) throws PushDeviceHandlerServerException {

        String deviceProviderType = device.getProvider();
        PushDeviceData pushDeviceData = buildPushDeviceDataFromDevice(device);
        try {
            List<PushSenderDTO> pushSenders = PushDeviceHandlerDataHolder.getInstance()
                    .getNotificationSenderManagementService().getPushSenders(true);
            for (PushSenderDTO pushSender : pushSenders) {
                if (deviceProviderType.equals(pushSender.getProvider())) {
                    PushProvider pushProvider = PushDeviceHandlerDataHolder.getInstance()
                            .getPushProvider(deviceProviderType);
                    pushProvider.unregisterDevice(pushDeviceData, pushSender);
                }
            }
        } catch (NotificationSenderManagementException e) {
            throw new PushDeviceHandlerServerException(
                    "Error occurred while retrieving the push notification senders.", e);
        } catch (PushProviderException e) {
            throw new PushDeviceHandlerServerException("Error occurred while unregistering the device.", e);
        }
    }

    /**
     * Handle the device edit and exceptions.
     *
     * @param device Device.
     * @param path   Path.
     * @param value  Value.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    private void handleEditDevice(Device device, String path, String value) throws PushDeviceHandlerServerException {

        switch (path) {
            case "/device-name":
                device.setDeviceName(value);
                break;
            case "/device-token":
                device.setDeviceToken(value);
                break;
            default:
                String errorMessage = String.format(ERROR_CODE_INVALID_EDIT_DEVICE_SCENARIO.toString(),
                        device.getDeviceId());
                throw new PushDeviceHandlerServerException(errorMessage);
        }
        handleUpdateDeviceForProvider(device);
        deviceDAO.editDevice(device.getDeviceId(), device);
    }

    /**
     * Handle the device update for the push notification providers.
     *
     * @param device Device.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    private void handleUpdateDeviceForProvider(Device device) throws PushDeviceHandlerServerException {

        String deviceProviderType = device.getProvider();
        PushDeviceData pushDeviceData = buildPushDeviceDataFromDevice(device);
        try {
            List<PushSenderDTO> pushSenders = PushDeviceHandlerDataHolder.getInstance()
                    .getNotificationSenderManagementService().getPushSenders(true);
            for (PushSenderDTO pushSender : pushSenders) {
                if (deviceProviderType.equals(pushSender.getProvider())) {
                    PushProvider pushProvider = PushDeviceHandlerDataHolder.getInstance()
                            .getPushProvider(deviceProviderType);
                    pushProvider.updateDevice(pushDeviceData, pushSender);
                }
            }
        } catch (NotificationSenderManagementException e) {
            throw new PushDeviceHandlerServerException(
                    "Error occurred while retrieving the push notification senders.", e);
        } catch (PushProviderException e) {
            throw new PushDeviceHandlerServerException("Error occurred while updating the device.", e);
        }
    }

    /**
     * Build the push device data from the device.
     *
     * @param device Device.
     * @return Push device data.
     */
    private PushDeviceData buildPushDeviceDataFromDevice(Device device) {

        return new PushDeviceData(device.getDeviceToken(), device.getDeviceHandle(), device.getProvider());
    }

    /**
     * Set up the push notification feature related endpoints.
     *
     * @param registrationDiscoveryData Registration discovery data.
     */
    private void setupEndpoints(RegistrationDiscoveryData registrationDiscoveryData) {

        String tenantDomain = registrationDiscoveryData.getTenantDomain();
        String organizationId = registrationDiscoveryData.getOrganizationId();
        String deviceId = registrationDiscoveryData.getDeviceId();


        String host = IdentityUtil.getServerURL(null, false, false);
        registrationDiscoveryData.setHost(host);

        String tenantPath = "/t/" + tenantDomain;
        registrationDiscoveryData.setTenantPath(tenantPath);

        if (StringUtils.isNotBlank(organizationId)) {
            String organizationPath = "/o/" + organizationId;
            registrationDiscoveryData.setOrganizationPath(organizationPath);
        }

        registrationDiscoveryData.setRegistrationEndpoint(PUSH_DEVICE_MGT_BASE_PATH);
        registrationDiscoveryData.setAuthenticationEndpoint(PUSH_AUTH_PATH);
        String removeDeviceEndpointPath = PUSH_DEVICE_MGT_BASE_PATH + "/" + deviceId + PUSH_DEVICE_REMOVE_PATH;
        registrationDiscoveryData.setRemoveDeviceEndpoint(removeDeviceEndpointPath);
    }
}
