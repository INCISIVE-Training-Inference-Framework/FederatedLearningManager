package platform_adapter.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import platform_adapter.PlatformAdapter;

import java.nio.charset.StandardCharsets;

public class Dummy implements PlatformAdapter {

    private static final Logger logger = LogManager.getLogger(Dummy.class);

    @Override
    public void communicateExecutionEnd(boolean isOk) {
        logger.debug("finishExecution method called with status: " + isOk);
    }

    @Override
    public void uploadModel(byte[] model) {
        logger.debug("uploadModel method called. Model contents: " + new String(model, StandardCharsets.UTF_8));
    }
}
