package org.micromanager.overlayarduino;

public class ArduinoInputEvent {
	private final int digitalValue_;

	public ArduinoInputEvent(int digital) {
		digitalValue_ = (digital & 0x03);
	}

	public int getDigitalValue() {
		return digitalValue_;
	}

	public boolean isHighAt0() {
		return ((digitalValue_ & 0x01) == 0x01);
	}

	public boolean isHighAt1() {
		return ((digitalValue_ & 0x02) == 0x02);
	}
}