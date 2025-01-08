package org.wso2.carbon.identity.notification.push.provider.constant;

/**
 * Constants related to push providers.
 */
public class PushProviderConstants {

    private PushProviderConstants() {

    }

    public static final String PUSH_PROVIDER_ERROR_PREFIX = "PPE";

    public static final String PUSH_PROVIDER_SECRET_TYPE = "PUSH_PROVIDER_SECRET_PROPERTIES";
    public static final String FCM_SERVICE_ACCOUNT_SECRET = "serviceAccountContent";

    /**
     * Enum for error messages related to push providers.
     */
    public enum ErrorMessages {

        ERROR_PUSH_NOTIFICATION_SENDING_FAILED("65001", "Failed to send the push notification."),
        ERROR_INVALID_DEVICE_HANDLE_FOR_CONFIGURED_PROVIDER("65002",
                "The device handle used to send the push notification is not valid for the configured "
                        + "push provider."),
        ERROR_DEVICE_HANDLE_EXPIRED_OR_NEW_REGISTRATION_REQUIRED("65003",
                "The device handle used to send the push notification has expired or a new registration "
                        + "is required."),
        ERROR_REQUIRED_PROPERTY_MISSING("65004",
                "Required property is missing in the push provider configurations : "),
        ERROR_WHILE_STORING_SECRETS_OF_PUSH_PROVIDER("65005",
                "Error occurred while storing the secrets of the push provider."),
        ERROR_WHILE_RETRIEVING_SECRETS_OF_PUSH_PROVIDER("65006",
                "Error occurred while retrieving the secrets of the push provider."),
        ERROR_WHILE_DELETING_SECRETS_OF_PUSH_PROVIDER("65007",
                "Error occurred while deleting the secrets of the push provider."),;

        private final String code;
        private final String message;

        ErrorMessages(String code, String message) {

            this.code = code;
            this.message = message;
        }

        public String getCode() {

            return PUSH_PROVIDER_ERROR_PREFIX + "-" + code;
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
