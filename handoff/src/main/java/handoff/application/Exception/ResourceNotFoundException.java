package handoff.application.Exception;

// Também usada para recurso de OUTRA organização: responder 404 (e não 403)
// não revela que o recurso existe.
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource) {
        super(resource + " não encontrado");
    }
}
