package handoff.application.Exception;


public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource) {
        super(resource + " não encontrado");
    }
}
