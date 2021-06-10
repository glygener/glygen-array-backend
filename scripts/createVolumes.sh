#!/bin/sh

# This will create ALL volumes
echo docker volume create -d local-persist -o mountpoint=$HOME/$SITE_CODE/log --name=$SITE_CODE-log
docker volume create -d local-persist -o mountpoint=$HOME/$SITE_CODE/log --name=$SITE_CODE-log
mkdir -p $HOME/$SITE_CODE/log/logs
mkdir -p $HOME/$SITE_CODE/log/data
echo docker volume create -d local-persist -o mountpoint=$HOME/$SITE_CODE/jenkins --name=$SITE_CODE-jenkins
docker volume create -d local-persist -o mountpoint=$HOME/$SITE_CODE/jenkins --name=$SITE_CODE-jenkins
mkdir -p $HOME/$SITE_CODE/jenkins/jenkins_home
echo docker volume create -d local-persist -o mountpoint=$HOME/$SITE_CODE/postgres --name=$SITE_CODE-postgres
docker volume create -d local-persist -o mountpoint=$HOME/$SITE_CODE/postgres --name=$SITE_CODE-postgres
mkdir $HOME/$SITE_CODE/postgres
echo docker volume create -d local-persist -o mountpoint=$HOME/$SITE_CODE/virtuoso --name=$SITE_CODE-virtuoso
docker volume create -d local-persist -o mountpoint=$HOME/$SITE_CODE/virtuoso --name=$SITE_CODE-virtuoso
mkdir -p $HOME/$SITE_CODE/virtuoso/virtuoso
