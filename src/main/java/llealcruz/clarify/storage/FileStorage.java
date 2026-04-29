package com.llealcruz.clarify.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.llealcruz.clarify.enums.StatusEnum;
import com.llealcruz.clarify.model.JoinPointRecord;
import com.llealcruz.clarify.translator.MessageTranslator;

/**
 * Visualizar Persistence_Architecture.md para melhor entendimento.
 */
public class FileStorage {
    private final ConcurrentLinkedQueue<JoinPointRecord> buffer = new ConcurrentLinkedQueue<>();
    private final MessageTranslator messageTranslator = new MessageTranslator();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // Método super leve para escapar caracteres especiais e não quebrar o JSON
    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(java.util.Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public FileStorage() {
        // Manda o Java rodar o método "flush" a cada 5 segundos
        executor.scheduleAtFixedRate(this::flush, 5, 5, TimeUnit.SECONDS);
    }

    public void add(JoinPointRecord record) {
        buffer.add(record);
    }

    private void flush() {
        if (buffer.isEmpty())
            return;

        StringBuilder logsToPersist = new StringBuilder();

        JoinPointRecord joinPointRecord;

        // Pega tudo que está no buffer
        while ((joinPointRecord = buffer.poll()) != null) {

            // Chama o MessageTranslator para buscar o Status (Regra de Negócio)
            StatusEnum status = messageTranslator.translate(joinPointRecord);

            // Formata a data/hora que já capturamos lá no Aspecto
            String dateTimeStr = joinPointRecord.startTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            // Pega os argumentos (parâmetros) que foram passados para o método e transforma em String
            String argsStr = java.util.Arrays.toString(joinPointRecord.args());

            // Pega a ação se existir
            String actionStr = joinPointRecord.action() != null && !joinPointRecord.action().isBlank()
                    ? joinPointRecord.action()
                    : "-";

            String finalMessage = joinPointRecord.errorMessage() != null
                    ? status.getMessage() + " Original error: " + joinPointRecord.errorMessage()
                    : status.getMessage();

            String cpuTimeStr = "-";
            if (joinPointRecord.cpuTimeNs() > 0) {
                long cpuMs = joinPointRecord.cpuTimeNs() / 1_000_000;
                cpuTimeStr = cpuMs == 0 ? "<1ms" : cpuMs + "ms";
            } else if (joinPointRecord.cpuTimeNs() == 0) {
                cpuTimeStr = "0ms";
            }

            String ramAllocatedStr = (joinPointRecord.ramAllocatedBytes() > 0) ? formatBytes(joinPointRecord.ramAllocatedBytes()) : "-";

            // Constrói o JSON manualmente para manter altíssima performance e não depender do Jackson (spring-boot-starter-web)
            String lineToPersist = String.format(
                    "{\"datetime\": \"%s\", \"status\": \"%s\", \"action\": \"%s\", \"method\": \"%s\", \"duration\": \"%dms\", \"cpuTime\": \"%s\", \"ramAllocated\": \"%s\", \"message\": \"%s\", \"args\": \"%s\"}\n",
                    dateTimeStr,
                    status.name(),
                    escapeJson(actionStr),
                    escapeJson(joinPointRecord.methodName()),
                    joinPointRecord.durationMs(),
                    cpuTimeStr,
                    ramAllocatedStr,
                    escapeJson(finalMessage),
                    escapeJson(argsStr));

            logsToPersist.append(lineToPersist);
        }

        // Pega o caminho completo do arquivo a partir das novas propriedades separadas
        Path logPath = com.llealcruz.clarify.config.ClarifyProperties.getFullLogPath();

        // Lógica de Rotação de Logs (Log Rotation)
        try {
            if (Files.exists(logPath)) {
                long size = Files.size(logPath);
                long maxSize = com.llealcruz.clarify.config.ClarifyProperties.getMaxSizeMb() * 1024L * 1024L;

                // Se o arquivo passou do limite configurado...
                if (size >= maxSize) {
                    String timeSuffix = java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    String baseName = com.llealcruz.clarify.config.ClarifyProperties.getLogFilename();
                    int dotIndex = baseName.lastIndexOf('.');

                    // Ex: clarify-logs-20260429_132050.jsonl
                    String rotatedName = (dotIndex != -1)
                            ? baseName.substring(0, dotIndex) + "-" + timeSuffix + baseName.substring(dotIndex)
                            : baseName + "-" + timeSuffix;

                    Path rotatedPath = logPath.getParent() == null
                            ? Paths.get(rotatedName)
                            : logPath.getParent().resolve(rotatedName);

                    // Move/renomeia o arquivo atual (o novo começará em branco no próximo Files.writeString)
                    Files.move(logPath, rotatedPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            System.err.println("Error rotating logs: " + e.getMessage());
        }

        // Abre o arquivo (ou cria um novo se foi rotacionado) e grava tudo de uma vez (Batch Write)
        try {
            Files.writeString(
                    logPath,
                    logsToPersist.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

        } catch (IOException ex) {
            System.err.println("Error writing logs to file: " + ex.getMessage());
        }
    }
}
