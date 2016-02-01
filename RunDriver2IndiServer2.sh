#!/bin/bash

#http://stackoverflow.com/questions/10751304/java-lang-unsatisfiedlinkerror-no-rxtxserial-in-java-library-path-thrown-while

java  -Djava.library.path=/usr/lib/jni -Djava.library.path=/usr/lib/rxtx  -jar ./I4JServer/dist/I4JServer.jar -add=/home/josemlp/workspace/proyectos/I4JArdufocuserDriver/dist/I4JArdufocuserDriver.jar
