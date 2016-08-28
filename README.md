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



