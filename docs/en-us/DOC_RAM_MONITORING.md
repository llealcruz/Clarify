# Monitoring Architecture: RAM (Memory Allocation)

This document describes how the **Clarify APM** library audits the memory consumption of individual methods, isolating the effects of the Garbage Collector (GC).

## The Challenge of the Garbage Collector

In Java, there is no concept of "manually deallocating memory." Heap memory is global. Measuring the total JVM memory before and after a method is **useless**, as the GC could run in the middle of the method, causing the final memory to be less than the initial, leading to negative and unrealistic results.

## The Solution: `Thread Allocated Bytes`

To measure the actual memory pressure a method exerts on the application, we don't measure "how much memory is left"; we measure **how many bytes the thread requested the JVM to create** while the method was executing.
This is done using a specific JVM extension from Oracle/OpenJDK: `com.sun.management.ThreadMXBean`.

```java
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

// Casting is necessary as this is an advanced internal JDK interface
ThreadMXBean sunThreadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
long bytes = sunThreadBean.getThreadAllocatedBytes(Thread.currentThread().getId());
```

### Interception Flow

1. `PerformanceMonitorAspect` invokes `getThreadAllocatedBytes()` as soon as the method starts (e.g., `10 MB` allocated since the thread was born in the application).
2. The original method executes, creating lists, strings, and temporary objects.
3. The aspect invokes the function again at the end (e.g., `15 MB`).
4. The calculation reveals that the method allocated **5 MB** of RAM (`15 - 10`).

### Why is this metric ingenious?
If a method allocates 50 MB of temporary strings per call, even if the GC cleans these strings instantly (avoiding out-of-memory errors), it will generate **very high pressure on the Garbage Collector**, causing global freezes (*GC Pauses*) in the application.
Clarify can identify these "wasteful" methods so the Product/QA team can open refactoring tickets for engineering.
