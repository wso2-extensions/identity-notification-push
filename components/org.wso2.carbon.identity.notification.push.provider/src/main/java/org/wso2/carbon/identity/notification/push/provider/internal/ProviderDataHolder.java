package org.wso2.carbon.identity.notification.push.provider.internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager;

/**
 * Push Provider Data Holder.
 */
public class ProviderDataHolder {

    private SecretManager secretManager;
    private SecretResolveManager secretResolveManager;
    private static ProviderDataHolder instance = new ProviderDataHolder();

    private ProviderDataHolder() {

    }

    @SuppressFBWarnings
    public static ProviderDataHolder getInstance() {

        return instance;
    }

    /**
     * Get Secret Manager.
     *
     * @return Secret Manager.
     */
    @SuppressFBWarnings
    public SecretManager getSecretManager() {

        return secretManager;
    }

    /**
     * Set Secret Manager.
     *
     * @param secretManager Secret Manager.
     */
    @SuppressFBWarnings
    public void setSecretManager(SecretManager secretManager) {

        this.secretManager = secretManager;
    }

    /**
     * Get Secret Resolve Manager.
     *
     * @return Secret Resolve Manager.
     */
    public SecretResolveManager getSecretResolveManager() {

        return secretResolveManager;
    }

    /**
     * Set Secret Resolve Manager.
     *
     * @param secretResolveManager Secret Resolve Manager.
     */
    public void setSecretResolveManager(SecretResolveManager secretResolveManager) {

        this.secretResolveManager = secretResolveManager;
    }
}
