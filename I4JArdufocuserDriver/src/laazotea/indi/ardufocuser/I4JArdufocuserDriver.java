/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package laazotea.indi.ardufocuser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import laazotea.indi.Constants.PropertyPermissions;
import laazotea.indi.Constants.PropertyStates;
import laazotea.indi.INDIException;
import laazotea.indi.driver.INDIBLOBElementAndValue;
import laazotea.indi.driver.INDIBLOBProperty;
import laazotea.indi.driver.INDIConnectionHandler;
import laazotea.indi.driver.INDIFocuserDriver;
import laazotea.indi.driver.INDINumberElement;
import laazotea.indi.driver.INDINumberElementAndValue;
import laazotea.indi.driver.INDINumberProperty;
import laazotea.indi.driver.INDIPortProperty;
import laazotea.indi.driver.INDISwitchElementAndValue;
import laazotea.indi.driver.INDISwitchProperty;
import laazotea.indi.driver.INDITextElementAndValue;
import laazotea.indi.driver.INDITextProperty;

import laazotea.indi.ardufocuser.ArduinoConnection;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import static java.lang.Thread.sleep;

/**
 *
 * @author @josemlp91 and @zerjillo
 */
public class I4JArdufocuserDriver extends INDIFocuserDriver implements INDIConnectionHandler, Runnable {

    /**
     * The PORTS property.
     */
    private INDIPortProperty portP;
    private OutputStream os;
    private BufferedReader br;
    private String portName;

    private FileInputStream fwInput;
    private FileOutputStream fwOutput;

    private Thread readingThread;
    private boolean readerEnd;
    private boolean readerEnded;

    private INDINumberProperty stepPerPulseP;
    private INDINumberElement stepPerPulseE;

    private ArduinoConnection ac;

    //private INDISwitchProperty motorP;
    //private INDISwitchElement motorE;
    //.. todas las variables privadas.
    public I4JArdufocuserDriver(InputStream inputStream, OutputStream outputStream) {
        super(inputStream, outputStream);

        portP = INDIPortProperty.createSaveablePortProperty(this, "/dev/ttyUSB0");

        // Creamos una instancia de Arduino.
        // motorP = new INDISwitchProperty(this, "factory_settings", "Factory Settings", "Expert Configuration", PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.AT_MOST_ONE);
        // motorE = new INDISwitchElement(motorP, "factory_setting", "Factory Settings", SwitchStatus.OFF);
        this.addProperty(portP);

        initializeStandardProperties();

        if (stepPerPulseP == null) {
            stepPerPulseP = INDINumberProperty.createSaveableNumberProperty(this, "stepPerPulse", "Steps per Pulse", "Configuration", PropertyStates.IDLE, PropertyPermissions.RW);
            stepPerPulseE = stepPerPulseP.getElement("stepPerPulse_value");
            if (stepPerPulseE == null) {
                stepPerPulseE = new INDINumberElement(stepPerPulseP, "stepPerPulse_value", "Steps per Pulse", "1", "1", "99", "1", "%.0f");
            }
        }

    }

    @Override
    public int getMaximumAbsPos() {
        return 9999999;
    }

    @Override
    public int getMinimumAbsPos() {
        return -9999999;
    }

    @Override
    public int getInitialAbsPos() {
        return 0;
    }

    @Override
    protected int getMaximumSpeed() {
        return 500;
    }

    @Override
    public void absolutePositionHasBeenChanged() {

        String msg = "AG?" + getDesiredAbsPosition();
        System.out.println(msg);

        sendMessageToArdufocus(msg);

    }

    @Override
    public void speedHasBeenChanged() {

        String msg = "ASPEED?" + getCurrentSpeed();
        //System.out.println(msg);
        sendMessageToArdufocus(msg);
        desiredSpeedSet();
    }

    @Override
    public void stopHasBeenRequested() {

        String msg = "ASTOP?";
        sendMessageToArdufocus(msg);
        stopped();
    }

    @Override
    public String getName() {
        return "ArduFocuser";
    }

    @Override
    public void processNewTextValue(INDITextProperty property, Date timestamp, INDITextElementAndValue[] elementsAndValues) {
        portP.processTextValue(property, elementsAndValues);
    }

    @Override
    public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp, INDIBLOBElementAndValue[] elementsAndValues) {
    }

    @Override
    public void processNewNumberValue(INDINumberProperty property, Date timestamp, INDINumberElementAndValue[] elementsAndValues) {
        super.processNewNumberValue(property, timestamp, elementsAndValues);

        if (property == stepPerPulseP) {
            System.out.println(elementsAndValues[0].getValue());

            stepPerPulseE.setValue("99");

            stepPerPulseP.setState(PropertyStates.OK);
            try {
                updateProperty(stepPerPulseP);
            } catch (INDIException e) {

            }
        }
    }

    @Override
    public void processNewSwitchValue(INDISwitchProperty property, Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {
        super.processNewSwitchValue(property, timestamp, elementsAndValues);
    }

    @Override
    public void driverConnect(Date timestamp) throws INDIException {

        // Creamos el conector.
        ac = new ArduinoConnection();
        boolean connected = ac.connectToBoard();

        if (connected) {
            System.out.println("Connected!");
        } else {
            System.out.println("Could not connect to Arduino :-(");
            return;
        }
        
       // Añadimos el listener.
       ac.addListener(new ArdufocuserListener(ac));

        showSpeedProperty();
        showStopFocusingProperty();
        addProperty(stepPerPulseP);

    }

    @Override
    public void driverDisconnect(Date timestamp) throws INDIException {

        System.out.println("Disconnecting Ardufocuser");
        System.out.flush();

        removeProperty(stepPerPulseP);
        hideSpeedProperty();
        hideStopFocusingProperty();

        try {
            if (readingThread != null) {
                readerEnd = true;
                readingThread = null;
            }

            sleep(200);

            if (fwInput != null) {
                fwInput.close();
                fwOutput.close();
            }

            fwInput = null;
            fwOutput = null;
        } catch (IOException e) {
        }

        System.out.println("Disconnected Ardufocuser");
        System.out.flush();

    }

    //Hebra lectora, meter dentro del evento de lectura. 
    String[] posCommand = new String[]{"APOSITION?", "ASTOP?"};

    
    // No necesitamos que sea una hebra .
    @Override
    public void run() {
        readerEnded = false;
        String buffer = "";
        byte[] readed = new byte[200];

        while (!readerEnd) {
            try {
                if (fwInput.available() > 0) {
                    int r = fwInput.read(readed, 0, 200);
                    buffer += new String(readed, 0, r);

                    boolean continueParsing = true;

                    while (continueParsing) {

                        //System.out.println(buffer);
                        int pos = 1000000;

                        for (int i = 0; i < posCommand.length; i++) {
                            int newPos = buffer.indexOf(posCommand[i]);

                            if (newPos != -1) {
                                if (pos > newPos) {
                                    pos = newPos;
                                }
                            }
                        }

                        if (pos != 1000000) {
                            buffer = buffer.substring(pos);

                            if (buffer.length() >= 20) {
                                ///Aqui el parseo de cada comando.
                                if (buffer.startsWith("APOSITION?")) {

                                    int pp = buffer.indexOf("?") + 1;
                                    String s = buffer.substring(pp, 20).trim();

                                    try {
                                        int p = Integer.parseInt(s);

                                        positionChanged(p);
                                    } catch (NumberFormatException e) {
                                        System.out.println("XXXX");

                                    }
                                } else if (buffer.startsWith("ASTOP?")) {

                                    finalPositionReached();

                                }

                                buffer = buffer.substring(20);
                            } else {
                                continueParsing = false;
                            }
                        } else {
                            continueParsing = false;
                        }
                    }
                }
            } catch (IOException e) {
                readerEnd = true;
            }

            sleep(200);
        }

        readerEnded = true;
    }

    // Metodo a la escucha del puerto serie del Ardufocuser.
    private static class ArdufocuserListener implements SerialPortEventListener {

        private ArduinoConnection ac;

        public ArdufocuserListener(ArduinoConnection ac) {
            this.ac = ac;
        }

        @Override
        public void serialEvent(SerialPortEvent serialPortEvent) {
            if (serialPortEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                String inLine = ac.readLine();
                
                //Implementar aqui el parser.
                
                System.out.println("GOT: " + inLine);
            }
        }
    }

    private void sendMessageToArdufocus(String message) {
       ac.sendString(message);
    }

    private void sleep(int milis) {
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
        }
    }

}
