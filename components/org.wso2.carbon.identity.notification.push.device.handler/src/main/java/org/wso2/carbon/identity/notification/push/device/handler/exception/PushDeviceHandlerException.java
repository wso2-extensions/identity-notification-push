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

package org.wso2.carbon.identity.notification.push.device.handler.exception;

import org.wso2.carbon.identity.base.IdentityException;

/**
 * Push device handler exception.
 */
public class PushDeviceHandlerException extends IdentityException {

    private static final long serialVersionUID = -8128089576435637828L;

    public PushDeviceHandlerException(String message) {

        super(message);
    }

    public PushDeviceHandlerException(String errorCode, String message) {

        super(errorCode, message);
    }

    public PushDeviceHandlerException(String message, Throwable cause) {

        super(message, cause);
    }

    public PushDeviceHandlerException(String errorCode, String message, Throwable cause) {

        super(errorCode, message, cause);
    }
}
