# Development Git Repository for the NMR Glycan Array Repository

## What is this

Git repository with git submodules to the various other system repo's  required by the NMR Glycan Array Repository developed at UGA.

Each module can be checked out on it's own, however some are prepared to be run independantly.  Thus this git repo is more of a tool for developers to get an environment up and running quickly.

## Warning

Please note everything developed in this repo is meant to be for development or test environments, to quickly get a system up and running with the latest edge code.

### In case of emergency

If unsure whom to contact with regards to problems of this environment, please raise an issue.

### bashrc environment variables

the bashrc file contains default environment variables that can be used to get rid of the docker-compose warnings.  it should contain default variables for a typical development environment to get it running on a local machine.

### postgres commands

startup the postgres instance:
```
docker-compose up -d postgres
```

localhost port 5432 should be available to connect

to confirm it's running:

```
aoki@bluegold:~/workspace/glygen-array$ docker-compose ps
         Name                       Command              State           Ports         
---------------------------------------------------------------------------------------
glygenarray_postgres_1   docker-entrypoint.sh postgres   Up      0.0.0.0:5432->5432/tcp

```

shutdown:

```
docker-compose stop postgres
```

disable completely:

```
docker-compose rm postgres
```
otherwise it will startup again on next reboot
