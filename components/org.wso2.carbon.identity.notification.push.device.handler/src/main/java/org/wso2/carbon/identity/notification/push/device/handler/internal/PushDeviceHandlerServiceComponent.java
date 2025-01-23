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

package org.wso2.carbon.identity.notification.push.device.handler.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.notification.push.device.handler.DeviceHandlerService;
import org.wso2.carbon.identity.notification.push.device.handler.DeviceRegistrationContextManager;
import org.wso2.carbon.identity.notification.push.device.handler.dao.DeviceDAO;
import org.wso2.carbon.identity.notification.push.device.handler.dao.DeviceDAOImpl;
import org.wso2.carbon.identity.notification.push.device.handler.impl.DeviceHandlerServiceImpl;
import org.wso2.carbon.identity.notification.push.device.handler.impl.DeviceRegistrationContextManagerImpl;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementService;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * Push Device Handler Service Component.
 */
@Component(
        name = "org.wso2.carbon.identity.notification.push.device.handler",
        immediate = true
)
public class PushDeviceHandlerServiceComponent {

    private static final Log LOG = LogFactory.getLog(PushDeviceHandlerServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            DeviceRegistrationContextManager deviceRegistrationContextManager =
                    new DeviceRegistrationContextManagerImpl();
            DeviceDAO deviceDAO = new DeviceDAOImpl();
            DeviceHandlerService deviceHandlerService =
                    new DeviceHandlerServiceImpl(deviceRegistrationContextManager, deviceDAO);
            context.getBundleContext().registerService(
                    DeviceHandlerService.class.getName(), deviceHandlerService, null);
        } catch (Throwable e) {
            LOG.error("Error occurred while activating Push Device Handler Service Component", e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Push Device Handler Service Component bundle activated successfully.");
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Push Device Handler Service Component bundle is deactivated.");
        }
    }

    @Reference(
            name = "organization.management.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        PushDeviceHandlerDataHolder.getInstance().setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        PushDeviceHandlerDataHolder.getInstance().setOrganizationManager(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.notification.sender.tenant.config",
            service = NotificationSenderManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetNotificationSenderManagementService"
    )
    protected void setNotificationSenderManagementService(
            NotificationSenderManagementService notificationSenderManagementService) {

        PushDeviceHandlerDataHolder.getInstance()
                .setNotificationSenderManagementService(notificationSenderManagementService);
    }

    protected void unsetNotificationSenderManagementService(
            NotificationSenderManagementService notificationSenderManagementService) {

        PushDeviceHandlerDataHolder.getInstance().setNotificationSenderManagementService(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.notification.push.provider",
            service = PushProvider.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetProvider"
    )
    protected void setProvider(PushProvider provider) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Provider: " + provider.getName() + " is registered.");
        }
        PushDeviceHandlerDataHolder.getInstance().addPushProvider(provider.getName(), provider);
    }

    protected void unsetProvider(PushProvider provider) {

        PushDeviceHandlerDataHolder.getInstance().removePushProvider(provider.getName());
    }
}
