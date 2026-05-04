package llealcruz.clarify.web;

import llealcruz.clarify.config.ClarifyProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clarify")
public class ClarifyDashboardController {

    private String cachedHtml = null;

    private final com.sun.management.OperatingSystemMXBean osMxBean =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String getDashboard() throws IOException {
        if (cachedHtml == null) {
            ClassPathResource resource = new ClassPathResource("clarify-dashboard.html");
            cachedHtml = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        }
        return cachedHtml;
    }

    @GetMapping("/api/logs")
    public List<String> getLogs() {
        try {
            Path logPath = ClarifyProperties.getFullLogPath();
            if (Files.exists(logPath)) {
                return Files.readAllLines(logPath);
            }
        } catch (IOException e) {
            System.err.println("Error reading log file for dashboard: " + e.getMessage());
        }
        return List.of();
    }

    // Endpoint leve para métricas em tempo real da JVM (usa apenas APIs nativas do JDK)
    @GetMapping("/api/stats")
    public Map<String, Object> getSystemStats() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double ramPercent = (double) usedMemory / maxMemory * 100;

        // Carga de CPU do processo JVM (retorna valor entre 0.0 e 1.0, ou -1 se indisponível)
        double cpuLoad = osMxBean.getProcessCpuLoad();
        double cpuPercent = cpuLoad >= 0 ? cpuLoad * 100 : 0;

        return Map.of(
                "cpuPercent", Math.round(cpuPercent * 10.0) / 10.0,
                "ramUsedMB", Math.round(usedMemory / (1024.0 * 1024.0) * 10.0) / 10.0,
                "ramMaxMB", Math.round(maxMemory / (1024.0 * 1024.0) * 10.0) / 10.0,
                "ramPercent", Math.round(ramPercent * 10.0) / 10.0
        );
    }
}
