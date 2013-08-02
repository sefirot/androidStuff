package android.widget;

import javax.swing.JTextArea;

import android.content.Context;

public class EditText extends TextView {

    public EditText(Context context) {
    	super(context);
    	getTextArea().setEditable(true);
    }

	public void setSelection(int start, int stop) {
		JTextArea textArea = getTextArea();
		textArea.setSelectionStart(start);
		textArea.setSelectionEnd(stop);
	}

}
