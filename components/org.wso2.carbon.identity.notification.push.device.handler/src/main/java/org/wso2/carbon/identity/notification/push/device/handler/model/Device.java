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
 * This class represents a device that receives push notifications.
 */
public class Device implements Serializable {

    private static final long serialVersionUID = 1270075422696486852L;
    private String userId;
    private String deviceId;
    private String deviceName;
    private String deviceModel;
    private String deviceToken;
    private String deviceHandle;
    private String provider;
    private String publicKey;

    public Device() {

    }

    public Device(String userId, String deviceId, String deviceName, String deviceModel, String deviceToken,
                  String deviceHandle, String provider, String publicKey) {

        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceModel = deviceModel;
        this.deviceToken = deviceToken;
        this.deviceHandle = deviceHandle;
        this.provider = provider;
        this.publicKey = publicKey;
    }

    public void setUserId(String userId) {

        this.userId = userId;
    }

    public void setDeviceId(String deviceId) {

        this.deviceId = deviceId;
    }

    public String getUserId() {

        return userId;
    }

    public String getDeviceId() {

        return deviceId;
    }

    public String getDeviceToken() {

        return deviceToken;
    }

    public String getProvider() {

        return provider;
    }

    public String getPublicKey() {

        return publicKey;
    }

    public String getDeviceName() {

        return deviceName;
    }

    public String getDeviceModel() {

        return deviceModel;
    }

    public void setDeviceName(String deviceName) {

        this.deviceName = deviceName;
    }

    public void setDeviceModel(String deviceModel) {

        this.deviceModel = deviceModel;
    }

    public void setDeviceToken(String deviceToken) {

        this.deviceToken = deviceToken;
    }

    public void setProvider(String provider) {

        this.provider = provider;
    }

    public void setPublicKey(String publicKey) {

        this.publicKey = publicKey;
    }

    public String getDeviceHandle() {

        return deviceHandle;
    }

    public void setDeviceHandle(String deviceHandle) {

        this.deviceHandle = deviceHandle;
    }
}
