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
import static java.lang.Thread.sleep;
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

import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import laazotea.indi.ardufocuser.ArduinoConnection;

/**
 * @author @josemlp  @zerjillo
 */
public class I4JArdufocuserDriver extends INDIFocuserDriver implements INDIConnectionHandler {

    static {
        System.setProperty("java.library.path", "/usr/lib/rxtx");
    }

  /**
     * The PORTS property.
     */
    private INDIPortProperty portP;
    private INDINumberProperty stepPerPulseP;
    private INDINumberElement stepPerPulseE;

    private ArduinoConnection ac;

    //private INDISwitchProperty motorP;
    //private INDISwitchElement motorE;
    //.. todas las variables privadas.
    public I4JArdufocuserDriver(InputStream inputStream, OutputStream outputStream) {
        super(inputStream, outputStream);

        // No se necesita. ya.
        //portP = INDIPortProperty.createSaveablePortProperty(this, "/dev/ttyUSB0");
        //this.addProperty(portP);
        // motorP = new INDISwitchProperty(this, "factory_settings", "Factory Settings", "Expert Configuration", PropertyStates.IDLE, PropertyPermissions.RW, 0, SwitchRules.AT_MOST_ONE);
        // motorE = new INDISwitchElement(motorP, "factory_setting", "Factory Settings", SwitchStatus.OFF);
        
        // Añadimos las propiedades iniciales del dispositivo.
        initializeStandardProperties();

        if (stepPerPulseP == null) {
            stepPerPulseP = INDINumberProperty.createSaveableNumberProperty(this, "stepPerPulse", "Steps per Pulse", "Configuration", PropertyStates.IDLE, PropertyPermissions.RW);
            stepPerPulseE = stepPerPulseP.getElement("stepPerPulse_value");
            if (stepPerPulseE == null) {
                stepPerPulseE = new INDINumberElement(stepPerPulseP, "stepPerPulse_value", "Steps per Pulse", "1", "1", "99", "1", "%.0f");
            }
        }

    }

  /**
     * Realiza conexión con el periferico en concreo creando el canal de
     * comunicación.
     *
     * @param timestamp
     * @throws INDIException
     */
    @Override
    public void driverConnect(Date timestamp) throws INDIException {

        ac = new ArduinoConnection();
        boolean connected = ac.connectToBoard();

        if (connected) {
            System.out.println("Connected!");
        } else {
            throw new INDIException("Connection to Ardufocuser failed");

        }

        // Add listener for Arduino serial output
        ac.addListener(new ArdufocuserListener(ac));

        // Añadimos propiedades cuando el dispositivo conectado.
        showSpeedProperty();
        showStopFocusingProperty();
        addProperty(stepPerPulseP);

    }

  /**
     * Clase asociada al LIstener, encargado de estar a la escucha de los
     * comando procedentes del periferico INDI.
     */
    private class ArdufocuserListener implements SerialPortEventListener {

        private ArduinoConnection ac;

        public ArdufocuserListener(ArduinoConnection ac) {
            this.ac = ac;
        }

        @Override
        public void serialEvent(SerialPortEvent serialPortEvent) {
            if (serialPortEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                String inLine = ac.readLine();
                System.out.println("GOT: " + inLine);
                ArdufocuserParserCmd(inLine);

            }

        }

    /**
         * @param cmd String con el comando leido del Ardufocuser.
         *
         */
        public void ArdufocuserParserCmd(String cmd) {

            if (cmd.startsWith("APOSITION?")) {
                int c = cmd.indexOf("?") + 1;
                // Extraemos el dato, del comando.
                String d = cmd.substring(c, 20).trim();

                try {
                    // Hacemos casting a entero.
                    int p = Integer.parseInt(d);
                    positionChanged(p);

                } catch (NumberFormatException e) {
                    throw new NumberFormatException("Invalid Data");

                }
            } else if (cmd.startsWith("ASTOP?")) {
                finalPositionReached();

            }

        }
    }

    /**
     * Desconecta el periferico INDI, del servidor, elimiando el canal de
     * comunicación.
     *
     * @param timestamp
     * @throws INDIException
     */
    @Override
    public void driverDisconnect(Date timestamp) throws INDIException {

        System.out.println("Disconnecting Ardufocuser");
        System.out.flush();

        removeProperty(stepPerPulseP);
        hideSpeedProperty();
        hideStopFocusingProperty();

        ac.close();
        System.out.println("Disconnected Ardufocuser");
        System.out.flush();

    }

  /**
     * Enviar comando al dispositivo Ardufocuser. 
     * @param message 
     */
    private void sendMessageToArdufocus(String message) {
        System.out.println("SENT: " + message);
        ac.sendString(message);
    }

   /**
     * Maximo desplazamiento absoluto del Ardufocuser.
     * @return int
     */
    @Override
    public int getMaximumAbsPos() {
        return 9999999;
    }

    /**
     * Minimo desplazamiento absoluto del Ardufocuser.
     *
     * @return int
     */
    @Override
    public int getMinimumAbsPos() {
        return -9999999;
    }

    /**
     * Posición inicial.
     *
     * @return int
     */
    @Override
    public int getInitialAbsPos() {
        return 0;
    }

    /**
     * Maxima velocidad soportada por Ardufocuser..
     *
     * @return int
     */
    @Override
    protected int getMaximumSpeed() {
        return 500;
    }

    /**
     * Cambio en la posición, del enfocador.
     *
     * @return int
     */
    @Override
    public void absolutePositionHasBeenChanged() {

        String msg = "AG?" + getDesiredAbsPosition();
        System.out.println(msg);
        sendMessageToArdufocus(msg);

    }

  /**
     * Cambio en la velocidad, del enfocador.
     *
     * @return int
     */
    @Override
    public void speedHasBeenChanged() {

        String msg = "ASPEED?" + getCurrentSpeed();
        //System.out.println(msg);
        sendMessageToArdufocus(msg);
        desiredSpeedSet();
    }

  /**
     * Parar Ardufocuser,
     * @return int
     */
    @Override
    public void stopHasBeenRequested() {

        String msg = "ASTOP?";
        sendMessageToArdufocus(msg);
        stopped();
    }

  /**
     * Ardufocuser,
     *
     * @return int
     */
    @Override
    public String getName() {
        return "ArduFocuser";
    }

    //Metodos para procesar las distintas propiedades válidas.
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

    

}
