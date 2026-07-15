/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.notification.push.device.handler.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.configuration.mgt.core.constant.ConfigurationConstants;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceTypeAdd;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerClientException;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerException;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerServerException;
import org.wso2.carbon.identity.notification.push.device.handler.internal.PushDeviceHandlerDataHolder;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationNotificationChannelEnum;
import org.wso2.carbon.identity.notification.push.device.handler.model.PushDeviceMgtConfigData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.configuration.mgt.core.constant.ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_DOES_NOT_EXISTS;
import static org.wso2.carbon.identity.configuration.mgt.core.constant.ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEFAULT_MAX_DEVICE_LIMIT_PER_USER;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.DEFAULT_MIN_DEVICE_LIMIT_PER_USER;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_DEVICE_LIMIT_PER_USER_EXCEEDS;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_INVALID_DEVICE_LIMIT_CONFIG;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_INVALID_DEVICE_LIMIT_VALUE;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_UPDATING_PUSH_DEVICE_CONFIG;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.MAX_DEVICE_LIMIT_PER_USER;

/**
 * Push device config manager.
 */
public class PushDeviceConfigManager {

    private static final Log LOG = LogFactory.getLog(PushDeviceConfigManager.class);
    private static final DeviceHandlerAuditLogger AUDIT_LOGGER = new DeviceHandlerAuditLogger();

    /**
     * Get the maximum device limit per user.
     *
     * @return maximum device limit per user.
     */
    private static int getMaxDeviceLimitPerUser() {

        String configValue = IdentityUtil.getProperty(MAX_DEVICE_LIMIT_PER_USER);
        if (StringUtils.isNotBlank(configValue)) {
            try {
                return Integer.parseInt(configValue.trim());
            } catch (NumberFormatException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Invalid value configured for '" + MAX_DEVICE_LIMIT_PER_USER + "': "
                            + configValue + ". Using default: " + DEFAULT_MAX_DEVICE_LIMIT_PER_USER);
                }
            }
        }
        return DEFAULT_MAX_DEVICE_LIMIT_PER_USER;
    }

    /**
     * Get the push device configuration resource for the given tenant.
     *
     * @param tenantDomain Tenant domain.
     * @return Push device configuration resource.
     * @throws PushDeviceHandlerServerException If an error occurs while retrieving the config.
     */
    public static PushDeviceMgtConfigData getPushDeviceConfig(String tenantDomain)
            throws PushDeviceHandlerServerException {

        Resource resource = getResource(tenantDomain, true);
        if (resource == null || resource.getAttributes() == null || resource.getAttributes().isEmpty()) {
            resource = getDefaultPushDeviceMgtConfigs();
        }

        return buildDTOFromResource(resource);
    }

    /**
     * Retrieve the configuration resource for the given tenant domain.
     *
     * @param tenantDomain Tenant domain.
     * @param resolve      Whether to resolve the resource from the parent organization hierarchy.
     * @return Configuration resource, or null if not found.
     * @throws PushDeviceHandlerServerException If an error occurs while retrieving the config.
     */
    private static Resource getResource(String tenantDomain, boolean resolve) throws PushDeviceHandlerServerException {

        Resource resource = null;

        try {
            resource = PushDeviceHandlerDataHolder.getInstance().getConfigurationManager()
                    .getResource(PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_TYPE,
                            PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_NAME, resolve);
        } catch (ConfigurationManagementException e) {
            if (!ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS.getCode().equals(e.getErrorCode())
                    && !ERROR_CODE_RESOURCE_DOES_NOT_EXISTS.getCode().equals(e.getErrorCode())) {
                throw new PushDeviceHandlerServerException(ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG.getCode(),
                        ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG.getMessage(), e);
            }
        }
        return resource;
    }

    /**
     * Update the push device configuration for the given tenant.
     *
     * @param config       Push device management configuration to persist.
     * @param tenantDomain Tenant domain.
     * @throws PushDeviceHandlerException If an error occurs while updating the config.
     */
    public static PushDeviceMgtConfigData updatePushDeviceConfig(PushDeviceMgtConfigData config, String tenantDomain)
            throws PushDeviceHandlerException {

        boolean multipleDeviceEnrollment = Boolean.TRUE.equals(config.getEnableMultipleDeviceEnrollment());
        Integer maximumDeviceLimit = config.getMaximumDeviceLimit();

        if (multipleDeviceEnrollment) {
            if (maximumDeviceLimit == null || maximumDeviceLimit < 1) {
                throw new PushDeviceHandlerClientException(ERROR_CODE_INVALID_DEVICE_LIMIT_VALUE.getCode(),
                        ERROR_CODE_INVALID_DEVICE_LIMIT_VALUE.getMessage());
            }
            int upperBound = getMaxDeviceLimitPerUser();
            if (maximumDeviceLimit > upperBound) {
                throw new PushDeviceHandlerClientException(ERROR_CODE_DEVICE_LIMIT_PER_USER_EXCEEDS.getCode(),
                        ERROR_CODE_DEVICE_LIMIT_PER_USER_EXCEEDS.getMessage()
                                + String.format(" Configured upper bound: %s", upperBound));
            }
        } else if (maximumDeviceLimit != null && maximumDeviceLimit > 1) {
            throw new PushDeviceHandlerClientException(ERROR_CODE_INVALID_DEVICE_LIMIT_CONFIG.getCode(),
                    ERROR_CODE_INVALID_DEVICE_LIMIT_CONFIG.getMessage());
        }

        Resource resource = buildResourceFromConfig(config);
        Resource existingResource = getResource(tenantDomain, false);

        if (existingResource != null && existingResource.getAttributes() != null
                && !existingResource.getAttributes().isEmpty()) {
            replaceResource(resource);
        } else {
            addResource(resource);
        }

        AUDIT_LOGGER.printAuditLog(
                DeviceHandlerAuditLogger.Operation.UPDATE_DEVICE_MGT_CONFIG,
                tenantDomain,
                CarbonContext.getThreadLocalCarbonContext().getUserId()
        );

        return config;
    }

    /**
     * Replace an existing configuration resource for the given tenant domain.
     *
     * @param resource Resource to replace.
     * @throws PushDeviceHandlerException If an error occurs while replacing the resource.
     */
    private static void replaceResource(Resource resource) throws PushDeviceHandlerServerException {

        try {
            PushDeviceHandlerDataHolder.getInstance().getConfigurationManager()
                    .replaceResource(PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_TYPE, resource);
        } catch (ConfigurationManagementException e) {
            throw new PushDeviceHandlerServerException(ERROR_CODE_UPDATING_PUSH_DEVICE_CONFIG.getCode(),
                    ERROR_CODE_UPDATING_PUSH_DEVICE_CONFIG.getMessage(), e);
        }
    }

    /**
     * Build a configuration resource from the given DTO.
     *
     * @param config Push device management configuration.
     * @return Configuration resource.
     */
    private static Resource buildResourceFromConfig(PushDeviceMgtConfigData config) {

        Resource resource = new Resource();
        resource.setResourceName(PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_NAME);

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute(PushDeviceHandlerConstants.ATTR_ENABLE_MULTIPLE_DEVICE_ENROLLMENT,
                String.valueOf(Boolean.TRUE.equals(config.getEnableMultipleDeviceEnrollment()))));

        int maximumDeviceLimit = config.getMaximumDeviceLimit() != null ? config.getMaximumDeviceLimit() :
                DEFAULT_MIN_DEVICE_LIMIT_PER_USER;
        attributes.add(new Attribute(PushDeviceHandlerConstants.ATTR_MAX_DEVICE_LIMIT,
                String.valueOf(maximumDeviceLimit)));
        attributes.add(new Attribute(PushDeviceHandlerConstants.ATTR_ENABLE_DEVICE_REGISTRATION_NOTIFICATIONS,
                String.valueOf(Boolean.TRUE.equals(config.getEnableDeviceRegistrationNotifications()))));
        attributes.add(new Attribute(PushDeviceHandlerConstants.ATTR_DEVICE_REGISTRATION_NOTIFICATION_CHANNELS,
                serializeChannels(config.getDeviceRegistrationNotificationChannels())));

        resource.setAttributes(attributes);
        return resource;
    }

    /**
     * Build a default configuration resource.
     *
     * @return Default configuration resource.
     */
    public static Resource getDefaultPushDeviceMgtConfigs() {

        Resource resource = new Resource(PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_NAME,
                        PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_TYPE);

        List<Attribute> attributeList = new ArrayList<>();
        attributeList.add(new Attribute(PushDeviceHandlerConstants.ATTR_ENABLE_MULTIPLE_DEVICE_ENROLLMENT, "false"));
        attributeList.add(new Attribute(PushDeviceHandlerConstants.ATTR_MAX_DEVICE_LIMIT, "2"));
        attributeList.add(
                new Attribute(PushDeviceHandlerConstants.ATTR_ENABLE_DEVICE_REGISTRATION_NOTIFICATIONS, "false"));
        attributeList.add(
                new Attribute(PushDeviceHandlerConstants.ATTR_DEVICE_REGISTRATION_NOTIFICATION_CHANNELS,
                        DeviceRegistrationNotificationChannelEnum.EMAIL.name()));

        resource.setAttributes(attributeList);
        return resource;
    }

    /**
     * Add a configuration resource for the given tenant domain.
     *
     * @param resource     Resource to add.
     * @return Added resource.
     * @throws IdentityException If an error occurs while adding the resource.
     */
    private static Resource addResource(Resource resource) throws PushDeviceHandlerServerException {

        try {
            return PushDeviceHandlerDataHolder.getInstance().getConfigurationManager()
                    .addResource(PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_TYPE, resource);
        } catch (ConfigurationManagementException e) {
            if (ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS.getCode()
                    .equals(e.getErrorCode())) {

                /* If the insert is failing due to the relevant resource-type is not existing in the database,
                create the resource-type and retry the configuration addition. */
                createResourceType();
                return addResource(resource);
            }
            throw new PushDeviceHandlerServerException(ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG.getCode(),
                    ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG.getMessage(), e);
        }
    }

    /**
     * Create the resource type for push authentication settings.
     *
     * @throws IdentityException If an error occurs while creating the resource
     *                           type.
     */
    private static void createResourceType() throws PushDeviceHandlerServerException {

        try {
            ResourceTypeAdd resourceTypeAdd = new ResourceTypeAdd();
            resourceTypeAdd.setName(PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_TYPE);
            resourceTypeAdd.setDescription("Resource Type for "
                    + PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_TYPE);
            PushDeviceHandlerDataHolder.getInstance().getConfigurationManager().addResourceType(resourceTypeAdd);
        } catch (ConfigurationManagementException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error while creating resource type: "
                        + PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_TYPE, e);
            }
            throw new PushDeviceHandlerServerException(ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG.getCode(),
                    ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG.getMessage(), e);
        }
    }

    /**
     * Build a configuration DTO from the given resource.
     *
     * @param resource Configuration resource.
     * @return Configuration resource or default if resource is empty.
     */
    private static PushDeviceMgtConfigData buildDTOFromResource(Resource resource) {

        PushDeviceMgtConfigData config = new PushDeviceMgtConfigData();
        resource.getAttributes().forEach(attribute -> {
            if (PushDeviceHandlerConstants.ATTR_ENABLE_MULTIPLE_DEVICE_ENROLLMENT.equals(attribute.getKey())) {
                config.setEnableMultipleDeviceEnrollment(Boolean.parseBoolean(attribute.getValue()));
            } else if (PushDeviceHandlerConstants.ATTR_MAX_DEVICE_LIMIT.equals(attribute.getKey())) {
                config.setMaximumDeviceLimit(parseMaxDeviceLimit(attribute.getValue()));
            } else if (PushDeviceHandlerConstants.ATTR_ENABLE_DEVICE_REGISTRATION_NOTIFICATIONS
                    .equals(attribute.getKey())) {
                config.setEnableDeviceRegistrationNotifications(Boolean.parseBoolean(attribute.getValue()));
            } else if (PushDeviceHandlerConstants.ATTR_DEVICE_REGISTRATION_NOTIFICATION_CHANNELS
                    .equals(attribute.getKey())) {
                config.setDeviceRegistrationNotificationChannels(parseNotificationChannels(attribute.getValue()));
            }
        });

        return config;
    }

    /**
     * Parse a stored maximum device limit value into an integer.
     *
     * @param value Stored attribute value.
     * @return Parsed maximum device limit, defaulting to 1.
     */
    private static int parseMaxDeviceLimit(String value) {

        if (StringUtils.isNotBlank(value)) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Invalid value stored for '" + PushDeviceHandlerConstants.ATTR_MAX_DEVICE_LIMIT
                            + "': " + value + ". Using default:" + DEFAULT_MIN_DEVICE_LIMIT_PER_USER + ".", e);
                }
            }
        }
        return DEFAULT_MIN_DEVICE_LIMIT_PER_USER;
    }

    /**
     * Serialize the configured notification channels into a comma-separated string.
     *
     * @param channels Channels to serialize.
     * @return Comma-separated channel names, or an empty string when none are set.
     */
    private static String serializeChannels(Set<DeviceRegistrationNotificationChannelEnum> channels) {

        if (channels == null || channels.isEmpty()) {
            return "";
        }
        return channels.stream()
                .map(Enum::name)
                .collect(Collectors.joining(PushDeviceHandlerConstants.NOTIFICATION_CHANNELS_SEPARATOR));
    }

    /**
     * Parse a stored notification channels value into the corresponding enum set.
     * Unknown tokens are skipped and a blank value yields an empty set.
     *
     * @param value Stored attribute value.
     * @return Parsed channels.
     */
    private static Set<DeviceRegistrationNotificationChannelEnum> parseNotificationChannels(String value) {

        if (StringUtils.isBlank(value)) {
            return Collections.emptySet();
        }
        Set<DeviceRegistrationNotificationChannelEnum> channels =
                EnumSet.noneOf(DeviceRegistrationNotificationChannelEnum.class);
        for (String token : value.split(PushDeviceHandlerConstants.NOTIFICATION_CHANNELS_SEPARATOR)) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                channels.add(DeviceRegistrationNotificationChannelEnum.valueOf(trimmed.toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unknown device registration notification channel: " + token, e);
                }
            }
        }
        return channels;
    }
}
