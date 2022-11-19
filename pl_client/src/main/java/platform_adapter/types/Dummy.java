package platform_adapter.types;

import exceptions.PlatformException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import platform_adapter.PlatformAdapter;

public class Dummy implements PlatformAdapter {

    private static final Logger logger = LogManager.getLogger(Dummy.class);

    @Override
    public void communicateExecutionEnd(boolean isOk) throws PlatformException {
        logger.debug("finishExecution method called with status: " + isOk);
    }
}
