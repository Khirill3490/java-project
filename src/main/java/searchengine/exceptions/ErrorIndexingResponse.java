package searchengine.exceptions;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ErrorIndexingResponse {
    private boolean result;
    private String error;
}
