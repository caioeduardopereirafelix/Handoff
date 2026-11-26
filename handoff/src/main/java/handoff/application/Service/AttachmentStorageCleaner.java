package handoff.application.Service;

import handoff.application.Config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttachmentStorageCleaner {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectDeleted(ProjectDeletedEvent event) {
        for (String key : event.storageKeys()) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(storageProperties.bucket())
                        .key(key)
                        .build());
            } catch (SdkException e) {
                log.warn("Não foi possível apagar o arquivo {} do armazenamento", key, e);
            }
        }
    }
}
