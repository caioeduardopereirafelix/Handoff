package handoff.application.Exception;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("Este e-mail já está cadastrado");
    }
}
