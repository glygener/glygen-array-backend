#!/bin/bash

echo "this is the /virtuoso directory:"
ls -al /virtuoso/

if [ ! -f /virtuoso/db/virtuoso.ini ]; then
    echo "ini file does not exist! copying default configuration from source replacing to /virtuoso folder"
    echo cp -R /usr/local/virtuoso-opensource/var/lib/virtuoso/db /virtuoso/
    cp -R /usr/local/virtuoso-opensource/var/lib/virtuoso/db /virtuoso/
    echo mkdir -p /virtuoso/db/
    mkdir -p /virtuoso/db/
#    echo sed -i -e 's/\/usr\/local\/virtuoso-opensource\/var\/lib\/virtuoso/\/virtuoso/g' /virtuoso/db/virtuoso.ini
#    sed -i -e 's/\/usr\/local\/virtuoso-opensource\/var\/lib\/virtuoso/\/virtuoso/g' /virtuoso/db/virtuoso.ini
fi

# in case of permissions issue
if [ ! -f /virtuoso/db/virtuoso.ini ]; then
    echo "no virtuoso configuration found, virtuoso cannot start, trying to rewrite permissions and copy default"
    echo chown -R $USER:$GROUPS /virtuoso/
    chown -R $USER:$GROUPS /virtuoso/
    cp -R /usr/local/virtuoso-opensource/var/lib/virtuoso/db /virtuoso/
#    echo sed -i -e 's/\/usr\/local\/virtuoso-opensource\/var\/lib\/virtuoso/\/virtuoso/g' /virtuoso/db/virtuoso.ini
#    sed -i -e 's/\/usr\/local\/virtuoso-opensource\/var\/lib\/virtuoso/\/virtuoso/g' /virtuoso/db/virtuoso.ini
    exit 1;
fi

echo "running:>/usr/local/virtuoso-opensource/bin/virtuoso-t -df +configfile /virtuoso/db/virtuoso.ini"
/usr/local/virtuoso-opensource/bin/virtuoso-t -df +configfile /virtuoso/db/virtuoso.ini
