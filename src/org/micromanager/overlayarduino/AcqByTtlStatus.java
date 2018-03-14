package org.micromanager.overlayarduino;

import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.ReportingUtils;


public class AcqByTtlStatus {

	private ScriptInterface gui_;
	private boolean isWaitingForTriggerTTLs = false;

	public AcqByTtlStatus(ScriptInterface gui) {
		gui_ = gui;
	}

	public boolean canStartWaitingTriggers() {
		if (isAcquiring()) {
			return false;
		}
		return !isWaitingForTriggerTTLs;
	}
	public boolean canStopWaitingTriggers() {
		if(isAcquiring()) {
			return false;
		}
		return isWaitingForTriggerTTLs;
	}

	public void setStatusWaitForTTL(boolean startWaiting) {
		if( isWaitingForTriggerTTLs == startWaiting) {
			ReportingUtils.logDebugMessage("Waiting flag was set to the same value.");
			return; // do nothing.
		}
		if (!isAcquiring()) {
			isWaitingForTriggerTTLs = startWaiting;
		}
	}
	public boolean isAcquiring() {
		return gui_.isAcquisitionRunning();
	}
	public boolean isLiveModeOn() {
		return gui_.isLiveModeOn();
	}


	public String getCurrentStatus() {
		if (isAcquiring()) {
			return "Acquring";
		}
		if (isWaitingForTriggerTTLs) {
			return "Waiting for TTL triggers...";
		} else {
			return "Idle";
		}
	}
}
