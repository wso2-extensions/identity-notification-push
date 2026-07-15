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

package org.wso2.carbon.identity.notification.push.device.handler.constant;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG;
import static org.wso2.carbon.identity.notification.push.device.handler.constant.PushDeviceHandlerConstants.ErrorMessages.ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG;

/**
 * Unit tests for PushDeviceHandlerConstants — covering newly added error codes.
 */
public class PushDeviceHandlerConstantsTest {

    @Test
    public void testErrorCodeGettingPushDeviceConfig_HasCorrectCode() {

        Assert.assertEquals(ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG.getCode(), "PDH-15014");
    }

    @Test
    public void testErrorCodeGettingPushDeviceConfig_HasNonEmptyMessage() {

        Assert.assertNotNull(ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG.getMessage());
        Assert.assertFalse(ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG.getMessage().isEmpty());
    }

    @Test
    public void testErrorCodeAddingPushDeviceConfig_HasCorrectCode() {

        Assert.assertEquals(ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG.getCode(), "PDH-15015");
    }

    @Test
    public void testErrorCodeAddingPushDeviceConfig_HasNonEmptyMessage() {

        Assert.assertNotNull(ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG.getMessage());
        Assert.assertFalse(ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG.getMessage().isEmpty());
    }

    @Test
    public void testErrorCodes_ToString_ContainsCodeAndMessage() {

        String gettingConfigStr = ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG.toString();
        Assert.assertTrue(gettingConfigStr.contains("PDH-15014"),
                "toString must include the error code.");
        Assert.assertTrue(gettingConfigStr.contains(ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG.getMessage()),
                "toString must include the error message.");

        String addingConfigStr = ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG.toString();
        Assert.assertTrue(addingConfigStr.contains("PDH-15015"),
                "toString must include the error code.");
        Assert.assertTrue(addingConfigStr.contains(ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG.getMessage()),
                "toString must include the error message.");
    }

    @Test
    public void testErrorCodes_AreDistinct() {

        Assert.assertNotEquals(ERROR_CODE_GETTING_PUSH_DEVICE_CONFIG.getCode(),
                ERROR_CODE_ADDING_PUSH_DEVICE_CONFIG.getCode(),
                "The two new error codes must have different code values.");
    }
}
