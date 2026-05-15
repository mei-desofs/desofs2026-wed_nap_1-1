package com.example.desofs.config;

/**
import org.apache.catalina.Container;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TomcatConfig Tests")
class TomcatConfigTest {

    private TomcatConfig tomcatConfig;
    private TomcatConfig.JsonErrorReportValve valve;

    @BeforeEach
    void setUp() {
        tomcatConfig = new TomcatConfig();
        valve = new TomcatConfig.JsonErrorReportValve();
    }

    @Test
    @DisplayName("jsonErrorReportValveCustomizer should remove the default ErrorReportValve and add JSON valve")
    void customizer_replacesDefaultErrorValve() throws Exception {
        TomcatServletWebServerFactory factory = mock(TomcatServletWebServerFactory.class);

        tomcatConfig.jsonErrorReportValveCustomizer().customize(factory);

        org.mockito.ArgumentCaptor<TomcatContextCustomizer> captor =
                org.mockito.ArgumentCaptor.forClass(TomcatContextCustomizer.class);
        verify(factory).addContextCustomizers(captor.capture());

        TomcatContextCustomizer customizer = captor.getValue();

        org.apache.catalina.Context context = mock(org.apache.catalina.Context.class);
        Container host = mock(Container.class);
        Pipeline pipeline = mock(Pipeline.class);
        Valve defaultValve = mock(ErrorReportValve.class);

        when(context.getParent()).thenReturn(host);
        when(host.getPipeline()).thenReturn(pipeline);
        when(pipeline.getValves()).thenReturn(new Valve[]{defaultValve});

        customizer.customize(context);

        verify(pipeline).removeValve(defaultValve);
        verify(pipeline).addValve(any(TomcatConfig.JsonErrorReportValve.class));
    }

    @Test
    @DisplayName("report should render 400 Bad Request as JSON")
    void report_writesBadRequestJson() throws Exception {
        assertRenderedJson(400, "Bad request");
    }

    @Test
    @DisplayName("report should render 404 Not Found as JSON")
    void report_writesNotFoundJson() throws Exception {
        assertRenderedJson(404, "Resource not found");
    }

    @Test
    @DisplayName("report should render 401 Unauthorized as JSON")
    void report_writesUnauthorizedJson() throws Exception {
        assertRenderedJson(401, "Unauthorized");
    }

    @Test
    @DisplayName("report should render 403 Access Denied as JSON")
    void report_writesAccessDeniedJson() throws Exception {
        assertRenderedJson(403, "Access denied");
    }

    @Test
    @DisplayName("report should render generic client and server errors as JSON")
    void report_writesGenericJsonMessages() throws Exception {
        assertRenderedJson(418, "Client error");
        assertRenderedJson(500, "An unexpected error occurred");
    }

    @Test
    @DisplayName("report should return early when the response already has written content")
    void report_skipsWhenContentAlreadyWritten() throws Exception {
        Request request = mock(Request.class);
        Response response = mock(Response.class);

        when(response.getStatus()).thenReturn(404);
        when(response.getContentWritten()).thenReturn(1L);

        valve.report(request, response, null);

        verify(response, never()).setContentType(any());
        verify(response, never()).getReporter();
    }

    private void assertRenderedJson(int statusCode, String expectedMessage) throws Exception {
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        StringWriter writer = new StringWriter();

        when(response.getStatus()).thenReturn(statusCode);
        when(response.getContentWritten()).thenReturn(0L);
        when(response.setErrorReported()).thenReturn(true);
        when(response.getReporter()).thenReturn(writer);

        valve.report(request, response, null);

        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("utf-8");
        assertThat(writer.toString())
                .contains("\"status\":" + statusCode)
                .contains("\"message\":\"" + expectedMessage + "\"")
                .contains("\"correlationId\":\"")
                .contains("\"timestamp\":\"");
    }
}

 */