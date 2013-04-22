package com.applang;

import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.parser.node.Node;

import android.util.Log;

import static com.applang.VelocityUtil.arrayOfStrings;

public class CursorDirective extends PromptDirective
{
    public CursorDirective() {
		super();
        arguments = new String[] {"message","variable",
        		"uri","projection","[selection]","[selectionArgs]","[sortOrder]",
        		"[flag]"};
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
		prompt = getRequiredValue(node, 0, arguments[0], context);
		var = getRequiredVariable(node, 1, arguments[1]);
		uri = getRequiredValue(node, 2, arguments[2], context).toString();
		values = arrayOfStrings(getRequiredValue(node, 3, arguments[3], context));
		selection = (String) getOptionalValue(node, 4, context);
		selectionArgs = arrayOfStrings(getOptionalValue(node, 5, context));
		selection = (String) getOptionalValue(node, 6, context);
		flag = getOptionalBoolean(node, 7, context, false);
		
		return prompt != null && var != null && uri != null && values != null;
	}

    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
		if (!getArguments(context, node)) {
			return false;
		}
		if (values.length < (flag ? 3 : 1)) {
			Log.e(TAG, argumentErrMsg(arguments[3]) + (flag ? 
					"array needs three members : primary key, label column, integer (isChecked) column" : 
					"array needs one member minimum : label column"));
			return false;
		}
		info.putString("uri", uri);
		info.putString("selection", selection);
		info.putStringArray("selectionArgs", selectionArgs);
		info.putString("sortOrder", sortOrder);
		return getAnswer(context, flag ? 
				Dialogs.DIALOG_MULTIPLE_CHOICE_CURSOR : Dialogs.DIALOG_SINGLE_CHOICE_CURSOR);
    }

}

