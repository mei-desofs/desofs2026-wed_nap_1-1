package com.example.desofs.config;

import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tomcat customization to ensure all container-level error responses
 * use JSON content type instead of the default HTML.
 *
 * <p>This covers errors rejected by Tomcat before reaching Spring MVC,
 * such as malformed URLs with encoded special characters.</p>
 */
@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> jsonErrorReportValveCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            Container host = context.getParent();

            // Remove the default ErrorReportValve
            for (Valve valve : host.getPipeline().getValves()) {
                if (valve instanceof ErrorReportValve) {
                    host.getPipeline().removeValve(valve);
                    break;
                }
            }

            // Add custom JSON error report valve
            host.getPipeline().addValve(new JsonErrorReportValve());
        });
    }

    /**
     * Custom Tomcat ErrorReportValve that returns JSON responses instead of HTML.
     * Handles container-level errors that occur before the request reaches Spring MVC.
     */
    static class JsonErrorReportValve extends ErrorReportValve {

        @Override
        protected void report(Request request, Response response, Throwable throwable) {
            int statusCode = response.getStatus();
            if (response.getContentWritten() > 0 || !response.setErrorReported()) {
                return;
            }

            // Override the text/html content type set by the parent invoke()
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");

            String correlationId = UUID.randomUUID().toString();
            String message;
            if (statusCode == 400) {
                message = "Bad request";
            } else if (statusCode == 404) {
                message = "Resource not found";
            } else if (statusCode == 401) {
                message = "Unauthorized";
            } else if (statusCode == 403) {
                message = "Access denied";
            } else if (statusCode >= 400 && statusCode < 500) {
                message = "Client error";
            } else {
                message = "An unexpected error occurred";
            }

            try {
                Writer writer = response.getReporter();
                if (writer != null) {
                    writer.write("{\"correlationId\":\"" + correlationId + "\","
                            + "\"status\":" + statusCode + ","
                            + "\"message\":\"" + message + "\","
                            + "\"timestamp\":\"" + LocalDateTime.now() + "\"}");
                    writer.flush();
                }
            } catch (IOException e) {
                // Cannot recover from write failure
            }
        }
    }
}
