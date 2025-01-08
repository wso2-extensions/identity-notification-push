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

package org.wso2.carbon.identity.notification.push.device.handler.model;

import java.io.Serializable;

/**
 * This class represents the context of a device registration request.
 */
public class DeviceRegistrationContext implements Serializable {

    private static final long serialVersionUID = -6514537036929825244L;
    private String challenge;
    private String username;
    private String tenantDomain;
    private boolean registered;
    private boolean isForceRegistration;

    public DeviceRegistrationContext(String challenge, String username, String tenantDomain, boolean registered,
                                     boolean isForceRegistration) {

        this.challenge = challenge;
        this.username = username;
        this.tenantDomain = tenantDomain;
        this.registered = registered;
        this.isForceRegistration = isForceRegistration;
    }

    public String getChallenge() {

        return challenge;
    }

    public void setChallenge(String challenge) {

        this.challenge = challenge;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public String getTenantDomain() {

        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {

        this.tenantDomain = tenantDomain;
    }

    public boolean isRegistered() {

        return registered;
    }

    public void setRegistered(boolean registered) {

        this.registered = registered;
    }

    public boolean isForceRegistration() {

        return isForceRegistration;
    }

    public void setForceRegistration(boolean forceRegistration) {

        isForceRegistration = forceRegistration;
    }
}
