package viettel.dac.toolserviceregistry.exception;

public class DependencyToolNotFoundException extends RuntimeException{
    public DependencyToolNotFoundException(String id) {
        super("Dependency tool not found");
    }

}
