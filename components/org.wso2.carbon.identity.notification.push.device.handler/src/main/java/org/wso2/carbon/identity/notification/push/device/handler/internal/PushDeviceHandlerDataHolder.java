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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.sender.tenant.config.NotificationSenderManagementService;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.HashMap;
import java.util.Map;

/**
 * Push device handler data holder.
 */
@SuppressFBWarnings
public class PushDeviceHandlerDataHolder {

    private RealmService realmService;
    private OrganizationManager organizationManager;
    private NotificationSenderManagementService notificationSenderManagementService;
    private final Map<String, PushProvider> pushNotificationProviders = new HashMap<>();

    private static PushDeviceHandlerDataHolder instance = new PushDeviceHandlerDataHolder();

    public static PushDeviceHandlerDataHolder getInstance() {

        return instance;
    }

    /**
     * Get the RealmService.
     *
     * @return RealmService.
     */
    public RealmService getRealmService() {

        return realmService;
    }

    /**
     * Set the RealmService.
     *
     * @param realmService RealmService.
     */
    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    /**
     * Get the OrganizationManager.
     *
     * @return OrganizationManager.
     */
    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    /**
     * Set the OrganizationManager.
     *
     * @param organizationManager OrganizationManager.
     */
    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    /**
     * Get the NotificationSenderManagementService.
     *
     * @return NotificationSenderManagementService.
     */
    public NotificationSenderManagementService getNotificationSenderManagementService() {

        return notificationSenderManagementService;
    }

    /**
     * Set the NotificationSenderManagementService.
     *
     * @param notificationSenderManagementService NotificationSenderManagementService.
     */
    public void setNotificationSenderManagementService(
            NotificationSenderManagementService notificationSenderManagementService) {

        this.notificationSenderManagementService = notificationSenderManagementService;
    }

    /**
     * Add a push notification provider.
     *
     * @param providerName Name of the provider.
     * @param provider     {@link org.wso2.carbon.identity.notification.push.provider.PushProvider} instance.
     */
    public void addPushProvider(String providerName, PushProvider provider) {

        pushNotificationProviders.put(providerName, provider);
    }

    /**
     * Remove a push notification provider.
     *
     * @param providerName Name of the provider.
     */
    public void removePushProvider(String providerName) {

        pushNotificationProviders.remove(providerName);
    }

    /**
     * Get a push notification provider.
     *
     * @param providerName Name of the provider.
     * @return {@link org.wso2.carbon.identity.notification.push.provider.PushProvider} instance.
     */
    public PushProvider getPushProvider(String providerName) {

        return pushNotificationProviders.get(providerName);
    }
}
