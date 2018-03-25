///////////////////////////////////////////////////////////////////////////////
//FILE:          OverlayArduinoProcessor.java
//PROJECT:       Micro-Manager  
//SUBSYSTEM:     OverlayArduinoProcessor plugin
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ikuo Obataya
//
// COPYRIGHT:    JPK Instruments AG, 2018
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.overlayarduino;

import java.awt.Rectangle;
import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.DataProcessor;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.MMScriptException;
import org.micromanager.utils.ReportingUtils;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import mmcorej.CMMCore;
import mmcorej.TaggedImage;

import org.micromanager.arduinoio.*;

/**
 *
 */
public class OverlayArduinoProcessor extends DataProcessor<TaggedImage> implements ArduinoInputListener {
	private OverlayArduinoMigForm myFrame_;
	@SuppressWarnings("unused")
	private CMMCore core_;
	private static int counterSize_ = 3;
	private static int currentCounter_ = 0;
	private static boolean drawBlocks = true;
	private static boolean embedTags_ = true;
	private static boolean digital0_ = false;
	private static boolean digital1_ = false;
	private static ArduinoPoller arduino_ = null;

	private static final Rectangle rectBound0_ = new Rectangle(0, 0, 8, 8);
	private static final Rectangle rectInner0_ = new Rectangle(1, 1, 6, 6);
	private static final Rectangle rectBound1_ = new Rectangle(8, 0, 8, 8);
	private static final Rectangle rectInner1_ = new Rectangle(9, 1, 6, 6);

	private static final String TAG_SEGMENT_INDEX = "Segment-Index";
	private static final String TAG_ARDUINO_DIGITAL0 = "Digital-Input0";
	private static final String TAG_ARDUINO_DIGITAL1 = "Digital-Input1";
	private static final String MSG_DONE_BLOCKS = "Painted blocks on the image";
	private static final String MSG_DONE_TAG = "Emedded only tags.";
	private static final String MSG_NOTHING_DONE = "No process done.";
	private static final String MSG_DONE_BOTH = "Drew blocks and embedded tags";
	private static final String ERR_ILLEGAL_TYPE = "Cannot handle images other than 8 or 16 bit grayscale";
	private static final String ERR_FRAME_MISSING = "myFrame_ is missing.";

	/**
	 * 
	 * @param nextImage
	 *            - image to be processed
	 * @return - Transformed tagged image, otherwise a copy of the input
	 * @throws JSONException
	 * @throws MMScriptException
	 */
	public TaggedImage processTaggedImage(TaggedImage nextImage) throws JSONException, MMScriptException, Exception {
		JSONObject newTags = nextImage.tags;

		// Check pixel depth
		String type = MDUtils.getPixelType(newTags);
		if (!(type.equals("GRAY8") || type.equals("GRAY16"))) {
			ReportingUtils.logError(ERR_ILLEGAL_TYPE);
			return nextImage;
		}
		int ijType = type.equals("GRAY16") ? ImagePlus.GRAY16 : ImagePlus.GRAY8;

		// Write current status to new tags
		if (embedTags_) {
			newTags.put(TAG_ARDUINO_DIGITAL0, String.valueOf(digital0_));
			newTags.put(TAG_ARDUINO_DIGITAL1, String.valueOf(digital1_));
			newTags.put(TAG_SEGMENT_INDEX, currentCounter_);
		}

		// Overlay or not ?
		if (drawBlocks == false) {
			if (embedTags_) {
				setStatus(MSG_DONE_TAG);
			} else {
				setStatus(MSG_NOTHING_DONE);
			}
			return new TaggedImage(nextImage.pix, newTags);
		}
		int width = MDUtils.getWidth(newTags);
		int height = MDUtils.getHeight(newTags);
		ImageProcessor imp;
		if (ijType == ImagePlus.GRAY8) {
			byte[] oldPixels = (byte[]) nextImage.pix;
			byte[] newPixels = Arrays.copyOf(oldPixels, oldPixels.length);
			imp = new ByteProcessor(width, height, newPixels);
			drawBlocks(imp, 0xFF);
		} else {
			short[] oldPixels = (short[]) nextImage.pix;
			short[] newPixels = Arrays.copyOf(oldPixels, oldPixels.length);
			imp = new ShortProcessor(width, height);
			imp.setPixels(newPixels);
			drawBlocks(imp, 0xFFFF);
		}
		if (embedTags_) {
			setStatus(MSG_DONE_BOTH);
		} else {
			setStatus(MSG_DONE_BLOCKS);
		}
		return new TaggedImage(imp.getPixels(), newTags);
	}

	private void drawBlocks(ImageProcessor imp, int maxValue) {
		imp.setRoi(rectBound0_);
		imp.setValue((digital0_ ? 0 : maxValue));
		imp.fill();
		imp.setRoi(rectInner0_);
		imp.setValue((digital0_ ? maxValue : 0));
		imp.fill();
		imp.setRoi(rectBound1_);
		imp.setValue((digital1_ ? 0 : maxValue));
		imp.fill();
		imp.setRoi(rectInner1_);
		imp.setValue((digital1_ ? maxValue : 0));
		imp.fill();
	}

	private void setStatus(String status) {
		if (myFrame_ != null) {
			myFrame_.setStatus(status);
		} else {
			ReportingUtils.logDebugMessage(ERR_FRAME_MISSING);
		}
	}

	public int getCurrentSegmentIdx() {
		return currentCounter_;
	}

	public void setSegmentCount(int c) {
		counterSize_ = c;
	}

	/**
	 * Size of segment size
	 * 
	 * @return
	 */
	public int getSegmentCount() {
		return counterSize_;
	}

	public void setMyFrameToNull() {
		myFrame_ = null;
	}

	public void setOverlaySignal(boolean overlay) {
		drawBlocks = overlay;
	}

	public boolean getOverlaySignal() {
		return drawBlocks;
	}

	public void setEmbedTags(boolean embedTags) {
		embedTags_ = embedTags;
	}

	public boolean getEmbedTags() {
		return embedTags_;
	}

	public void resetCounter() {
		currentCounter_ = 0;
	}

	//
	// DataProcessor
	//

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (myFrame_ != null) {
			myFrame_.updateProcessorEnabled(enabled);
		}
	}

	/**
	 * Polls for tagged images, and processes them if their size and type matches
	 */
	@Override
	public void process() {
		if (arduino_ == null) {
			try {
				arduino_ = ArduinoPoller.getInstance(gui_);
			} catch (Exception e) {
				ReportingUtils.logError(e);
			}
			arduino_.addListener(this);
		}
		try {
			TaggedImage nextImage = poll();
			if (nextImage != TaggedImageQueue.POISON) {
				try {
					produce(processTaggedImage(nextImage));
				} catch (Exception ex) {
					produce(nextImage);
					ReportingUtils.logError(ex);
					setStatus(ex.getMessage());
				}
			} else {
				// Must produce Poison (sentinel) image to terminate tagged image pipeline
				produce(nextImage);
			}
		} catch (Exception ex) {
			ReportingUtils.logError(ex);
			setStatus(ex.getMessage());
		}
	}

	@Override
	public void makeConfigurationGUI() {
		if (myFrame_ == null) {
			myFrame_ = new OverlayArduinoMigForm(this, gui_);
			gui_.addMMBackgroundListener(myFrame_);
		}
		myFrame_.setVisible(true);
	}

	@Override
	public void dispose() {
		if (myFrame_ != null) {
			myFrame_.dispose();
			myFrame_ = null;
		}
	}

	@Override
	public void ValueChanged(ArduinoInputEvent e) {
		digital0_ = e.isHighAt0();
		digital1_ = e.isHighAt1();
	}

	@Override
	public void IsRisingAt0() {
		currentCounter_ += 1;
		if (currentCounter_ >= counterSize_) {
			currentCounter_ = 0;
		}
	}

	@Override
	public void IsFallingAt0() {
		currentCounter_ += 1;
		if (currentCounter_ >= counterSize_) {
			currentCounter_ = 0;
		}
	}

	@Override
	public void IsRisingAt1() {
	}

	@Override
	public void IsFallingAt1() {
	}
}
