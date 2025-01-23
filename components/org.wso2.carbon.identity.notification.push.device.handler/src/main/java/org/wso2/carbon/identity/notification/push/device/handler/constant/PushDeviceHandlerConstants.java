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

package org.wso2.carbon.identity.notification.push.device.handler.constant;

/**
 * Constants related to Push Device Handler.
 */
public class PushDeviceHandlerConstants {

    public static final String HASHING_ALGORITHM = "SHA256withRSA";
    public static final String SIGNATURE_ALGORITHM = "RSA";

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
                "IDN_PUSH_DEVICE_STORE WHERE USER_ID = :USER_ID;";
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
                "Device already registered for the device ID: %s."
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
