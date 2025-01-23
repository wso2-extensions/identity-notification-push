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
import org.wso2.carbon.identity.notification.push.device.handler.dao.DeviceDAO;
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
import org.wso2.carbon.identity.notification.push.provider.model.PushSenderData;
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
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.SIGNATURE_ALGORITHM;

/**
 * Device handler service implementation.
 */
public class DeviceHandlerServiceImpl implements DeviceHandlerService {

    private static final Log LOG = LogFactory.getLog(DeviceHandlerServiceImpl.class);
    private DeviceDAO deviceDAO;
    private DeviceRegistrationContextManager deviceRegistrationContextManager;
    private static final String DEFAULT_PUSH_SENDER_NAME = "PushPublisher";

    /**
     * Constructor of DeviceHandlerServiceImpl.
     *
     * @param deviceRegistrationContextManager Device registration context manager.
     */
    public DeviceHandlerServiceImpl(DeviceRegistrationContextManager deviceRegistrationContextManager,
                                    DeviceDAO deviceDAO) {

        this.deviceRegistrationContextManager = deviceRegistrationContextManager;
        this.deviceDAO = deviceDAO;
    }

    @Override
    public Device registerDevice(RegistrationRequest registrationRequest, String tenantDomain)
            throws PushDeviceHandlerException {

        String deviceId = registrationRequest.getDeviceId();
        DeviceRegistrationContext context = deviceRegistrationContextManager.getContext(deviceId, tenantDomain);
        if (context == null) {
            throw new PushDeviceHandlerClientException(ERROR_CODE_REGISTRATION_CONTEXT_NOT_FOUND.getCode(),
                    String.format(ERROR_CODE_REGISTRATION_CONTEXT_NOT_FOUND.getMessage(), deviceId));
        }

        Device device;
        if (!context.isRegistered()) {
            handleSignatureVerification(registrationRequest, context);
            device = handleDeviceRegistration(registrationRequest, context);
            if (context.isRegistered()) {
                deviceRegistrationContextManager.clearContext(registrationRequest.getDeviceId(), tenantDomain);
            } else {
                throw new PushDeviceHandlerClientException(ERROR_CODE_DEVICE_REGISTRATION_FAILED.getCode(),
                        String.format(ERROR_CODE_DEVICE_REGISTRATION_FAILED.getMessage(), deviceId));
            }
        } else {
            deviceRegistrationContextManager.clearContext(registrationRequest.getDeviceId(), tenantDomain);
            throw new PushDeviceHandlerClientException(ERROR_CODE_REGISTRATION_CONTEXT_ALREADY_USED.getCode(),
                    String.format(ERROR_CODE_REGISTRATION_CONTEXT_ALREADY_USED.getMessage(), deviceId));
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
            throw new PushDeviceHandlerClientException(ERROR_CODE_DEVICE_NOT_FOUND.getCode(),
                    String.format(ERROR_CODE_DEVICE_NOT_FOUND.getMessage(), deviceId));
        }
    }

    @Override
    public void unregisterDeviceMobile(String deviceId, String token) throws PushDeviceHandlerException {

        Device device;
        Optional<Device> devices = deviceDAO.getDevice(deviceId);
        if (devices.isPresent()) {
            device = devices.get();
        } else {
            throw new PushDeviceHandlerClientException(ERROR_CODE_DEVICE_NOT_FOUND.getCode(),
                    String.format(ERROR_CODE_DEVICE_NOT_FOUND.getMessage(), deviceId));
        }
        try {
            PushChallengeValidator.getValidatedClaimSet(token, device.getPublicKey());
        } catch (PushTokenValidationException e) {
            throw new PushDeviceHandlerClientException(ERROR_CODE_TOKEN_CLAIM_VERIFICATION_FAILED.getCode(),
                    String.format(ERROR_CODE_TOKEN_CLAIM_VERIFICATION_FAILED.getMessage(), deviceId), e);
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
            throw new PushDeviceHandlerClientException(ERROR_CODE_DEVICE_NOT_FOUND_FOR_USER_ID.getCode(),
                    String.format(ERROR_CODE_DEVICE_NOT_FOUND_FOR_USER_ID.getMessage(), userId));
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
            throw new PushDeviceHandlerClientException(ERROR_CODE_DEVICE_NOT_FOUND_FOR_USER_ID.getCode(),
                    String.format(ERROR_CODE_DEVICE_NOT_FOUND_FOR_USER_ID.getMessage(), userId));
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

        // Set organization ID and organization name if the user is associated with an organization.
        try {
            resolveTenantAndOrganizationInfo(registrationDiscoveryData, tenantDomain);
        } catch (OrganizationManagementException e) {
            throw new PushDeviceHandlerServerException("Error occurred while resolving the tenant and organization " +
                    "information.", e);
        }

        // Set up the host.
        String host = IdentityUtil.getServerURL(null, false, false);
        registrationDiscoveryData.setHost(host);

        // Store to cache.
        DeviceRegistrationContext deviceRegistrationContext = new DeviceRegistrationContext(
                challenge, username, tenantDomain, false);
        deviceRegistrationContextManager.storeRegistrationContext(deviceId, deviceRegistrationContext, tenantDomain);

        return registrationDiscoveryData;
    }

    @Override
    public String getPublicKey(String deviceId) throws PushDeviceHandlerException {

        Optional<String> publicKey = deviceDAO.getPublicKey(deviceId);
        if (publicKey.isPresent()) {
            return publicKey.get();
        } else {
            throw new PushDeviceHandlerClientException(ERROR_CODE_PUBLIC_KEY_NOT_FOUND.getCode(),
                    String.format(ERROR_CODE_PUBLIC_KEY_NOT_FOUND.getMessage(), deviceId));
        }
    }

    /**
     * Resolve the tenant and organization information.
     *
     * @param registrationDiscoveryData Registration discovery data.
     * @param domainIdentifier          Tenant domain or Org Id of the user.
     * @throws OrganizationManagementException Organization Management Exception.
     */
    private void resolveTenantAndOrganizationInfo(RegistrationDiscoveryData registrationDiscoveryData,
                                                  String domainIdentifier) throws OrganizationManagementException {

        if (OrganizationManagementUtil.isOrganization(domainIdentifier)) {
            OrganizationManager organizationManager = PushDeviceHandlerDataHolder.getInstance()
                    .getOrganizationManager();
            String orgId = organizationManager.resolveOrganizationId(domainIdentifier);
            String organizationName = organizationManager.getOrganizationNameById(orgId);
            String primaryOrgId = organizationManager.getPrimaryOrganizationId(orgId);
            String primaryTenantDomain = organizationManager.resolveTenantDomain(primaryOrgId);

            registrationDiscoveryData.setTenantDomain(primaryTenantDomain);
            registrationDiscoveryData.setOrganizationId(orgId);
            registrationDiscoveryData.setOrganizationName(organizationName);
        } else {
            registrationDiscoveryData.setTenantDomain(domainIdentifier);
        }
    }

    /**
     * Handle the signature verification and exceptions.
     *
     * @param registrationRequest Registration request.
     * @param context             Device registration context.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    private void handleSignatureVerification(RegistrationRequest registrationRequest, DeviceRegistrationContext context)
            throws PushDeviceHandlerServerException, PushDeviceHandlerClientException {

        try {
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
            boolean isSignatureVerified = sign.verify(signatureBytes);

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
     * Handle the device registration and exceptions.
     *
     * @param registrationRequest Registration request.
     * @param context             Device registration context.
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

        try {
            Device existingDevice = getDeviceByUserId(userId, tenantDomain);
            if (existingDevice != null) {
                String errorMessage = String.format(ERROR_CODE_DEVICE_ALREADY_REGISTERED.toString(),
                        registrationRequest.getDeviceId());
                throw new PushDeviceHandlerClientException(
                        ERROR_CODE_DEVICE_ALREADY_REGISTERED.getCode(), errorMessage);
            }
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
                null, registrationRequest.getPublicKey()
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
            PushSenderDTO pushSender = PushDeviceHandlerDataHolder.getInstance()
                    .getNotificationSenderManagementService().getPushSender(DEFAULT_PUSH_SENDER_NAME, true);
            String pushProviderName = pushSender.getProvider();
            PushProvider pushProvider = PushDeviceHandlerDataHolder.getInstance().getPushProvider(pushProviderName);
            pushProvider.registerDevice(pushDeviceData, buildPushSenderData(pushSender));
            device.setProvider(pushProviderName);
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
            PushSenderDTO pushSender = PushDeviceHandlerDataHolder.getInstance()
                    .getNotificationSenderManagementService().getPushSender(DEFAULT_PUSH_SENDER_NAME, true);
            if (deviceProviderType.equals(pushSender.getProvider())) {
                PushProvider pushProvider = PushDeviceHandlerDataHolder.getInstance()
                        .getPushProvider(deviceProviderType);
                pushProvider.unregisterDevice(pushDeviceData, buildPushSenderData(pushSender));
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
            PushSenderDTO pushSender = PushDeviceHandlerDataHolder.getInstance()
                    .getNotificationSenderManagementService().getPushSender(DEFAULT_PUSH_SENDER_NAME, true);
            if (deviceProviderType.equals(pushSender.getProvider())) {
                PushProvider pushProvider = PushDeviceHandlerDataHolder.getInstance()
                        .getPushProvider(deviceProviderType);
                pushProvider.updateDevice(pushDeviceData, buildPushSenderData(pushSender));
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
     * Build the push sender data from the push sender DTO.
     *
     * @param pushSenderDTO Push sender DTO.
     * @return Push sender data.
     */
    public static PushSenderData buildPushSenderData(PushSenderDTO pushSenderDTO) {

        PushSenderData pushSenderData = new PushSenderData();
        pushSenderData.setName(pushSenderDTO.getName());
        pushSenderData.setProvider(pushSenderDTO.getProvider());
        pushSenderData.setProperties(pushSenderDTO.getProperties());
        pushSenderData.setProviderId(pushSenderDTO.getProviderId());
        return pushSenderData;
    }
}
