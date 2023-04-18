package communication_adapter;

import exceptions.CommunicationException;

public interface CommunicationAdapter {

    void cleanEnvironment() throws CommunicationException;
}
