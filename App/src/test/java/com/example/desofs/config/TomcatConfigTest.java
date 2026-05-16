package com.example.desofs.config;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TomcatConfig")
class TomcatConfigTest {

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private Context context;

    @Mock
    private Container host;

    @Mock
    private Pipeline pipeline;

    private TomcatConfig.JsonErrorReportValve valve;
    private StringWriter stringWriter;

    @BeforeEach
    void setUp() {
        valve = new TomcatConfig.JsonErrorReportValve();
        stringWriter = new StringWriter();
    }

    // ---- Customizer bean ----

    @Test
    @DisplayName("Customizer should remove default ErrorReportValve and add JsonErrorReportValve")
    void customizer_removesDefaultAndAddsCustomValve() {
        ErrorReportValve defaultValve = new ErrorReportValve();
        when(context.getParent()).thenReturn(host);
        when(host.getPipeline()).thenReturn(pipeline);
        when(pipeline.getValves()).thenReturn(new Valve[]{defaultValve});

        TomcatConfig config = new TomcatConfig();
        WebServerFactoryCustomizer<TomcatServletWebServerFactory> customizer =
                config.jsonErrorReportValveCustomizer();

        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        customizer.customize(factory);
        factory.getTomcatContextCustomizers().forEach(c -> c.customize(context));

        verify(pipeline).removeValve(defaultValve);
        ArgumentCaptor<Valve> captor = ArgumentCaptor.forClass(Valve.class);
        verify(pipeline).addValve(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TomcatConfig.JsonErrorReportValve.class);
    }

    @Test
    @DisplayName("Customizer should add custom valve when no default ErrorReportValve exists")
    void customizer_addsCustomValveWhenNoDefault() {
        when(context.getParent()).thenReturn(host);
        when(host.getPipeline()).thenReturn(pipeline);
        when(pipeline.getValves()).thenReturn(new Valve[]{});

        TomcatConfig config = new TomcatConfig();
        WebServerFactoryCustomizer<TomcatServletWebServerFactory> customizer =
                config.jsonErrorReportValveCustomizer();

        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        customizer.customize(factory);
        factory.getTomcatContextCustomizers().forEach(c -> c.customize(context));

        verify(pipeline, never()).removeValve(any());
        ArgumentCaptor<Valve> captor = ArgumentCaptor.forClass(Valve.class);
        verify(pipeline).addValve(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TomcatConfig.JsonErrorReportValve.class);
    }

    @Test
    @DisplayName("Customizer should skip non-ErrorReportValve valves")
    void customizer_skipsNonErrorReportValves() {
        Valve otherValve = mock(Valve.class);
        when(context.getParent()).thenReturn(host);
        when(host.getPipeline()).thenReturn(pipeline);
        when(pipeline.getValves()).thenReturn(new Valve[]{otherValve});

        TomcatConfig config = new TomcatConfig();
        WebServerFactoryCustomizer<TomcatServletWebServerFactory> customizer =
                config.jsonErrorReportValveCustomizer();

        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        customizer.customize(factory);
        factory.getTomcatContextCustomizers().forEach(c -> c.customize(context));

        verify(pipeline, never()).removeValve(any());
        ArgumentCaptor<Valve> captor = ArgumentCaptor.forClass(Valve.class);
        verify(pipeline).addValve(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TomcatConfig.JsonErrorReportValve.class);
    }

    // ---- report() early returns ----

    @Test
    @DisplayName("report() should return early when content already written")
    void report_returnsEarly_whenContentAlreadyWritten() {
        when(response.getContentWritten()).thenReturn(100L);

        valve.report(request, response, null);

        verify(response, never()).setContentType(anyString());
    }

    @Test
    @DisplayName("report() should return early when setErrorReported returns false")
    void report_returnsEarly_whenErrorAlreadyReported() {
        when(response.getContentWritten()).thenReturn(0L);
        when(response.setErrorReported()).thenReturn(false);

        valve.report(request, response, null);

        verify(response, never()).setContentType(anyString());
    }

    // ---- JSON responses by status code ----

    @Test
    @DisplayName("report() should return JSON with 'Bad request' for status 400")
    void report_returnsJson_forStatus400() throws Exception {
        setupReportResponse(400);

        valve.report(request, response, null);

        verifyJsonResponse();
        assertThat(stringWriter.toString()).contains("\"message\":\"Bad request\"");
        assertThat(stringWriter.toString()).contains("\"status\":400");
    }

    @Test
    @DisplayName("report() should return JSON with 'Resource not found' for status 404")
    void report_returnsJson_forStatus404() throws Exception {
        setupReportResponse(404);

        valve.report(request, response, null);

        verifyJsonResponse();
        assertThat(stringWriter.toString()).contains("\"message\":\"Resource not found\"");
        assertThat(stringWriter.toString()).contains("\"status\":404");
    }

    @Test
    @DisplayName("report() should return JSON with 'Unauthorized' for status 401")
    void report_returnsJson_forStatus401() throws Exception {
        setupReportResponse(401);

        valve.report(request, response, null);

        verifyJsonResponse();
        assertThat(stringWriter.toString()).contains("\"message\":\"Unauthorized\"");
        assertThat(stringWriter.toString()).contains("\"status\":401");
    }

    @Test
    @DisplayName("report() should return JSON with 'Access denied' for status 403")
    void report_returnsJson_forStatus403() throws Exception {
        setupReportResponse(403);

        valve.report(request, response, null);

        verifyJsonResponse();
        assertThat(stringWriter.toString()).contains("\"message\":\"Access denied\"");
        assertThat(stringWriter.toString()).contains("\"status\":403");
    }

    @Test
    @DisplayName("report() should return JSON with 'Client error' for other 4xx statuses")
    void report_returnsJson_forOther4xx() throws Exception {
        setupReportResponse(429);

        valve.report(request, response, null);

        verifyJsonResponse();
        assertThat(stringWriter.toString()).contains("\"message\":\"Client error\"");
        assertThat(stringWriter.toString()).contains("\"status\":429");
    }

    @Test
    @DisplayName("report() should return JSON with 'An unexpected error occurred' for 5xx statuses")
    void report_returnsJson_for5xx() throws Exception {
        setupReportResponse(500);

        valve.report(request, response, null);

        verifyJsonResponse();
        assertThat(stringWriter.toString()).contains("\"message\":\"An unexpected error occurred\"");
        assertThat(stringWriter.toString()).contains("\"status\":500");
    }

    @Test
    @DisplayName("report() should include correlationId and timestamp in response")
    void report_includesCorrelationIdAndTimestamp() throws Exception {
        setupReportResponse(400);

        valve.report(request, response, null);

        String body = stringWriter.toString();
        assertThat(body).contains("\"correlationId\":\"");
        assertThat(body).contains("\"timestamp\":\"");
    }

    // ---- Edge cases ----

    @Test
    @DisplayName("report() should handle null writer gracefully")
    void report_handlesNullWriter() throws Exception {
        when(response.getContentWritten()).thenReturn(0L);
        when(response.setErrorReported()).thenReturn(true);
        when(response.getStatus()).thenReturn(500);
        when(response.getReporter()).thenReturn(null);

        valve.report(request, response, null);

        verify(response).setContentType("application/json");
    }

    @Test
    @DisplayName("report() should handle IOException during write gracefully")
    void report_handlesWriteFailure() throws Exception {
        PrintWriter failingWriter = mock(PrintWriter.class);
        doThrow(new IOException("broken pipe")).when(failingWriter).write(anyString());
        when(response.getContentWritten()).thenReturn(0L);
        when(response.setErrorReported()).thenReturn(true);
        when(response.getStatus()).thenReturn(500);
        when(response.getReporter()).thenReturn(failingWriter);

        valve.report(request, response, null);

        verify(response).setContentType("application/json");
    }

    // ---- Helpers ----

    private void setupReportResponse(int statusCode) throws IOException {
        when(response.getContentWritten()).thenReturn(0L);
        when(response.setErrorReported()).thenReturn(true);
        when(response.getStatus()).thenReturn(statusCode);
        when(response.getReporter()).thenReturn(new PrintWriter(stringWriter));
    }

    private void verifyJsonResponse() {
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("utf-8");
    }
}
