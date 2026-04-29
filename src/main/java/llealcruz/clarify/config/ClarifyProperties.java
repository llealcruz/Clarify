package com.llealcruz.clarify.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ClarifyProperties {
    private static String logPath = ""; // Raiz do projeto por padrão
    private static String logFilename = "clarify-logs.jsonl";
    private static int maxSizeMb = 10;
    
    private static String messageOk = "Method executed as expected.";
    private static String messageWarn = "Method is performing below expectations but within acceptable limits.";
    private static String messageDanger = "Method is performing below expectations and requires attention.";
    private static String messageError = "Method execution failed with an exception.";

    public static String getMessageOk() { return messageOk; }
    public static void setMessageOk(String message) { if (message != null && !message.isBlank()) messageOk = message; }
    
    public static String getMessageWarn() { return messageWarn; }
    public static void setMessageWarn(String message) { if (message != null && !message.isBlank()) messageWarn = message; }
    
    public static String getMessageDanger() { return messageDanger; }
    public static void setMessageDanger(String message) { if (message != null && !message.isBlank()) messageDanger = message; }
    
    public static String getMessageError() { return messageError; }
    public static void setMessageError(String message) { if (message != null && !message.isBlank()) messageError = message; }

    public static String getLogPath() {
        return logPath;
    }

    public static void setLogPath(String path) {
        if (path != null) {
            logPath = path;
        }
    }

    public static String getLogFilename() {
        return logFilename;
    }

    public static void setLogFilename(String filename) {
        if (filename != null && !filename.trim().isEmpty()) {
            logFilename = filename;
        }
    }

    public static int getMaxSizeMb() {
        return maxSizeMb;
    }

    public static void setMaxSizeMb(int sizeMb) {
        if (sizeMb > 0) {
            maxSizeMb = sizeMb;
        }
    }

    public static Path getFullLogPath() {
        if (logPath == null || logPath.trim().isEmpty()) {
            return Paths.get(logFilename);
        }
        return Paths.get(logPath, logFilename);
    }
}
