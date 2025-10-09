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

package org.wso2.carbon.identity.notification.push.device.handler.utils;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.AuditLog;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import static org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils.jsonObjectToMap;
import static org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils.triggerAuditLogEvent;

/**
 * Device handler audit logger class.
 */
public class DeviceHandlerAuditLogger {

    /**
     * Print device audit log related to the operation with minimal data.
     *
     * @param operation Operation associated with the state change.
     * @param deviceId  Device ID to be logged.
     * @param userId    User ID associated with the device.
     */
    public void printAuditLog(Operation operation, String deviceId, String userId, String initiator) {

        JSONObject data = createAuditLogEntry(deviceId, userId, initiator);
        buildAuditLog(operation, data);
    }

    /**
     * Build audit log using the provided data.
     *
     * @param operation Operation to be logged.
     * @param data      Data to be logged.
     */
    private void buildAuditLog(Operation operation, JSONObject data) {

        AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(getInitiatorId(),
                LoggerUtils.getInitiatorType(getInitiatorId()),
                LoggerUtils.Initiator.System.name(),
                LogConstants.TARGET_DEVICE,
                operation.getLogAction()).
                data(jsonObjectToMap(data));
        triggerAuditLogEvent(auditLogBuilder);
    }

    /**
     * Create audit log data with minimal device information.
     *
     * @param deviceId Device ID to be logged.
     * @param userId   User ID associated with the device.
     * @return Audit log data.
     */
    private JSONObject createAuditLogEntry(String deviceId, String userId, String initiator) {

        JSONObject data = new JSONObject();
        data.put(LogConstants.DEVICE_ID_FIELD, deviceId != null ? deviceId : JSONObject.NULL);
        data.put(LogConstants.USER_ID_FIELD, userId != null ? userId : JSONObject.NULL);
        data.put(LogConstants.INITIATOR, initiator != null ? initiator : JSONObject.NULL);

        return data;
    }

    /**
     * To get the current user, who is doing the current task.
     *
     * @return Current logged-in user.
     */
    private String getUser() {

        String user = CarbonContext.getThreadLocalCarbonContext().getUsername();
        if (StringUtils.isNotEmpty(user)) {
            user = UserCoreUtil.addTenantDomainToEntry(user, CarbonContext.getThreadLocalCarbonContext()
                    .getTenantDomain());
        } else {
            user = CarbonConstants.REGISTRY_SYSTEM_USERNAME;
        }

        return user;
    }

    /**
     * Get the initiator for audit logs.
     *
     * @return Initiator id despite masking.
     */
    private String getInitiatorId() {

        String initiator = null;
        String username = MultitenantUtils.getTenantAwareUsername(getUser());
        String tenantDomain = MultitenantUtils.getTenantDomain(getUser());
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(tenantDomain)) {
            initiator = IdentityUtil.getInitiatorId(username, tenantDomain);
        }
        if (StringUtils.isBlank(initiator)) {
            if (username.equals(CarbonConstants.REGISTRY_SYSTEM_USERNAME)) {
                // If the initiator is wso2.system, we need not mask the username.
                return LoggerUtils.Initiator.System.name();
            }
            initiator = LoggerUtils.getMaskedContent(getUser());
        }

        return initiator;
    }

    /**
     * Device handler operations to be logged.
     */
    public enum Operation {
        REMOVE_DEVICE("remove-device");

        private final String logAction;

        Operation(String logAction) {

            this.logAction = logAction;
        }

        public String getLogAction() {

            return this.logAction;
        }
    }

    /**
     * Device handler related log constants.
     */
    private static class LogConstants {

        public static final String TARGET_DEVICE = "Device";
        public static final String DEVICE_ID_FIELD = "DeviceId";
        public static final String USER_ID_FIELD = "UserId";
        public static final String INITIATOR = "Initiator";
    }
}
