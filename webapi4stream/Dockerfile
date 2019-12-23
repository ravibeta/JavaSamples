# =====================================================================================
FROM gradle:5.4.1-jdk8 as GradleBuilder

ARG BUILD_VERSION=0.0.0-localdev

COPY --chown=gradle:gradle logservice /home/gradle/src/

WORKDIR /home/gradle/src

ENV GRADLE_USER_HOME=/home/gradle
RUN ls /home/gradle/src

ENV APP_NAME logservice
ENV APP_HOME /opt/$APP_NAME
ENV TOOLS_HOME /opt/tools

