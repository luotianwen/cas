package org.apereo.cas.authentication;

import org.apereo.cas.authentication.principal.DefaultPrincipalFactory;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServicesManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.test.annotation.DirtiesContext;

import javax.security.auth.login.FailedLoginException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link PolicyBasedAuthenticationManager}.
 *
 * @author Marvin S. Addison
 * @since 4.0.0
 */
@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
public class PolicyBasedAuthenticationManagerTests {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final AuthenticationTransaction transaction = AuthenticationTransaction.wrap(CoreAuthenticationTestUtils.getService(),
            mock(Credential.class), mock(Credential.class));

    @Test
    public void verifyAuthenticateAnySuccess() throws Exception {
        final Map<AuthenticationHandler, PrincipalResolver> map = new HashMap<>();
        map.put(newMockHandler(true), null);
        map.put(newMockHandler(false), null);

        final PolicyBasedAuthenticationManager manager = new PolicyBasedAuthenticationManager(map, mockServicesManager());
        final Authentication auth = manager.authenticate(transaction);
        assertEquals(1, auth.getSuccesses().size());
        assertEquals(2, auth.getCredentials().size());
    }

    @Test
    public void verifyAuthenticateAnyButTryAllSuccess() throws Exception {
        final Map<AuthenticationHandler, PrincipalResolver> map = new HashMap<>();
        map.put(newMockHandler(true), null);
        map.put(newMockHandler(false), null);
        final PolicyBasedAuthenticationManager manager = new PolicyBasedAuthenticationManager(map,
                mockServicesManager(),
                new AnyAuthenticationPolicy(true));
        final Authentication auth = manager.authenticate(transaction);
        assertEquals(1, auth.getSuccesses().size());
        assertEquals(1, auth.getFailures().size());
        assertEquals(2, auth.getCredentials().size());
    }

    protected ServicesManager mockServicesManager() {
        final ServicesManager svc = mock(ServicesManager.class);
        final RegisteredService reg = CoreAuthenticationTestUtils.getRegisteredService();
        when(svc.findServiceBy(any(Service.class))).thenReturn(reg);
        when(svc.getAllServices()).thenReturn(Collections.singletonList(reg));
        return svc;
    }

    @Test
    public void verifyAuthenticateAnyFailure() throws Exception {
        final Map<AuthenticationHandler, PrincipalResolver> map = new LinkedHashMap<>();
        map.put(newMockHandler(false), null);
        map.put(newMockHandler(false), null);

        final PolicyBasedAuthenticationManager manager = new PolicyBasedAuthenticationManager(map, mockServicesManager());

        this.thrown.expect(AuthenticationException.class);
        this.thrown.expectMessage("2 errors, 0 successes");

        manager.authenticate(transaction);

        fail("Should have thrown authentication exception");
    }

    @Test
    public void verifyAuthenticateAllSuccess() throws Exception {
        final Map<AuthenticationHandler, PrincipalResolver> map = new LinkedHashMap<>();
        map.put(newMockHandler(true), null);
        map.put(newMockHandler(true), null);

        final PolicyBasedAuthenticationManager manager = new PolicyBasedAuthenticationManager(map,
                mockServicesManager(),
                new AllAuthenticationPolicy());
        final Authentication auth = manager.authenticate(transaction);
        assertEquals(2, auth.getSuccesses().size());
        assertEquals(0, auth.getFailures().size());
        assertEquals(2, auth.getCredentials().size());
    }

    @Test
    public void verifyAuthenticateAllFailure() throws Exception {
        final Map<AuthenticationHandler, PrincipalResolver> map = new LinkedHashMap<>();
        map.put(newMockHandler(false), null);
        map.put(newMockHandler(false), null);

        final PolicyBasedAuthenticationManager manager = new PolicyBasedAuthenticationManager(map,
                mockServicesManager(),
                new AllAuthenticationPolicy());

        this.thrown.expect(AuthenticationException.class);
        this.thrown.expectMessage("2 errors, 0 successes");

        manager.authenticate(transaction);

        fail("Should have thrown authentication exception");
    }

    @Test
    public void verifyAuthenticateRequiredHandlerSuccess() throws Exception {
        final Map<AuthenticationHandler, PrincipalResolver> map = new LinkedHashMap<>();
        map.put(newMockHandler("HandlerA", true), null);
        map.put(newMockHandler("HandlerB", false), null);

        final PolicyBasedAuthenticationManager manager = new PolicyBasedAuthenticationManager(map,
                null,
                new RequiredHandlerAuthenticationPolicy("HandlerA"));

        final Authentication auth = manager.authenticate(transaction);
        assertEquals(1, auth.getSuccesses().size());
        assertEquals(0, auth.getFailures().size());
        assertEquals(2, auth.getCredentials().size());
    }

    @Test
    public void verifyAuthenticateRequiredHandlerFailure() throws Exception {
        final Map<AuthenticationHandler, PrincipalResolver> map = new LinkedHashMap<>();
        map.put(newMockHandler("HandlerA", true), null);
        map.put(newMockHandler("HandlerB", false), null);

        final PolicyBasedAuthenticationManager manager = new PolicyBasedAuthenticationManager(map,
                mockServicesManager(),
                new RequiredHandlerAuthenticationPolicy("HandlerB"));

        this.thrown.expect(AuthenticationException.class);
        this.thrown.expectMessage("1 errors, 1 successes");

        manager.authenticate(transaction);

        fail("Should have thrown AbstractAuthenticationException");
    }

    @Test
    public void verifyAuthenticateRequiredHandlerTryAllSuccess() throws Exception {
        final Map<AuthenticationHandler, PrincipalResolver> map = new LinkedHashMap<>();
        map.put(newMockHandler("HandlerA", true), null);
        map.put(newMockHandler("HandlerB", false), null);

        final PolicyBasedAuthenticationManager manager = new PolicyBasedAuthenticationManager(map,
                mockServicesManager(),
                new RequiredHandlerAuthenticationPolicy("HandlerA", true));

        final Authentication auth = manager.authenticate(transaction);
        assertEquals(1, auth.getSuccesses().size());
        assertEquals(1, auth.getFailures().size());
        assertEquals(2, auth.getCredentials().size());
    }

    /**
     * Creates a new mock authentication handler that either successfully validates all credentials or fails to
     * validate all credentials.
     *
     * @param success True to authenticate all credentials, false to fail all credentials.
     * @return New mock authentication handler instance.
     * @throws Exception On errors.
     */
    private static AuthenticationHandler newMockHandler(final boolean success) throws Exception {
        return newMockHandler("MockAuthenticationHandler" + System.nanoTime(), success);
    }

    /**
     * Creates a new named mock authentication handler that either successfully validates all credentials or fails to
     * validate all credentials.
     *
     * @param name    Authentication handler name.
     * @param success True to authenticate all credentials, false to fail all credentials.
     * @return New mock authentication handler instance.
     * @throws Exception On errors.
     */
    private static AuthenticationHandler newMockHandler(final String name, final boolean success) throws Exception {
        final AuthenticationHandler mock = mock(AuthenticationHandler.class);
        when(mock.getName()).thenReturn(name);
        when(mock.supports(any(Credential.class))).thenReturn(true);
        if (success) {
            final Principal p = new DefaultPrincipalFactory().createPrincipal("nobody");

            final HandlerResult result = new DefaultHandlerResult(mock, mock(CredentialMetaData.class), p);
            when(mock.authenticate(any(Credential.class))).thenReturn(result);
        } else {
            when(mock.authenticate(any(Credential.class))).thenThrow(new FailedLoginException());
        }
        return mock;
    }
}
