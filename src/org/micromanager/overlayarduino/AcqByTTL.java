package org.micromanager.overlayarduino;

import org.micromanager.api.ScriptInterface;

import mmcorej.CMMCore;
import org.micromanager.api.MMPlugin;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;
import org.micromanager.MMStudio;
import org.micromanager.api.MMListenerInterface;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.FileDialogs;
import org.micromanager.utils.GUIUtils;
import org.micromanager.utils.MMDialog;
import org.micromanager.utils.MMException;
import org.micromanager.utils.ReportingUtils;

public class AcqByTTL implements MMPlugin {
	public static final String menuName = "ACQ by TTL";
	public static final String tooltipDescription = "Displays a frame to control triggering of acq by external TTL signal";

	// Provides access to the Micro-Manager Java API (for GUI control and high-
	// level functions).
	private ScriptInterface gui_;
	
	private static MMStudio mmStudio_;
	// Provides access to the Micro-Manager Core API (for direct hardware
	// control)
	private CMMCore core_;

	private AcqByTtlMigForm acqform_;
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		    mmStudio_ = new MMStudio(false);			
			AcqByTTL acqbyttl = new AcqByTTL();
			acqbyttl.setApp(mmStudio_);
			acqbyttl.show();

		} catch (ClassNotFoundException e) {
			ReportingUtils.showError(e, "A java error has caused Micro-Manager to exit.");
			System.exit(1);
		} catch (IllegalAccessException e) {
			ReportingUtils.showError(e, "A java error has caused Micro-Manager to exit.");
			System.exit(1);
		} catch (InstantiationException e) {
			ReportingUtils.showError(e, "A java error has caused Micro-Manager to exit.");
			System.exit(1);
		} catch (UnsupportedLookAndFeelException e) {
			ReportingUtils.showError(e, "A java error has caused Micro-Manager to exit.");
			System.exit(1);
		}		
	}

	@Override
	public String getDescription() {
		return tooltipDescription;
	}

	@Override
	public String getInfo() {
		return tooltipDescription;
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getCopyright() {
		return "JPK Instruments AG, 2018";
	}

	@Override
	public void dispose() {
	}

	@Override
	public void setApp(ScriptInterface app) {
		gui_ = app;
		core_ = gui_.getMMCore();
	}

	@Override
	public void show() {
		acqform_ = new AcqByTtlMigForm(mmStudio_);
	}

}
