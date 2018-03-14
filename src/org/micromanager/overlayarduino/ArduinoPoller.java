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

/**
 * Poll Arduino digital input0 and input1 via serial device.
 * 
 * @author iobataya
 *
 */
public class ArduinoPoller implements Runnable {
	private static ScriptInterface gui_;
	private static CMMCore mmc_;
	private static final String DEV_ARDUINO = "Arduino-Input";
	private static final String PROP_DIGITAL_IN = "DigitalInput";
	private boolean deviceExists_ = true;
	private static ArduinoPoller instance_ = new ArduinoPoller();
	private int lastDigital = 0;
	private boolean lastBit0 = false;
	private boolean lastBit1 = false;
	private boolean reqStop_ = false;
	private ArrayList<ArduinoInputListener> listeners_ = new ArrayList<ArduinoInputListener>();

	private ArduinoPoller() {}

	public static ArduinoPoller getInstance(ScriptInterface gui) {
		gui_ = gui;
		mmc_ = gui.getMMCore();
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
		while (!reqStop_) {
			try {
				poll();
			} catch (Exception ex) {
				ReportingUtils.logError(ex);
			}
		}
	}

	private synchronized void poll() {
		if (listeners_.size() == 0 || deviceExists_ == false) {
			return; // nothing to do.
		}
		// Get digital input from Arduino. Bit 0 and bit 1 are used.
		String strDigital = null;
		try {
			strDigital = mmc_.getProperty(DEV_ARDUINO, PROP_DIGITAL_IN);
			deviceExists_ = true;
		} catch (Exception ex) {
			ReportingUtils.logError(ex);
			JOptionPane.showMessageDialog(null, ex.getMessage(), "ERROR", JOptionPane.PLAIN_MESSAGE);
			deviceExists_ = false;
			return;
		}
		int currentDigital = Integer.parseInt(strDigital);
		if (currentDigital == lastDigital) {
			return; // do nothing
		}
		// tell listeners the change
		ArduinoInputEvent inputEvent = new ArduinoInputEvent(currentDigital);
		boolean currentBit0 = inputEvent.isHighAt0();
		boolean currentBit1 = inputEvent.isHighAt1();
		for (ArduinoInputListener l : listeners_) {
			l.ValueChanged(inputEvent);
		}

		// tell listeners rise/fall, if any
		if (lastBit0 ^ currentBit0) {
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
		if (lastBit1 ^ currentBit1) {
			if (currentBit1) {
				for (ArduinoInputListener l:listeners_) {
					l.IsRisingAt1();
				}
			} else {
				for (ArduinoInputListener l:listeners_) {
					l.IsFallingAt1();
				}
			}
		}
		lastDigital = currentDigital;
		lastBit0 = currentBit0;
		lastBit1 = currentBit1;
	}
	
	public void requestStop() {
		reqStop_ = true;
	}
	public void requestContinue() {
		reqStop_ = false;
	}
}
