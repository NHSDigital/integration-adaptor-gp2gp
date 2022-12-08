FROM openjdk:17-jdk-alpine

ENV ACTIVEMQ_VERSION 5.16.3
ENV ACTIVEMQ apache-activemq-$ACTIVEMQ_VERSION
ENV ACTIVEMQ_AMQP=5672 ACTIVEMQ_UI=8161
ENV SHA512_VAL=3394afe9963f55cf62dafb287bdc58cac1fb76a614cc87939bec7b7909972eb5b3069022a9eb324242a315899b1096335c0531af75152c74eb309c0c5981b217

ENV ACTIVEMQ_HOME /opt/activemq

RUN set -x && \
    mkdir -p /opt && \
    apk --update add --virtual build-dependencies curl

# Uncomment the next line to use a local binary
#COPY ./$ACTIVEMQ-bin.tar.gz $ACTIVEMQ-bin.tar.gz
# Comment out the next line to use a local binary
RUN curl https://archive.apache.org/dist/activemq/$ACTIVEMQ_VERSION/$ACTIVEMQ-bin.tar.gz -o $ACTIVEMQ-bin.tar.gz

# Validate checksum
RUN if [ "$SHA512_VAL" != "$(sha512sum $ACTIVEMQ-bin.tar.gz | awk '{print($1)}')" ];\
    then \
        echo "sha512 values doesn't match! exiting."  && \
        exit 1; \
    fi;


RUN tar xzf $ACTIVEMQ-bin.tar.gz -C  /opt && \
    ln -s /opt/$ACTIVEMQ $ACTIVEMQ_HOME && \
    addgroup -S activemq && adduser -S -H -G activemq -h $ACTIVEMQ_HOME activemq && \
    chown -R activemq:activemq /opt/$ACTIVEMQ && \
    chown -h activemq:activemq $ACTIVEMQ_HOME && \
    apk del build-dependencies && \
    rm -rf /var/cache/apk/*

USER activemq

COPY --chown=activemq:activemq ./activemq.xml $ACTIVEMQ_HOME/conf/activemq.xml
COPY --chown=activemq:activemq ./jetty.xml $ACTIVEMQ_HOME/conf/jetty.xml

WORKDIR $ACTIVEMQ_HOME
EXPOSE $ACTIVEMQ_AMQP $ACTIVEMQ_UI

CMD ["/bin/sh", "-c", "bin/activemq console"]