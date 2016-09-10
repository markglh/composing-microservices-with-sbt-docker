# Composing Microservices with Docker & SBT

```bash
# Stop & Clean up old containers and Volumes
docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)
docker volume rm markglh-cassandra-node1-data
docker volume rm markglh-cassandra-node2-data
docker volume rm markglh-cassandra-node3-data

# Create named volumes for Cassandra
docker volume create --name markglh-cassandra-node1-data
docker volume create --name markglh-cassandra-node2-data
docker volume create --name markglh-cassandra-node3-data
```

## Blog

All the source code and a markdown version of this blog is available on my github repo.

A client that I’m working with recently joined the "Docker revolution". One of the great things about Docker is how it allows you to replicate the production environment on you local machine, allowing you to run comprehensive integration tests both locally and as part of CI/CD.

With the “Microservice revolution” it has become more and more difficult to setup and manage your environment. We no longer simply deploy our single Monolith into an app container and point that at (god forbid) Oracle. Rather we have 5-500 individual (perhaps clustered) services which all need to communicate with each other, either directly or via an event bus. This is not trivial.

In production you now have a number of choices for managing these environments, Mesos DC/OS, Kunernetes, Amazon ECS, Docker Swarm….. the list gets bigger all the time. At Cake our preferred solution is Mesos DC/OS (see Ani’s blog), however locally we can replicate this setup using docker-compose to define and manage services.

In this first blog I will discuss using docker-compose to manage several Microservices and their dependencies, creating a reproducible environment that can be used for testing any number of services with a single command.

Getting setup
First you’ll need to install docker on your machine. I’d recommend familiarising yourself with Docker first (https://docs.docker.com/engine/understanding-docker/), then installing the appropriate version for your platform (https://docs.docker.com/engine/installation/#installation).

Our demo app
Let’s pretend for a second we have a huge indoor conference venue, and we want to track where each of our attendees go within it - perhaps so we can do some super cool machine learning around it? Maybe we can improve future events automatically? Who knows?!?
Regardless, we’re ploughing ahead and will implement this using a naive microservices approach. We don’t have any gps signal within the venue so we’re going to use Bluetooth Beacons to track everyone in conjunction with our incredible cross platform mobile app.

We have 3 simple (tightly coupled) Scala services which communicate via REST.

- `Beacon Service`, which is responsible for information relating to Bluetooth Beacons (such as the location).
- `Tracking Service`, which is responsible for tracking the location of our users - we’ll call this each time a user connects to a beacon.
- `Aggregation Service`` which acts as a front for the two services, providing a way for clients to retrieve data without multiple simultaneous WAN calls.

Whilst the architecture is somewhat questionable, it’ll serve us just fine for this demonstration.

What we’re aiming to do is find everyone that was at a specific location at a specific time. We’ll ignore how that data came to exist and simply query it as follows:

- The client will call the aggregator service with a location and time.
- The aggregator service will call the Beacon service to find the Id of the Beacon at that location.
- The aggregator service will call the Tracking service to find all attendees connected to the Beacon at the specified time


the user calls the "API aggregator" service, which calls the “Random Service”, which calls Cassandra…. then the whole thing unwinds and the response is returned up the chain and back to the user.

All code is available in my github repo (https://github.com/markglh/composing-microservices-with-sbt-docker), The services are implemented using Scala, HTTP4 for the REST API & Quill for Cassandra. The implementation details aren’t important however (I used this as an excuse to try the aforementioned frameworks).
For this tutorial we’ll make the assumption that the three services have already been implemented and walk through building the Docker images and defining the docker-compose YAML.

Building the images
Before we can start `composing` our services, we need to create and build the docker images. We’ll do this using `sbt-docker` (https://github.com/marcuslonnberg/sbt-docker). I would recommend familiarising yourself with the official Dockerfile reference (https://docs.docker.com/engine/reference/builder/).

First though, we need a `fat jar` which can be executed within our container. For this we’re using sbt-assembly (https://github.com/sbt/sbt-assembly). So let’s get started by importing the required sbt plugins and preparing our `build.sbt` with `sbt-assembly`.

plugins.sbt
build.sbt
…..

With the above, we have a `build.sbt` which creates a `fat jar` (by running `sbt assembly`). We can now get to work defining our Docker image.

….. vars etc

Above we enable the plugin and define the source and target paths of files to be added to our image.

…. docker image

So what’s going on here?

- We extend the official openjdk docker image (https://hub.docker.com/_/openjdk/)
- Ports are exposed for our service
- Environment variables are defined for configuration paths, we can then access these at runtime and in the `entrypoint`
- We copy the resources, configs and the jar to our image. The `sbt docker` task depends upon the assembly task, so this jar has been automatically created for us by this point.
- We define a unversioned `symlink` to our versioned jar.
- We define an entrypoint which describes what to do when the container (a running image) starts, more on that soon.

We’re almost done with our `build.sbt`, one more thing...

…. Image tags

By default `sbt-docker` will tag the image with the `version` defined in our `build.sbt`. Sometimes though, this isn’t what we want - in our CI environment for example we may want to assign custom tags immediately. To allow this we use an `IMAGE_TAG` argument which will be used instead if present. You would use this as follows:
sbt -DIMAGE_TAG=“SomeCustomTag” //TODO validate this!!!

That’s concludes our `build.sbt`, but there’s a few things we’re missing...

docker-resources
Our image requires a few resources external to our service, we’ve created the `docker-resources` directory at the root for this purpose. Let’s walk through these in detail.

- `scripts/wait-for-it.sh` - This is gives us a reliable way to wait for resources to become available on specific ports. We use this to control the startup order of our services, more specifically we're waiting for Cassandra to become available. Whilst Docker compose allows us to control the startup order of containers, it doesn’t wait until the application with the container has completely started on it’s own (https://docs.docker.com/compose/startup-order/) (at least not yet: https://github.com/docker/compose/issues/374). One thing worth noting here, a reliable service should be able to automatically retry and restart should a connection be unavailable rather than simply dying. This is essential for a resilient application (which these are not!).
- `docker-entrypoint.sh` - This file deserves it’s own section..

Docker Entrypoint
Our Docker image sets an entrypoint to defines what happens on startup (https://docs.docker.com/engine/reference/builder/#entrypoint). For us that means invoking a the `docker-entrypoint.sh` script, let’s cover this in more detail. TODO use exec form in build.sbt

…entrypoint

As you can see, we first block startup until all Cassandra nodes are available. Obviously it’s not good practice to hard code this behaviour into your image, it’s much better to wait for one node to become available and handle any retries or restarts properly within your resilient service. In a future blog, we’ll walk through how to properly initialise and manage Cassandra schemas from a docker container, using pillar. Having this sort of delayed startup becomes much more useful in that scenario.

Finally we set our jvm arguments and start the service, notice that we make use of the `LOG_CONF` and `APP_BASE` environment variables which we defined in our `build.sbt` image definition. We have hard-coded the various GC properties - generally these are targeted at a known production environment. However it wouldn’t require much effort to make these configurable and instead provide them at runtime.

This is identical in both services, so we’ll won’t describe it twice!

With that, we can now build our image(s).

`sbt docker`
…..

What about Cassandra?
So a few things to note before we continue. We’re connecting to a Cassandra cluster, which will be defined later using the official Cassandra image from dockerhub (https://hub.docker.com/_/cassandra/). However we need to configure our app to connect to it, we do this in the “Random Service`’s `application.conf`.

….

You’ll see we default to localhost, but allow the value to be overridden using an environment variable. We’ll discuss this further soon.


