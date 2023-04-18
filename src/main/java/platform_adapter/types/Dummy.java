package platform_adapter.types;

import exceptions.PlatformException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import platform_adapter.PlatformAdapter;

public class Dummy implements PlatformAdapter {

    private static final Logger logger = LogManager.getLogger(Dummy.class);

    @Override
    public void communicateExecutionFailure(String failureEndpoint, String message) throws PlatformException {
        logger.debug(String.format("communicateExecutionFailure method called with message %s and endpoint %d", message, failureEndpoint));
    }

}
