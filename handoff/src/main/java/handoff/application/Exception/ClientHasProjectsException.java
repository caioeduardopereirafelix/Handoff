package handoff.application.Exception;

public class ClientHasProjectsException extends RuntimeException {

    public ClientHasProjectsException() {
        super("O cliente tem projetos; apague ou mova os projetos antes");
    }
}
