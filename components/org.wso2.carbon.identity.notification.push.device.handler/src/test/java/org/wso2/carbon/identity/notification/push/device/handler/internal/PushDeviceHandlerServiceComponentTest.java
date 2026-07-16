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

import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for PushDeviceHandlerServiceComponent — covering the ConfigurationManager OSGi lifecycle methods.
 */
public class PushDeviceHandlerServiceComponentTest {

    private PushDeviceHandlerServiceComponent serviceComponent;
    private PushDeviceHandlerDataHolder mockDataHolderInstance;
    private MockedStatic<PushDeviceHandlerDataHolder> mockedDataHolder;

    @BeforeMethod
    public void setUp() {

        serviceComponent = new PushDeviceHandlerServiceComponent();
        mockDataHolderInstance = mock(PushDeviceHandlerDataHolder.class);
        mockedDataHolder = mockStatic(PushDeviceHandlerDataHolder.class);
        mockedDataHolder.when(PushDeviceHandlerDataHolder::getInstance).thenReturn(mockDataHolderInstance);
    }

    @AfterMethod
    public void tearDown() {

        mockedDataHolder.close();
    }

    @Test
    public void testSetConfigurationManager_DelegatesToDataHolder() {

        ConfigurationManager mockManager = mock(ConfigurationManager.class);

        serviceComponent.setConfigurationManager(mockManager);

        verify(mockDataHolderInstance).setConfigurationManager(mockManager);
    }

    @Test
    public void testUnsetConfigurationManager_ClearsDataHolderEntry() {

        ConfigurationManager mockManager = mock(ConfigurationManager.class);

        serviceComponent.unsetConfigurationManager(mockManager);

        verify(mockDataHolderInstance).setConfigurationManager(null);
    }

    @Test
    public void testSetThenUnsetConfigurationManager_LeavesDataHolderNull() {

        ConfigurationManager mockManager = mock(ConfigurationManager.class);

        serviceComponent.setConfigurationManager(mockManager);
        serviceComponent.unsetConfigurationManager(mockManager);

        verify(mockDataHolderInstance).setConfigurationManager(mockManager);
        verify(mockDataHolderInstance).setConfigurationManager(null);
    }
}
