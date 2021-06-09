#!/bin/bash
docker-compose down
docker run --rm  -it -v $HOME/glygen-array/virtuoso:/data tenforce/virtuoso:1.3.1-virtuoso7.2.2 virtuoso-t +restore-backup backups/backup_1.bp +configfile /data/virtuoso.ini
docker-compose up -d
