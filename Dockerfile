# Base image extended with ffmpeg, which tomcat:11-jdk25 does not include.
FROM tomcat:11-jdk25

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*
