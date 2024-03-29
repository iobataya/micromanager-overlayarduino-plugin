///////////////////////////////////////////////////////////////////////////////
//FILE:          OverlayArduinoMigForm.java
//PROJECT:       Micro-Manager  
//SUBSYSTEM:     OverlayArduinoMigForm plugin
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.micromanager.MMStudio;
import org.micromanager.api.ScriptInterface;
import org.micromanager.arduinoio.ArduinoInputEvent;
import org.micromanager.arduinoio.ArduinoInputListener;
import org.micromanager.arduinoio.ArduinoIoMigForm;
import org.micromanager.utils.MMDialog;
import org.micromanager.utils.ReportingUtils;

import net.miginfocom.swing.MigLayout;

/**
 *
 */
@SuppressWarnings("serial")
public class OverlayArduinoMigForm extends MMDialog implements ArduinoInputListener {
	private final ScriptInterface gui_;
	private final mmcorej.CMMCore mmc_;
	private final Preferences prefs_;
	private final OverlayArduinoProcessor processor_;
	private String statusMessage_;
	private final JCheckBox chkEnable_;
	private final JCheckBox chkEmbedTags_;
	private final JCheckBox chkDrawBlocks_;
	private final JRadioButton radioInput0_;
	private final JRadioButton radioInput1_;
	private final JLabel statusLabel_;
	private final Font fontSmall;
	private final Font fontSmallBold_;
	private OverlayArduinoMigForm mcsPluginWindow;
	private static int digitalInput_;

	private static final String LABEL_ENABLE = "Enabling Arduino input plugin";
	private static final String PREF_ENABLE = "UseArduinoInput";
	private static final String PREF_EMBEDTAGS = "EmbedDigitalInputTags";
	private static final String PREF_OVERLAY = "DrawDigitalInputBlocks";

	/**
	 * entry point to test
	 * 
	 * @param arg
	 */
	public static void main(String[] arg) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			MMStudio mmStudio = new MMStudio(false);
			OverlayArduinoProcessor processor = new OverlayArduinoProcessor();
			processor.setApp(mmStudio);
			processor.makeConfigurationGUI();
			mmStudio.getAcquisitionEngine().getImageProcessors().add(processor);

			ArduinoIoMigForm arduino = ArduinoIoMigForm.getInstance(mmStudio);
			arduino.setVisible(true);
		} catch (Exception e) {
			ReportingUtils.logError(e);
			System.exit(1);
		}
	}

	/**
	 * Creates new form OverlayArduinoMigForm
	 * 
	 * @param processor
	 * @param gui
	 */
	@SuppressWarnings("LeakingThisInConstructor")
	public OverlayArduinoMigForm(OverlayArduinoProcessor processor, ScriptInterface gui) {
		processor_ = processor;
		gui_ = gui;
		gui_.addMMBackgroundListener(this);
		mmc_ = gui_.getMMCore();
		prefs_ = this.getPrefsNode();		
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				dispose();
			}
		});

		fontSmall = new Font("Arial", Font.PLAIN, 12);
		fontSmallBold_ = new Font("Arial", Font.BOLD, 14);

		mcsPluginWindow = this;
		this.setLayout(new MigLayout("flowx, fill, insets 8"));
		this.setTitle(OverlayArduino.menuName);
		loadAndRestorePosition(100, 100, 350, 250);

		// Checkbox to enable this processor
		chkEnable_ = new JCheckBox();
		chkEnable_.setText(LABEL_ENABLE);
		chkEnable_.setFont(fontSmallBold_);
		chkEnable_.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				processor_.setEnabled(chkEnable_.isSelected());
				prefs_.putBoolean(PREF_ENABLE, chkEnable_.isSelected());
				statusLabel_.setText(" ");
			}
		});
		chkEnable_.setSelected(prefs_.getBoolean(PREF_ENABLE, true));
		add(chkEnable_, "span 3, wrap");

		// Checkbox for overlay or not
		chkDrawBlocks_ = new JCheckBox();
		chkDrawBlocks_.setFont(fontSmall);
		chkDrawBlocks_.setText("Draw blocks on image");
		chkDrawBlocks_.setToolTipText("Draw black-white blocks indicators at top-left.");
		chkDrawBlocks_.setSelected(prefs_.getBoolean(PREF_OVERLAY, true));
		chkDrawBlocks_.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				processor_.setOverlaySignal(chkDrawBlocks_.isSelected());
				prefs_.putBoolean(PREF_OVERLAY, chkDrawBlocks_.isSelected());
				statusLabel_.setText(" ");
			}
		});
		chkEmbedTags_ = new JCheckBox();
		chkEmbedTags_.setFont(fontSmall);
		chkEmbedTags_.setText("Embed tags to images");
		chkEmbedTags_.setToolTipText("Embed text tags in JSON text to every images");
		chkEmbedTags_.setSelected(prefs_.getBoolean(PREF_EMBEDTAGS, true));
		chkEmbedTags_.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				processor_.setEmbedTags(chkEmbedTags_.isSelected());
				prefs_.putBoolean(PREF_EMBEDTAGS, chkEmbedTags_.isSelected());
				statusLabel_.setText(" ");
			}
		});

		add(chkEmbedTags_);
		add(chkDrawBlocks_, "wrap");

		// Input signals
		radioInput0_ = new JRadioButton("Input0");
		radioInput0_.setFont(fontSmall);
		radioInput0_.setEnabled(false);
		radioInput1_ = new JRadioButton("Input1");
		radioInput1_.setFont(fontSmall);
		radioInput1_.setEnabled(false);
		// Add to GUI
		add(radioInput0_);
		add(radioInput1_, "wrap");

		// Status bar
		statusLabel_ = new JLabel(" ");
		add(statusLabel_, "span 3, wrap");
		// Add to GUI
		processor_.setEnabled(chkEnable_.isSelected());
		
		// Setup ArduinoPoller and listener
		try {
			ArduinoIoMigForm arduino = ArduinoIoMigForm.getInstance(gui_);
			arduino.addListener(this);
		} catch (Exception ex) {
			ReportingUtils.logError(ex);
		}
	}

	public mmcorej.CMMCore getMMCore() {
		return mmc_;
	}

	@Override
	public void dispose() {
		super.dispose();
		processor_.setMyFrameToNull();
	}

	public final JButton mcsButton(Dimension buttonSize, Font font) {
		JButton button = new JButton();
		button.setPreferredSize(buttonSize);
		button.setMinimumSize(buttonSize);
		button.setFont(font);
		button.setMargin(new Insets(0, 0, 0, 0));

		return button;
	}

	public synchronized void setStatus(final String status) {
		statusMessage_ = status;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// update the statusLabel from this thread
				if (status != null) {
					statusLabel_.setText(status);
				}
			}
		});
	}

	public synchronized String getStatus() {
		String status = statusMessage_;
		statusMessage_ = null;
		return status;
	}

	public void updateProcessorEnabled(boolean enabled) {
		chkEnable_.setSelected(enabled);
		prefs_.putBoolean(PREF_ENABLE, enabled);
	}

	public int getDigitalInput() {
		return digitalInput_;
	}
	@Override
	public void ValueChanged(ArduinoInputEvent e) {
		digitalInput_ = e.getDigitalValue();
		this.setStatus(String.format("Value changed to %d", e.getDigitalValue()));
		radioInput0_.setSelected(e.isHighAt0());
		radioInput1_.setSelected(e.isHighAt1());
	}

	@Override
	public void IsRisingAt0() {
	}

	@Override
	public void IsFallingAt0() {
	}

	@Override
	public void IsRisingAt1() {
	}

	@Override
	public void IsFallingAt1() {
	}
}
