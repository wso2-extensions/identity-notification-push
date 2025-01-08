package org.wso2.carbon.identity.notification.push.provider.exception;

import org.wso2.carbon.identity.base.IdentityException;

/**
 * Push provider exception.
 */
public class PushProviderException extends IdentityException {

    private static final long serialVersionUID = -6434143706981075440L;

    public PushProviderException(String message) {

        super(message);
    }

    public PushProviderException(String errorCode, String message) {

        super(errorCode, message);
    }

    public PushProviderException(String message, Throwable cause) {

        super(message, cause);
    }

    public PushProviderException(String errorCode, String message, Throwable cause) {

        super(errorCode, message, cause);
    }
}
