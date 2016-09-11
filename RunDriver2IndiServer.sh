#!/bin/bash

java  -Djava.library.path=/usr/lib/jni -Djava.library.path=/usr/lib/rxtx  -jar ./I4JServer/dist/I4JServer.jar -add=./I4JArdufocuserDriver/dist/I4JArdufocuserDriver.jar
