package com.applang;

import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.parser.node.Node;

import static com.applang.Util.*;
import static com.applang.VelocityUtil.*;

public class CursorDirective extends PromptDirective
{
    public CursorDirective() {
		super();
        arguments = strings("message_S","variable_R",
        		"uri_U",
        		"projection_L",
        		optionalize("selection_S"),
        		optionalize("selectionArgs_L"),
        		optionalize("sortOrder_S"),
        		optionalize("option_S"));
	}

	public String getName() {
        return "cursor";
    }

    public int getType() {
        return LINE;
    }

	protected String uri;
	protected String selection;
	protected String[] selectionArgs;
	protected String sortOrder;
	
    @Override
	protected boolean getArguments(InternalContextAdapter context, Node node) {
		prompt = getRequiredValue(node, 0, argumentName(arguments[0]), context);
		var = getRequiredVariable(node, 1, argumentName(arguments[1]));
		uri = getRequiredValue(node, 2, argumentName(arguments[2]), context).toString();
		values = arrayOfStrings(getRequiredValue(node, 3, argumentName(arguments[3]), context));
		selection = (String) getOptionalValue(node, 4, context);
		selectionArgs = arrayOfStrings(getOptionalValue(node, 5, context));
		selection = (String) getOptionalValue(node, 6, context);
		option = getOptionalString(node, 7, context, "");
		
		setOptions(3);
		
		return prompt != null && var != null && uri != null && values != null;
	}

    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
		if (!getArguments(context, node)) {
			return false;
		}
		Integer type = valueOrElse(defaultOption, options.get(option));
		boolean flag = type == defaultOption;
		if (values.length < (flag ? 1 : 3)) {
			String msg = flag ? 
					"array needs one member minimum : label column" : 
					"array needs three members : primary key, label column, integer (isChecked) column";
			log.error(TAG, new Exception(argumentErrMsg(argumentName(arguments[3])) + msg));
			return false;
		}
		info.putString("uri", uri);
		info.putString("selection", selection);
		info.putStringArray("selectionArgs", selectionArgs);
		info.putString("sortOrder", sortOrder);
		return getAnswer(context, type);
    }

}

