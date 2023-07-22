package ru.igorit.andrk.service.processor.openclose;


import lombok.*;
import ru.igorit.andrk.service.processor.ProcessResult;


@Getter
@EqualsAndHashCode(of={"id"})
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class OpenCloseResult {

    private final String id;
    private final String code;
    @Setter
    private String text;

    public static ProcessResult toProcessResult(OpenCloseResult res){
        return new ProcessResult(res.getCode(),res.getText());
    }

    public OpenCloseResult(OpenCloseResult srcRes){
        this(srcRes.id,srcRes.code, srcRes.getText());
    }
}
