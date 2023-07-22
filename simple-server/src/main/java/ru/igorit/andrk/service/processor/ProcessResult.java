package ru.igorit.andrk.service.processor;

import lombok.*;
import org.springframework.lang.NonNull;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(of = "statusCode")
public class ProcessResult {

    @NonNull
    private final String statusCode;
    @Setter
    private String statusMessage;
    @Setter
    private String data;

    public ProcessResult(String statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public static ProcessResult successResult() {
        return new ProcessResult("OK", "Message processed successfully");
    }

    public static ProcessResult errorResult() {
        return new ProcessResult("ERROR", "Error processing data");
    }


    public String getDataIgnoreCR() {
        return data == null ? "" : data.replace("\r", "");
    }

}
