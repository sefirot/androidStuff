package com.applang;

import java.io.StringWriter;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.parser.node.ASTBlock;
import org.apache.velocity.runtime.parser.node.Node;

import static com.applang.Util.*;

public class SpinnerDirective extends PromptDirective
{
    public SpinnerDirective() {
		super();
	}

	public String getName() {
        return "spinner";
    }

    public int getType() {
        return BLOCK;
    }

    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
		if (!getArguments(context, node)) {
			return false;
		}
		if (notAvailable(0, values)) {
			values = getBlockContent(context, node);
		}
		return getAnswer(context, Dialogs.DIALOG_LIST);
    }

	private String[] getBlockContent(InternalContextAdapter context, Node node) {
        for(int i=0; i<node.jjtGetNumChildren(); i++) {
            if (node.jjtGetChild(i) != null ) {
                if(node.jjtGetChild(i) instanceof ASTBlock) {
                    try {
						//reading block content and rendering it  
						StringWriter blockContent = new StringWriter();
						node.jjtGetChild(i).render(context, blockContent);
						return blockContent.toString().split("\n");
					} catch (Exception e) {
						log.error(TAG, e);
					}
                    break;
                }
            }
        }
        return null;
	}

}

