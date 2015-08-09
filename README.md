# Setup Linux

### Instalacion RXTX:

  sudo mkdir /usr/lib/rxtx
  cd /usr/lib/rxtx
  sudo wget http://jlog.org/v4/linux/i686-pc-linux-gnu/RXTXcomm.jar  	   # Arquitectura x32
  sudo wget http://jlog.org/v4/linux/x86_64-unknown-linux-gnu/RXTXcomm.jar   # Arquitectura x64
  
  sudo wget http://jlog.org/v4/linux/i686-pc-linux-gnu/librxtxSerial.so         # Arquitectura x32
  sudo wget http://jlog.org/v4/linux/x86_64-unknown-linux-gnu/librxtxSerial.so  # Arquitectura x64

### Añadir al Run VM options del proyecto Netbeans:

  -Djava.library.path=/usr/lib/rxtx


  cd /dev
  sudo ln -s ttyACM0 ttyUSB0
