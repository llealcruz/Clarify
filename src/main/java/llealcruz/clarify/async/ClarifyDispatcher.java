package com.llealcruz.clarify.async;

import java.time.LocalDateTime;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.llealcruz.clarify.annotation.ClarifyMonitor;
import com.llealcruz.clarify.model.JoinPointRecord;
import com.llealcruz.clarify.storage.FileStorage;

public class ClarifyDispatcher {
    private final FileStorage fileStorage = new FileStorage();

    public void dispatch(ProceedingJoinPoint capturedJoinPoint,
            LocalDateTime startTimeForRecord,
            long durationMs,
            long cpuTimeNs,
            long ramAllocatedBytes,
            Throwable exception) {

        // Iniciamos uma nova virtual thread para continuarmos nosso processamento do
        // Clarify
        // em background para não afetarmos o tempo de processamento do método alvo
        // (joinpoint).
        Thread.startVirtualThread(() -> {
            MethodSignature methodSignature = (MethodSignature) capturedJoinPoint.getSignature();
            ClarifyMonitor annotation = (ClarifyMonitor) methodSignature.getMethod()
                    .getAnnotation(ClarifyMonitor.class);

            // Se for exceção e o usuário não quer logar, aborta
            if (exception != null && !annotation.recordExceptions()) {
                return;
            }

            String errorMessage = exception != null
                    ? exception.getClass().getSimpleName() + ": " + exception.getMessage()
                    : null;

            JoinPointRecord joinPointRecord = new JoinPointRecord(
                    methodSignature.getName(),
                    methodSignature.getDeclaringTypeName(),
                    durationMs, // convertendo de nanos para millisegundos
                    cpuTimeNs,
                    ramAllocatedBytes,
                    capturedJoinPoint.getArgs(),
                    annotation.tag(), // Exemplo: "GENERAL" ou o que o usuário tiver digitado
                    annotation.action(), // Exemplo: "Salva Chave Pública" ou o que o usuário tiver digitado
                    annotation.warnMs(), // Exemplo: 500 ou o que o usuário tiver digitado
                    annotation.dangerMs(), // Exemplo: 1000 ou o que o usuário tiver digitado
                    startTimeForRecord,
                    errorMessage);

            this.fileStorage.add(joinPointRecord);
        });
    }
}
