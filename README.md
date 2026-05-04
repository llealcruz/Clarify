# 🟢 Clarify APM - Enterprise Grade

Clarify is a lightweight library (JAR) for Java 21+ designed for performance monitoring and auditing, focusing on business transparency.
Unlike purely technical tools, Clarify acts as a bridge between code and the product team (PO, SM, QA).

[Versão em Português aqui](./docs/pt-br/README.md)

---

## 🎓 Study Project & Language Note

This project was developed for **study and technical improvement purposes**. 

To maintain a balance between professional practice and personal learning:
- **Application & Documentation:** All public APIs, method names, and UI elements are in **English**, following industry standards.
- **Internal Comments:** I have intentionally kept code comments in **Portuguese**. This choice was made to facilitate my own learning process, making it easier to review complex logic and consolidate knowledge during the development phase.

This was a conscious decision to maximize the educational value of the repository while still delivering a professional-grade tool.

---


## 🚀 Key Features

- **Zero Config AOP:** Uses native Spring AOP to monitor your application without build configuration needs (*Low Overhead* Architecture).
- **Optimized Storage:** Structured logs in **JSON Lines (JSONL)** format, ideal for log parsing tools and high-performance read/write.
- **Automatic Log Rotation:** Prevents disk exhaustion by intelligently partitioning logs (via date suffix) when they reach the configured limit.
- **Exception Handling:** Allows capturing logs for both slowness AND catastrophic failures, with per-method granularity to avoid false-positives from business validations.
- **Embedded Dashboard:** Rich, dynamic, and readable visualization of reports directly in your application's browser.
- **Virtual Threads:** 100% asynchronous I/O. Monitoring disk writes will never delay your main system's response time.

---

## 📦 Installation (Plug & Play)

Installation was designed to have zero impact on your project's lifecycle. Just add the dependency:

### Maven (`pom.xml`)

```xml
<dependency>
    <groupId>llealcruz</groupId>
    <artifactId>clarify</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

*(No complex build plugins required. Clarify leverages Spring Boot's native auto-configuration magic!)*

---

## 🛠️ Usage (Quick Start)

To monitor any method in your Spring Bean, simply add the `@ClarifyMonitor` annotation:

```java
import llealcruz.clarify.annotation.ClarifyMonitor;
import org.springframework.stereotype.Service;

@Service
public class MyService {

    // Standard Monitoring
    @ClarifyMonitor(action = "Customer Search")
    public void searchCustomer() { ... }

    // Extreme Monitoring (Warns on dashboard if an Exception occurs)
    @ClarifyMonitor(action = "Payment Integration", recordExceptions = true)
    public void integrate() { ... }
}
```

### Annotation Parameters

- `action`: (String) A readable name that will appear in the report to facilitate understanding for QAs and POs. *Default: Empty (Hidden).*
- `tag`: (String) A custom marker (e.g., "CRITICAL", "ROUTINE"). *Default: Empty.*
- `warnMs`: (long) Time in milliseconds to consider the method "Below Expectation" (Yellow Alert). *Default: 500.*
- `dangerMs`: (long) Time in milliseconds to consider the method "Critical" (Red Alert). *Default: 1000.*
- `recordExceptions`: (boolean) If set to `true`, any exception thrown by the method will be recorded in the error log (red ERROR badge) and then re-thrown to avoid breaking the original application. *Default: false.*

---

## ⚙️ Configuration (`application.yml`)

Clarify allows extreme flexibility, from storage rules to Dashboard message internationalization.
In your `application.yml`, you can override any properties you want:

```yaml
clarify:
  # === STORAGE SETTINGS ===
  log:
    path: /var/logs/my-application/ # Folder where the log will be generated (Default: project root)
    filename: my-system-logs.jsonl # File name (Default: clarify-logs.jsonl)
    max-size-mb: 50 # Maximum size before triggering Log Rotation (Default: 10)

  # === MESSAGE INTERNATIONALIZATION (Optional) ===
  # Allows translating the Dashboard from default English to any language!
  messages:
    ok: "Perfect execution."
    warn: "Warning: Slow method."
    danger: "Danger: Possible server bottleneck!"
    error: "Critical: Method failed."
```

---

## 📊 Dashboard Access and Security

Upon starting your application, the interactive performance Dashboard will be automatically available at your server's base route:
`http://your-url/clarify`

### 🔒 Delegated Security (IMPORTANT)

The `/clarify` route is **PUBLIC** by default to ensure easy *Plug & Play* in local development environments.
For Production environments, **NEVER LEAVE THIS ROUTE OPEN**. You must use your application's native security mechanism (e.g., `Spring Security`) to protect the endpoint.

**Spring Security Example:**

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        // Locks the Clarify dashboard for ADMIN users only
        .requestMatchers("/clarify/**").hasRole("ADMIN")
        
        // Your other business routes...
        .anyRequest().authenticated()
    );
    return http.build();
}
```
