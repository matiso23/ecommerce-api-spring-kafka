package dev.williamnogueira.ecommerce.infrastructure.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @InjectMocks
    private LoggingAspect loggingAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private org.aspectj.lang.Signature signature;

    @Test
    void logControllerCalls_LogsAndProceedsCorrectly() throws Throwable {
        Object[] args = { "arg1", 123 };

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(TestController.class);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(args);

        when(joinPoint.proceed()).thenReturn("OK");

        Object result = loggingAspect.logControllerCalls(joinPoint);

        assertThat(result).isEqualTo("OK");
    }

    private static class TestController {}
}