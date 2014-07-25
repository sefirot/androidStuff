package com.applang.berichtsheft.plugin;

import java.awt.Color;
import java.io.StringWriter;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;

import console.Console;
import console.ConsolePane;
import console.ConsolePlugin;
import console.Output;
import console.Shell;

import android.util.Log;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.PluginUtils.*;

public class BerichtsheftShell extends Shell
{
    private static final String TAG = BerichtsheftShell.class.getSimpleName();

	public static void print(Object... params) {
		final String string = write(new StringWriter(), params).toString();
		Console console = getConsole(true);
		if (console != null) {
			perform(console, false, new Job<Console>() {
				public void perform(Console console, Object[] parms) throws Exception {
					String string = param("", 0, parms);
					Output output = console.getOutput();
					output.writeAttrs(ConsolePane.colorAttributes(Color.BLACK), string);
				}
			}, string);
			return;
		}
	}
	
	public static void perform(Console console, boolean animated, Job<Console> job, Object...params) {
		setBerichtsheftShell(console);
		try {
			if (animated)
				consoleWait(console, true);
			job.perform(console, params);
		} catch (Exception e) {
			Log.e(TAG, "perform", e);
		}
		finally {
			if (animated)
				consoleWait(console, false);
		}
	}
	
	public static void setBerichtsheftShell(Console console) {
		String name = getProperty("berichtsheft.shell.title");
		if (!name.equals(console.getShell().getName())) 
			console.setShell(name);
	}
	
	public static void consoleWait(Console console, boolean start) {
		if (start)
			console.startAnimation();
		else
			console.stopAnimation();
	}
	
	public static Console getConsole(boolean show) {
		Console console = null;
		View view = jEdit.getActiveView();
		if (view != null) {
			if (show)
				BerichtsheftPlugin.showDockable(view, "console");
			console = ConsolePlugin.getConsole(view);
		}
		return console;
	}
	
	@Override
	public void printInfoMessage(Output output) {
		// TODO Auto-generated method stub
//		super.printInfoMessage(output);
	}

	@Override
	public void printPrompt(Console console, Output output) {
		// TODO Auto-generated method stub
//		super.printPrompt(console, output);
	}

	public BerichtsheftShell(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void execute(Console console, String input, Output output, Output error, String command) {
		// TODO Auto-generated method stub

	}

}
