package searchengine.dto.searching;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data

public class ErrorSearchingResponse {

    private boolean result;
    private String error;

    public ErrorSearchingResponse(String error) {
        this.result = false;
        this.error = error;
    }
}
