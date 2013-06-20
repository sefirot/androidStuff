package android.content;

public interface DialogInterface {

	public interface OnClickListener {
        public void onClick(DialogInterface dialog, int which);
	}

	public interface OnMultiChoiceClickListener {
        public void onClick(DialogInterface dialog, int which, boolean isChecked);
	}

	public interface OnCancelListener {
        public void onCancel(DialogInterface dialog);
	}

}
