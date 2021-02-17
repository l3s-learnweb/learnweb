package de.l3s.learnweb.web;

import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.l3s.learnweb.app.ConfigProvider;

@ExtendWith(MockitoExtension.class)
class RequestFilterTest {
    @Mock
    private ConfigProvider configProvider;
    @Mock
    private RequestManager requestManager;
    @InjectMocks
    private RequestFilter requestFilter;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @BeforeEach
    void beforeEach() {
        when(requestManager.isBanned(anyString())).thenReturn(false);

        when(request.getRequestURL()).thenReturn(new StringBuffer("https://learnweb.l3s.uni-hannover.de/"));
        when(request.getRequestURI()).thenReturn("/lw/group/forum.jsf");
        when(request.getQueryString()).thenReturn("group_id=1157");
        when(request.getHeader("Forwarded")).thenReturn("130.75.236.125");
    }

    @Test
    void suspiciousRequest1() throws IOException, ServletException {
        when(request.getHeader("Forwarded")).thenReturn("130.75.236.125");
        requestFilter.doFilter(request, response, chain);
        verify(requestManager).recordRequest("130.75.236.125", "https://learnweb.l3s.uni-hannover.de/lw/group/forum.jsf?group_id=1157");
        verify(requestManager).isBanned("130.75.236.125");
        verifyNoMoreInteractions(requestManager);
    }

    @Test
    void suspiciousRequest2() throws IOException, ServletException {
        when(request.getHeader("Forwarded")).thenReturn("41.63.1.45, 82.145.209.238");
        requestFilter.doFilter(request, response, chain);
        verify(requestManager).recordRequest("41.63.1.45", "https://learnweb.l3s.uni-hannover.de/lw/group/forum.jsf?group_id=1157");
        verify(requestManager).isBanned("41.63.1.45");
        verifyNoMoreInteractions(requestManager);
    }

    @Test
    void suspiciousRequest3() throws IOException, ServletException {
        when(request.getHeader("Forwarded")).thenReturn("}__test|O:21:\"\"JDatabaseDriverMysqli\"\":3:{s:2:\"\"fc\"\";O:17:");
        requestFilter.doFilter(request, response, chain);
        verify(requestManager).recordRequest(anyString(), eq("https://learnweb.l3s.uni-hannover.de/lw/group/forum.jsf?group_id=1157"));
        verify(requestManager).isBanned(anyString());
        verify(requestManager).tempBan(eq(request.getHeader("Forwarded")), anyString());
        verifyNoMoreInteractions(requestManager);
    }

    @Test
    void sqlInjection1() throws IOException, ServletException {
        requestFilter.doFilter(request, response, chain);
        verify(requestManager).recordRequest("130.75.236.125", "https://learnweb.l3s.uni-hannover.de/lw/group/forum.jsf?group_id=1157");
        verify(requestManager).isBanned(anyString());
        verifyNoMoreInteractions(requestManager);
    }

    @Test
    void sqlInjection2() throws IOException, ServletException {
        when(request.getQueryString()).thenReturn("group_id=1157%27A=0");
        requestFilter.doFilter(request, response, chain);
        verify(requestManager).recordRequest("130.75.236.125", "https://learnweb.l3s.uni-hannover.de/lw/group/forum.jsf?group_id=1157%27A=0");
        verify(requestManager).isBanned(anyString());
        verify(requestManager).ban(anyString(), anyString());
        verifyNoMoreInteractions(requestManager);
    }
}
