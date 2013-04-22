package com.applang;

import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.parser.node.Node;

import com.applang.VelocityUtil.CustomDirective;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import static com.applang.VelocityUtil.*;

/**
 * A directive to prompt the user for a value.
 */
public class PromptDirective extends CustomDirective
{
	protected static final String TAG = CustomDirective.class.getSimpleName();
	
	public PromptDirective() {
		super();
		arguments = new String[] {"message","variable","[values]","[flag]"};
	}

	public static VelocityContext userContext = null;
	
	public static void _wait() {
        synchronized (syncToken)
        {
            try {
                syncToken.wait();
            } catch (InterruptedException e) {
				Log.e(TAG, "_wait", e);
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

	/**
	 * Return name of this directive.
	 */
	public String getName()
	{
		return "prompt";
	}
	
	/**
	 * Return type of this directive.
	 */
	public int getType()
	{
		return LINE;
    }

    @Override
    public void init(RuntimeServices rs, InternalContextAdapter context, Node node) throws TemplateInitException {
        super.init(rs, context, node);
	}
	
	/**
	 * Prompt the user for a value.
	 */
	public boolean render(InternalContextAdapter context, Writer writer, Node node)
	{
		if (!getArguments(context, node)) {
			return false;
		}
		else
			return getAnswer(context, 0);
	}
	
	protected Bundle info = new Bundle();
	
	protected boolean getAnswer(InternalContextAdapter context, int type) {
		info.putString("prompt", prompt.toString());
		info.putString("var", var);
		info.putStringArray("values", values);
		
		if (type == 0)
			switch (values.length) {
			case 1:
				type = Dialogs.DIALOG_TEXT_ENTRY;
				break;
			case 2:
				type = flag ? Dialogs.DIALOG_YES_NO_LONG_MESSAGE : Dialogs.DIALOG_YES_NO_MESSAGE;
				break;
			default:
				type = flag ? Dialogs.DIALOG_MULTIPLE_CHOICE : Dialogs.DIALOG_SINGLE_CHOICE;
				break;
			}
		info.putInt("type", type);
		
		Object value = null;
		try {
			userContext = (VelocityContext) context.getInternalUserContext();
			
			Intent intent = new Intent(Dialogs.PROMPT_ACTION)
					.putExtras(info);
			
			VelocityContext.EvaluationTask task = userContext.getEvaluationTask();
			task.doInForeground(intent);
			
			_wait();
			
			value = userContext.get(var);
			context.getInternalUserContext().put(var, value);
		} 
		finally {
			userContext = null;
		}
		return value != null;
	}
	
	protected Object prompt;
	protected String var;
	protected String[] values;
	protected boolean flag;

	protected boolean getArguments(InternalContextAdapter context, Node node) {
		prompt = getRequiredValue(node, 0, arguments[0], context);
		var = getRequiredVariable(node, 1, arguments[1]);
		values = arrayOfStrings(getOptionalValue(node, 2, context));
		flag = getOptionalBoolean(node, 3, context, false);
		
		return prompt != null && var != null;
	}
}

