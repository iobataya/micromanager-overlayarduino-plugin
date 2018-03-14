package org.micromanager.overlayarduino;

import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.micromanager.MMStudio;
import org.micromanager.api.DataProcessor;
import org.micromanager.api.ScriptInterface;
import org.micromanager.overlayarduino.*;
import org.micromanager.subtractbackground.*;
import org.micromanager.utils.MMDialog;

import mmcorej.CMMCore;
import net.miginfocom.swing.MigLayout;
import org.micromanager.utils.ReportingUtils;

@SuppressWarnings("serial")
public class AcqByTtlMigForm extends MMDialog implements ArduinoInputListener {
	private final ScriptInterface gui_;
	private final mmcorej.CMMCore mmc_;
	
	private final JLabel lblTitleStatus_;
	private final JLabel lblStatus_;
	private final JCheckBox chkACQ_;
	private final JButton btnAcquire_;
	private final JButton btnStop_;
	private final JCheckBox chkBG_;
	private final JButton btnBG_;	
	private final JRadioButton radioInput0_;
	private final JRadioButton radioInput1_;
	private final JLabel lblMessage_;

	private final Font arialSmallFont_;
	private final Font arialLargeFont_;

	private SubtractBackgroundProcessor subtract_ = null;
	private OverlayArduinoProcessor arduino_ = null;
	private AcqByTtlStatus acqStatus_;

	private static final int IDLE = 0x01;
	private static final int WAITING = 0x02;
	private static final int ACQ = 0x03;
	
	private int currentStatus_ = IDLE;
	
	public AcqByTtlMigForm(MMStudio gui)  {
		/*
		 * ｂｓｈマクロでやっていることをこのプラグインで独立したスレッドで実行したい。
		 * 
		 * 1. プラグインを開く 2. 撮像待ちボタンを押すーシグナルを待ち受ける。 2-1. もしTTL-0が立ち下がったらAcquisitionスタート
		 * 2-2. もしTTL‐１が立ち上がったら背景画像としてsnapしてsubtractionに登録。 3. STOPボタンを押したらシグナル待ち受けをやめる。
		 * 
		 */

		gui_ = gui;
		gui_.addMMBackgroundListener(this);
		mmc_ = gui.getMMCore();
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				dispose();
			}
		});
		
		
		// TODO: If pipeline alreday has these processors, use them.
//		//
//		/*
//		subtract_ = new SubtractBackgroundProcessor();
//		subtract_.setApp(gui_);
//		subtract_.makeConfigurationGUI();
//
//		arduino_ = new OverlayArduinoProcessor();
//		arduino_.setApp(gui_);
//		arduino_.makeConfigurationGUI();
//		*/
//
//		org.micromanager.
//		
//	    
////		gui_.addImageProcessor(subtract_);
//	//	gui_.addImageProcessor(arduino_);
//		if(!gui_.isLiveModeOn()) {
//			gui_.enableLiveMode(true);
//		}

		// tell processors instance
		ArduinoPoller poller = ArduinoPoller.getInstance(gui_);
		if(poller!=null) {
			poller.addListener(this);
		}

		arialSmallFont_ = new Font("Arial", Font.PLAIN, 12);
		arialLargeFont_ = new Font("Arial", Font.PLAIN, 16);
		this.setLayout(new MigLayout("flowx, fill, insets 8"));
		this.setTitle(AcqByTTL.menuName);
		loadAndRestorePosition(100, 100, 350, 250);

		gui_.getImageProcessorPipeline();
		
		// Status title
		lblTitleStatus_ = new JLabel("Status:");
		lblTitleStatus_.setFont(arialSmallFont_);
		// Status label
		lblStatus_ = new JLabel(" ");
		lblStatus_.setFont(arialSmallFont_);
		add(lblTitleStatus_);
		add(lblStatus_,"wrap");
		
		// Checkbox wait for trigger to ACQ by DigitalInput0
		chkACQ_ = new JCheckBox();
		chkACQ_.setText("Use Trigger 0 HIGH->LOW for acquisition");
		chkACQ_.setFont(arialSmallFont_);
		add(chkACQ_,"wrap");

		// Button Start
		btnAcquire_ = new JButton("ACQUIRE");
		btnAcquire_.setFont(arialSmallFont_);
		btnAcquire_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				// TODO: Start acquisition
				ReportingUtils.logDebugMessage("Acquisition button clicked");
			}
		});
		// Button Stop
		btnStop_ = new JButton("STOP");
		btnStop_.setFont(arialSmallFont_);
		btnStop_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				// TODO: Stop acquisition
				ReportingUtils.logDebugMessage("Stop acquisition button cliked.");
		}
		});
		add(btnAcquire_);
		add(btnStop_,"wrap");
		
		// Checkbox wait for trigger to snap background
		chkBG_ = new JCheckBox("Use Trigger 1 LOW>HIGH for snap BG");
		chkBG_.setFont(arialSmallFont_);		
		// Button Snap
		btnBG_ = new JButton("Snap as BG");
		btnBG_.setFont(arialSmallFont_);
		btnBG_.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				ReportingUtils.logDebugMessage("Snap button clicked");
			}
		});
		add(chkBG_);
		add(btnBG_,"wrap");

		// Input signals
		radioInput0_ = new JRadioButton("Input0");
		radioInput0_.setEnabled(false);
		radioInput1_ = new JRadioButton("Input1");
		radioInput1_.setEnabled(false);
		// Add to GUI
		add(radioInput0_);
		add(radioInput1_,"wrap");
		
		// message label
		lblMessage_ = new JLabel(" ");
		add(lblMessage_, "span 3, wrap");
		
		setVisible(true);
	}

	public synchronized void setStatus(final String status) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (status != null) {
					lblMessage_.setText(status);
				}
			}
		});
	}

	private int currentDigitalIn_ = 0;

	@Override
	public void ValueChanged(ArduinoInputEvent e) {
		radioInput0_.setSelected(e.isHighAt0());
		radioInput1_.setSelected(e.isHighAt1());
	}

	@Override
	public void IsRisingAt0() {
		this.setStatus(String.format("Signal Input0 rising to HIGH (%d)", currentDigitalIn_));
	}

	@Override
	public void IsFallingAt0() {
		ReportingUtils.logDebugMessage("TTL 0 is falling! Acquisition start!");
		//gui_.runAcquisition();
		this.setStatus(String.format("Signal Input0 falling to LOW (%d)", currentDigitalIn_));
	}

	@Override
	public void IsRisingAt1() {
		this.setStatus(String.format("Signal Input1 rising to HIGH (%d)", currentDigitalIn_));
	}

	@Override
	public void IsFallingAt1() {
		this.setStatus(String.format("Signal Input1 falling to LOW", currentDigitalIn_));
	}
}
