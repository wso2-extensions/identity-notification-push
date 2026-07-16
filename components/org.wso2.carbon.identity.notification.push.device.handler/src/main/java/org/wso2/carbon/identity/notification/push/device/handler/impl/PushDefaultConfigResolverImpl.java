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

package org.wso2.carbon.identity.notification.push.device.handler.impl;


import org.wso2.carbon.identity.configuration.mgt.core.DefaultConfigResolver;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceIdentifier;
import org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants;
import org.wso2.carbon.identity.notification.push.device.handler.utils.PushDeviceConfigManager;

/**
 * Default config resolver for push device management configurations. Serves the default
 * device management configs when no resource is available in the configuration store.
 */
public class PushDefaultConfigResolverImpl implements DefaultConfigResolver {

    @Override
    public ResourceIdentifier getResourceIdentifier() {

        return new ResourceIdentifier(
                PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_TYPE,
                PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_NAME);
    }

    @Override
    public Resource getDefaultConfigs(
            String resourceTypeName, String resourceName) {

        return PushDeviceConfigManager.getDefaultPushDeviceMgtConfigs();
    }
}
