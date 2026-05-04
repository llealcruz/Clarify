package llealcruz.clarify.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import llealcruz.clarify.aspect.PerformanceMonitorAspect;
import org.springframework.context.annotation.Bean;

@Configuration
@ConditionalOnWebApplication // Só liga o Dashboard se o cliente já tiver um Servidor Web
@ComponentScan("llealcruz.clarify.web")
public class ClarifyAutoConfiguration {

    @Bean
    public PerformanceMonitorAspect performanceMonitorAspect() {
        return new PerformanceMonitorAspect();
    }

    public ClarifyAutoConfiguration(Environment env) {
        String path = env.getProperty("clarify.log.path");
        ClarifyProperties.setLogPath(path);

        String filename = env.getProperty("clarify.log.filename");
        ClarifyProperties.setLogFilename(filename);

        String maxSize = env.getProperty("clarify.log.max-size-mb");
        if (maxSize != null) {
            try {
                ClarifyProperties.setMaxSizeMb(Integer.parseInt(maxSize));
            } catch (NumberFormatException e) {
                // Ignore parsing errors, will use default
            }
        }

        ClarifyProperties.setMessageOk(env.getProperty("clarify.messages.ok"));
        ClarifyProperties.setMessageWarn(env.getProperty("clarify.messages.warn"));
        ClarifyProperties.setMessageDanger(env.getProperty("clarify.messages.danger"));
        ClarifyProperties.setMessageError(env.getProperty("clarify.messages.error"));
    }
}
