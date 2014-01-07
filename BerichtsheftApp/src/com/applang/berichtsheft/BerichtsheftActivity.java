package com.applang.berichtsheft;

import com.applang.BaseDirective;
import com.applang.Util.Job;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import static com.applang.Util.*;

public class BerichtsheftActivity extends Activity
{
	public static final String packageName = getPackageNameByClass(R.class);

	{
		setPackageInfo(BerichtsheftActivity.packageName);
	}
	
	Job<String> followUp = null;
	Object[] params = null;
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 0:
			if (followUp != null)
				try {
					String result = data.getExtras().getString(BaseDirective.RESULT);
					followUp.perform("null".equals(String.valueOf(result)) ? null : result, params);
				} catch (Exception e) {
					Log.e(TAG, "prompt...followUp", e);
				}
			break;
		}
	}

	public BerichtsheftActivity(Job<String> followUp, Object...params) {
		super();
		this.followUp = followUp;
		this.params = params;
	}

	public BerichtsheftActivity() {
		super();
	}

}
