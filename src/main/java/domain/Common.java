package domain;

import ai_engine_adapter.linkage.AIEngineLinkageAdapter;
import communication_adapter.CommunicationAdapter;
import exceptions.AIEngineException;
import exceptions.CommunicationException;
import exceptions.FailureEndSignal;
import exceptions.PlatformException;
import org.apache.logging.log4j.Logger;
import platform_adapter.PlatformAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Common {

    public static void finishExecution(
            boolean success,
            String failureMessage,
            String failureEndpoint,
            PlatformAdapter platformAdapter,
            CommunicationAdapter communicationAdapter,
            AIEngineLinkageAdapter aiEngineLinkageAdapter,
            Logger logger
    ) throws FailureEndSignal {
        Exception exceptionsThrown = null;

        if (!success) {
            try {

                // contact external endpoint if required
                if (failureEndpoint != null) {
                    try {
                        logger.debug("Communicating execution end to the platform with status: " + success);
                        platformAdapter.communicateExecutionFailure(failureEndpoint, failureMessage);
                    } catch (PlatformException e) {
                        e.print(logger);
                        exceptionsThrown = e;
                    }
                }

                // extract error message to file
                Path path = Paths.get("error_message.txt");
                byte[] strToBytes = failureMessage.getBytes();
                Files.write(path, strToBytes);

            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
                exceptionsThrown = e;
            }
        }

        try {
            // clean communication adapter environment
            logger.debug("Cleaning environment");
            communicationAdapter.cleanEnvironment();
        } catch (CommunicationException e) {
            e.print(logger);
            exceptionsThrown = e;
        }

        try {
            // end AI Engine execution own server
            logger.debug("Ending AI Engine execution own server");
            aiEngineLinkageAdapter.clean();
        } catch (AIEngineException e) {
            e.print(logger);
            exceptionsThrown = e;
        }

        try {
            // end AI Engine execution
            logger.debug("Ending AI Engine execution");
            aiEngineLinkageAdapter.end();
        } catch (AIEngineException e) {
            e.print(logger);
            exceptionsThrown = e;
        }

        if (exceptionsThrown != null || !success) {
            if (!success) throw new FailureEndSignal(failureMessage);
            else throw new FailureEndSignal(exceptionsThrown.getMessage());
        }
    }

}
