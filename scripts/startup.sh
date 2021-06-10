#!/bin/sh
# set environment variables
. ~/workspace/glygen-array-backend/bashrc
# start apache
cd ~/workspace/glygen-array-backend/apache && docker-compose up -d
# start postgres
cd ~/workspace/glygen-array-backend/postgres && docker-compose up -d
# start virtuoso
cd ~/workspace/glygen-array-backend/virtuoso && docker-compose up -d
# start backend application
cd ~/workspace/glygen-array-backend/glygen-array-app && docker-compose up -d
