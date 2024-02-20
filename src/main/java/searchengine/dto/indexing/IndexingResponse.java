package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@AllArgsConstructor
public class IndexingResponse {
    private boolean result;
    private String message;

    public IndexingResponse(boolean result) {
        this.result = result;
    }
}
