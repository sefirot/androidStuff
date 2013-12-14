package com.applang;

import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.parser.node.Node;

import android.app.Activity;
import android.content.Intent;

import static com.applang.Util.*;
import static com.applang.VelocityUtil.*;

/**
 * A directive to prompt the user for a value.
 */
public class PromptDirective extends BaseDirective
{
	public PromptDirective() {
		super();
		arguments = strings("message_S","variable_R",
				VelocityUtil.optionalize("values_L"),
				VelocityUtil.optionalize("option_S"));
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
	
	protected boolean getAnswer(InternalContextAdapter context, int type) {
		info.putString(BaseDirective.PROMPT, prompt.toString());
		info.putString(BaseDirective.VARIABLE, var);
		info.putStringArray(BaseDirective.VALUES, values);
		
		if (type == 0) {
			type = valueOrElse(defaultOption, options.get(option));
		}
		info.putInt(BaseDirective.TYPE, type);
		
		Object value = null;
		try {
			userContext = (UserContext) context.getInternalUserContext();
			
			performDialog(Dialogs.PROMPT_ACTION);
			
			value = userContext.get(var);
			context.getInternalUserContext().put(var, value);
		} 
		finally {
			userContext = null;
		}
		return value != null;
	}

	protected boolean getArguments(InternalContextAdapter context, Node node) {
		prompt = getRequiredValue(node, 0, VelocityUtil.argumentName(arguments[0]), context);
		var = getRequiredVariable(node, 1, VelocityUtil.argumentName(arguments[1]));
		values = arrayOfStrings(getOptionalValue(node, 2, context));
		option = getOptionalString(node, 3, context, "");
		
		setOptions(Math.min(2, values.length - 1));
		
		return prompt != null && var != null;
	}

	public static void prompt(Activity activity, 
			int type, String title, String message, String[] values, 
			String...defaults)
	{
		Intent intent = new Intent(Dialogs.PROMPT_ACTION)
				.putExtra(BaseDirective.TYPE, type % 100)
				.putExtra(BaseDirective.TITLE, title)
				.putExtra(BaseDirective.PROMPT, message)
				.putExtra(BaseDirective.VALUES, values)
				.putExtra(BaseDirective.DEFAULTS, defaults);
		activity.startActivityForResult(intent, 0);
	}
}

