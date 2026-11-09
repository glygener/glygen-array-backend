FROM httpd:2.4-alpine
ADD ./httpd.conf /usr/local/apache2/conf/httpd.conf
ARG GLYGEN_BASEPATH
RUN echo "RequestHeader set X-Forwarded-Prefix" $GLYGEN_BASEPATH > /usr/local/apache2/conf/extra/forwardheader.conf
