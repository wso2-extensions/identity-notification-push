package org.wso2.carbon.identity.notification.push.provider.internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.push.provider.impl.FCMPushProvider;
import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager;

/**
 * Push Provider Service Component.
 */
@Component(
        name = "org.wso2.carbon.identity.notification.push.provider",
        immediate = true
)
@SuppressFBWarnings
public class ProviderServiceComponent {

    private static final Log LOG = LogFactory.getLog(ProviderServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            context.getBundleContext().registerService(PushProvider.class.getName(), new FCMPushProvider(), null);
        } catch (Throwable e) {
            LOG.error("Error occurred while activating Push Provider Service Component", e);
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Push Provider Service Component bundle activated successfully.");
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Push Provider Service Component bundle is deactivated.");
        }
    }

    @Reference(
            name = "org.wso2.carbon.identity.secret.mgt.core.SecretManager",
            service = SecretManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetSecretManager"
    )
    private void setSecretManager(SecretManager secretManager) {

        ProviderDataHolder.getInstance().setSecretManager(secretManager);
    }

    private void unsetSecretManager(SecretManager secretManager) {

        ProviderDataHolder.getInstance().setSecretManager(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager",
            service = SecretResolveManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetSecretResolveManager"
    )
    private void setSecretResolveManager(SecretResolveManager secretResolveManager) {

        ProviderDataHolder.getInstance().setSecretResolveManager(secretResolveManager);
    }

    private void unsetSecretResolveManager(SecretResolveManager secretResolveManager) {

        ProviderDataHolder.getInstance().setSecretResolveManager(null);
    }


}
