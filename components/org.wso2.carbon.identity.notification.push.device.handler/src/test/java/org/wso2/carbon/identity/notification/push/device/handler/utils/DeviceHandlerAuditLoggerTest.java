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

import org.json.JSONObject;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit test class for DeviceHandlerAuditLogger class.
 */
public class DeviceHandlerAuditLoggerTest {

    private DeviceHandlerAuditLogger auditLogger;
    private CarbonContext carbonContext;

    private MockedStatic<CarbonContext> mockedCarbonContext;
    private MockedStatic<UserCoreUtil> mockedUserCoreUtil;
    private MockedStatic<MultitenantUtils> mockedMultitenantUtils;
    private MockedStatic<IdentityUtil> mockedIdentityUtil;
    private MockedStatic<LoggerUtils> mockedLoggerUtils;

    @BeforeMethod
    public void setUp() {

        System.setProperty("carbon.home", ".");
        MockitoAnnotations.openMocks(this);
        auditLogger = new DeviceHandlerAuditLogger();

        mockedCarbonContext = mockStatic(CarbonContext.class);
        mockedUserCoreUtil = mockStatic(UserCoreUtil.class);
        mockedMultitenantUtils = mockStatic(MultitenantUtils.class);
        mockedIdentityUtil = mockStatic(IdentityUtil.class);
        mockedLoggerUtils = mockStatic(LoggerUtils.class);

        carbonContext = mock(CarbonContext.class);
        mockedCarbonContext.when(CarbonContext::getThreadLocalCarbonContext).thenReturn(carbonContext);
        when(carbonContext.getUsername()).thenReturn("testUser");
        when(carbonContext.getTenantDomain()).thenReturn("carbon.super");
        mockedIdentityUtil.when(() -> IdentityUtil.getInitiatorId("testUser", "carbon.super")).
                thenReturn("initiator-id-test");
    }

    @AfterMethod
    public void tearDown() {
        mockedCarbonContext.close();
        mockedUserCoreUtil.close();
        mockedMultitenantUtils.close();
        mockedIdentityUtil.close();
        mockedLoggerUtils.close();
    }

    /**
     * Test the private method 'getUser' for a regular, tenant-aware user.
     */
    @Test
    public void testGetUserRegularUser() throws Exception {
        // Act: Invoke the private method using reflection.
        Method getUserMethod = DeviceHandlerAuditLogger.class.getDeclaredMethod("getUser");
        getUserMethod.setAccessible(true);
        String result = (String) getUserMethod.invoke(auditLogger);

        // Assert
        Assert.assertEquals(result, "admin@carbon.super");
    }

    /**
     * Test the private method 'getUser' for the system user.
     */
    @Test
    public void testGetUserWithSystemUser() throws Exception {
        // Act
        Method getUserMethod = DeviceHandlerAuditLogger.class.getDeclaredMethod("getUser");
        getUserMethod.setAccessible(true);
        String result = (String) getUserMethod.invoke(auditLogger);

        // Assert
        Assert.assertEquals(result, CarbonConstants.REGISTRY_SYSTEM_USERNAME);
    }

    /**
     * Test the private method 'createAuditLogEntry' with valid data.
     */
    @Test
    public void testCreateAuditLogEntryWithValidData() throws Exception {
        // Arrange
        String deviceId = "device-123";
        String userId = "user-456";
        String initiator = "admin";

        // Act
        Method createAuditLogEntryMethod = DeviceHandlerAuditLogger.class.getDeclaredMethod("createAuditLogEntry",
                String.class, String.class, String.class);
        createAuditLogEntryMethod.setAccessible(true);
        JSONObject result = (JSONObject) createAuditLogEntryMethod.invoke(auditLogger, deviceId, userId, initiator);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getString("DeviceId"), deviceId);
        Assert.assertEquals(result.getString("UserId"), userId);
        Assert.assertEquals(result.getString("Initiator"), initiator);
    }

    /**
     * Test the private method 'createAuditLogEntry' with null data to ensure it's handled correctly.
     */
    @Test
    public void testCreateAuditLogEntryWithNullData() throws Exception {
        // Act
        Method createAuditLogEntryMethod = DeviceHandlerAuditLogger.class.getDeclaredMethod("createAuditLogEntry",
                String.class, String.class, String.class);
        createAuditLogEntryMethod.setAccessible(true);
        JSONObject result = (JSONObject) createAuditLogEntryMethod.invoke(auditLogger, null, null, null);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isNull("DeviceId"));
        Assert.assertTrue(result.isNull("UserId"));
        Assert.assertTrue(result.isNull("Initiator"));
    }
}
