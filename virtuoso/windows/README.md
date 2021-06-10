# Virtuoso docker for Windows

This folder contains a docker-compose.yml file for hosting a Virtuoso container on Windows

In Windows, container persistence cannot be achieved using a folder. We will instead create a volume for the same.
The following creates the named volume used in the compose file for persistence:

```
docker volume create --name=virtuoso-volume
```

Now you can run ```docker-compose up``` in the same way as mentioned in the previous folder.

# Note for building Virtuoso on Windows

For building a Virtuoso image from scratch, the Dockerfile references virtuoso.sh and virtuoso.ini. If for some reason should Windows complain about CRLF-style line endings, simply make sure to change the line endings for these two files to LF-style. Most advanced text editors(Eg. notepad++) have this option built-in.