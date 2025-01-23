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

import org.wso2.carbon.database.utils.jdbc.NamedPreparedStatement;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants;
import org.wso2.carbon.identity.notification.push.device.handler.exception.PushDeviceHandlerServerException;
import org.wso2.carbon.identity.notification.push.device.handler.model.Device;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.SQLQueries.EDIT_DEVICE;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.SQLQueries.GET_DEVICE_BY_DEVICE_ID;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.SQLQueries.GET_DEVICE_BY_USER_ID;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.SQLQueries.GET_PUBLIC_KEY_BY_ID;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.SQLQueries.REGISTER_DEVICE;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.SQLQueries.UNREGISTER_DEVICE;

/**
 * Implementation of DeviceDAO interface.
 */
public class DeviceDAOImpl implements DeviceDAO {

    @Override
    public void registerDevice(Device device, int tenantId) throws PushDeviceHandlerServerException {

        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection, REGISTER_DEVICE)) {
            statement.setString(PushDeviceHandlerConstants.ColumnNames.ID, device.getDeviceId());
            statement.setString(PushDeviceHandlerConstants.ColumnNames.USER_ID, device.getUserId());
            statement.setString(PushDeviceHandlerConstants.ColumnNames.DEVICE_NAME, device.getDeviceName());
            statement.setString(PushDeviceHandlerConstants.ColumnNames.DEVICE_MODEL, device.getDeviceModel());
            statement.setString(PushDeviceHandlerConstants.ColumnNames.DEVICE_TOKEN, device.getDeviceToken());
            statement.setString(PushDeviceHandlerConstants.ColumnNames.DEVICE_HANDLE, device.getDeviceHandle());
            statement.setString(PushDeviceHandlerConstants.ColumnNames.PUBLIC_KEY, device.getPublicKey());
            statement.setString(PushDeviceHandlerConstants.ColumnNames.PROVIDER, device.getProvider());
            statement.setInt(PushDeviceHandlerConstants.ColumnNames.TENANT_ID, tenantId);
            statement.executeUpdate();
            IdentityDatabaseUtil.commitTransaction(connection);
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw new PushDeviceHandlerServerException("Error occurred while registering the device.", e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    @Override
    public void unregisterDevice(String deviceId) throws PushDeviceHandlerServerException {

        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection, UNREGISTER_DEVICE)) {
            statement.setString(PushDeviceHandlerConstants.ColumnNames.ID, deviceId);
            statement.executeUpdate();
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw new PushDeviceHandlerServerException("Error occurred while unregistering the device.", e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    @Override
    public void editDevice(String deviceId, Device updatedDevice) throws PushDeviceHandlerServerException {

        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection, EDIT_DEVICE)) {
            statement.setString(PushDeviceHandlerConstants.ColumnNames.DEVICE_NAME, updatedDevice.getDeviceName());
            statement.setString(PushDeviceHandlerConstants.ColumnNames.DEVICE_TOKEN, updatedDevice.getDeviceToken());
            statement.setString(PushDeviceHandlerConstants.ColumnNames.ID, updatedDevice.getDeviceId());
            statement.executeUpdate();
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw new PushDeviceHandlerServerException("Error occurred while editing the device.", e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    @Override
    public Optional<Device> getDevice(String deviceId) throws PushDeviceHandlerServerException {

        Device device = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection, GET_DEVICE_BY_DEVICE_ID)) {
            statement.setString(PushDeviceHandlerConstants.ColumnNames.ID, deviceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    device = new Device();
                    device.setDeviceId(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.ID));
                    device.setUserId(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.USER_ID));
                    device.setDeviceName(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_NAME));
                    device.setDeviceModel(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_MODEL));
                    device.setDeviceToken(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_TOKEN));
                    device.setDeviceHandle(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_HANDLE));
                    device.setPublicKey(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.PUBLIC_KEY));
                    device.setProvider(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.PROVIDER));
                }
            }
        } catch (SQLException e) {
            throw new PushDeviceHandlerServerException("Error occurred while retrieving the device.", e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }

        return Optional.ofNullable(device);
    }

    @Override
    public Optional<Device> getDeviceByUserId(String userId, int tenantId) throws PushDeviceHandlerServerException {

        Device device = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection, GET_DEVICE_BY_USER_ID)) {
            statement.setString(PushDeviceHandlerConstants.ColumnNames.USER_ID, userId);
            statement.setInt(PushDeviceHandlerConstants.ColumnNames.TENANT_ID, tenantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    device = new Device();
                    device.setDeviceId(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.ID));
                    device.setUserId(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.USER_ID));
                    device.setDeviceName(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_NAME));
                    device.setDeviceModel(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_MODEL));
                    device.setDeviceToken(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_TOKEN));
                    device.setDeviceHandle(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.DEVICE_HANDLE));
                    device.setPublicKey(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.PUBLIC_KEY));
                    device.setProvider(resultSet.getString(PushDeviceHandlerConstants.ColumnNames.PROVIDER));
                }
            }
        } catch (SQLException e) {
            throw new PushDeviceHandlerServerException("Error occurred while retrieving the device.", e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }

        return Optional.ofNullable(device);
    }

    @Override
    public Optional<String> getPublicKey(String deviceId) throws PushDeviceHandlerServerException {

        String publicKey = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection, GET_PUBLIC_KEY_BY_ID)) {
            statement.setString(PushDeviceHandlerConstants.ColumnNames.ID, deviceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    publicKey = resultSet.getString(PushDeviceHandlerConstants.ColumnNames.PUBLIC_KEY);
                }
            }
        } catch (SQLException e) {
            throw new PushDeviceHandlerServerException("Error occurred while retrieving the public key.", e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }

        return Optional.ofNullable(publicKey);
    }
}
