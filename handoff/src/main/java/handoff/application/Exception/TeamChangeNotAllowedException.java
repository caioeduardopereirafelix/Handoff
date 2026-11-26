package handoff.application.Exception;

public class TeamChangeNotAllowedException extends RuntimeException {

    public TeamChangeNotAllowedException(String message) {
        super(message);
    }
}
