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

package org.wso2.carbon.identity.notification.push.device.handler.internal;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;

import static org.mockito.Mockito.mock;

/**
 * Unit tests for PushDeviceHandlerDataHolder.
 */
public class PushDeviceHandlerDataHolderTest {

    private PushDeviceHandlerDataHolder dataHolder;

    @BeforeMethod
    public void setUp() {

        dataHolder = PushDeviceHandlerDataHolder.getInstance();
        dataHolder.setConfigurationManager(null);
    }

    @AfterMethod
    public void tearDown() {

        dataHolder.setConfigurationManager(null);
    }

    @Test
    public void testGetInstance_ReturnsSingleton() {

        PushDeviceHandlerDataHolder instance1 = PushDeviceHandlerDataHolder.getInstance();
        PushDeviceHandlerDataHolder instance2 = PushDeviceHandlerDataHolder.getInstance();
        Assert.assertNotNull(instance1);
        Assert.assertSame(instance1, instance2, "getInstance must always return the same singleton.");
    }

    @Test
    public void testSetAndGetConfigurationManager_StoresAndRetrievesCorrectly() {

        ConfigurationManager mockManager = mock(ConfigurationManager.class);

        dataHolder.setConfigurationManager(mockManager);
        ConfigurationManager result = dataHolder.getConfigurationManager();

        Assert.assertSame(result, mockManager, "getConfigurationManager must return the same instance that was set.");
    }

    @Test
    public void testGetConfigurationManager_ReturnsNull_WhenNotSet() {

        ConfigurationManager result = dataHolder.getConfigurationManager();

        Assert.assertNull(result, "getConfigurationManager must return null when no manager has been set.");
    }

    @Test
    public void testSetConfigurationManager_AllowsNull() {

        ConfigurationManager mockManager = mock(ConfigurationManager.class);
        dataHolder.setConfigurationManager(mockManager);

        dataHolder.setConfigurationManager(null);

        Assert.assertNull(dataHolder.getConfigurationManager(),
                "setConfigurationManager(null) must clear the stored manager.");
    }

    @Test
    public void testSetConfigurationManager_AllowsReplacement() {

        ConfigurationManager firstManager = mock(ConfigurationManager.class);
        ConfigurationManager secondManager = mock(ConfigurationManager.class);

        dataHolder.setConfigurationManager(firstManager);
        dataHolder.setConfigurationManager(secondManager);

        Assert.assertSame(dataHolder.getConfigurationManager(), secondManager,
                "Second manager must replace the first one.");
    }
}
