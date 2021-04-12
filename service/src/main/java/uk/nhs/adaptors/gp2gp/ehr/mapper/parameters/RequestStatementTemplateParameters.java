package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.Singular;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Getter
@Setter
@Builder
public class RequestStatementTemplateParameters {
    private boolean isNested;
    private String requestStatementId;
    private String availabilityTime;
    private String defaultReasonCode;
    private String code;
    private String participant;
    private String responsibleParty;
    @Singular
    private List<String> texts;

    public Function<Object, String> compileText() {
        return $ -> {
            List<String> reversedText = new ArrayList<>(texts);
            Collections.reverse(reversedText);

            return String.join(" ", reversedText).trim();
        };
    }
}
