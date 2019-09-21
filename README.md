# Blockchain Demo Platform
[![Build Status](https://travis-ci.org/pwegrzyn/Blockchain-Demo-Platform.svg?branch=master)](https://travis-ci.org/pwegrzyn/Blockchain-Demo-Platform)
[![codebeat badge](https://codebeat.co/badges/d8f160aa-959c-4e4e-9d29-1b6c3ac6be70)](https://codebeat.co/projects/github-com-pwegrzyn-blockchain-demo-platform-master)

Blockchain demo platform for educational purposes

## Prerequisites
* JDK8+ or JDK12 and OpenJFX (check out `gradle.build` for more info)
* Node.js 10.15.x

## Starting
```bash
mvn packge
java -jar <generated.jar>
```

## Notes
The application has the ability to connect to an external client to pass the data for visualization
(for example by using the provided Electron client)
