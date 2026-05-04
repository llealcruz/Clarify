package llealcruz.clarify.web;

import llealcruz.clarify.config.ClarifyProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/clarify")
public class ClarifyDashboardController {

    private String cachedHtml = null;

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
}
