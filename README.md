# PLClient
This component is the client in the scenario of federated/parallel learning in the INCISIVE infrastructure. Check the official documentation of the Orchestrator component for a full specification of this component and the overall infrastructure.

It is a JAVA application with maven, can be set up in development like any other maven application. For running the application with docker follow the next instructions (replace first the variables docker_image_name and container_name :
- build image -> docker build --rm -f Dockerfile -t docker_image_name .
- define all required environment variables (check the full documentation)
- run container
    - running container as root user: docker run -dit --rm --name container_name docker_image_name
    - running container as not root user: docker run -dit -u $(id -u):$(id -g) --rm  --name container_name docker_image_name

For deploying the component in a Kubernetes cluster check the documentation of the Orchestrator component.
