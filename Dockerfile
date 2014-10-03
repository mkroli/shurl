FROM dockerfile/java:openjdk-7-jre
MAINTAINER mkroli

ENV JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64

ADD build.sbt /tmp/shurl/build.sbt
ADD web.sbt /tmp/shurl/web.sbt
ADD project/plugins.sbt /tmp/shurl/project/plugins.sbt
ADD src /tmp/shurl/src

WORKDIR /tmp/shurl
RUN wget http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.13.6/sbt-launch.jar -O sbt.jar && \
    java -XX:MaxPermSize=128m -jar sbt.jar packArchive && \
    mkdir -p /opt/shurl && \
    tar --strip-components=1 -C /opt/shurl -xzf /tmp/shurl/target/shurl*.tar.gz && \
    rm -rf /root/.ivy /root/.sbt /tmp/shurl

WORKDIR /opt/shurl
EXPOSE 8080
ENTRYPOINT JVM_OPT=-Dcassandra.seeds.0=${CASSANDRA_PORT_9042_TCP_ADDR} /opt/shurl/bin/shurl
