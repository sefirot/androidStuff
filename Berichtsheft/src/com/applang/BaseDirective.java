package com.applang;

import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.parser.node.Node;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import static com.applang.Util.*;
import static com.applang.VelocityUtil.*;

/**
 * A directive to prompt the user for a value.
 */
public class BaseDirective extends CustomDirective
{
	@Override
	public boolean render(InternalContextAdapter context, Writer writer, Node node)
	{
		return false;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	private String name = "";

	public void setName(String directive) {
		name = firstIdentifierFrom(directive);
	}

	@Override
	public int getType()
	{
		return type;
    }
	
	private int type = LINE;

	public void deconstruct(String signature, UserContext context) {
		if (signature.matches("" + CONTENT_PATTERN + END_PATTERN + "$"))
			type = BLOCK;
		
		setName(signature);
		
		if (arguments == null) {
			arguments = toStrings(argumentsFrom(signature));
		}
	}

	public Object construct(String signature, UserContext context) {
		deconstruct(signature, context);
		
		info.putString(PROMPT, getName());
		info.putString(VARIABLE, var = UserContext.hidden("default"));
		info.putStringArray(VALUES, arguments);
		info.putInt(TYPE, (Integer)getConstantByName("DIALOG_CONSTRUCT", "ConstructDialogs", null));
		
		Node node = (Node) context.get(UserContext.hidden(NODE));
		if (node != null) 
			context.put(var, Visitor.getEssentials(node));
		
		Object value = null;
		try {
			userContext = context;
			
			performDialog((String)getConstantByName("CONSTRUCT_ACTION", "ConstructDialogs", null));
			
			value = userContext.get(var);
		} 
		finally {
			userContext = null;
		}
		if (value instanceof ValList) {
			ValList list = (ValList)value;
			if (node != null) {
				Visitor.putEssentials(node, list);
				return node;
			}
			else
				return assemble(signature, list);
		}
		return null;
	}
	
	protected Bundle info = new Bundle();
	
	protected Object prompt;
	protected String var;
	protected String[] values;
	protected String option;

	public static final String TITLE = "title";
	public static final String PROMPT = "prompt";
	public static final String VARIABLE = "var";
	public static final String VALUES = "values";
	public static final String DEFAULTS = "defaults";
	public static final String TYPE = "type";
	public static final String RESULT = "result";
	public static final String STYLE = "style";
	
	public static ValMap options = vmap();
	public static Integer defaultOption = 0;
	
	public static void setOptions(int kind) {
        options.clear();
		switch (kind) {
		default:
			defaultOption = 0;
			for (int i = 0; i < 5; i++) 
				addOptions(i);
			break;
		case 0:
			defaultOption = Dialogs.DIALOG_TEXT_ENTRY;
			addOptions(kind);
			break;
		case 1:
			defaultOption = Dialogs.DIALOG_YES_NO_MESSAGE;
			addOptions(kind);
			break;
		case 2:
			defaultOption = Dialogs.DIALOG_SINGLE_CHOICE;
			addOptions(kind);
			break;
		case 3:
			defaultOption = Dialogs.DIALOG_SINGLE_CHOICE_CURSOR;
			addOptions(kind);
			break;
		}
	}
	
	private static void addOptions(int kind) {
		switch (kind) {
		case 0:
			options.put("TEXT_ENTRY", Dialogs.DIALOG_TEXT_ENTRY);
			options.put("TEXT_INFO", Dialogs.DIALOG_TEXT_INFO);
			break;
		case 1:
			options.put("YES_NO", Dialogs.DIALOG_YES_NO_MESSAGE);
			options.put("YES_NO_CANCEL", Dialogs.DIALOG_YES_NO_LONG_MESSAGE);
			break;
		case 2:
			options.put("SINGLE_CHOICE", Dialogs.DIALOG_SINGLE_CHOICE);
			options.put("MULTIPLE_CHOICE", Dialogs.DIALOG_MULTIPLE_CHOICE);
			break;
		case 3:
			options.put("CURSOR_SINGLE_CHOICE", Dialogs.DIALOG_SINGLE_CHOICE_CURSOR);
			options.put("CURSOR_MULTIPLE_CHOICE", Dialogs.DIALOG_MULTIPLE_CHOICE_CURSOR);
			break;
		case 4:
			options.put("LIST", Dialogs.DIALOG_LIST);
			break;
		}
	}
	
	public static UserContext userContext = null;
	
	public static void _wait() {
        synchronized (syncToken)
        {
            try {
                syncToken.wait();
            } catch (InterruptedException e) {
				log.error(TAG, e);
            }
        }
	}
	
	private static final Object syncToken = new Object();
	
	public static void _notify() {
        synchronized(syncToken)
        {
            syncToken.notify();
        }
	}
	
	protected void performDialog(String action) {
		Intent intent = new Intent(action)
				.putExtras(info);
		
		UserContext.EvaluationTask task = userContext.getEvaluationTask();
		task.doInForeground(intent);
		
		_wait();
	}
}

