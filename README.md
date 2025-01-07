# Federated Learning Manager

_This component was created as an output of the INCISIVE European project software, forming part of the final platform_

### Introduction
The Federated Learning Manager is the component in charge of controlling the training pipeline when more than one data node is included. It manages the communication between the different data nodes and runs the AI Engines in an ordered and controlled pipeline. The AI Engines are agnostic about the pipeline execution or communication and do not need any further modifications to work with federated learning instead of single-node training. 

Check the last version of the D.3.X report for the full abstract description of the component and its functionalities.

### Implementation
The component is implemented in Java with the help of the Maven package manager. There are two behaviours available:
- Server: The component will act as the manager of the federated learning. It can only exist one of this type for every training execution. It can be deployed on the cloud or in a data node. I will be on charge of receiving and merging the multiple output models and sending them back to the clients in every learning iteration.
- Client: The component will act as a client of the federated learning. It needs to be deployed in every node that forms part of the federated learning. It will be on charge of creating a new model in every learning iteration with the node data and sending it back to the server for merging.

The implementation of the behaviours is located at `src/main/java/domain/Server` and `src/main/java/domain/Client` respectively. As aforementioned, please check the official project documentation for a proper description of the logic of the two behaviours here implemented.

The component contains four submodules that define the interactions with external elements. The component accesses each of these submodules through interfaces, that can implemented in any way possible to support multiple scenarios. In this way, the domain of the component is agnostic about all external elements. Specifically, the submodules are the following:
- Communication adapter: It implements the communication between the server and the clients. It is located inside the folder `src/main/java/communication_adapter`. Right now, this submodule is implemented in the following ways:
  - Dummy: Only for testing, prints method calling.
  - Kafka: Using the Kafka framework to received and send messages. It should be already deployed.
- AI Engine Linkage adapter: It implements the communication between the component and the AI Engine. It is located inside the folder `src/main/java/ai_engine_adapter/linkage`. Right now, this submodule is implemented in the following ways:
  - Dummy: Only for testing, prints method calling.
  - Async Rest API: Uses asynchronous HTTP calls to run and stop the AI Engine.
- AI Engine Model Management adapter: It implements how to prepare the models for the AI Engine usage. It is located inside the folder `src/main/java/ai_engine_adapter/model_management`. Right now, this submodule is implemented in the following ways:
  - Dummy: Only for testing, prints method calling.
  - Default: Follows the folder structure defined by the AI Engine documentation. The component and the AI Engine should share the same filesystem (or part of it).
- Platform adapter: It implements how to communicate with the platform in the case of any failure. This communication is optional, it depends on the input arguments provided when running the component. It is located inside the folder `src/main/java/platform_adapter`. Right now, this submodule is implemented in the following ways:
  - Dummy: Only for testing, prints method calling.
  - INCISIVE: Uses an HTTP call to communicate with the Orchestrator following its documentation.

If willing to add new implementations for the interfaces / submodules, it is only needed to inherit the corresponding interface and update the factory class located at `src/main/java/Factory`.

### How to set up
The component can be built with Maven or any modern IDE. Nevertheless, the instructions described here show how to build the component with docker.

It is only necessary to build the docker image with the provided Dockerfile i.e. `docker build -f Dockerfile -t federated-learning-manager .`

### How to run
There are two needed inputs for running the component: environment variables and input arguments.

Regarding the environment variables, the following list shows the required ones:
- COMMUNICATION_ADAPTER: tells the component which implementation to use for the Communication adapter interface. Possible values: `KAFKA` and `DUMMY`.
- AI_ENGINE_LINKAGE_ADAPTER: tells the component which implementation to use for the AI Engine Linkage adapter interface. Possible values: `ASYNC_REST_API` and `DUMMY`.
- AI_ENGINE_MODEL_MANAGEMENT_ADAPTER: tells the component which implementation to use for the AI Engine Model Management adapter interface. Possible values: `DEFAULT` and `DUMMY`.
- PLATFORM_ADAPTER: tells the component which implementation to use for the Platform adapter interface. Possible values: `INCISIVE` and `DUMMY`.

Notice that the submodules that implement these interfaces can contain other required environment variables, or even optional ones. Check the implementation of the submodules for their proper configuration following the previously described directory locations.

Regarding the input arguments, they differ depending on the chosen component behaviour:
- Server: `server $EXECUTION_ID $NUMBER_ITERATIONS $NUMBER_CLIENTS $USE_CASE`.
  - server: tells the component that needs to run the server behaviour.
  - EXECUTION_ID: an execution identifier that should be shared between the server and all the clients that are in the same federated learning execution. It should be unique, it is the way to have multiple learning executions running together without clashes.
  - NUMBER_ITERATIONS: the number of iterations that should be run in the federated learning execution.
  - NUMBER_CLIENTS: the number of clients that are run in the federated learning execution.
  - USE_CASE: the use case to run in the AI Engine. In the case of INCISIVE, this should correspond to `merging_models`.
- Client: `client $EXECUTION_ID $CLIENT_ID $NUMBER_ITERATIONS $USE_CASE`.
  - client: tells the component that needs to run the client behaviour.
  - EXECUTION_ID: idem
  - CLIENT_ID: the identifier of the client. It should be unique between the clients of the same federated learning execution.
  - NUMBER_ITERATIONS: idem
  - USE_CASE: the use case to run in the AI Engine. In the case of INCISIVE, this should correspond to `training_from_scratch` or `training_from_pretrained_model`.

After setting up the environment variables and choosing the proper input arguments, the component can run as follows (completing $ENV_X $INPUT_ARGS with the corresponding environment variables and input arguments):
```
docker run -it federated-learning-manager \ 
-e COMMUNICATION_ADAPTER=$ENV_1 \ 
-e AI_ENGINE_LINKAGE_ADAPTER=$ENV_2 \ 
-e AI_ENGINE_MODEL_MANAGEMENT_ADAPTER=$ENV_3 \
-e AI_ENGINE_MODEL_MANAGEMENT_ADAPTER=$ENV_4 \
-e PLATFORM_ADAPTER=$ENV_5 \
$INPUT_ARGS
```
In the case that an external element needs to be notified in case of failure, please use the following command:
```
docker run -it federated-learning-manager \ 
-e COMMUNICATION_ADAPTER=$ENV_1 \ 
-e AI_ENGINE_LINKAGE_ADAPTER=$ENV_2 \ 
-e AI_ENGINE_MODEL_MANAGEMENT_ADAPTER=$ENV_3 \
-e AI_ENGINE_MODEL_MANAGEMENT_ADAPTER=$ENV_4 \
-e PLATFORM_ADAPTER=$ENV_5 \
$INPUT_ARGS \
--failure-endpoint http://HOSTNAME:PORT/PATH_TO_HIT_IN_CASE_OF_ERROR
```
