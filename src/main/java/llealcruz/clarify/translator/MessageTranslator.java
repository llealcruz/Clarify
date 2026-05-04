package llealcruz.clarify.translator;

import llealcruz.clarify.enums.StatusEnum;
import llealcruz.clarify.model.JoinPointRecord;

public class MessageTranslator {
    public StatusEnum translate(JoinPointRecord joinPointRecord) {
        if (joinPointRecord.errorMessage() != null) {
            return StatusEnum.ERROR;
        }

        if (joinPointRecord.durationMs() < joinPointRecord.warnMs()) {
            return StatusEnum.OK;
        } else if (joinPointRecord.durationMs() < joinPointRecord.dangerMs()) {
            return StatusEnum.WARN;
        } else {
            return StatusEnum.DANGER;
        }
    }

    public String buildSubjectPhrase(JoinPointRecord joinPointRecord) {
        if (joinPointRecord.action() != null && !joinPointRecord.action().isBlank()) {
            return "Method '" + joinPointRecord.methodName() + "' (Action: '" + joinPointRecord.action()
                    + "') executed in " + joinPointRecord.durationMs() + "ms";
        }
        return "Method '" + joinPointRecord.methodName() + "' executed in " + joinPointRecord.durationMs() + "ms";
    }
}
