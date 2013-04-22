package com.applang;

import java.io.Writer;
import javax.swing.JOptionPane;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.runtime.parser.node.Node;

import com.applang.VelocityUtil.CustomDirective;


//import org.gjt.sp.jedit.textarea.JEditTextArea;
//import velocity.VelocityConstants;

/**
 * A directive to prompt the user for a value.
 */
public class Prompt extends CustomDirective
//    implements VelocityConstants
{

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

   /**
    * Prompt the user for a value.
    */
   public boolean render(InternalContextAdapter context,
                         Writer writer, Node node)
   throws MethodInvocationException
   {
      Object prompt = getRequiredValue(node, 0, "label", context);
      if (prompt == null) {
         return false;
      }
      String key = getRequiredVariable(node, 1, "key");
      Object defaultValue = getOptionalValue(node, 2, context);
      boolean overrideContext = getOptionalBoolean(node, 3, context);

      if (!overrideContext && context.getInternalUserContext().get(key) != null) {
         return true;
      }

//      JEditTextArea textArea = (JEditTextArea) context.get(TEXT_AREA);
      Object value = JOptionPane.showInputDialog(null, prompt,
                                                 "Velocity Prompt",
                                                 JOptionPane.QUESTION_MESSAGE,
                                                 null, null, defaultValue);
      if (value != null) {
         context.getInternalUserContext().put(key, value);
      }
      return true;
   }

}

