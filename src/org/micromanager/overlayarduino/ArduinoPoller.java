package org.micromanager.overlayarduino;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import javax.swing.JOptionPane;

import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MMDialog;
import org.micromanager.utils.MMException;
import org.micromanager.utils.ReportingUtils;

import mmcorej.CMMCore;
import mmcorej.StrVector;

/**
 * Poll Arduino digital input0 and input1 via serial device, set digital output
 * 8 and 13 if different values are requested.
 * 
 * @author iobataya
 *
 */
public class ArduinoPoller implements Runnable {
	private static ScriptInterface gui_;
	private static CMMCore mmc_;
	private static final String DEV_HUB = "Arduino-Hub";
	private static final String DEV_ARDUINO = "Arduino-Input";
	private static final String DEV_ARDUINO_SWITCH = "Arduino-Switch";
	private static final String DEV_ARDUINO_SHUTTER = "Arduino-Shutter";
	private static final String PROP_DIGITAL_IN = "DigitalInput";
	private static final String PROP_STATE = "State";
	private static final String ERR_NOT_LOADED = "Arduino is not loaded.";
	private static ArduinoPoller instance_;
	private int lastDigitalIn = 0;
	private boolean lastInBit0 = false;
	private boolean lastInBit1 = false;
	private boolean reqStop_ = false;
	private static int lastDigitalOutValue = 0;
	private static int currentDigitalOutValue = 0;
	private ArrayList<ArduinoInputListener> listeners_ = new ArrayList<ArduinoInputListener>();
	private static boolean deviceAvailable = false;
	private static boolean digitalInAvailable = false;
	private static boolean digitalOutAvailable = false;
	private static boolean shutterAvailable = false;

	private ArduinoPoller() {
	}

	public static ArduinoPoller getInstance(ScriptInterface gui) throws Exception {
		if (instance_ == null) {
			instance_ = new ArduinoPoller();
		}
		gui_ = gui;
		mmc_ = gui.getMMCore();
		StrVector devices = mmc_.getLoadedDevices();
		for (int i = 0; i < devices.size(); i++) {
			if (devices.get(i).equals(DEV_HUB)) {
				deviceAvailable = true;
			}
		}
		if (!deviceAvailable) {
			ReportingUtils.logError(ERR_NOT_LOADED);
			JOptionPane.showMessageDialog(null, ERR_NOT_LOADED, "ERROR", JOptionPane.PLAIN_MESSAGE);
			return instance_;
		}
		StrVector peripherals = mmc_.getLoadedPeripheralDevices(DEV_HUB);
		for (int j = 0; j < peripherals.size(); j++) {
			if (peripherals.get(j).equals(DEV_ARDUINO)) {
				digitalInAvailable = true;
			} else if (peripherals.get(j).equals(DEV_ARDUINO_SWITCH)) {
				digitalOutAvailable = true;
			} else if (peripherals.get(j).equals(DEV_ARDUINO_SHUTTER)) {
				shutterAvailable = true;
			}
		}
		return instance_;
	}

	public void addListener(ArduinoInputListener l) {
		listeners_.add(l);
	}

	public void removeListener(ArduinoInputListener l) {
		listeners_.remove(listeners_.indexOf(l));
	}

	@Override
	public void run() {
		while (!reqStop_ && deviceAvailable) {
			try {
				poll();
			} catch (Exception ex) {
				ReportingUtils.logError(ex);
			}
		}
	}

	private synchronized void poll() throws Exception {
		if (listeners_.size() == 0 || deviceAvailable == false) {
			return; // nothing to do.
		}
		/////////////////////////
		// Digital Input 0,1
		// Get digital input from Arduino. Bit 0 and bit 1 are used.
		if(digitalInAvailable) {
			String strDigital = null;
			strDigital = mmc_.getProperty(DEV_ARDUINO, PROP_DIGITAL_IN);
			int currentDigital = Integer.parseInt(strDigital);
			if (currentDigital != lastDigitalIn) {
				// tell listeners the change
				ArduinoInputEvent inputEvent = new ArduinoInputEvent(currentDigital);
				boolean currentBit0 = inputEvent.isHighAt0();
				boolean currentBit1 = inputEvent.isHighAt1();
				for (ArduinoInputListener l : listeners_) {
					if(l!=null) {
						l.ValueChanged(inputEvent);
					}
				}
				// tell listeners rise/fall, if any
				if (lastInBit0 ^ currentBit0) {
					if (currentBit0) {
						for (ArduinoInputListener l : listeners_) {
							l.IsRisingAt0();
						}
					} else {
						for (ArduinoInputListener l : listeners_) {
							l.IsFallingAt0();
						}
					}
				}
				if (lastInBit1 ^ currentBit1) {
					if (currentBit1) {
						for (ArduinoInputListener l : listeners_) {
							l.IsRisingAt1();
						}
					} else {
						for (ArduinoInputListener l : listeners_) {
							l.IsFallingAt1();
						}
					}
				}
				lastDigitalIn = currentDigital;
				lastInBit0 = currentBit0;
				lastInBit1 = currentBit1;
			}
		}		
		//////
		// Output
		if (digitalOutAvailable && shutterAvailable) {
			if(!mmc_.deviceBusy("Arduino-Switch")) {
				if(lastDigitalOutValue!=currentDigitalOutValue) {
					mmc_.setProperty(DEV_ARDUINO_SWITCH, PROP_STATE, String.valueOf(currentDigitalOutValue));
				}
			}
		}
	}
	public synchronized void setDigitalOut(int digitalVal) {
		lastDigitalOutValue = currentDigitalOutValue;
		currentDigitalOutValue = (digitalVal & 0x2F);
	}

	public void requestStop() {
		reqStop_ = true;
	}

	public void requestContinue() {
		reqStop_ = false;
	}

}
