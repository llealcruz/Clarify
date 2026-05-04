package llealcruz.clarify.enums;

import llealcruz.clarify.config.ClarifyProperties;

public enum StatusEnum {
    OK,
    WARN,
    DANGER,
    ERROR;

    public String getMessage() {
        return switch (this) {
            case OK -> ClarifyProperties.getMessageOk();
            case WARN -> ClarifyProperties.getMessageWarn();
            case DANGER -> ClarifyProperties.getMessageDanger();
            case ERROR -> ClarifyProperties.getMessageError();
        };
    }
}
