FROM debian:jessie
MAINTAINER Nobuyuki Paul Aoki <aokinobu@gmail.com>

EXPOSE 8890

RUN apt-get update && \
  apt-get -y install dpkg-dev build-essential autoconf automake libtool flex bison gperf gawk m4 make odbcinst libxml2-dev libssl-dev libreadline-dev unzip git-core openssl net-tools procps && \
  apt-get clean -qq -y && \
  apt-get autoclean -qq -y && \
  apt-get autoremove -qq -y &&  \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /tmp/*

#ADD /virtuoso-opensource /virtuoso-opensource
#https://github.com/openlink/virtuoso-opensource/blob/stable/7/NEWS
#https://github.com/openlink/virtuoso-opensource/issues/251
RUN git clone -v https://github.com/openlink/virtuoso-opensource.git 2>&1 > /var/log/virtusos-git.log && cd /virtuoso-opensource && \
  git fetch origin && \
  git checkout tags/v7.2.4.2 && \
  ./autogen.sh && \
  CFLAGS="-O2 -m64" && export CFLAGS && ./configure && \
  make && make install 2>&1 > /var/log/virtusos-compile.log

ADD run.sh /run.sh
RUN chmod a+x /run.sh

RUN mkdir /virtuoso

RUN echo vm.swappiness=10 >> /etc/sysctl.conf

# let's reuse all the work done in the script
ADD config/virtuoso.ini /usr/local/virtuoso-opensource/var/lib/virtuoso/db/virtuoso.ini
VOLUME [/virtuoso]
#ENTRYPOINT /sbin/sysctl -w vm.swappiness=10; /run.sh
CMD ["/run.sh"]
