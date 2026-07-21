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

package org.wso2.carbon.identity.notification.push.device.handler.constant;

/**
 * Constants related to Push Device Handler.
 */
public class PushDeviceHandlerConstants {

    public static final String HASHING_ALGORITHM = "SHA256withRSA";
    public static final String SIGNATURE_ALGORITHM = "RSA";
    public static final String DEVICE_REGISTRATION_REQUEST_CACHE = "PushDeviceRegistrationRequestCache";
    public static final String DEFAULT_PUSH_PROVIDER = "defaultPushProvider";
    public static final String DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD =
            "PushAuthenticator.DeviceRegistrationContext.ValidityPeriod";
    public static final int DEFAULT_DEVICE_REGISTRATION_CONTEXT_VALIDITY_PERIOD = 180;

    public static final String MAX_DEVICE_LIMIT_PER_USER = "PushAuthenticator.DeviceManagement.MaxDeviceLimitPerUser";
    public static final int DEFAULT_MAX_DEVICE_LIMIT_PER_USER = 10;
    public static  final int DEFAULT_MIN_DEVICE_LIMIT_PER_USER = 2;
    public static final String PUSH_DEVICE_MGT_RESOURCE_TYPE = "DEVICE_MANAGEMENT";
    public static final String PUSH_DEVICE_MGT_RESOURCE_NAME = "PUSH_DEVICE_MANAGEMENT";
    public static final String ATTR_ENABLE_MULTIPLE_DEVICE_ENROLLMENT = "enableMultipleDeviceEnrollment";
    public static final String ATTR_MAX_DEVICE_LIMIT = "maximumDeviceLimit";
    public static final String ATTR_ENABLE_DEVICE_REGISTRATION_NOTIFICATIONS = "enableDeviceRegistrationNotifications";
    public static final String ATTR_DEVICE_REGISTRATION_NOTIFICATION_CHANNELS =
            "deviceRegistrationNotificationChannels";
    public static final String NOTIFICATION_CHANNELS_SEPARATOR = ",";
    public static final String REGISTRATION_TIME_FORMATTER_PATTERN = "yyyy-MM-dd HH:mm:ss 'UTC'";



    /**
     * Constants related to email notifications sent during device management operations.
     */
    public static class EmailNotificationConstants {

        public static final String ARBITRARY_SEND_TO = "send-to";
        public static final String EMAIL_TEMPLATE_TYPE = "TEMPLATE_TYPE";
        public static final String PUSH_DEVICE_REGISTRATION_TEMPLATE = "PushDeviceRegistration";

        // Placeholders used in the template body
        public static final String DEVICE_NAME_PLACEHOLDER = "push-device-name";
        public static final String DEVICE_MODEL_PLACEHOLDER = "push-device-model";
        public static final String REGISTRATION_TIME_PLACEHOLDER = "registration-time";
        public static final String IP_ADDRESS_PLACEHOLDER = "ip-address";
    }

    /**
     * Constants related to push notifications sent during device management operations.
     */
    public static final class PushNotificationConstants {

        public static final String PUSH_NOTIFICATION_EVENT_NAME = "TRIGGER_PUSH_NOTIFICATION";
        public static final String PUSH_NOTIFICATION_CHANNEL = "PUSH_NOTIFICATION";
        public static final String NOTIFICATION_SCENARIO = "NOTIFICATION_SCENARIO";
        public static final String DEVICE_REGISTRATION_SCENARIO = "DEVICE_REGISTRATION";
        public static final String NOTIFICATION_PROVIDER = "notificationProvider";
        public static final String DEVICE_TOKEN = "deviceToken";
        public static final String DEVICE_ID = "deviceId";
        public static final String DEVICE_HANDLE = "deviceHandle";
        public static final String IP_ADDRESS = "ipAddress";

        // Placeholders rendered in the push notification template.
        public static final String DEVICE_NAME_PLACEHOLDER = "push-device-name";
        public static final String DEVICE_MODEL_PLACEHOLDER = "push-device-model";
        public static final String REGISTRATION_TIME_PLACEHOLDER = "registration-time";

        /**
         * Private constructor to prevent initialization of the class.
         */
        private PushNotificationConstants() {

        }
    }

    /**
     * Constants related to diagnostic logging of device management operations.
     */
    public static class LogConstants {

        public static final String PUSH_DEVICE_HANDLER_SERVICE = "push-device-handler-service";

        /**
         * Action identifiers used in diagnostic logs.
         */
        public static class ActionIDs {

            public static final String TRIGGER_DEVICE_REGISTRATION_EMAIL_NOTIFICATION =
                    "trigger-device-registration-email-notification";
            public static final String TRIGGER_DEVICE_REGISTRATION_PUSH_NOTIFICATION =
                    "trigger-device-registration-push-notification";

            private ActionIDs() {

            }
        }

        /**
         * Input parameter keys used in diagnostic logs.
         */
        public static class InputKeys {

            public static final String DEVICE_ID = "device-id";
            public static final String USER_ID = "user-id";
            public static final String TENANT_DOMAIN = "tenant-domain";

            private InputKeys() {

            }
        }

        private LogConstants() {

        }
    }

    /**
     * Private constructor to prevent initialization of the class.
     */
    private PushDeviceHandlerConstants() {

    }

    /**
     * Class containing SQL query constants.
     */
    public static class SQLQueries {

        public static final String REGISTER_DEVICE = "INSERT INTO IDN_PUSH_DEVICE_STORE (ID, USER_ID, " +
                "DEVICE_NAME, DEVICE_MODEL, DEVICE_TOKEN, DEVICE_HANDLE, PROVIDER, PUBLIC_KEY, " +
                "TENANT_ID) VALUES (:ID;, :USER_ID;, :DEVICE_NAME;, :DEVICE_MODEL;, :DEVICE_TOKEN;, :DEVICE_HANDLE;, " +
                ":PROVIDER;, :PUBLIC_KEY;, :TENANT_ID;)";
        public static final String GET_DEVICE_BY_DEVICE_ID = "SELECT ID, USER_ID, DEVICE_NAME, DEVICE_MODEL, " +
                "DEVICE_TOKEN, DEVICE_HANDLE, PROVIDER, PUBLIC_KEY, TENANT_ID FROM " +
                "IDN_PUSH_DEVICE_STORE WHERE ID = :ID;";
        public static final String GET_DEVICE_BY_USER_ID = "SELECT ID, USER_ID, DEVICE_NAME, DEVICE_MODEL, " +
                "DEVICE_TOKEN, DEVICE_HANDLE, PROVIDER, PUBLIC_KEY, TENANT_ID FROM " +
                "IDN_PUSH_DEVICE_STORE WHERE USER_ID = :USER_ID; AND TENANT_ID = :TENANT_ID;";
        public static final String GET_PUBLIC_KEY_BY_ID = "SELECT PUBLIC_KEY FROM IDN_PUSH_DEVICE_STORE " +
                "WHERE ID = :ID;";
        public static final String UNREGISTER_DEVICE = "DELETE FROM IDN_PUSH_DEVICE_STORE WHERE ID = :ID;";
        public static final String EDIT_DEVICE = "UPDATE IDN_PUSH_DEVICE_STORE SET DEVICE_NAME = :DEVICE_NAME; " +
                "DEVICE_TOKEN = :DEVICE_TOKEN; WHERE ID = :ID;";
    }

    /**
     * Class containing column names.
     */
    public static class ColumnNames {

        public static final String ID = "ID";
        public static final String USER_ID = "USER_ID";
        public static final String DEVICE_NAME = "DEVICE_NAME";
        public static final String DEVICE_MODEL = "DEVICE_MODEL";
        public static final String DEVICE_TOKEN = "DEVICE_TOKEN";
        public static final String DEVICE_HANDLE = "DEVICE_HANDLE";
        public static final String PUBLIC_KEY = "PUBLIC_KEY";
        public static final String PROVIDER = "PROVIDER";
        public static final String TENANT_ID = "TENANT_ID";
    }

    /**
     * Enum for error messages.
     */
    public enum ErrorMessages {

        ERROR_CODE_ERROR_IDENTIFY_ORG_NAME(
                "PDH-15001",
                "Error occurred while identifying the organization name of tenant: %s."
        ),
        ERROR_CODE_REGISTRATION_CONTEXT_NOT_FOUND(
                "PDH-15002",
                "Error occurred while retrieving the registration context for the device ID: %s."
        ),
        ERROR_CODE_REGISTRATION_CONTEXT_ALREADY_USED(
                "PDH-15002",
                "Registration context already used for the device ID: %s."
        ),
        ERROR_CODE_DEVICE_ALREADY_REGISTERED(
                "PDH-15003",
                "The corresponding user has already registered a device for push notification."
        ),
        ERROR_CODE_SIGNATURE_VERIFICATION_FAILED(
                "PDH-15004",
                "Error occurred while verifying signature for the device ID: %s."
        ),
        ERROR_CODE_INVALID_SIGNATURE(
                "PDH-15005",
                "Invalid signature for the device ID: %s."
        ),
        ERROR_CODE_TOKEN_CLAIM_VERIFICATION_FAILED(
                "PDH-15006",
                "Error occurred while verifying the token claim for the device ID: %s."
        ),
        ERROR_CODE_DEVICE_REGISTRATION_FAILED(
                "PDH-15007",
                "Error occurred while registering the device for the device ID: %s."
        ),
        ERROR_CODE_FAILED_TO_GET_USER_ID(
                "PDH-15008",
                "Error occurred while retrieving the user ID for the username: %s."
        ),
        ERROR_CODE_DEVICE_NOT_FOUND(
                "PDH-15009",
                "Device not found for the device ID: %s."
        ),
        ERROR_CODE_DEVICE_NOT_FOUND_FOR_USER_ID(
                "PDH-15010",
                "Registered device not found for the user ID: %s."
        ),
        ERROR_CODE_PUBLIC_KEY_NOT_FOUND(
                "PDH-15011",
                "Public key not found for the device ID: %s."
        ),
        ERROR_CODE_INVALID_EDIT_DEVICE_SCENARIO(
                "PDH-15012",
                "Invalid scenario for editing the device for the device ID: %s."
        ),
        ERROR_CODE_FAILED_TO_RESOLVE_PUSH_PROVIDER(
                "PDH-15013",
                "Failed to resolve the correct push provider for the request."
        ),
        ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG(
                "PDH-15014",
                "Failed to get push device configuration for the tenant."
        ),
        ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG(
                "PDH-15015",
                "Failed to add push device configuration for the tenant."
        ),
        ERROR_CODE_UPDATING_PUSH_DEVICE_CONFIG(
                "PDH-15016",
                "Failed to update push device configuration for the tenant."
        ),
        ERROR_CODE_INVALID_DEVICE_LIMIT_CONFIG(
                "PDH-15017",
                "Maximum device limit can only be updated when multiple device enrollment is enabled."
        ),
        ERROR_CODE_DEVICE_LIMIT_PER_USER_EXCEEDS(
                "PDH-15018",
                "Maximum device limit cannot exceed the server-configured device registration per user."
        ),
        ERROR_CODE_MAX_DEVICE_LIMIT_REACHED(
                "PDH-15019",
                "Maximum device limit reached for the user: %s."
        ),
        ERROR_CODE_DEVICE_ID_ALREADY_REGISTERED(
                "PDH-15020",
                "Device is already registered for the device ID: %s."
        ),
        ERROR_CODE_INVALID_DEVICE_LIMIT_VALUE(
                "PDH-15021",
                "Maximum device limit must be a positive value when multiple device enrollment is enabled."
        );

        private final String code;
        private final String message;

        ErrorMessages(String code, String message) {

            this.code = code;
            this.message = message;
        }

        public String getCode() {

            return code;
        }

        public String getMessage() {

            return message;
        }

        @Override
        public String toString() {

            return code + " - " + message;
        }
    }
}
