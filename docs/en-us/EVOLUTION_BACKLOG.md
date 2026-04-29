# 🚀 Evolution Backlog and New Features

This document consolidates ideas for continuous improvements and new functionalities to be developed to elevate Clarify to an *Enterprise* maturity level for production environments.

## 1. Humanized Translation with "Action" (New Feature)

**Problem:** Currently, the library records the technical method name (e.g., `saveCp()`), which may still sound too technical for QAs and POs reading the dashboard.
**Solution:**

- Add an optional `action` attribute to the `@ClarifyMonitor(action = "Save Public Key")` annotation.
- When `MessageTranslator` builds the phrase, it will check: if the user provided an `action`, the log will read *"Action 'Save Public Key' executed in Xms"*, instead of *"Method 'saveCp' executed in Xms"*.
- **Impact:** Definitely transforms technical reading into business domain (Domain-Driven) reading.

## 2. Log Rotation

**Problem:** In systems with high traffic, the `clarify-logs.jsonl` file will grow infinitely until it exhausts the server's hard drive storage.
**Solution:**

- Implement a file size check (e.g., 10MB limit) or time-based check (daily closure) in `FileStorage`.
- When the limit is reached, the current file is renamed (e.g., `clarify-logs-2026-04-29.jsonl`) and a new blank `clarify-logs.jsonl` is created.
- **Impact:** Ensures infrastructure stability and facilitates cleaning old logs by DevOps tools.

## 3. Exception Handling in the Aspect

**Problem:** Currently, `PerformanceMonitorAspect` only measures time if execution completes successfully. If the `proceed()` method throws an exception, the timer stops and nothing is recorded in the log.
**Solution:**

- Wrap `capturedJoinPoint.proceed()` in a `try-catch-finally` block.
- Capture the duration even if an error occurs and send it to the queue with an `ERROR` or `EXCEPTION` status.
- **Impact:** The Dashboard becomes a powerful tool for detecting not just slowness, but also system failures (showing the Exception's error message in the arguments).

## 4. Web Dashboard Security (Architectural Decision)

**Solution:** Adopt the "Delegated Security" pattern (like Swagger/Actuator). Do not include proprietary authentication to avoid conflicts. Instructions for protecting the route will be moved to Step 6 (Documentation).

## 5. Add New Application Properties

Allow the client system to modify the messages displayed on the dashboard.
Currently, the default is Portuguese, but we should change this default to English, which is the globally accepted standard. However, the client can edit the messages in their system's application properties using any language they choose.

## 6. Update README Documentation

Explain in the README how to configure properties in the client application's properties file, including configuration examples.
We should also list the attributes that can be passed to our `@ClarifyMonitor` and mention the default values if they are omitted.
**Security:** Include a warning block teaching the developer to use their own `Spring Security` to lock the `/clarify/**` route.

## 7. Deep Hardware Monitoring (CPU and RAM)
**Problem:** Measuring only "Wall Clock Time" doesn't indicate if the application is actually performing heavy processing (CPU) or allocating too much memory (RAM), or if it's just idle waiting for a database.
**Solution:**
- Use `ThreadMXBean` in the Aspect to capture exact CPU nanoseconds and Heap bytes allocated by the current thread.
- Enrich `JoinPointRecord` and the JSONL with `cpuTime` and `ramAllocated` metrics.
- **Impact:** Elevates Clarify to an elite "Enterprise APM" level, providing method-level infrastructure metrics without relying on heavy Java agents.

## 8. Bottleneck Ranking (Dashboard Analytics)
**Problem:** With the addition of CPU and RAM, the dashboard will list many loose executions. A PO or QA cannot quickly see "what is the system's worst method."
**Solution:** 
- Create a section or tab in the Dashboard with the Top 10 worst methods (Ranking) categorized by: Highest RAM Consumption (Memory Leak), Highest CPU Time (CPU Leak), and Longest Wall Time duration.
- **Impact:** Transforms the Clarify library into an active Analytics tool, giving clear directions on where engineering should act first to optimize infrastructure.

## 9. Clarify Needs to be in English
All code and dashboard information must be in English so the library can be used globally. The only thing we can keep in Portuguese are the code comments.
