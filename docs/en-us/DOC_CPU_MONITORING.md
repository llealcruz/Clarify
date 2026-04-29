# Monitoring Architecture: CPU Time

This document describes how the **Clarify APM** library captures real CPU processing time per method.

## Wall Clock Time vs. CPU Time

Standard monitoring uses `System.nanoTime()` to measure the difference between the start and end of a method. This is called **Wall Clock Time**. 
If a method takes 2 seconds, it doesn't mean it used 2 seconds of the processor. Most of the time, the method spends 1.9 seconds idle (blocked), waiting for a Database, an External API, or disk read (I/O).

For an enterprise-level APM, we need to know the **CPU Time** (the time the thread was effectively scheduled on the processor performing mathematical/logical calculations).

## The Solution: `ThreadMXBean`

The JVM provides a management interface called `java.lang.management.ThreadMXBean`.
It has the native method:
```java
ThreadMXBean bean = ManagementFactory.getThreadMXBean();
long cpuTimeNs = bean.getCurrentThreadCpuTime();
```

### Interception Flow

1. When the client method is triggered, `PerformanceMonitorAspect` asks `ThreadMXBean` for the current thread's CPU counter (e.g., `1000ns`).
2. The client method (`joinPoint.proceed()`) executes and makes database calls.
3. Upon method return, the aspect asks `ThreadMXBean` again for the current CPU counter (e.g., `1500ns`).
4. The difference (`500ns`) is the **real** time the processor "labored" to execute the method.

### Added Overhead
Invoking `getCurrentThreadCpuTime()` causes a transition to native code (JNI/OS Kernel) to read Operating System counters. This costs approximately `100 to 200 nanoseconds`.
By capturing this value on the Original Thread, we ensure millimeter precision with negligible performance impact.
