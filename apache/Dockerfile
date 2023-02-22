FROM httpd:alpine
MAINTAINER Nobuyuki Aoki <aoki@sparqlite.com>
ADD ./httpd.conf /usr/local/apache2/conf/httpd.conf
ARG GLYGEN_BASEPATH
ENV GLYGEN_URL=$GLYGEN_BASEPATH
RUN echo "RequestHeader set X-Forwarded-Prefix" $GLYGEN_URL > /usr/local/apache2/conf/extra/forwardheader.conf
