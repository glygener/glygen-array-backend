#!/bin/sh
# https://github.com/CWSpear/local-persist
#curl -L https://github.com/CWSpear/local-persist/releases/download/v1.3.0/local-persist-linux-amd64 > /usr/local/bin/docker-volume-local-persist
cd /tmp
wget https://github.com/CWSpear/local-persist/releases/download/v1.3.0/local-persist-linux-amd64
sudo cp local-persist-linux-amd64 /usr/local/bin/docker-volume-local-persist
rm local-persist-linux-amd64

chmod +x /usr/local/bin/docker-volume-local-persist

#curl -L https://github.com/CWSpear/local-persist/blob/master/init/systemd.service > /etc/systemd/system/docker-volume-local-persist.service
#wget https://github.com/CWSpear/local-persist/blob/master/init/systemd.service
sudo cp docker-volume-local-persist.service /etc/systemd/system/docker-volume-local-persist.service
sudo systemctl daemon-reload
sudo systemctl enable docker-volume-local-persist
sudo systemctl start docker-volume-local-persist
