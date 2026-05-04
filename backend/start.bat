@echo off
set MAVEN_OPTS=-Xmx512m
cd /d "%~dp0"
mvn spring-boot:run