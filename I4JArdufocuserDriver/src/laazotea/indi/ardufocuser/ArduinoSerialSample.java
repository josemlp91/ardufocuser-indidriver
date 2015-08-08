package laazotea.indi.ardufocuser;

import laazotea.indi.ardufocuser.ArduinoConnection;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import static java.lang.Thread.sleep;

public class ArduinoSerialSample {

    public static void main(String[] args) throws InterruptedException {
        // Open the connection
        ArduinoConnection ac = new ArduinoConnection();
        boolean connected = ac.connectToBoard();

        if (connected) {
            System.out.println("Connected!");
        } else {
            System.out.println("Could not connect to Arduino :-(");
            return;
        }

        // Add listener for Arduino serial output
        ac.addListener(new SampleListener(ac));

        // Write a message to the Arduino over Serial
        ac.sendString("0");
        sleep(100);
        ac.sendString("1");
        sleep(100);
        ac.sendString("2");
        sleep(100);
        ac.sendString("3");

        // Make sure to call this!
        ac.close();
    }

    private static class SampleListener implements SerialPortEventListener {

        private ArduinoConnection ac;

        public SampleListener(ArduinoConnection ac) {
            this.ac = ac;
        }

        @Override
        public void serialEvent(SerialPortEvent serialPortEvent) {
            if (serialPortEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                String inLine = ac.readLine();
                System.out.println("GOT: " + inLine);
            }
        }
    }
}


/*        
       // CÓDIGO ARDUINO:           

            const int LED_Rojo = 13;
            const int LED_Amarillo=12;
            int inByte = 0;

            void setup(){
                Serial.begin(9600); //Open the serial port
                pinMode(LED_Amarillo, OUTPUT);
                pinMode(LED_Rojo, OUTPUT);
                digitalWrite(LED_Amarillo, LOW);
                digitalWrite(LED_Rojo, LOW);
              }

            void loop(){


                if(Serial.available() > 0){

                    inByte = Serial.read();
                    Serial.println(inByte);
                    if(inByte == '0')
                        digitalWrite(LED_Amarillo, LOW);
                    else if(inByte=='1')
                        digitalWrite(LED_Amarillo, HIGH);
                    else if(inByte=='2')
                        digitalWrite(LED_Rojo, LOW);
                    else if(inByte=='3')
                        digitalWrite(LED_Rojo, HIGH);
                }
            }



*/
