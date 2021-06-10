# Postgres docker for Windows

This folder contains a docker-compose.yml file for hosting a Postgres container on Windows

In Windows, container persistence cannot be achieved using a folder. We will instead create a volume for the same.
The following creates the named volume used in the compose file for persistence:

```
docker volume create --name=postgres-volume
```

Now you can run ```docker-compose up``` in the same way as mentioned in the previous folder.