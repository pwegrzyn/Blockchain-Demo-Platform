# Blockchain Demo Platform
[![Build Status](https://travis-ci.org/pwegrzyn/Blockchain-Demo-Platform.svg?branch=master)](https://travis-ci.org/pwegrzyn/Blockchain-Demo-Platform)
[![codebeat badge](https://codebeat.co/badges/d8f160aa-959c-4e4e-9d29-1b6c3ac6be70)](https://codebeat.co/projects/github-com-pwegrzyn-blockchain-demo-platform-master)

Blockchain demo platform for educational purposes

## Prerequisites
* JDK8+ or JDK12 and OpenJFX (check out `gradle.build` for more info)
* Node.js 10.15.x

## Starting
Use the provided `bdp.sh` bash script to run the platform:
```bash
bash ./bdp.sh --help
```

## Current progress
* First version of the SHA256 kernel done
* Stub of the network layer done
* GUI initial views
* Protocol mostly implemented
* Electron visualization module stub done

## TODO
* Tests
* Network layer communication
* Visualization
* Improvement of the SHA256 kernel and effective parallelization
* Communication between Java and Node

## Known Issues
