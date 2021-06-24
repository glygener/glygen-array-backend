#!/bin/bash
echo "executing restore"
docker run --rm  -it -v $HOME/glygen-array/virtuoso:/data tenforce/virtuoso:1.3.1-virtuoso7.2.2 virtuoso-t +restore-backup backup_ +configfile /data/virtuoso.ini
echo "done"
