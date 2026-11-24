package handoff.application.Exception;

public class AttachmentNotUploadedException extends RuntimeException {

    public AttachmentNotUploadedException() {
        super("O arquivo não foi enviado ou não corresponde ao que foi autorizado");
    }
}
