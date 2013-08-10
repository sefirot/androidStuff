package android.app;

import java.awt.Frame;

import javax.swing.JDialog;

public class Dialog extends JDialog
{
	public Dialog(Frame owner, boolean modal) {
		super(owner, modal);
	}

	public void dismiss() {
		setVisible(false);
	}

	public void open() {
		setVisible(true);
		toFront();
	}
}
