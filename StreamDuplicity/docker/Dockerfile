FROM gradle:5.4.1-jdk8
ADD bin/ /app/bin/
ADD conf/ /app/conf/
ADD build/resources/main/config.properties /app/conf/config.properties
ADD build/resources/main/config.properties /app/lib/config.properties
ARG BUILD_VERSION
ADD streamduplicity-${BUILD_VERSION}-all.jar /app/lib/streamduplicity.jar
