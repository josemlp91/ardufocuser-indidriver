package i4jardufocuserdriver;

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
import laazotea.indi.Constants.SwitchStatus;

import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import i4jardufocuserdriver.ArduinoConnection;
import laazotea.indi.Constants.SwitchRules;
import laazotea.indi.driver.INDISwitchElement;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;


/**
 * @author @josemlp @zerjillo
 */
public class I4JArdufocuserDriver extends INDIFocuserDriver implements INDIConnectionHandler {
    
   
    // Protocolo serie de comunicación con el firmware.
    //
    // AINIT                                              // Iniciar modo funcionamiento Ardufocuser.
    // AMODE                                           // Estableceer modo de funcionamiento.
    // AG                                                  // Mover enfocador hasta posición pasada como dato.
    // APOSITION                                     // Solicitar posición actual del enfocador.
    // ATEMP                                            // Solicitar temperatura de la lente al enfocador.
    // AMICRO                                         // Establecer multiplicación del micropaso.
    // AFINE                                            // Establecer pasos que da el enfocador por cada pulso.
    // ASPEED                                         // Establecer la velocidad del movimiento del enfocador.
    // AACC   			 // Establecer la aceleración del movimiento del enfocador.
    // AR                                                // Fijar posición reletiva del enfocador en un valor personalizado.
    // AHLIMIT   		                 // Consultar si el enfocador ha llegado a un limite hardware.
    // ASLIMIT                                       // Consultar si el enfocador ha llegado a un limite software.
    // ASILIMIT                                        // Establecer  limite software inware.
    // ASOLIMIT  		                  // Establecer  limite software outware.
    // AVERS                                           // Consultar version del firmware.
    // AMOV   			 // Consulta si el enfocador esta moviendose.
    // ARUNA                                         // Comando para debug: Mueve el motor del enfocador en una dirreción.
    // ARUNB   			 // Comando para debug: Mueve el motor del enfocador en una dirreción.
    // ASTOP                                          // Comando para debug: Detiene motor del enfocador
    // ALUX                                            // Modifica la intesindad de la ilumincaicon de la pantalla.
    // ACALIBRATE                                 // Configura los limites software.
    // ALCDPRINT                                  // Imprime mensaje en lcd
    

    // Instancia conector serie Arduino.
    private ArduinoConnection ac;
    
    // Propiedad puerto.
    private INDIPortProperty portP;
    
    // Propiedad y Elemento para pasos por pulso.
    private INDINumberProperty stepPerPulseP;
    private INDINumberElement stepPerPulseE;

    // Propiedad y Elemento para posición relativa.
    private INDINumberProperty relativePositionP;
    private INDINumberElement relativePositionE;

    // Propiedad y Elemento para luz limite hardware.
    private INDISwitchProperty hardwareLimitP;
    private INDISwitchElement hardwareLimitE;

    // Propiedad y Elemento para posición del limite software.
    private INDINumberProperty softwareLimitPositionP;
    private INDINumberElement softwareLimitPositionE;

    // Propiedad y Elemento para luz limite software.
    private INDISwitchProperty softwareLimitP;
    private INDISwitchElement softwareLimitE;

    // Propiedad y elemento para informar de la temperatura.
    private INDINumberProperty temperatureP;
    private INDINumberElement temperatureE;

    // Propiedad y elemento para encender y apagar led de la lcd.
    private INDISwitchProperty lcdLuxP;
    private INDISwitchElement lcdLuxE;
    private Boolean lcdstatus;
    

    public I4JArdufocuserDriver(InputStream inputStream, OutputStream outputStream) {
        super(inputStream, outputStream);

        // Añadimos las propiedades iniciales del dispositivo.
        initializeStandardProperties();

        // Propiaedades adicionales de Ardufocuser.
        inizializeTempretureProperty();
        inizializeLCDLuxProperty();
        inizializeSoftwareLimitProperty();
        inizializeBumperLimitProperty();
        inizializeStepPerPulseProperty();
        inizializeRelativePositionProperty();
        inizializeSoftwareLimitPositionProperty();
        
        lcdstatus = true;

    }

  /**
     * Realiza conexión con el periferico en concreo creando el canal de
     * comunicación.
     *
     * @param timestamp Fecha hora conexión con driver.
     * @throws INDIException
     */
    @Override
    public void driverConnect(Date timestamp) throws INDIException {
        
        // Creamos instancia del conector arduino.
        ac = new ArduinoConnection();
        
        // Realizamos conexión.
        boolean connected = ac.connectToBoard();

        if (connected) {
            System.out.println("Connected!");
        } else {
            throw new INDIException("Connection to Ardufocuser failed");
        }

        // Creamos instancia para estar a la escucha mensajes Arduino.
        ac.addListener(new ArdufocuserListener(ac));

        // Añadimos propiedades para que se muestren en la interfaz.
        showSpeedProperty();
        showStopFocusingProperty();
        addProperty(stepPerPulseP);
        addProperty(temperatureP);
        addProperty(hardwareLimitP);
        addProperty(softwareLimitP);
        addProperty(lcdLuxP);
        addProperty(relativePositionP);
        addProperty(softwareLimitPositionP);

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

        // Eliminamos propiedades.
        removeProperty(stepPerPulseP);
        removeProperty(temperatureP);
        removeProperty(hardwareLimitP);
        removeProperty(softwareLimitP);
        removeProperty(lcdLuxP);
        removeProperty(relativePositionP);
        removeProperty(softwareLimitPositionP);
                
        hideSpeedProperty();
        hideStopFocusingProperty();

        // Cerramos la conexión con arduino.
        ac.close();
        System.out.println("Disconnected Ardufocuser");
        System.out.flush();

    }

    // Inicializador propiedad para manejar temperatura.
    private void inizializeTempretureProperty() {
        if (temperatureP == null) {
            temperatureP = new INDINumberProperty(this, "temperature", "Temperature", "Control", PropertyStates.IDLE, PropertyPermissions.RO);
            temperatureE = temperatureP.getElement("temperature_value");
            if (temperatureE == null) {
                temperatureE = new INDINumberElement(temperatureP, "temperature", "Temperature", "1", "1", "99", "1", "%f");
            }
        }

    }
    
    // Inicializador propiedad para manejar control del led de la LCD..
    private void inizializeLCDLuxProperty() {
        if (lcdLuxP == null) {
            lcdLuxP = new INDISwitchProperty(this, "lcdLux", "LCD Lux", "Configuration", PropertyStates.OK, PropertyPermissions.RW, 0, SwitchRules.AT_MOST_ONE);

            lcdLuxE = lcdLuxP.getElement("lcdLux_value");
            if (lcdLuxE == null) {
                lcdLuxE = new INDISwitchElement(lcdLuxP, "lcdLux", "LCD Lux", SwitchStatus.OFF);
            }
        }
    }

    // Inicalizador propiedad fin de carrera hardware.
    private void inizializeBumperLimitProperty() {
        if (hardwareLimitP == null) {
            hardwareLimitP = new INDISwitchProperty(this, "hardwareLimit", "Hardware Limit", "Control", PropertyStates.OK, PropertyPermissions.RO, 0, SwitchRules.AT_MOST_ONE);

            hardwareLimitE = hardwareLimitP.getElement("hardwareLimit_value");
            if (hardwareLimitE == null) {
                hardwareLimitE = new INDISwitchElement(hardwareLimitP, "hardwareLimit", "Hardware Limit", SwitchStatus.OFF);
            }
        }
    }

    // Inicializador propiedad fin de carrera software.
    private void inizializeSoftwareLimitProperty() {
        if (softwareLimitP == null) {
            softwareLimitP = new INDISwitchProperty(this, "softwareLimit", "Software Limit", "Control", PropertyStates.OK, PropertyPermissions.RO, 0, SwitchRules.AT_MOST_ONE);

            softwareLimitE = softwareLimitP.getElement("softwareLimit_value");
            if (softwareLimitE == null) {
                softwareLimitE = new INDISwitchElement(softwareLimitP, "softwareLimit", "Software Limit", SwitchStatus.OFF);
            }
        }
    }

    // Inicializador propiedad posición relativa.
    private void inizializeRelativePositionProperty() {
        if (relativePositionP == null) {
            relativePositionP = INDINumberProperty.createSaveableNumberProperty(this, "relativePosition", "Relative Position", "Configuration", PropertyStates.IDLE, PropertyPermissions.RW);
            relativePositionE = relativePositionP.getElement("relativePosition_value");
            if (relativePositionE == null) {
                relativePositionE = new INDINumberElement(relativePositionP, "relativePosition_value", "Relative Position", "1", "1", "99", "1", "%.0f");
            }
        }
    }

    // Inicializador posición del fin de carrera software.
    private void inizializeSoftwareLimitPositionProperty() {

        if (softwareLimitPositionP == null) {
            softwareLimitPositionP = INDINumberProperty.createSaveableNumberProperty(this, "softwareLimitPosition", "Software Limit Position", "Configuration", PropertyStates.IDLE, PropertyPermissions.RW);
            softwareLimitPositionE = softwareLimitPositionP.getElement("softwareLimitPosition_value");
            if (softwareLimitPositionE == null) {
                softwareLimitPositionE = new INDINumberElement(softwareLimitPositionP, "softwareLimitPosition_value", "Software Limit Position", "1", "-1000", "1000", "1", "%.0f");
            }
        }

    }

    // Inicializador pasos por pulso.
    private void inizializeStepPerPulseProperty() {
        if (stepPerPulseP == null) {
            stepPerPulseP = INDINumberProperty.createSaveableNumberProperty(this, "stepPerPulse", "Steps per Pulse", "Configuration", PropertyStates.IDLE, PropertyPermissions.RW);
            stepPerPulseE = stepPerPulseP.getElement("stepPerPulse_value");
            if (stepPerPulseE == null) {
                stepPerPulseE = new INDINumberElement(stepPerPulseP, "stepPerPulse_value", "Steps per Pulse", "1", "1", "99", "1", "%.0f");
            }
        }
    }

    /**
     * Enviar comando al dispositivo Ardufocuser.
     *
     * @param message
     */
    private void sendMessageToArdufocus(String message) {
        System.out.println("SENT: " + message);
        ac.sendString(message + "\n");
    }

    /**
     * Maximo desplazamiento absoluto del Ardufocuser.
     *
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
        String msg = "AG?" + getDesiredAbsPosition() + "\n";
        sendMessageToArdufocus(msg);

    }

    /**
     * Cambio en la velocidad, del enfocador.
     *
     * @return int
     */
    @Override
    public void speedHasBeenChanged() {
        String msg = "ASPEED?" + getCurrentSpeed() + "\n";
        sendMessageToArdufocus(msg);
        desiredSpeedSet();
    }

    private void temperatureChanged(double temperature) {
        temperatureP.setState(PropertyStates.OK);
        temperatureE.setValue(temperature);

        try {
            updateProperty(temperatureP);
        } catch (INDIException e) {

        }

    }

    private void softwareLimitHasBeenReached() {
        softwareLimitP.setState(PropertyStates.ALERT);

        try {
            updateProperty(softwareLimitP);
        } catch (INDIException e) {

        }

    }

    private void softwareLimitHasBeenUnreached() {
        softwareLimitP.setState(PropertyStates.OK);

        try {
            updateProperty(softwareLimitP);
        } catch (INDIException e) {

        }

    }

    private void hardwareLimitHasBeenReached() {
        hardwareLimitP.setState(PropertyStates.ALERT);

        try {
            updateProperty(hardwareLimitP);
        } catch (INDIException e) {

        }

    }

    private void hardwareLimitHasBeenUnreached() {
        hardwareLimitP.setState(PropertyStates.OK);

        try {
            updateProperty(hardwareLimitP);
        } catch (INDIException e) {

        }

    }

    /**
     * Parar Ardufocuser,
     *
     * @return int
     */
    @Override
    public void stopHasBeenRequested() {

        String msg = "ASTOP?";
        sendMessageToArdufocus(msg);
        stopped();
    }
    
    private void setNewstepPerPulse(Double nstep) {

        int int_nstep = nstep.intValue();

        String msg = "AFINE?" + int_nstep + "\n";

        stepPerPulseE.setValue(nstep);
        stepPerPulseP.setState(PropertyStates.OK);

        try {
            sendMessageToArdufocus(msg);
            updateProperty(stepPerPulseP);
        } catch (INDIException e) {

        }

    }

    private void setNewSoftwareLimitPosition(Double nslimit) {

        int int_nslimit = nslimit.intValue();
        String msg = "ASLIMIT?" + int_nslimit + "\n";

        softwareLimitPositionE.setValue(nslimit);
        softwareLimitPositionP.setState(PropertyStates.OK);

        try {
            sendMessageToArdufocus(msg);
            updateProperty(softwareLimitPositionP);
        } catch (INDIException e) {

        }

    }
    
    private void toggleLCDLux() {

        String msg = "ALUX?" + "\n";
        this.lcdstatus=!this.lcdstatus;
        
        if (this.lcdstatus){ 
            lcdLuxP.setState(PropertyStates.OK);
        }else{
            lcdLuxP.setState(PropertyStates.ALERT);
        }
        
        try {
            sendMessageToArdufocus(msg);
            updateProperty(lcdLuxP);
        } catch (INDIException e) {

        }
    }
    
        

    private void setNewRelativePosition(Double nrelativep) {

        int int_nrelativep = nrelativep.intValue();
        String msg = "AR?" + int_nrelativep + "\n";

        relativePositionE.setValue(nrelativep);
        relativePositionP.setState(PropertyStates.OK);

        try {
            sendMessageToArdufocus(msg);
            updateProperty(relativePositionP);
        } catch (INDIException e) {

        }
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

        if (property == portP) {
            portP.processTextValue(property, elementsAndValues);
        }
    }

    @Override
    public void processNewBLOBValue(INDIBLOBProperty property, Date timestamp, INDIBLOBElementAndValue[] elementsAndValues) {
        throw new NotImplementedException();
    }

    
    
    @Override
    public void processNewNumberValue(INDINumberProperty property, Date timestamp, INDINumberElementAndValue[] elementsAndValues) {

        super.processNewNumberValue(property, timestamp, elementsAndValues);
        Double newValue = elementsAndValues[0].getValue();
       
        if (property == stepPerPulseP) {
            setNewstepPerPulse(newValue);
        }

        if (property == softwareLimitPositionP) {
            setNewSoftwareLimitPosition(newValue);
        }

        if (property == relativePositionP) {
            setNewRelativePosition(newValue);

        }
    }

    @Override
    public void processNewSwitchValue(INDISwitchProperty property, Date timestamp, INDISwitchElementAndValue[] elementsAndValues) {
        super.processNewSwitchValue(property, timestamp, elementsAndValues);
        
         if (property == lcdLuxP) {
             toggleLCDLux();
        }
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
                ArdufocuserParserCmd(inLine);
            }
        }

      /**
         * @param cmd String con el comando leido del Ardufocuser.
         *
         */
        public void ArdufocuserParserCmd(String cmd) {

            // System.out.println(cmd);
            String param;
            param = cmd.substring(cmd.indexOf("?") + 1);

            if (cmd.startsWith("APOSITION?")) {

                try {
                    int integer_param = Integer.parseInt(param);
                    positionChanged(integer_param);

                } catch (NumberFormatException e) {
                    throw new NumberFormatException("Invalid Data");

                }

            } else if (cmd.startsWith("ASLIMIT?")) {
                softwareLimitHasBeenReached();

            } else if (cmd.startsWith("AHLIMIT?")) {
                hardwareLimitHasBeenReached();

            } else if (cmd.startsWith("ATEMP?")) {

                try {
                    int integer_param = Integer.parseInt(param);
                    temperatureChanged(integer_param);

                } catch (NumberFormatException e) {
                    throw new NumberFormatException("Invalid Data");

                }

            } else if (cmd.startsWith("ARUN?")) {
                // Si detectamos que el motor se mueve, deshabilitamos avisos limite software y hardware.
                softwareLimitHasBeenUnreached();
                hardwareLimitHasBeenUnreached();

            } else if (cmd.startsWith("ASTOP?")) {
                finalPositionReached();

            }
        }

    }
}
