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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;

/**
 * Registration discovery data.
 */
public class RegistrationDiscoveryData implements Serializable {

    private static final long serialVersionUID = -2668070968345200524L;
    private String deviceId;
    private String username;
    private String host;
    private String tenantDomain;
    private String tenantPath;
    private String organizationId;
    private String organizationName;
    private String organizationPath;
    private String challenge;

    public RegistrationDiscoveryData() {

    }

    public String getDeviceId() {

        return deviceId;
    }

    public void setDeviceId(String deviceId) {

        this.deviceId = deviceId;
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

    public String getChallenge() {

        return challenge;
    }

    public void setChallenge(String challenge) {

        this.challenge = challenge;
    }

    public String getOrganizationName() {

        return organizationName;
    }

    public void setOrganizationName(String organizationName) {

        this.organizationName = organizationName;
    }

    public String getOrganizationId() {

        return organizationId;
    }

    public void setOrganizationId(String organizationId) {

        this.organizationId = organizationId;
    }

    public String getHost() {

        return host;
    }

    public void setHost(String host) {

        this.host = host;
    }

    public String getTenantPath() {

        return tenantPath;
    }

    public void setTenantPath(String tenantPath) {

        this.tenantPath = tenantPath;
    }

    public String getOrganizationPath() {

        return organizationPath;
    }

    public void setOrganizationPath(String organizationPath) {

        this.organizationPath = organizationPath;
    }

    public String buildJSON() {

        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }
}
