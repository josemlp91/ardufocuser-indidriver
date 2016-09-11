## Dependencias


RXTX for Java:
```
sudo apt-get install librxtx-java
```
## Run Script

```
#!/bin/bash

java  -Djava.library.path=/usr/lib/jni -Djava.library.path=/usr/lib/rxtx \
      -jar ./I4JServer/dist/I4JServer.jar  \
      -add=./I4JArdufocuserDriver/dist/I4JArdufocuserDriver.jar
```

### Añadir al Run VM options del proyecto Netbeans:

  ```bash
  -Djava.library.path=/usr/lib/rxtx
  cd /dev
  sudo ln -s ttyACM0 ttyUSB0
  ```
## Instalación en Raspberry PI.

Contamos con Raspbindi una imagen personalizada de Rasbian, con todas las herramientas y servicios INDI instalado.
Puede descargarla en el [enlace](https://drive.google.com/file/d/0Bz7iXJ4BvZ9SbnJPZWkweVhUVjQ/view?usp=sharing)



