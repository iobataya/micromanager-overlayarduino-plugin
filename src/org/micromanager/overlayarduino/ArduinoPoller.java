package org.micromanager.overlayarduino;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.ReportingUtils;

import mmcorej.CMMCore;

/**
 * Poll Arduino digital input0 and input1
 * 
 * @author iobataya
 *
 */
public class ArduinoPoller implements Runnable {
	private static ScriptInterface gui_;
	private static CMMCore mmc_;
	private static final String DEV_ARDUINO = "Arduino-Input";
	private static final String PROP_DIGITAL_IN = "DigitalInput";
	private ArduinoInputListener listener = null;
	private static ArduinoPoller instance_ = new ArduinoPoller();
	private int lastDigital = 0;
	private int count_ = 0;
	private boolean lastBit0 = false;
	private boolean lastBit1 = false;

	private ArduinoPoller() {
	}

	public static ArduinoPoller getInstance(ScriptInterface gui) {
		gui_ = gui;
		mmc_ = gui.getMMCore();
		return instance_;
	}

	public void setListener(ArduinoInputListener l) {
		listener = l;
	}

	public void removeListener() {
		listener = null;
	}

	@Override
	public void run() {
		while (true) {
			try {
				poll();
			} catch (Exception ex) {
				ReportingUtils.logError(ex);
			}
		}
	}

	private synchronized void poll() {
		if (listener == null) {
			return; // nothing to do.
		}
		// Get digital input from Arduino. Bit 0 and bit 1 are used.
		String strDigital = null;
		try {
			strDigital = mmc_.getProperty(DEV_ARDUINO, PROP_DIGITAL_IN);
		} catch (Exception ex) {
			ReportingUtils.logError(ex);
			return;
		}
		int currentDigital = Integer.parseInt(strDigital);
		if (currentDigital == lastDigital) {
			return; // do nothing
		}
		// tell listeners the change
		if (count_ < Integer.MAX_VALUE) {
			count_++;
		} else {
			count_ = 0;
		}
		ArduinoInputEvent inputEvent = new ArduinoInputEvent(currentDigital, count_);
		boolean currentBit0 = inputEvent.isHighAt0();
		boolean currentBit1 = inputEvent.isHighAt1();
		listener.ValueChanged(inputEvent);

		// tell listeners rise/fall, if any
		if (lastBit0 ^ currentBit0) {
			if (currentBit0) {
				listener.IsRisingAt0();
			} else {
				listener.IsFallingAt0();
			}
		}
		if (lastBit1 ^ currentBit1) {
			if (currentBit1) {
				listener.IsRisingAt1();
			} else {
				listener.IsFallingAt1();
			}
		}
		lastDigital = currentDigital;
		lastBit0 = currentBit0;
		lastBit1 = currentBit1;

	}
}
