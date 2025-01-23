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

package org.wso2.carbon.identity.notification.push.device.handler.dao;

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.database.utils.jdbc.NamedPreparedStatement;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants;
import org.wso2.carbon.identity.notification.push.device.handler.model.Device;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Optional;

/**
 * Unit tests for DeviceDAOImpl.
 */
public class DeviceDAOImplTest {

    public static final String REGISTER_DEVICE_TEST = "INSERT INTO IDN_PUSH_DEVICE_STORE (ID, USER_ID, DEVICE_NAME, " +
            "DEVICE_MODEL, DEVICE_TOKEN, DEVICE_HANDLE, PROVIDER, PUBLIC_KEY, TENANT_ID) VALUES " +
            "( ? ,  ? ,  ? ,  ? ,  ? ,  ? ,  ? ,  ? ,  ? )";
    public static final String UNREGISTER_DEVICE_TEST = "DELETE FROM IDN_PUSH_DEVICE_STORE WHERE ID =  ? ";
    public static final String EDIT_DEVICE_TEST = "UPDATE IDN_PUSH_DEVICE_STORE SET DEVICE_NAME =  ?  " +
            "DEVICE_TOKEN =  ?  WHERE ID =  ? ";
    private DeviceDAOImpl deviceDAO;

    @BeforeMethod
    public void setUp() {

        deviceDAO = new DeviceDAOImpl();
    }

    @Test
    public void testRegisterDevice() throws Exception {

        Device device = new Device("id", "did", "name", "model", "token", "handle", "provider", "key");
        int tenantId = 1;

        try (MockedStatic<IdentityDatabaseUtil> mockedDbUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {
            Connection connection = Mockito.mock(Connection.class);
            NamedPreparedStatement statement = Mockito.mock(NamedPreparedStatement.class);

            // Mock database utilities
            mockedDbUtil.when(() -> IdentityDatabaseUtil.getDBConnection(true)).thenReturn(connection);
            Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(statement);

            // Call the method under test
            deviceDAO.registerDevice(device, tenantId);

            // Capture and verify the SQL query
            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(connection).prepareStatement(queryCaptor.capture());
            Assert.assertEquals(queryCaptor.getValue(), REGISTER_DEVICE_TEST);

            // Verify parameter bindings
            Mockito.verify(statement).setString(1, device.getDeviceId());
            Mockito.verify(statement).setString(2, device.getUserId());
            Mockito.verify(statement).setString(3, device.getDeviceName());
            Mockito.verify(statement).setString(4, device.getDeviceModel());
            Mockito.verify(statement).setString(5, device.getDeviceToken());
            Mockito.verify(statement).setString(6, device.getDeviceHandle());
            Mockito.verify(statement).setString(7, device.getProvider());
            Mockito.verify(statement).setString(8, device.getPublicKey());
            Mockito.verify(statement).setInt(9, tenantId);

            // Verify execution and commit
            Mockito.verify(statement).executeUpdate();
            mockedDbUtil.verify(() -> IdentityDatabaseUtil.commitTransaction(connection));
        }
    }

    @Test
    public void testUnregisterDevice() throws Exception {

        String deviceId = "id";

        try (MockedStatic<IdentityDatabaseUtil> mockedDbUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {
            Connection connection = Mockito.mock(Connection.class);
            NamedPreparedStatement statement = Mockito.mock(NamedPreparedStatement.class);

            mockedDbUtil.when(() -> IdentityDatabaseUtil.getDBConnection(true)).thenReturn(connection);
            Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(statement);

            deviceDAO.unregisterDevice(deviceId);

            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(connection).prepareStatement(queryCaptor.capture());
            Assert.assertEquals(queryCaptor.getValue(), UNREGISTER_DEVICE_TEST);

            Mockito.verify(statement).setString(1, deviceId);
            Mockito.verify(statement).executeUpdate();
        }
    }

    @Test
    public void testEditDevice() throws Exception {

        String deviceId = "id";
        Device updatedDevice = new Device("id", "did", "name", "model", "token", "handle", "provider", "key");

        try (MockedStatic<IdentityDatabaseUtil> mockedDbUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {
            Connection connection = Mockito.mock(Connection.class);
            NamedPreparedStatement statement = Mockito.mock(NamedPreparedStatement.class);

            mockedDbUtil.when(() -> IdentityDatabaseUtil.getDBConnection(true)).thenReturn(connection);
            Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(statement);

            deviceDAO.editDevice(deviceId, updatedDevice);

            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(connection).prepareStatement(queryCaptor.capture());
            Assert.assertEquals(queryCaptor.getValue(), EDIT_DEVICE_TEST);

            Mockito.verify(statement).setString(1, updatedDevice.getDeviceName());
            Mockito.verify(statement).setString(2, updatedDevice.getDeviceToken());
            Mockito.verify(statement).setString(3, updatedDevice.getDeviceId());
            Mockito.verify(statement).executeUpdate();
        }
    }

    @Test
    public void testGetDevice() throws Exception {

        String deviceId = "id";
        Device device = new Device("id", "did", "name", "model", "token", "handle", "provider", "key");

        try (MockedStatic<IdentityDatabaseUtil> mockedDbUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {
            Connection connection = Mockito.mock(Connection.class);
            NamedPreparedStatement statement = Mockito.mock(NamedPreparedStatement.class);
            ResultSet resultSet = Mockito.mock(ResultSet.class);

            mockedDbUtil.when(() -> IdentityDatabaseUtil.getDBConnection(true)).thenReturn(connection);
            Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);
            Mockito.when(resultSet.next()).thenReturn(true);
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.ID))
                    .thenReturn(device.getDeviceId());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.USER_ID))
                    .thenReturn(device.getUserId());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_NAME))
                    .thenReturn(device.getDeviceName());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_MODEL))
                    .thenReturn(device.getDeviceModel());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_TOKEN))
                    .thenReturn(device.getDeviceToken());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_HANDLE))
                    .thenReturn(device.getDeviceHandle());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.PUBLIC_KEY))
                    .thenReturn(device.getPublicKey());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.PROVIDER))
                    .thenReturn(device.getProvider());

            Optional<Device> result = deviceDAO.getDevice(deviceId);

            Assert.assertTrue(result.isPresent());
            Assert.assertEquals(result.get().getDeviceId(), device.getDeviceId());
        }
    }

    @Test
    public void testGetDeviceByUserId() throws Exception {

        String userId = "user";
        int tenantId = 1;
        Device device = new Device("id", "did", "name", "model", "token", "handle", "provider", "key");

        try (MockedStatic<IdentityDatabaseUtil> mockedDbUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {
            Connection connection = Mockito.mock(Connection.class);
            NamedPreparedStatement statement = Mockito.mock(NamedPreparedStatement.class);
            ResultSet resultSet = Mockito.mock(ResultSet.class);

            mockedDbUtil.when(() -> IdentityDatabaseUtil.getDBConnection(true)).thenReturn(connection);
            Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);
            Mockito.when(resultSet.next()).thenReturn(true);
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.ID))
                    .thenReturn(device.getDeviceId());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.USER_ID))
                    .thenReturn(device.getUserId());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_NAME))
                    .thenReturn(device.getDeviceName());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_MODEL))
                    .thenReturn(device.getDeviceModel());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_TOKEN))
                    .thenReturn(device.getDeviceToken());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_HANDLE))
                    .thenReturn(device.getDeviceHandle());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.PUBLIC_KEY))
                    .thenReturn(device.getPublicKey());
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.PROVIDER))
                    .thenReturn(device.getProvider());

            Optional<Device> result = deviceDAO.getDeviceByUserId(userId, tenantId);

            Assert.assertTrue(result.isPresent());
            Assert.assertEquals(result.get().getDeviceId(), device.getDeviceId());
        }
    }

    @Test
    public void testGetPublicKey() throws Exception {

        String deviceId = "id";
        String publicKey = "key";

        try (MockedStatic<IdentityDatabaseUtil> mockedDbUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {
            Connection connection = Mockito.mock(Connection.class);
            NamedPreparedStatement statement = Mockito.mock(NamedPreparedStatement.class);
            ResultSet resultSet = Mockito.mock(ResultSet.class);

            mockedDbUtil.when(() -> IdentityDatabaseUtil.getDBConnection(true)).thenReturn(connection);
            Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);
            Mockito.when(resultSet.next()).thenReturn(true);
            Mockito.when(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.PUBLIC_KEY)).thenReturn(publicKey);

            Optional<String> result = deviceDAO.getPublicKey(deviceId);

            Assert.assertTrue(result.isPresent());
            Assert.assertEquals(result.get(), publicKey);
        }
    }
}
