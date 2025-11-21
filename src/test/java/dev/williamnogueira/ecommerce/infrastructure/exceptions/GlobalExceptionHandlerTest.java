package dev.williamnogueira.ecommerce.infrastructure.exceptions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private WebRequest webRequest;

    private GlobalExceptionHandler handler;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        handler = new GlobalExceptionHandler();
        when(webRequest.getDescription(false)).thenReturn("uri=/test-endpoint");
    }

    @Test
    void testHandleResponseStatusException() {
        // Arrange
        var ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request");

        // Act
        var response = handler.handleResponseStatusException(ex, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> body = mapper.convertValue(response.getBody(), new TypeReference<>() {});

        assertThat(body)
                .isNotNull()
                .containsEntry("status", 400)
                .containsEntry("message", HttpStatus.BAD_REQUEST + " \"Invalid request\"")
                .containsEntry("error", "Invalid request")
                .containsEntry("path", "/test-endpoint")
                .containsKey("timestamp");
    }

    @Test
    void testHandleGenericException() {
        // Arrange
        var ex = new RuntimeException("Something went wrong");

        // Act
        var response = handler.handleGenericException(ex, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        Map<String, Object> body = mapper.convertValue(response.getBody(), new TypeReference<>() {});

        assertThat(body)
                .isNotNull()
                .containsEntry("status", 500)
                .containsEntry("error", "Internal Server Error")
                .containsEntry("message", "Something went wrong")
                .containsEntry("path", "/test-endpoint")
                .containsKey("timestamp");
    }
}
