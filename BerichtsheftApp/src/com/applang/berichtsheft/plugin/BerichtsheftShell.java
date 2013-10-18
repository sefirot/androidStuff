package com.applang.berichtsheft.plugin;

import static com.applang.Util2.*;

import java.awt.Color;
import java.io.StringWriter;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;

import com.applang.Util2;

import console.Console;
import console.ConsolePane;
import console.ConsolePlugin;
import console.Output;
import console.Shell;

public class BerichtsheftShell extends Shell
{
	public static void print(Object... params) {
		String string = format(new StringWriter(), params).toString();
		View view = jEdit.getActiveView();
		if (view != null) {
			Console console = ConsolePlugin.getConsole(view);
			if (console != null) {
				String name = BerichtsheftPlugin.getProperty("berichtsheft.shell.title");
				if (!name.equals(console.getShell().getName())) {
					console.setShell(name);
				}
				Output output = console.getOutput();
				output.writeAttrs(ConsolePane.colorAttributes(Color.BLACK), string);
				return;
			}
		}
		Util2.print(string);
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
