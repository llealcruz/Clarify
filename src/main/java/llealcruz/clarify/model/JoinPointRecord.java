package llealcruz.clarify.model;

import java.time.LocalDateTime;

public record JoinPointRecord(
        String methodName,
        String className,
        long durationMs,
        long cpuTimeNs,
        long ramAllocatedBytes,
        Object[] args,
        String tag,
        String action,
        long warnMs,
        long dangerMs,
        LocalDateTime startTime,
        String errorMessage) {
}