package org.micromanager.overlayarduino;

public class ArduinoInputEvent {
	private final int digitalValue_;
	private final int count_;

	public ArduinoInputEvent(int digital, int count) {
		digitalValue_ = (digital & 0x03);
		count_ = count;
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

	public int getCount() {
		return count_;
	}
}