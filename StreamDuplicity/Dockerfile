FROM gradle:5.4.1-jdk8 as gradlebuilder
ARG BUILD_VERSION
CMD mkdir -p /app
CMD mkdir -p /app/lib
CMD mkdir -p /app/conf
CMD mkdir -p /data/config
COPY build /app/build
COPY build/libs /app/lib
COPY build/resources/main /app/conf
COPY src/main/resources/config.properties /app/conf/config.properties
COPY src/main/resources/config.properties /data/config/config.properties
COPY build/libs/streamduplicity-${BUILD_VERSION}-all.jar /app/lib/streamduplicity.jar
COPY src/conf/logback.xml /app/conf/logback.xml

WORKDIR /app
