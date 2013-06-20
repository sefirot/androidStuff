package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.database.Cursor;
import android.view.View;

public class AlertDialog extends Dialog implements DialogInterface {

	public AlertDialog(Context context) {
		// TODO Auto-generated constructor stub
	}

	public static class Builder {

		Context context;
		
		public Builder(Context context) {
			this.context = context;
		}

		public Builder setCancelable(boolean b) {
			return this;
		}

        public Builder setIcon(int iconId) {
            return this;
        }
        
        public Builder setMessage(CharSequence message) {
            return this;
        }

		public Builder setTitle(String title) {
			return this;
		}

		public Builder setPositiveButton(String string, OnClickListener onClickListener) {
			return this;
		}

		public Builder setPositiveButton(int id, OnClickListener onClickListener) {
			return this;
		}

		public Builder setNegativeButton(String string, OnClickListener onClickListener) {
			return this;
		}

		public Builder setNegativeButton(int id, OnClickListener onClickListener) {
			return this;
		}

		public Builder setNeutralButton(int buttonCancel, OnClickListener onClickListener) {
			return this;
		}
        
        public Builder setOnCancelListener(OnCancelListener onCancelListener) {
            return this;
        }
        
        public AlertDialog create() {
            final AlertDialog dialog = new AlertDialog(this.context);
            return dialog;
        }

        public Builder setItems(CharSequence[] items, final OnClickListener listener) {
            return this;
        }

		public Builder setSingleChoiceItems(String[] values, int checkedItem, OnClickListener onClickListener) {
			return this;
		}
        
        public Builder setMultiChoiceItems(CharSequence[] items, boolean[] checkedItems, final OnMultiChoiceClickListener listener) {
            return this;
        }
        
        public Builder setSingleChoiceItems(Cursor cursor, int checkedItem, String labelColumn, final OnClickListener listener) {
            return this;
        }
        
        public Builder setMultiChoiceItems(Cursor cursor, String isCheckedColumn, String labelColumn, final OnMultiChoiceClickListener listener) {
            return this;
        }
        
        public Builder setView(View view) {
            return this;
        }
        
	}

}
