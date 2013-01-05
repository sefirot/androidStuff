package com.applang.berichtsheft.ui;

import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.msg.ViewUpdate;

/**
 * The Berichtsheft plugin
 * 
 */
public class BerichtsheftPlugin extends EditPlugin {
	public static final String NAME = "berichtsheft";
	public static final String OPTION_PREFIX = "options.berichtsheft.";
	public static final String MENU = "";
	
	@Override
	public void start() {
		BerichtsheftToolBar.init();
		EditBus.addToBus(this);
	}
	
	@Override
	public void stop() {
		EditBus.removeFromBus(this);
		BerichtsheftToolBar.remove();
	}
	
	@EBHandler
	public void handleViewUpdate(ViewUpdate vmsg)
	{
		if (vmsg.getWhat() == ViewUpdate.CREATED)
		{
			View v = vmsg.getView();
			BerichtsheftToolBar.create(v);
		}
		if (vmsg.getWhat() == ViewUpdate.CLOSED) {
			View v = vmsg.getView();
			BerichtsheftToolBar.remove(v);
		}
	}
}
