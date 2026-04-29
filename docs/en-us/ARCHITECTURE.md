# 🏛️ Architectural Decisions (ADR - Architecture Decision Records)

This document records the crucial architectural decisions made during the development of **Clarify APM**, the challenges faced, and the trade-offs accepted to justify the final library design.

---

## 1. Spring AOP vs AspectJ (Compile-Time Weaving)

### Context

The primary technical goal at the project's inception was to create a monitoring library with absolute **Zero Overhead**. To achieve this, the first version of the architecture utilized pure *AspectJ* with *Compile-Time Weaving (CTW)*. In this approach, the `aspectj-maven-plugin` injected the timer directly into the client's *bytecode* during the build phase.

### The Problem Encountered

Despite being the highest performance solution in theory, *Compile-Time Weaving* created significant friction in the Developer Experience (DevEx):

1. **Intrusion:** The end client was forced to pollute their `pom.xml` or `build.gradle` with complex AspectJ plugins just to use the library.
2. **Local Conflicts:** Development tools and internal IDE compilers (like VS Code) frequently ignored the Maven plugin when running applications or local tests. This resulted in classes that hadn't been "woven," causing monitoring to fail silently in the developer's environment.

### The Decision: Migration to Spring AOP

We chose to abandon the AspectJ compiler and fully embrace **Spring AOP**.
We registered our Aspect (`PerformanceMonitorAspect`) as a managed `@Bean` in our `ClarifyAutoConfiguration`. Now, the Spring Boot ecosystem handles creating dynamic *Proxies* (CGLIB) in RAM during application startup to intercept calls to annotated methods.

### Accepted Trade-offs

* ✅ **Pro (Extreme Plug & Play):** Installation became incredibly simple. The client just imports the library's `.jar`, and everything works via auto-configuration. No build changes are required.
* ✅ **Pro (IDE Reliability):** It works perfectly and consistently in any IDE (VS Code, IntelliJ, Eclipse) with native `Run`/`Debug` buttons.
* ❌ **Con (Low Overhead vs Zero Overhead):** We lost true "Zero Overhead." Using Spring Proxies adds a slight penalty (about 5 to 15 nanoseconds) per method interception, as the original call passes through a reflection/proxy layer.

### Conclusion

We consciously accepted the minimal nanosecond overhead in favor of an **infinitely superior and user-friendly DevEx (Developer Experience)**. Since Clarify is designed to monitor business logic methods, database queries, and external integrations (where duration is often measured in tens or hundreds of milliseconds), the impact of Spring AOP is completely imperceptible for 99% of commercial applications.

---

## 2. Persistence Architecture

This document records the architectural decision and functioning of log persistence in the Clarify library, utilizing the **Producer-Consumer Pattern** with a Memory Buffer.

### The Problem: I/O Bottleneck (Physical Disk)

The hard drive (I/O) is the slowest component in computing. If the target system handles 1,000 requests per second, there would be 1,000 Virtual Threads trying to open the `clarify-logs.jsonl` file, write a line, and close it simultaneously.
The Operating System applies "Lock" mechanisms to prevent two processes from corrupting the file by writing to the same byte at the same time.
**Result:** The disk becomes a bottleneck, and performance plummets, causing wait queues in the Operating System.

### The Solution: Batch Write with Memory Queue

To ensure the library has **Zero Overhead** and scales infinitely, we avoid writing to the file on every request. Instead, we use RAM, which is built for extreme concurrency, and delegate writing to a "janitor."

#### Step A: The Producers (Our Virtual Threads)

The Virtual Threads created by `ClarifyDispatcher` **do not access the disk**.
Their job is only to generate the `JoinPointRecord` and perform a simple `queue.add(record)`.
This `queue` is a `ConcurrentLinkedQueue<JoinPointRecord>`, a thread-safe Java structure designed to support thousands of threads inserting data simultaneously without locks (non-blocking algorithm). Inserting data into RAM takes fractions of nanoseconds. The Virtual Thread completes its work instantly.

#### Step B: The Consumer (Background Cronjob)

There is **only ONE solitary thread** dedicated to persistence (managed by a `ScheduledExecutorService`).
It sleeps and wakes up periodically (e.g., every 5 seconds).
Its work cycle is:

1. Checks the `ConcurrentLinkedQueue`.
2. If there are items (e.g., 500 accumulated reports), it pulls them all from memory at once.
3. Opens the physical `clarify-logs.jsonl` file **exactly once**.
4. Dumps all 500 lines into the file in one go (this is called *Batch Write*).
5. Closes the file and goes back to sleep.

### Trade-off (The Architectural "Catch")

Every architectural decision has a price.

* **Colossal Advantage:** The physical disk is only triggered once every 5 seconds, regardless of whether the traffic is 10 or 10,000 requests per second. The I/O system never becomes overloaded.
* **The Acceptable Risk:** Since data sits in RAM waiting for the scheduler every 5 seconds, **if the server suffers a fatal crash (power failure, hardware fault)**, the logs from those last few seconds in the Queue (not yet consolidated to disk) will be lost.

For APM (Application Performance Monitoring) libraries, losing 5 seconds of metrics in a server catastrophe scenario is a **totally acceptable price** in exchange for massive performance and scalability gains.
