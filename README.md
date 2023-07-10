# Development Git Repository for the Glycan Array Repository

## What is this

Git repository with git submodules to the various other system repo's  required by the Glycan Array Repository developed at UGA.

Each module can be checked out on its own; however some are prepared to be run independently.  Thus, this git repo is more of a tool for developers to get an environment up and running quickly.

## Warning

Please note everything developed in this repo is meant to be for development or test environments, to quickly get a system up and running with the latest edge code.

### In case of emergency

If unsure whom to contact with regards to problems of this environment, please raise an issue.

### bashrc environment variables

The bashrc file contains default environment variables that can be used to get rid of the docker-compose warnings.  It should contain default variables for a typical development environment to get it running on a local machine.

The passwords are stored in .secrets file on the server (array2018@ggarray's home directory) and bashrc includes those environment variables from .secrets file in addition to the ones listed in the file.

### postgres commands

to startup postgres, please make sure the "glygen-array" folder is created in your $HOME.
Then cd into the postgres folder, and execute the following:
```
docker-compose up
```

localhost port 5432 should be available to connect

to confirm it's running:

```
docker-compose ps
         Name                       Command              State           Ports         
---------------------------------------------------------------------------------------
glygenarray_postgres_1   docker-entrypoint.sh postgres   Up      0.0.0.0:5432->5432/tcp

```

shutdown:

```
docker-compose stop
```

disable completely:

```
docker-compose rm
```
otherwise it will startup again on next reboot
