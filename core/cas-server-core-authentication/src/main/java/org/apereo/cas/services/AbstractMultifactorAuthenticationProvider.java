package org.apereo.cas.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;

import java.io.Serializable;

/**
 * The {@link AbstractMultifactorAuthenticationProvider} is responsible for
 * as the parent of all providers.
 *
 * @author Misagh Moayyed
 * @since 4.3
 */
public abstract class AbstractMultifactorAuthenticationProvider implements MultifactorAuthenticationProvider, Serializable {

    private static final long serialVersionUID = 4789727148134156909L;

    protected transient Logger logger = LoggerFactory.getLogger(this.getClass());

    private MultifactorAuthenticationProviderBypass bypassEvaluator;

    private String globalFailureMode;

    private String id;

    private int order;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getOrder() {
        return this.order;
    }


    public void setId(final String id) {
        this.id = id;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public void setGlobalFailureMode(final String globalFailureMode) {
        this.globalFailureMode = globalFailureMode;
    }

    @Override
    public final boolean supports(final Event e,
                                  final Authentication authentication,
                                  final RegisteredService registeredService) {
        if (e == null || !e.getId().matches(getId())) {
            logger.debug("Provided event id {} is not applicable to this provider identified by {}", e.getId(), getId());
            return false;
        }
        if (bypassEvaluator != null && !bypassEvaluator.isAuthenticationRequestHonored(
                authentication, registeredService, this)) {
            logger.debug("Request cannot be supported by provider {} as it's configured for bypass", getId());
            return false;
        }

        if (supportsInternal(e, authentication, registeredService)) {
            logger.debug("{} voted to support this authentication request", getClass().getSimpleName());
            return true;
        }

        logger.debug("{} voted does not support this authentication request", getClass().getSimpleName());
        return false;
    }

    /**
     * Determine internally if provider is able to support this authentication request
     * for multifactor, and account for bypass rules..
     *
     * @param e                 the event
     * @param authentication    the authentication
     * @param registeredService the registered service
     * @return the boolean
     */
    protected boolean supportsInternal(final Event e,
                                       final Authentication authentication,
                                       final RegisteredService registeredService) {
        return true;
    }

    @Override
    public boolean isAvailable(final RegisteredService service) throws AuthenticationException {
        RegisteredServiceMultifactorPolicy.FailureModes failureMode = RegisteredServiceMultifactorPolicy.FailureModes.CLOSED;
        final RegisteredServiceMultifactorPolicy policy = service.getMultifactorPolicy();
        if (policy != null) {
            failureMode = policy.getFailureMode();
            logger.debug("Multifactor failure mode for {} is defined as {}", service.getServiceId(), failureMode);
        } else if (StringUtils.isNotBlank(this.globalFailureMode)) {
            failureMode = RegisteredServiceMultifactorPolicy.FailureModes.valueOf(this.globalFailureMode);
            logger.debug("Using global multifactor failure mode for {} defined as {}", service.getServiceId(), failureMode);
        }

        if (failureMode != RegisteredServiceMultifactorPolicy.FailureModes.NONE) {
            if (isAvailable()) {
                return true;
            }
            if (failureMode == RegisteredServiceMultifactorPolicy.FailureModes.CLOSED) {
                logger.warn("{} could not be reached. Authentication shall fail for {}",
                        getClass().getSimpleName(), service.getServiceId());
                throw new AuthenticationException();
            }

            logger.warn("{} could not be reached. Since the authentication provider is configured for the "
                            + "failure mode of {} authentication will proceed without {} for service {}",
                    getClass().getSimpleName(), failureMode, getClass().getSimpleName(), service.getServiceId());
            return false;
        }
        logger.debug("Failure mode is set to {}. Assuming the provider is available.", failureMode);
        return true;
    }

    /**
     * Is provider available?
     *
     * @return the true/false
     */
    protected abstract boolean isAvailable();

    public void setBypassEvaluator(final MultifactorAuthenticationProviderBypass bypassEvaluator) {
        this.bypassEvaluator = bypassEvaluator;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final MultifactorAuthenticationProvider rhs = (MultifactorAuthenticationProvider) obj;
        return new EqualsBuilder()
                .append(this.getOrder(), rhs.getOrder())
                .append(this.getId(), rhs.getId())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getOrder())
                .append(getId())
                .toHashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean matches(final String identifier) {
        return getId().matches(identifier);
    }
}
