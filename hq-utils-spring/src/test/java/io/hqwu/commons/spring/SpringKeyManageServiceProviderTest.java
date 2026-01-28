package io.hqwu.commons.spring;

import io.hqwu.commons.security.KeyManageService;
import io.hqwu.commons.security.KeyManageServiceLocalImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringKeyManageServiceProviderTest {

    private SpringKeyManageServiceProvider provider;

    @Mock
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        provider = new SpringKeyManageServiceProvider();
        provider.setApplicationContext(applicationContext);
    }

    @Test
    void testGet_NoBeans() {
        when(applicationContext.getBeansOfType(KeyManageService.class)).thenReturn(new HashMap<>());
        assertNull(provider.get());
    }

    @Test
    void testGet_OnlyLocalImpl() {
        Map<String, KeyManageService> beans = new HashMap<>();
        KeyManageServiceLocalImpl localImpl = new KeyManageServiceLocalImpl();
        beans.put("local", localImpl);

        when(applicationContext.getBeansOfType(KeyManageService.class)).thenReturn(beans);

        KeyManageService result = provider.get();
        assertEquals(localImpl, result);
    }

    @Test
    void testGet_OnlyRemoteImpl() {
        Map<String, KeyManageService> beans = new HashMap<>();
        KeyManageService remoteImpl = mock(KeyManageService.class);
        beans.put("remote", remoteImpl);

        when(applicationContext.getBeansOfType(KeyManageService.class)).thenReturn(beans);

        KeyManageService result = provider.get();
        assertEquals(remoteImpl, result);
    }

    @Test
    void testGet_MixedImpl_PreferRemote() {
        Map<String, KeyManageService> beans = new HashMap<>();
        KeyManageServiceLocalImpl localImpl = new KeyManageServiceLocalImpl();
        KeyManageService remoteImpl = mock(KeyManageService.class);
        beans.put("local", localImpl);
        beans.put("remote", remoteImpl);

        when(applicationContext.getBeansOfType(KeyManageService.class)).thenReturn(beans);

        KeyManageService result = provider.get();
        assertEquals(remoteImpl, result);
    }

    @Test
    void testGet_ContextNull() {
        SpringKeyManageServiceProvider nullContextProvider = new SpringKeyManageServiceProvider();
        assertNull(nullContextProvider.get());
    }
}
