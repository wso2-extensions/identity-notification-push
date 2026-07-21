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

package org.wso2.carbon.identity.notification.push.device.handler.impl;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.annotation.bundle.Capability;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.context.IdentityContext;
import org.wso2.carbon.identity.core.context.model.Request;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.notification.push.common.PushChallengeValidator;
import org.wso2.carbon.identity.notification.push.common.exception.PushTokenValidationException;
import org.wso2.carbon.identity.notification.push.device.handler.DeviceHandlerService;
import org.wso2.carbon.identity.notification.push.device.handler.DeviceRegistrationContextManager;
import org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants;
import org.wso2.carbon.identity.notification.push.device.handler.dao.DeviceDAO;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerClientException;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerException;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerServerException;
import org.wso2.carbon.identity.notification.push.device.handler.internal.PushDeviceHandlerDataHolder;
import org.wso2.carbon.identity.notification.push.device.handler.model.Device;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationContext;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationNotificationChannelEnum;
import org.wso2.carbon.identity.notification.push.device.handler.model.PushDeviceMgtConfigData;
import org.wso2.carbon.identity.notification.push.device.handler.model.RegistrationDiscoveryData;
import org.wso2.carbon.identity.notification.push.device.handler.model.RegistrationRequest;
import org.wso2.carbon.identity.notification.push.device.handler.model.RegistrationRequestProviderData;
import org.wso2.carbon.identity.notification.push.device.handler.utils.DeviceHandlerAuditLogger;
import org.wso2.carbon.identity.notification.push.device.handler.utils.PushDeviceConfigManager;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderClientException;
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
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.utils.DiagnosticLog;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEFAULT_MIN_DEVICE_LIMIT_PER_USER;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEFAULT_PUSH_PROVIDER;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.EmailNotificationConstants.ARBITRARY_SEND_TO;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.EmailNotificationConstants.EMAIL_TEMPLATE_TYPE;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.EmailNotificationConstants.PUSH_DEVICE_REGISTRATION_TEMPLATE;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_DEVICE_ALREADY_REGISTERED;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_DEVICE_NOT_FOUND;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_DEVICE_NOT_FOUND_FOR_USER_ID;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_DEVICE_REGISTRATION_FAILED;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_FAILED_TO_GET_USER_ID;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_FAILED_TO_RESOLVE_PUSH_PROVIDER;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_INVALID_EDIT_DEVICE_SCENARIO;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_INVALID_SIGNATURE;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_MAX_DEVICE_LIMIT_REACHED;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_PUBLIC_KEY_NOT_FOUND;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_REGISTRATION_CONTEXT_ALREADY_USED;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_REGISTRATION_CONTEXT_NOT_FOUND;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_SIGNATURE_VERIFICATION_FAILED;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_TOKEN_CLAIM_VERIFICATION_FAILED;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.HASHING_ALGORITHM;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.LogConstants.ActionIDs.TRIGGER_DEVICE_REGISTRATION_EMAIL_NOTIFICATION;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.LogConstants.ActionIDs.TRIGGER_DEVICE_REGISTRATION_PUSH_NOTIFICATION;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.LogConstants.InputKeys.TENANT_DOMAIN;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.LogConstants.PUSH_DEVICE_HANDLER_SERVICE;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.PushNotificationConstants.DEVICE_HANDLE;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.PushNotificationConstants.DEVICE_ID;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.PushNotificationConstants.DEVICE_REGISTRATION_SCENARIO;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.PushNotificationConstants.DEVICE_TOKEN;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.PushNotificationConstants.IP_ADDRESS;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.PushNotificationConstants.NOTIFICATION_PROVIDER;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.PushNotificationConstants.NOTIFICATION_SCENARIO;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.PushNotificationConstants.PUSH_NOTIFICATION_CHANNEL;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.PushNotificationConstants.PUSH_NOTIFICATION_EVENT_NAME;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.REGISTRATION_TIME_FORMATTER_PATTERN;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.SIGNATURE_ALGORITHM;
import static org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementConstants.PUSH_PUBLISHER_TYPE;

/**
 * Device handler service implementation.
 */
@Capability(
        namespace = "osgi.service",
        attribute = {
                "objectClass=org.wso2.carbon.identity.notification.push.device.handler.DeviceHandlerService",
                "service.scope=singleton"
        }
)
public class DeviceHandlerServiceImpl implements DeviceHandlerService {

    private static final Log LOG = LogFactory.getLog(DeviceHandlerServiceImpl.class);
    private DeviceDAO deviceDAO;
    private DeviceRegistrationContextManager deviceRegistrationContextManager;
    private static final DeviceHandlerAuditLogger AUDIT_LOGGER = new DeviceHandlerAuditLogger();
    private static final DateTimeFormatter REGISTRATION_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(REGISTRATION_TIME_FORMATTER_PATTERN).withZone(ZoneOffset.UTC);

    /**
     * Constructor of DeviceHandlerServiceImpl.
     *
     * @param deviceRegistrationContextManager Device registration context manager.
     * @param deviceDAO                        Device DAO.
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

        AUDIT_LOGGER.printAuditLog(
                DeviceHandlerAuditLogger.Operation.REGISTER_DEVICE,
                deviceId,
                device.getUserId()
        );

        return device;
    }

    @Override
    public void unregisterDevice(String deviceId) throws PushDeviceHandlerException {

        Optional<Device> device = deviceDAO.getDevice(deviceId);
        if (device.isPresent()) {
            Device deviceToDelete = device.get();

            handleDeleteDeviceForProvider(device.get());
            deviceDAO.unregisterDevice(deviceId);
            AUDIT_LOGGER.printAuditLog(
                    DeviceHandlerAuditLogger.Operation.UNREGISTER_DEVICE,
                    deviceId,
                    deviceToDelete.getUserId()
            );
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
        AUDIT_LOGGER.printAuditLog(
                DeviceHandlerAuditLogger.Operation.UNREGISTER_DEVICE,
                deviceId,
                device.getUserId()
        );
    }

    @Override
    public void unregisterDeviceByUserId(String userId, String tenantDomain) throws PushDeviceHandlerException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        Optional<Device> device = deviceDAO.getDeviceByUserId(userId, tenantId);
        if (device.isPresent()) {
            handleDeleteDeviceForProvider(device.get());
            String deviceId = device.get().getDeviceId();

            deviceDAO.unregisterDevice(deviceId);
            AUDIT_LOGGER.printAuditLog(
                    DeviceHandlerAuditLogger.Operation.UNREGISTER_DEVICE,
                    deviceId,
                    userId
            );
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
    public List<Device> getDevicesByUserId(String userId, String tenantDomain) throws PushDeviceHandlerException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        List<Device> devices = deviceDAO.getDevicesByUserId(userId, tenantId);

        return devices != null ? devices : Collections.emptyList();
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
     * @param domainIdentifier          Tenant domain or Org ID of the user.
     * @throws OrganizationManagementException Organization Management Exception.
     */
    private void resolveTenantAndOrganizationInfo(RegistrationDiscoveryData registrationDiscoveryData,
                                                  String domainIdentifier) throws OrganizationManagementException {

        if (OrganizationManagementUtil.isOrganization(domainIdentifier)) {
            OrganizationManager organizationManager = PushDeviceHandlerDataHolder.getInstance()
                    .getOrganizationManager();
            String orgId = organizationManager.resolveOrganizationId(domainIdentifier);
            String organizationName = organizationManager.getOrganizationNameById(orgId);

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
     * @throws PushDeviceHandlerClientException Push Device Handler Client Exception.
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

        String username = context.getUsername();
        String tenantDomain = context.getTenantDomain();
        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

        PushDeviceMgtConfigData tenantConfig = PushDeviceConfigManager.getPushDeviceConfig(tenantDomain);
        User user = resolveUser(username, tenantDomain, tenantConfig);
        String userId = user.getUserID();

        List<Device> existingDevices = deviceDAO.getDevicesByUserId(userId, tenantId);

        // if multiple device enrollment is not enabled, check if the user already has a registered device
        if (!Boolean.TRUE.equals(tenantConfig.getEnableMultipleDeviceEnrollment())) {
            if (!existingDevices.isEmpty()) {
                throw new PushDeviceHandlerClientException(
                        ERROR_CODE_DEVICE_ALREADY_REGISTERED.getCode(),
                        ERROR_CODE_DEVICE_ALREADY_REGISTERED.toString());
            }
        } else {
            int maxDeviceLimit = tenantConfig.getMaximumDeviceLimit() != null
                    ? tenantConfig.getMaximumDeviceLimit() : DEFAULT_MIN_DEVICE_LIMIT_PER_USER;
            if (existingDevices.size() >= maxDeviceLimit) {
                throw new PushDeviceHandlerClientException(ERROR_CODE_MAX_DEVICE_LIMIT_REACHED.getCode(),
                        String.format(ERROR_CODE_MAX_DEVICE_LIMIT_REACHED.getMessage(), userId));
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
        handleDeviceRegistrationForProvider(device, registrationRequest.getProvider());

        try {
            deviceDAO.registerDevice(device, tenantId);
            context.setRegistered(true);
        } catch (PushDeviceHandlerException e) {
            String errorMessage = String.format(ERROR_CODE_DEVICE_REGISTRATION_FAILED.toString(),
                    registrationRequest.getDeviceId());
            throw new PushDeviceHandlerServerException(errorMessage, e);
        }

        // Trigger notifications about the new device registration.
        if (Boolean.TRUE.equals(tenantConfig.getEnableDeviceRegistrationNotifications())) {
            Set<DeviceRegistrationNotificationChannelEnum> channels =
                    tenantConfig.getDeviceRegistrationNotificationChannels();
            if (channels != null && channels.contains(DeviceRegistrationNotificationChannelEnum.EMAIL)) {
                triggerEmailNotification(device, tenantDomain, user);
            }
            if (channels != null && channels.contains(DeviceRegistrationNotificationChannelEnum.PUSH_NOTIFICATION)) {
                triggerPushNotification(device, existingDevices, user, tenantDomain);
            }
        }

        return device;
    }

    /**
     * Resolve the user owning the device registration, along with the claims
     * required for the device registration notifications.
     *
     * @param username     Username from the device registration context.
     * @param tenantDomain Tenant domain of the user.
     * @return Resolved user; never {@code null}.
     * @throws PushDeviceHandlerServerException If the user cannot be resolved.
     */
    private User resolveUser(String username, String tenantDomain, PushDeviceMgtConfigData tenantConfig)
            throws PushDeviceHandlerServerException {

        try {
            UserRealm userRealm = IdentityTenantUtil.getRealm(tenantDomain, username);
            AbstractUserStoreManager userStoreManager = (AbstractUserStoreManager) userRealm.getUserStoreManager();
            String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(username);
            String userId = userStoreManager.getUserIDFromUserName(tenantAwareUsername);
            if (StringUtils.isBlank(userId)) {
                String errorMessage = String.format(ERROR_CODE_FAILED_TO_GET_USER_ID.toString(), username);
                throw new PushDeviceHandlerServerException(errorMessage);
            }

            User user;
            if (Boolean.TRUE.equals(tenantConfig.getEnableDeviceRegistrationNotifications())) {
                String[] requestedClaims = new String[] {NotificationChannels.EMAIL_CHANNEL.getClaimUri()};
                user = userStoreManager.getUserWithID(userId, requestedClaims, UserCoreConstants.DEFAULT_PROFILE);
            } else {
                user = new User(userId, tenantAwareUsername, null);
            }

            return user;
        } catch (UserStoreException | IdentityException e) {
            String errorMessage = String.format(ERROR_CODE_FAILED_TO_GET_USER_ID.toString(), username);
            throw new PushDeviceHandlerServerException(errorMessage, e);
        }
    }

    /**
     * Handle the device registration for the push notification providers.
     *
     * @param device Device.
     * @param providerData Provider-specific metadata from the registration request.
     * @throws PushDeviceHandlerClientException Push Device Handler Client Exception.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    private void handleDeviceRegistrationForProvider(Device device, RegistrationRequestProviderData providerData)
            throws PushDeviceHandlerClientException, PushDeviceHandlerServerException {

        try {
            PushDeviceData pushDeviceData = buildPushDeviceDataFromDevice(device);
            pushDeviceData = setProviderMetadataToPushDeviceData(pushDeviceData, providerData);
            String pushProviderName = getPushProviderName(providerData);
            PushProvider pushProvider = PushDeviceHandlerDataHolder.getInstance().getPushProvider(pushProviderName);
            PushSenderDTO pushSender = getPushSenderForProvider(pushProvider.getName());
            pushProvider.registerDevice(pushDeviceData, buildPushSenderData(pushSender));
            device.setProvider(pushProviderName);
            device.setDeviceHandle(pushDeviceData.getDeviceHandle());
        } catch (PushProviderException e) {
            if (e instanceof PushProviderClientException) {
                throw new PushDeviceHandlerClientException(e.getErrorCode(), e.getMessage(), e.getCause());
            }
            throw new PushDeviceHandlerServerException("Error occurred while registering the device.", e);
        }
    }

    /**
     * Handle the device deletion for the push notification providers.
     *
     * @param device Device.
     * @throws PushDeviceHandlerClientException Push Device Handler Client Exception.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    private void handleDeleteDeviceForProvider(Device device)
            throws PushDeviceHandlerServerException, PushDeviceHandlerClientException {

        String deviceProviderType = device.getProvider();
        PushDeviceData pushDeviceData = buildPushDeviceDataFromDevice(device);
        try {
            PushProvider pushProvider = PushDeviceHandlerDataHolder.getInstance()
                    .getPushProvider(deviceProviderType);
            PushSenderDTO pushSender = getPushSenderForProvider(pushProvider.getName());
            pushProvider.unregisterDevice(pushDeviceData, buildPushSenderData(pushSender));
        } catch (PushProviderException e) {
            if (e instanceof PushProviderClientException) {
                throw new PushDeviceHandlerClientException(e.getErrorCode(), e.getMessage(), e.getCause());
            }
            throw new PushDeviceHandlerServerException("Error occurred while unregistering the device.", e);
        }
    }

    /**
     * Handle the device edit and exceptions.
     *
     * @param device Device.
     * @param path   Path.
     * @param value  Value.
     * @throws PushDeviceHandlerClientException Push Device Handler Client Exception.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    private void handleEditDevice(Device device, String path, String value)
            throws PushDeviceHandlerServerException, PushDeviceHandlerClientException {

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
     * @throws PushDeviceHandlerClientException Push Device Handler Client Exception.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    private void handleUpdateDeviceForProvider(Device device)
            throws PushDeviceHandlerServerException, PushDeviceHandlerClientException {

        String deviceProviderType = device.getProvider();
        PushDeviceData pushDeviceData = buildPushDeviceDataFromDevice(device);
        try {
            PushProvider pushProvider = PushDeviceHandlerDataHolder.getInstance()
                    .getPushProvider(deviceProviderType);
            PushSenderDTO pushSender = getPushSenderForProvider(pushProvider.getName());
            pushProvider.updateDevice(pushDeviceData, buildPushSenderData(pushSender));
            device.setDeviceHandle(pushDeviceData.getDeviceHandle());

        } catch (PushProviderException e) {
            if (e instanceof PushProviderClientException) {
                throw new PushDeviceHandlerClientException(e.getErrorCode(), e.getMessage(), e.getCause());
            }
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
     * Get the push provider name to use for device registration.
     *
     * @param providerData Provider data from the registration request, or {@code null} if not specified.
     * @return Name of the push provider to use.
     * @throws PushDeviceHandlerClientException Push Device Handler Client Exception.
     * @throws PushDeviceHandlerServerException Push Device Handler Server Exception.
     */
    private String getPushProviderName(RegistrationRequestProviderData providerData)
            throws PushDeviceHandlerServerException, PushDeviceHandlerClientException {

        /*
            1) If the provider data is provided in the registration request, we will use that to register the device.
            2) If the provider data is not provided, we will use the default push provider to register the device.
            3) If the default push provider is not configured,
               we will check if there is only one provider available and use it as the provider.
            4) If all the above conditions are not met, we will throw an exception indicating that the
               provider is not specified.
        */

        if (providerData != null && StringUtils.isNotBlank(providerData.getName())) {
            return providerData.getName();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Provider data is not provided in the registration request. Retrieving the default push " +
                    "sender.");
        }
        try {
            Map<String, String> configs = PushDeviceHandlerDataHolder.getInstance()
                    .getNotificationSenderManagementService()
                    .getNotificationSenderConfigurations(PUSH_PUBLISHER_TYPE, true);
            if (configs.containsKey(DEFAULT_PUSH_PROVIDER)) {
                return configs.get(DEFAULT_PUSH_PROVIDER);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Default push notification provider is not configured. " +
                            "Checking if there is only one push sender available.");
                }

                List<PushSenderDTO> pushSenders = PushDeviceHandlerDataHolder.getInstance()
                        .getNotificationSenderManagementService().getPushSenders(true);
                if (pushSenders.size() == 1) {
                    String pushProvider = pushSenders.get(0).getProvider();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Only one push sender is available: %s. " +
                                "Using it as the push provider.", pushProvider));
                    }
                    return pushProvider;
                }

                LOG.debug("Cannot determine a push provider as default fallback.");

                throw new PushDeviceHandlerServerException(ERROR_CODE_FAILED_TO_RESOLVE_PUSH_PROVIDER.getCode(),
                        ERROR_CODE_FAILED_TO_RESOLVE_PUSH_PROVIDER.getMessage());
            }
        } catch (NotificationSenderManagementException e) {

            LOG.debug("Error occurred while retrieving the default push notification provider", e);
            throw new PushDeviceHandlerServerException(
                    "Error occurred while retrieving the default push notification provider.", e);
        }
    }

    /**
     * Get the push sender for the given provider.
     *
     * @param providerName Provider name.
     * @return PushSenderDTO.
     * @throws PushDeviceHandlerServerException Notification Sender Management Exception.
     */
    private PushSenderDTO getPushSenderForProvider(String providerName) throws PushDeviceHandlerServerException {

        try {
            List<PushSenderDTO> pushSenders = PushDeviceHandlerDataHolder.getInstance()
                    .getNotificationSenderManagementService()
                    .getPushSenders(true);
            for (PushSenderDTO pushSender : pushSenders) {
                if (pushSender.getProvider().equals(providerName)) {
                    return pushSender;
                }
            }
            // This means no push sender is found for the provider.
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("No push sender found for the provider: %s", providerName));
            }
            throw new PushDeviceHandlerServerException(
                    String.format("No push sender found for the provider: %s", providerName));
        } catch (NotificationSenderManagementException e) {
            throw new PushDeviceHandlerServerException(
                    "Error occurred while retrieving the push notification senders.", e);
        }
    }

    /**
     * Set provider metadata to push device data.
     *
     * @param pushDeviceData Push device data to update.
     * @param providerData   Provider data containing metadata, or {@code null} if not provided.
     * @return Updated push device data.
     */
    private PushDeviceData setProviderMetadataToPushDeviceData(
            PushDeviceData pushDeviceData, RegistrationRequestProviderData providerData) {

        if (providerData != null) {
            pushDeviceData.setProviderMetadata(providerData.getMetadata());
        }
        return pushDeviceData;
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

    /**
     * Trigger an email notification informing the user that a new device has
     * been registered for their account.
     *
     * @param device       Newly registered device.
     * @param tenantDomain Tenant domain of the user.
     * @param user         Resolved user, used to obtain the username and email claim.
     */
    private void triggerEmailNotification(Device device, String tenantDomain, User user) {

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format(
                    "Sending device registration email notification for the device ID: %s.",
                    device.getDeviceId()));
        }

        String userId = user.getUserID();
        String email = getEmailAddress(user);
        if (StringUtils.isBlank(email)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Email address not found for userId: " + userId
                        + ". Skipping device registration email notification.");
            }
            return;
        }

        HashMap<String, Object> properties = new HashMap<>();

        properties.put(IdentityEventConstants.EventProperty.USER_NAME, user.getUsername());
        properties.put(IdentityEventConstants.EventProperty.TENANT_DOMAIN, tenantDomain);
        properties.put(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN, user.getUserStoreDomain());

        // Explicit recipient so the handler does not need to re-resolve claims.
        properties.put(ARBITRARY_SEND_TO, email);

        // Tell the notification handler which template to render.
        properties.put(EMAIL_TEMPLATE_TYPE, PUSH_DEVICE_REGISTRATION_TEMPLATE);

        // Custom placeholders the template can reference.
        properties.put(PushDeviceHandlerConstants.EmailNotificationConstants.DEVICE_NAME_PLACEHOLDER,
                device.getDeviceName());
        properties.put(PushDeviceHandlerConstants.EmailNotificationConstants.DEVICE_MODEL_PLACEHOLDER,
                device.getDeviceModel());
        properties.put(PushDeviceHandlerConstants.EmailNotificationConstants.REGISTRATION_TIME_PLACEHOLDER,
                formatRegistrationTime(System.currentTimeMillis()));
        String ipAddress = resolveClientIpAddress();
        if (StringUtils.isNotBlank(ipAddress)) {
            properties.put(PushDeviceHandlerConstants.EmailNotificationConstants.IP_ADDRESS_PLACEHOLDER, ipAddress);
        }

        try {
            Event event = new Event(IdentityEventConstants.Event.TRIGGER_NOTIFICATION, properties);
            PushDeviceHandlerDataHolder.getInstance().getIdentityEventService().handleEvent(event);
        } catch (IdentityEventException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error while triggering device registration email "
                        + "notification for userId: " + userId, e);
            }

            if (LoggerUtils.isDiagnosticLogsEnabled()) {
                DiagnosticLog.DiagnosticLogBuilder diagnosticLogBuilder = new DiagnosticLog.DiagnosticLogBuilder(
                        PUSH_DEVICE_HANDLER_SERVICE, TRIGGER_DEVICE_REGISTRATION_EMAIL_NOTIFICATION);
                diagnosticLogBuilder
                        .resultMessage("Error while triggering the device registration email notification.")
                        .logDetailLevel(DiagnosticLog.LogDetailLevel.APPLICATION)
                        .resultStatus(DiagnosticLog.ResultStatus.FAILED)
                        .inputParam(PushDeviceHandlerConstants.LogConstants.InputKeys.DEVICE_ID, device.getDeviceId())
                        .inputParam(PushDeviceHandlerConstants.LogConstants.InputKeys.USER_ID, userId)
                        .inputParam(TENANT_DOMAIN, tenantDomain);
                LoggerUtils.triggerDiagnosticLogEvent(diagnosticLogBuilder);
            }
        }
    }

    /**
     * Get the email address from the resolved user.
     *
     * @param user Resolved user.
     * @return Email address, or {@code null} if not available.
     */
    private String getEmailAddress(User user) {

        Map<String, String> userAttributes = user.getAttributes();
        if (MapUtils.isEmpty(userAttributes)) {
            return null;
        }
        return userAttributes.get(NotificationChannels.EMAIL_CHANNEL.getClaimUri());
    }

    /**
     * Trigger push notifications to the user's previously registered devices to
     * notify that a new device has been registered for the account.
     *
     * @param registeredDevice Newly registered device.
     * @param existingDevices  Previously registered devices of the user.
     * @param user             Resolved user who owns the devices.
     * @param tenantDomain     Tenant domain of the user.
     */
    private void triggerPushNotification(Device registeredDevice, List<Device> existingDevices, User user,
                                         String tenantDomain) {

        if (existingDevices == null || existingDevices.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("No previously registered devices found to notify about "
                                + "the registration of device ID: %s.",
                        registeredDevice.getDeviceId()));
            }
            return;
        }

        String registrationTime = formatRegistrationTime(System.currentTimeMillis());
        String ipAddress = resolveClientIpAddress();

        HashMap<String, Object> properties = new HashMap<>();

        // Standard identity event properties expected by the handler.
        properties.put(IdentityEventConstants.EventProperty.USER_ID, registeredDevice.getUserId());
        properties.put(IdentityEventConstants.EventProperty.USER_NAME, user.getUsername());
        properties.put(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN, user.getUserStoreDomain());
        properties.put(IdentityEventConstants.EventProperty.TENANT_DOMAIN, tenantDomain);
        properties.put(IdentityEventConstants.EventProperty.NOTIFICATION_CHANNEL, PUSH_NOTIFICATION_CHANNEL);
        properties.put(NOTIFICATION_SCENARIO, DEVICE_REGISTRATION_SCENARIO);

        // Details of the newly registered device rendered in the notification.
        properties.put(PushDeviceHandlerConstants.PushNotificationConstants.DEVICE_NAME_PLACEHOLDER,
                StringUtils.defaultString(registeredDevice.getDeviceName()));
        properties.put(PushDeviceHandlerConstants.PushNotificationConstants.DEVICE_MODEL_PLACEHOLDER,
                StringUtils.defaultString(registeredDevice.getDeviceModel()));
        properties.put(PushDeviceHandlerConstants.PushNotificationConstants.REGISTRATION_TIME_PLACEHOLDER,
                registrationTime);
        if (StringUtils.isNotBlank(ipAddress)) {
            properties.put(IP_ADDRESS, ipAddress);
        }

        for (Device device : existingDevices) {

            // The notification is delivered to the previously registered device.
            properties.put(NOTIFICATION_PROVIDER, device.getProvider());
            properties.put(DEVICE_TOKEN, device.getDeviceToken());
            properties.put(DEVICE_ID, device.getDeviceId());
            properties.put(DEVICE_HANDLE, device.getDeviceHandle());

            try {
                Event event = new Event(PUSH_NOTIFICATION_EVENT_NAME, properties);
                PushDeviceHandlerDataHolder.getInstance().getIdentityEventService().handleEvent(event);
            } catch (IdentityEventException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format(
                            "Error while triggering the device registration push notification to the device ID: %s.",
                            device.getDeviceId()), e);
                }

                if (LoggerUtils.isDiagnosticLogsEnabled()) {
                    DiagnosticLog.DiagnosticLogBuilder diagnosticLogBuilder = new DiagnosticLog.DiagnosticLogBuilder(
                            PUSH_DEVICE_HANDLER_SERVICE, TRIGGER_DEVICE_REGISTRATION_PUSH_NOTIFICATION);
                    diagnosticLogBuilder
                            .resultMessage("Error while triggering the device registration push notification.")
                            .logDetailLevel(DiagnosticLog.LogDetailLevel.APPLICATION)
                            .resultStatus(DiagnosticLog.ResultStatus.FAILED)
                            .inputParam(PushDeviceHandlerConstants.LogConstants.InputKeys.DEVICE_ID,
                                    device.getDeviceId())
                            .inputParam(PushDeviceHandlerConstants.LogConstants.InputKeys.USER_ID,
                                    registeredDevice.getUserId())
                            .inputParam(TENANT_DOMAIN, tenantDomain);
                    LoggerUtils.triggerDiagnosticLogEvent(diagnosticLogBuilder);
                }
            }
        }
    }

    /**
     * Resolve the client IP address of the current request.
     *
     * @return Client IP address, or {@code null} if it cannot be resolved.
     */
    private String resolveClientIpAddress() {

        Request request = IdentityContext.getThreadLocalIdentityContext().getRequest();
        if (request == null || StringUtils.isBlank(request.getIpAddress())) {
            return null;
        }

        return request.getIpAddress();
    }

    /**
     * Format a registration timestamp consistently across notification channels.
     *
     * @param time Registration time.
     * @return Human-readable UTC timestamp.
     */
    private String formatRegistrationTime(long time) {

        Instant instant = Instant.ofEpochMilli(time);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(REGISTRATION_TIME_FORMATTER_PATTERN)
                .withZone(ZoneOffset.UTC);
        return formatter.format(instant);
    }
}
