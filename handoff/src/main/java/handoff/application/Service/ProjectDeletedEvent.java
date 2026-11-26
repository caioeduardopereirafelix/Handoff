package handoff.application.Service;

import java.util.List;

public record ProjectDeletedEvent(List<String> storageKeys) {
}
