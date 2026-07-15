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

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceIdentifier;
import org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants;
import org.wso2.carbon.identity.notification.push.device.handler.model.DeviceRegistrationNotificationChannelEnum;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unit tests for PushDefaultConfigResolverImpl.
 */
public class PushDefaultConfigResolverImplTest {

    private PushDefaultConfigResolverImpl pushDefaultConfigResolver;

    @BeforeMethod
    public void setUp() {

        pushDefaultConfigResolver = new PushDefaultConfigResolverImpl();
    }

    @Test
    public void testGetResourceIdentifier_ReturnsExpectedTypeAndName() {

        ResourceIdentifier identifier = pushDefaultConfigResolver.getResourceIdentifier();

        Assert.assertNotNull(identifier, "ResourceIdentifier should not be null.");
        Assert.assertEquals(identifier.getResourceType(), PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_TYPE);
        Assert.assertEquals(identifier.getResourceName(), PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_NAME);
    }

    @Test
    public void testGetDefaultConfigs_ReturnsDefaultPushDeviceMgtConfigs() {

        Resource resource = pushDefaultConfigResolver.getDefaultConfigs(
                PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_TYPE,
                PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_NAME);

        Assert.assertNotNull(resource, "Default config resource should not be null.");
        Assert.assertEquals(resource.getResourceName(), PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_NAME);
        Assert.assertEquals(resource.getResourceType(), PushDeviceHandlerConstants.PUSH_DEVICE_MGT_RESOURCE_TYPE);

        Map<String, String> attributes = resource.getAttributes().stream()
                .collect(Collectors.toMap(Attribute::getKey, Attribute::getValue));
        Assert.assertEquals(attributes.get(PushDeviceHandlerConstants.ATTR_ENABLE_MULTIPLE_DEVICE_ENROLLMENT), "false");
        Assert.assertEquals(attributes.get(PushDeviceHandlerConstants.ATTR_MAX_DEVICE_LIMIT), "2");
        Assert.assertEquals(
                attributes.get(PushDeviceHandlerConstants.ATTR_ENABLE_DEVICE_REGISTRATION_NOTIFICATIONS),
                "false");
        Assert.assertEquals(
                attributes.get(PushDeviceHandlerConstants.ATTR_DEVICE_REGISTRATION_NOTIFICATION_CHANNELS),
                DeviceRegistrationNotificationChannelEnum.EMAIL.name());
    }
}
