package com.applang;

import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.AbstractContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.ExtendedParseException;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.directive.MacroParseException;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.Token;
import org.apache.velocity.runtime.parser.node.ASTAddNode;
import org.apache.velocity.runtime.parser.node.ASTAndNode;
import org.apache.velocity.runtime.parser.node.ASTAssignment;
import org.apache.velocity.runtime.parser.node.ASTBlock;
import org.apache.velocity.runtime.parser.node.ASTComment;
import org.apache.velocity.runtime.parser.node.ASTDirective;
import org.apache.velocity.runtime.parser.node.ASTDivNode;
import org.apache.velocity.runtime.parser.node.ASTEQNode;
import org.apache.velocity.runtime.parser.node.ASTElseIfStatement;
import org.apache.velocity.runtime.parser.node.ASTElseStatement;
import org.apache.velocity.runtime.parser.node.ASTEscape;
import org.apache.velocity.runtime.parser.node.ASTEscapedDirective;
import org.apache.velocity.runtime.parser.node.ASTExpression;
import org.apache.velocity.runtime.parser.node.ASTFalse;
import org.apache.velocity.runtime.parser.node.ASTFloatingPointLiteral;
import org.apache.velocity.runtime.parser.node.ASTGENode;
import org.apache.velocity.runtime.parser.node.ASTGTNode;
import org.apache.velocity.runtime.parser.node.ASTIdentifier;
import org.apache.velocity.runtime.parser.node.ASTIfStatement;
import org.apache.velocity.runtime.parser.node.ASTIntegerLiteral;
import org.apache.velocity.runtime.parser.node.ASTIntegerRange;
import org.apache.velocity.runtime.parser.node.ASTLENode;
import org.apache.velocity.runtime.parser.node.ASTLTNode;
import org.apache.velocity.runtime.parser.node.ASTMap;
import org.apache.velocity.runtime.parser.node.ASTMethod;
import org.apache.velocity.runtime.parser.node.ASTModNode;
import org.apache.velocity.runtime.parser.node.ASTMulNode;
import org.apache.velocity.runtime.parser.node.ASTNENode;
import org.apache.velocity.runtime.parser.node.ASTNotNode;
import org.apache.velocity.runtime.parser.node.ASTObjectArray;
import org.apache.velocity.runtime.parser.node.ASTOrNode;
import org.apache.velocity.runtime.parser.node.ASTReference;
import org.apache.velocity.runtime.parser.node.ASTSetDirective;
import org.apache.velocity.runtime.parser.node.ASTStringLiteral;
import org.apache.velocity.runtime.parser.node.ASTSubtractNode;
import org.apache.velocity.runtime.parser.node.ASTText;
import org.apache.velocity.runtime.parser.node.ASTTrue;
import org.apache.velocity.runtime.parser.node.ASTWord;
import org.apache.velocity.runtime.parser.node.ASTprocess;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.runtime.visitor.BaseVisitor;

import android.util.Log;

import static com.applang.Util.*;
 
public class VelocityUtil
{
	public static final Character VRI = '$';	//	Reference Indicator
	public static final Character VDI = '#';	//	Directive Indicator
	
	public static final Character ARGUMENT_TYPER = '_';
	public static final String[] ARGUMENT_SEPARATORS = {", ", " "};
	public static final Pattern SUFFIX_PATTERN = Pattern.compile("\\d*$");
	
	public static int argumentTyperIndex(String arg) {
		int offs = 0;
		if (arg.startsWith(String.valueOf(ARGUMENT_TYPER))) {
			arg = strip(Constraint.START, arg, ARGUMENT_TYPER);
			offs = 1;
		}
		return offs + arg.indexOf(ARGUMENT_TYPER);
	}
	
	public static String argumentName(String arg) {
		int index = argumentTyperIndex(arg);
		if (index < 0)
			return strip(Constraint.START, arg, ARGUMENT_TYPER, VRI);
		else
			return strip(Constraint.START, arg.substring(0, index), ARGUMENT_TYPER, VRI);
	}
	
	public static MatchResult argumentSuffix(String arg) {
		int index = argumentTyperIndex(arg);
		return findFirstIn(arg.substring(0, index), SUFFIX_PATTERN);
	}
	
	public static boolean isOptionalArgument(String arg) {
		return arg.startsWith(ARGUMENT_TYPER + "");
	}
	
	public static String optionalize(String arg) {
		return isOptionalArgument(arg) ? arg : String.valueOf(ARGUMENT_TYPER).concat(arg);
	}
	
	public static String argumentType(String arg) {
		int index = argumentTyperIndex(arg);
		if (index < 0)
			return "";
		else
			return arg.substring(index + 1);
	}
	
    public static abstract class CustomDirective extends Directive
    {
    	protected static final String TAG = CustomDirective.class.getSimpleName();
    	protected static final org.apache.velocity.runtime.log.Log log = 
    			new org.apache.velocity.runtime.log.Log();
        
    	protected String[] arguments = null;
    	
    	public String signature(boolean full) {
			return VDI + getName() + 
    				argumentListString(full ? arguments : strings()) + 
    				(BLOCK == getType() ? BLOCK_END : "");
    	}
		
    	public String argumentListString(String... args) {
			return "(" + join(ARGUMENT_SEPARATORS[0], args) + ")";
    	}
		
		protected String argumentErrMsg(String name) {
			return signature(false) + " error : " + enclose("'", name, "' ");
		}
    	
		/**
		 * Returns the indexed argument as a required value.
		 */
		protected Object getRequiredValue(Node node, int idx, String argumentName, InternalContextAdapter context) throws MethodInvocationException
		{
			if (!requireArgument(node, idx, argumentName)) {
				return null;
			}
			Object obj = node.jjtGetChild(idx).value(context);
			if (obj == null) {
				rsvc.getLog().error(argumentErrMsg(argumentName) + "value of argument cannot be null");
				return null;
			}
			return obj;
		}
		
		/**
		 * Returns the indexed argument as a required literal.
		 */
		protected String getRequiredLiteral(Node node, int idx, String argumentName)
		{
			if (!requireArgument(node, idx, argumentName)) {
				return null;
			}
			return node.jjtGetChild(idx).literal();
		}
		
		/**
		 * Returns the index argument as a required variable name.
		 */
		protected String getRequiredVariable(Node node, int idx, String argumentName)
		{
			String var = getRequiredLiteral(node, idx, argumentName);
			return var == null ? null : var.substring(1);
		}
		
		/**
		 * Returns the indexed argument as an optional boolean.
		 */
		protected boolean getOptionalBoolean(Node node, int idx, InternalContextAdapter ctx) throws MethodInvocationException
		{
			return getOptionalBoolean(node, idx, ctx, false);
		}
		
		/**
		 * Returns the indexed argument as an optional boolean.
		 */
		protected boolean getOptionalBoolean(Node node, int idx, InternalContextAdapter ctx, boolean defaultValue) throws MethodInvocationException
		{
			Object obj = getOptionalValue(node, idx, ctx);
			if (obj == null || !(obj instanceof Boolean))
				return defaultValue;
			return ((Boolean) obj).booleanValue();
		}
		
		/**
		 * Returns the indexed argument as an optional string.
		 */
		protected String getOptionalString(Node node, int idx, InternalContextAdapter ctx, String defaultValue) throws MethodInvocationException
		{
			Object obj = getOptionalValue(node, idx, ctx);
			if (obj == null || !(obj instanceof String))
				return defaultValue;
			return (String) obj;
		}
		
		/**
		 * Returns an optional argument as a value.
		 */
		protected Object getOptionalValue(Node node, int idx, InternalContextAdapter context) throws MethodInvocationException
		{
			Node target = getOptionalNode(node, idx);
			if (target == null) {
				return null;
			}
			return target.value(context);
		}
		
		/**
		 * Returns an optional node.
		 */
		protected Node getOptionalNode(Node parent, int idx)
		{
			if (hasArgument(parent, idx)) {
				return parent.jjtGetChild(idx);
			}
			return null;
		}
		
		/**
		 * Validates that a required argument is available.
		 */
		protected boolean requireArgument(Node node, int idx, String argName)
		{
			if (!hasArgument(node, idx)) {
				rsvc.getLog().error(argumentErrMsg(argName) + "argument required");
				return false;
			}
			return true;
		}
		
		/**
		 * Returns <code>true</code> if the given specified argument exists.
		 */
		protected boolean hasArgument(Node node, int idx)
		{
			return idx < node.jjtGetNumChildren();
		}
		
		/**
		 * Returns the block child node.
		 */
		protected Node getBlockNode(Node parent)
		{
			for (int i=0; i<parent.jjtGetNumChildren(); i++) {
				if (parent.jjtGetChild(i) instanceof ASTBlock)
					return parent.jjtGetChild(i);
			}
			return null;
		}
		
    }
    
	public static final int CONTENT = 0;
	public static final int DIRECTIVE = 1;
	public static final int REFERENCE = 2;
	public static final int EXPRESSION = 3;
	public static final int IDENTIFIER = 4;
	public static final int METHOD = 0;

    public static class Visitor extends BaseVisitor
    {
    	protected static final String TAG = CustomContext.class.getSimpleName();
    	protected static final org.apache.velocity.runtime.log.Log log = 
    			new org.apache.velocity.runtime.log.Log();
        
    	public static Stack<Object[]> lostAndFound = new Stack<Object[]>();
    	
    	public static void visitLostAndFound(Job<Object[]> checkout, Object[] params, int...lc) {
    		for (int i = lostAndFound.size() - 1; i > -1; i--) {
    			Object[] objects = lostAndFound.get(i);
    			Token t = (Token) objects[2];
				if (compareLC(t, lc) > 0) {
					try {
						checkout.perform(objects, params);
						lostAndFound.remove(objects);
					} catch (Exception e) {
						log.error(TAG, e);
					}
				}
			}
    	}
    	
    	public static Predicate<Node> isEssential = new Predicate<Node>() {
			@Override
			public boolean apply(Node node) {
				int group = nodeGroup(node);
				if (group == DIRECTIVE) {
					Token lastToken = node.getLastToken();
					if (isEndToken(lastToken)) {
						lostAndFound.push(new Object[] {
								indent,
								node,
								lastToken});
					}
					return true;
				}
				int groupParent = nodeGroup(node.jjtGetParent());
				if (group == IDENTIFIER) {
					if (groupParent == METHOD)
						return false;
					else
						return true;
				}
				else {
					if (groupParent == EXPRESSION)
						return false;
				}
				if (group == REFERENCE) 
					return true;
				else if (group == EXPRESSION)
					return true;
				else
					return false;
			}
		};

		public static int nodeGroup(Object node) {
			if (node == null)
				return -1;
			String name = node.getClass().getSimpleName();
			int ast = name.indexOf("AST");
			if (ast > -1)
				name = name.substring(ast + 3);
			if ("ObjectArray".equals(name) || "Map".equals(name)) return EXPRESSION;
			if (name.endsWith("Literal")) return EXPRESSION;
			if (name.endsWith("Range")) return EXPRESSION;
			if (name.endsWith("Expression")) return EXPRESSION;
			if (name.endsWith("True")) return EXPRESSION;
			if (name.endsWith("False")) return EXPRESSION;
			if (name.endsWith("Text")) return EXPRESSION;
			if (name.endsWith("EQNode")) return EXPRESSION;
			if (name.endsWith("NENode")) return EXPRESSION;
			if (name.endsWith("GENode")) return EXPRESSION;
			if (name.endsWith("GTNode")) return EXPRESSION;
			if (name.endsWith("LENode")) return EXPRESSION;
			if (name.endsWith("LTNode")) return EXPRESSION;
			if (name.endsWith("Directive")) return DIRECTIVE;
			if (name.endsWith("Statement")) return DIRECTIVE;
			if (name.endsWith("Reference")) return REFERENCE;
			if (name.endsWith("Method")) return METHOD;
			if (name.endsWith("Identifier")) return IDENTIFIER;
			return CONTENT;
		};
		
		public static String nodeCategory(Object node) {
			switch (nodeGroup(node)) {
			case -1:		return "";
			case DIRECTIVE:		return "Directive";
			case REFERENCE:		return "Reference";
			case METHOD:		return "Method";
			case EXPRESSION:		return "Expression";
			case IDENTIFIER:		return "Identifier";
			default:
				return "Content";
			}
		}
		
		public static int blockDepth(Node node) {
			int blocks = 0;
			if (node != null) {
				while ((node = node.jjtGetParent()) != null) {
					if (node instanceof ASTBlock)
						blocks++;
				}
			}
			return blocks;
		}
    	
    	public static int partOfIfStatement(Object node) {
    		if (node instanceof ASTIfStatement)
    			return 1;
    		else if (node instanceof ASTElseIfStatement)
    			return 2;
    		else if (node instanceof ASTElseStatement)
    			return 3;
    		else 
    			return 0;
		}
    	
    	public static boolean isProcessNode(Object node) {
    		return node instanceof ASTprocess;
		}
    	
    	public static boolean isMethodNode(Object node) {
    		return node instanceof ASTReference && 
    				((Node)node).jjtGetNumChildren() > 0 && 
    				((Node)node).jjtGetChild(0) instanceof ASTMethod;
		}

		public static String nodeInfo(Node node, Object...params) {
			Token first = node.getFirstToken();
			int detail = param(0, 0, params);
			if (detail > 0) {
				String tokens = "";
				tokens = formatLC(beginLC(node)) + " - " + formatLC(endLC(node));
				tokens += TAB + enclose("'", totalImage(first));
				return node.getClass().getSimpleName() + TAB + tokens;
			}
			else {
				int group = nodeGroup(node);
				switch (group) {
				case EXPRESSION:
					Token last = node.getLastToken();
					StringBuilder sb = new StringBuilder();
					for (Token t = first; t != null; ) {
						if (t.next != null) {
							sb.append(t.image);
							if (t == last)
								break;
						}
						t = t.next;
					}
					return sb.toString();
				case DIRECTIVE:
					return VDI + firstIdentifierFrom(first.image);
				case REFERENCE:
					if (isMethodNode(node))
						return VRI + firstMethodFrom(tokens(node));
				}
				return first.image;
			}
		}

		public static String totalImage(Token t) {
			String special = "";
			if (t.specialToken != null && ! t.specialToken.image.startsWith("##"))
				special = t.specialToken.image;
			return special + t.image;
		}

		public static int[] span(Object nodeOrToken) {
			if (nodeOrToken instanceof Node) {
				Node node = (Node) nodeOrToken;
				Token first = node.getFirstToken();
				Token last = node.getLastToken();
				return ints( first.beginLine, first.beginColumn,
						last.endLine, last.endColumn );
			}
			else if (nodeOrToken instanceof Token) {
				Token t = (Token) nodeOrToken;
				return ints( t.beginLine, t.beginColumn, 
						t.endLine, t.endColumn );
			}
			else
				return null;
		}

		public static boolean isEndToken(Token t) {
			Matcher matcher = END_PATTERN.matcher(t.image);
			return matcher.find();
		}

		public static String tokens(Node node, Object...params) {
			String tokens = "";
			if (node != null) {
				char stumble = param(Character.valueOf((char) 0), 0, params);
				Token last = node.getLastToken();
				for (Token t = node.getFirstToken(); t != null; t = t.next) {
					if (t.next != null) {
						String image = t.image;
						int limit = image.indexOf(stumble);
						if (limit < 0 || tokens.length() < 1)
							tokens += image;
						else {
							tokens += image.substring(0, limit);
							break;
						}
						if (t == last)
							break;
					}
				}
			}
			return tokens;
		}

		public static String tail(Node node) {
			StringBuilder sb = new StringBuilder();
			Token t = node.getFirstToken();
			while (t != null) {
				if (t.next != null)
					sb.append(t.image);
				t = t.next;
			}
			return sb.toString();
		}
		
		public static ValList getEssentials(Node parent) {
			ValList list = vlist();
			for (int i = 0; i < parent.jjtGetNumChildren(); i++) {
				Node child = parent.jjtGetChild(i);
				if (isEssential.apply(child))
					list.add(tokens(child));
			}
			return list;
		}
		
		public static void putEssentials(Node parent, ValList list) {
			for (int i = 0, j = 0; i < parent.jjtGetNumChildren() && j < list.size(); i++) {
				Node child = parent.jjtGetChild(i);
				if (isEssential.apply(child)) {
					String value = list.get(j).toString();
					update(child, value);
					j++;
				}
			}
		}
		
		public static int[] getBeginToken(Token t) {
			return ints(t.beginLine, t.beginColumn);
		}
		public static int[] getEndToken(Token t) {
			return ints(t.endLine, t.endColumn);
		}
		public static void setBeginToken(Token t, int...lc) {
			t.beginLine = lc[0]; t.beginColumn = lc[1];
		}
		public static void setEndToken(Token t, int...lc) {
			t.endLine = lc[0]; t.endColumn = lc[1];
		}
		
		public static void changeToken(Token t, String image, int...begin) {
			setBeginToken(t, begin);
			MatchResult[] m = findAllIn(image, LINE_BREAK_PATTERN);
			int lines = m.length;
			int len = image.length();
			setEndToken(t, 
					begin[0] + lines,
					lines < 1 ?
							begin[1] + len - 1 : 
							len - m[m.length - 1].end());
			t.image = image;
			t.specialToken = null;
		}
		
		private static int[] diffLC(int[] minuend, int[] subtrahend) {
			return ints(
				minuend[0] - subtrahend[0], 
				minuend[1] - subtrahend[1], 
				minuend[0]);
		}
		private static void shiftLC(Token t, int...diff) {
			int line = diff[2];
			if (t.beginLine == line)
				t.beginColumn -= diff[1];
			if (t.endLine == line)
				t.endColumn -= diff[1];
			t.beginLine -= diff[0];
			t.endLine -= diff[0];
		}

		public static String formatLC(int...lc) {
			return "L" + lc[0] + "C" + lc[1];
		}

		public static int compareLC(Token t, int...lc) {
			int cmp;
			switch (cmp = lc[0] - t.beginLine) {
			case 0:
				cmp = lc[1] - t.beginColumn;
				return cmp < 0 ? -1 : 1;

			default:
				return cmp < 0 ? -1 : 1;
			}
		}

		public static int[] beginLC(Node node) {
			return getBeginToken(node.getFirstToken());
		}
		public static int[] endLC(Node node) {
			return getEndToken(node.getLastToken());
		}

		public static void update(Node node, Object value) {
			Token first = node.getFirstToken();
			int[] beginFirst = getBeginToken(first);
			Token last = node.getLastToken();
			int[] endLast = getEndToken(last);
			
			changeToken(first, value.toString(), beginFirst);
			int[] endFirst = getEndToken(first);
			int[] diff = diffLC(endLast, endFirst);
			
			boolean erase = last != first;
			if (erase || diff[0] != 0 || diff[1] != 0) {
				int[] beginNext = endFirst;
				beginNext[1] += 1;
				for (Token t = first.next; t != null; t = t.next) {
					if (erase) 
						changeToken(t, "", beginNext);
					else 
						shiftLC(t, diff);
					
					if (t == last)
						erase = false;
				}
			}
		}
    	
    	private static class ThrowableNode extends RuntimeException
    	{
    		public Node node;
    		
    		public ThrowableNode(Node node) {
    			this.node = node;
    		}
    	}
    	
    	public static Node find(SimpleNode document, int[] startLC, final String regex) {
    		try {
    			Object[] params = new Object[] {startLC, Pattern.compile(regex)};
				walk(document, new Function<Object>() {
					public Object apply(Object...params) {
						Node node = param(null, 0, params);
						Object[] data = param(null, 2, params);
						int[] startLC = (int[]) data[0];
						if (!isProcessNode(node) && compareLC(node.getFirstToken(), startLC) < 0) {
							Pattern pattern = (Pattern) data[1];
							String tokens = tokens(node);
							if (pattern.matcher(tokens).find())
								throw new ThrowableNode(node);
						}
						return data;
					}
				}, params);
				return null;
			} catch (ThrowableNode found) {
				return found.node;
			}
    	}
    	
    	public static Object walk(SimpleNode document, Function<Object> func, Object...params) {
    		lostAndFound.clear();
            Visitor visitor = new Visitor(func);
            return document.jjtAccept(visitor, params);
    	}
    	
    	Function<Object> func = null;
    	
        public Visitor(Function<Object> func) {
    		super();
    		this.func = func;
    	}

        private static int indent = 0;

		private Object selectNode(Node node, Object data) {
			if (func != null) 
				data = func.apply(node, indent, data);
	        ++indent;
	        data = node.childrenAccept(this, data);
	        --indent;
			return data;
		}

		@Override public Object visit(SimpleNode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTprocess node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTExpression node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTAssignment node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTOrNode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTAndNode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTEQNode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTNENode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTLTNode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTGTNode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTLENode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTGENode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTAddNode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTSubtractNode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTMulNode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTDivNode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTModNode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTNotNode node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTIntegerLiteral node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTFloatingPointLiteral node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTStringLiteral node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTIdentifier node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTMethod node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTReference node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTTrue node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTFalse node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTBlock node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTText node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTIfStatement node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTElseStatement node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTElseIfStatement node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTComment node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTObjectArray node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTWord node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTSetDirective node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTDirective node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTEscapedDirective node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTEscape node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTMap node, Object data) { return selectNode(node, data); }
		@Override public Object visit(ASTIntegerRange node, Object data) { return selectNode(node, data); }
		
		public static boolean checkLC(Token t, int[] lc, boolean begin) {
			int[] tlc = begin ?
				getBeginToken(t) : getEndToken(t);
			if (tlc[0] == lc[0] && tlc[1] == lc[1])
				return true;
			Log.d(TAG, String.format("%s : %sLC expected %s, but was %s", 
					t.image, 
					begin ? "begin" : "end", 
					Arrays.toString(lc), 
					Arrays.toString(tlc)));
			return false;
	    }
    }
    
    public static boolean checkTokenConsistency(SimpleNode document) {
    	boolean retval = true;
    	
    	int[] lc = ints(1,1);
    	
    	Token first = document.getFirstToken();
    	Token last = document.getLastToken();
    	for (Token t = first; t != null; t = t.next) {
    		if (t == last)
    			break;
    		
    		String image = t.image;
    		
    		retval &= Visitor.checkLC(t, lc, true);
    		t.beginLine = lc[0];
    		t.beginColumn = lc[1];
    		
    		int index;
    		while ((index = image.substring(0, image.length() - 1).indexOf(NEWLINE)) > -1) {
    			lc[0]++;
    			lc[1] = 1;
    			image = image.substring(index + 1);
    		} 
    		int length = image.length();
			lc[1] = length == t.image.length() ? 
					lc[1] + length - 1 :
					length;
    		
    		retval &= Visitor.checkLC(t, lc, false);
    		t.endLine = lc[0];
    		t.endColumn = lc[1];
    		
    		if (image.endsWith(NEWLINE)) {
    			lc[0]++;
    			lc[1] = 1;
    		}
    		else
    			lc[1]++;
    	}
    	
    	return retval;
    }
	
	public static SimpleNode parse(Reader reader, String templateName) {
		try {
			problem = null;
			SimpleNode doc = RuntimeSingleton.parse(reader, templateName);
			if (!checkTokenConsistency(doc)) {
				Log.d(TAG, templateName + " : token inconsistencies encountered");
			}
			return doc;
		} catch (ParseException e) {
			problem = e;
			return null;
		}
	}
	
	private static ParseException problem = null;
	
	public static boolean problem() {
		return problem != null;
	}
	
	public static String getMessage() {
		return problem.getMessage();
	}
	
	public static int[] getProblemCoordinates() {
		if (problem instanceof ExtendedParseException) {
			ExtendedParseException epe = (ExtendedParseException) problem;
			return ints(epe.getLineNumber(), epe.getColumnNumber());
		}
		else {
			Token tok = problem.currentToken;
			return ints(tok.endLine, tok.endColumn);
		}
	}
	
	public static int[] getTextOffsets(String text, int[] lineColumns) {
		MatchResult[] mr = findAllIn(text, LINE_BREAK_PATTERN);
		int[] offsets = new int[lineColumns.length / 2];
		for (int i = 0; i < lineColumns.length - 1; i+=2) {
			int line = lineColumns[i];
			int column = lineColumns[i+1];
			int offset = column - 1;
			if (line > 1) {
				if (line - 1 <= mr.length)
					offset += mr[line - 2].end();
			}
			if (i > 0)
				offset++;
			offsets[i/2] = offset;
		}
		return offsets;
	}
	
	private static Pattern ARGUMENT_EXTRACTOR = Pattern.compile("arg \\#(\\d+)");
	
	public static Integer getProblemArgumentInfo() {
		if (problem instanceof MacroParseException) {
			MatchResult m = findFirstIn(problem.getMessage(), ARGUMENT_EXTRACTOR);
			if (m != null)
				return Integer.parseInt(m.group(1));
		}
		return null;
	}
	
	private static Pattern MACRO_EXTRACTOR = Pattern.compile("\\#([^\\d\\s]+)");
	
	public static String getProblemMacroInfo() {
		if (problem instanceof MacroParseException) {
			MatchResult m = findFirstIn(problem.getMessage(), MACRO_EXTRACTOR);
			if (m != null)
				return m.group(1);
		}
		return "";
	}
	
	public static ArrayList<String> getSuggestionsFromProblem() {
		ArrayList<String> list = alist();
		if (problem != null) {
			int[][] expTokSeq = problem.expectedTokenSequences;
		    if (expTokSeq != null) {
				for (int i = 0; i < expTokSeq.length; i++) {
					String expected = "";
					for (int j = 0; j < expTokSeq[i].length; j++) {
						expected += problem.tokenImage[expTokSeq[i][j]] + " ";
					}
					if (expTokSeq[i][expTokSeq[i].length - 1] != 0) {
						expected += UNKNOWN;
					}
					list.add(expected);
				}
			}
		}
		return list;
	}
	
	public static String evaluate(Context context, String template, String logTag) {
		StringWriter w = new StringWriter();
		Velocity.evaluate( context, w, logTag, template );
		return w.toString();
	}
	
	public static String merge(Context context, Template template) {
		StringWriter w = new StringWriter();
		template.merge( context, w );
		return w.toString();
	}

	public static class CustomContext extends AbstractContext
	{
    	protected static final String TAG = CustomContext.class.getSimpleName();
    	protected static final org.apache.velocity.runtime.log.Log log = 
    			new org.apache.velocity.runtime.log.Log();
        
		protected ValMap map;
		
		public CustomContext(ValMap map) {
			this.map = map;
		}
	
		@Override
		public Object internalGet(String key) {
			return map == null ? null : map.get(key);
		}
	
		@Override
		public Object internalPut(String key, Object value) {
			return map == null ? null : map.put(key, value);
		}
	
		@Override
		public boolean internalContainsKey(Object key) {
			return map == null ? false : map.containsKey(key);
		}
	
		@Override
		public Object[] internalGetKeys() {
			return map == null ? new Object[0] : map.keySet().toArray();
		}
	
		@Override
		public Object internalRemove(Object key) {
			return map == null ? null : map.remove(key);
		}
	}

	@SuppressWarnings("rawtypes")
	public static String[] arrayOfStrings(Object value) {
		Object[] values;
		if (value == null) 
			return strings();
		else if (value instanceof String[]) 
			return (String[]) value;
		else if (value instanceof ArrayList) 
			values = ((ArrayList) value).toArray();
		else
			values = new Object[]{value};
		
		for (int i = 0; i < values.length; i++) 
			if (!(values[i] instanceof CharSequence)) {
				String string = String.valueOf(values[i]);
				values[i] = "null".equals(string) ? null : string;
			}
		
		return toStrings(values);
	}

	public static final Pattern VRI_PATTERN = Pattern.compile("\\" + VRI);
	public static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z]+[a-zA-Z0-9_\\-]*");
	public static final Pattern IDENTIFIER_PATTERN2 = 
			Pattern.compile(IDENTIFIER_PATTERN + "(\\." + IDENTIFIER_PATTERN + ")?");
	public static final Pattern REFERENCE_PATTERN = 
			Pattern.compile("\\" + VRI + "\\!?\\{?(" + IDENTIFIER_PATTERN + ")\\}?");
	public static final Pattern REFERENCE_PATTERN2 = 
			Pattern.compile("\\" + VRI + "\\!?\\{?(" + IDENTIFIER_PATTERN2 + ")\\}?");
	public static final Pattern PARENS_TERM = Pattern.compile("([^\\)]+)?");
	public static final Pattern METHOD_PATTERN = 
			Pattern.compile("\\" + VRI + "\\!?\\{?(" + IDENTIFIER_PATTERN + DOT_REGEX + IDENTIFIER_PATTERN + ")" +
					"\\(\\s*" + PARENS_TERM + "\\s*\\)\\}?");
	
	public static List<String> referencesIn(String template) {
		ArrayList<String> list = alist();
		for (MatchResult m : findAllIn(template, REFERENCE_PATTERN2))
			list.add(m.group());
		return list;
	}
	
	public static boolean isName(String string) {
		return string.matches("^" + IDENTIFIER_PATTERN + "$");
	}
	
	public static boolean isReference(String string) {
		return string.matches("^" + REFERENCE_PATTERN2 + "$");
	}
	
	public static boolean isMethodCall(String string) {
		return string.matches("^" + METHOD_PATTERN + "$");
	}
	
	public static boolean isDirective(String string) {
		return string.matches("^" + DIRECTIVE_PATTERN + "$");
	}
	
	public static final Pattern VDI_PATTERN = Pattern.compile("\\" + VDI);
	public static final String UNKNOWN = "...";
	public static final Pattern UNKNOWN_PATTERN = Pattern.compile("[\\.]{3}");
	public static final Pattern CONTENT_PATTERN = Pattern.compile(".+");
	public static final String END = "end";
	public static final Pattern END_PATTERN = Pattern.compile(VDI_PATTERN + enclose("\\{?", END, "\\}?"));
	public static final String BLOCK_END = UNKNOWN + VDI + enclose(Util1.BRACES[0], END, Util1.BRACES[1]);
	public static final Pattern DIRECTIVE_PATTERN = 
			Pattern.compile(VDI_PATTERN + "\\!?\\{?(" + IDENTIFIER_PATTERN + ")\\}?" +
					"\\(\\s*" + PARENS_TERM + "\\s*\\)");
	
	public static Pattern directivePattern(String name) {
		return Pattern.compile(VDI_PATTERN + "\\!?\\{?(" + name + ")\\}?");
	}
	
	public static final String[] UNARY_OPERATORS = strings("!","not","");
	public static final String[] ASSIGN_OPERATORS = 
			strings("=","in");
	public static final String[] COMPARE_OPERATORS = 
			strings("==","!=","<",">","<=",">=","eq","ne","lt","gt","le","ge");
	public static final String[] ARITHMETIC_OPERATORS = 
			strings("+","-","*","/","%");
	public static ArrayList<Pattern> termPatterns = alist();
	static {
		for (String op : ASSIGN_OPERATORS) {
			termPatterns.add(Pattern.compile("^(\\S+)\\s*" + op + "\\s*(\\S+)$"));
		}
		for (String op : COMPARE_OPERATORS) {
			termPatterns.add(Pattern.compile("^(\\S+)\\s*" + op + "\\s*(\\S+)$"));
		}
		for (String op : UNARY_OPERATORS) {
			termPatterns.add(Pattern.compile("^" + op + "\\s*(\\S+)$"));
		}
		for (String op : ARITHMETIC_OPERATORS) {
			termPatterns.add(Pattern.compile("^(\\S+)\\s*\\" + op + "\\s*(\\S+)$"));
		}
	}
	public static final int CONDITIONAL_OPERATORS_INDEX = 2;
	public static final int ARITHMETIC_OPERATORS_INDEX = 17;
	
	public static String[] dissolve(String term, int startAt, int stopAt) {
		for (int i = startAt; i < termPatterns.size(); i++) {
			if (stopAt > -1 && i >= stopAt)
				break;
			MatchResult m = findFirstIn(term, termPatterns.get(i));
			if (m != null) {
				if (m.groupCount() < 2)
					return strings(m.group(1));
				else
					return strings(m.group(1), m.group(2));
			}
		}
		return strings();
	}
	
	public static final String[] DATA_TYPES = {"R", "S", "I", "F", "B", "L", "M", "N", "U", "X", "Y"};
	public static final int IDENTIFIER_TYPE_INDEX = 7;
	public static final int URI_TYPE_INDEX = 8;
	public static final int EXPR_TYPE_INDEX = 9;
	public static final String ANY = DATA_TYPES[DATA_TYPES.length - 1];

	public static boolean isType(int index, String type) {
		return DATA_TYPES[index].compareToIgnoreCase(type) == 0;
	}
	
	private static final ValMap DUMMIES = vmap();
	static {
		for (int i = 0; i < DATA_TYPES.length; i++) {
			Object dummy;
			switch (i) {
			case 0:		dummy = new Object[]{VRI + UNKNOWN, VRI + enclose("{", UNKNOWN, "}")}; 	break;
			case 1:		dummy = new Object[]{enclose("'", UNKNOWN), enclose("\"", UNKNOWN)}; 	break;
			case 2:		dummy = new Object[]{"###"}; 	break;
			case 3:		dummy = new Object[]{"-###.##"}; 	break;
			case 4:		dummy = new Object[]{"false", "true"}; 	break;
			case 5:		dummy = new Object[]{enclose("[", UNKNOWN, "]")}; 	break;
			case 6:		dummy = new Object[]{enclose("{", UNKNOWN, "}")}; 	break;
			case 7:		dummy = new Object[]{"xxx"}; 	break;
			case 8:		dummy = new Object[]{"uri"}; 	break;
			case 9:		dummy = new Object[]{UNKNOWN}; 	break;
			default:	dummy = new Object[0]; 	break;
			}
			DUMMIES.put(DATA_TYPES[i], dummy);
		}
	}

	public static Object[] getDummies(String type) {
		return (Object[])DUMMIES.get(type.toUpperCase(Locale.getDefault()));
	}

	public static Object[] getDummies(int typeIndex) {
		return (Object[])DUMMIES.get(DATA_TYPES[typeIndex]);
	}

	public static boolean isDummy(int typeIndex, String text) {
		return asList(getDummies(typeIndex)).contains(text);
	}
	
	public static Boolean compliesWith(int typeIndex, String text) {
		if (isDummy(typeIndex, text))
			return true;
		
		switch (typeIndex) {
		case 0:
			return isReference(text);
		case 1:
			return (text.startsWith("'") && text.endsWith("'")) || (text.startsWith("\"") && text.endsWith("\""));
		case 2:
			return toInt(Integer.MIN_VALUE, text) != Integer.MIN_VALUE;
		case 3:
			return !toDouble(Double.NaN, text).isNaN();
		case 4:
			return toBool(null, text) != null;
		case 5:
			return text.startsWith("[") && text.endsWith("]");
		case 6:
			return text.startsWith("{") && text.endsWith("}");
		case 7:
			return isName(text);
		default:
			return null;
		}
	}
	
	public static final Pattern LINE_BREAK_PATTERN = Pattern.compile(NEWLINE_REGEX);
	public static final Pattern TAB_PATTERN = Pattern.compile(TAB_REGEX);
	public static final Pattern ARGUMENT_PATTERN = 
			Pattern.compile("((" + ARGUMENT_TYPER + ")?" + "(" + VRI_PATTERN + ")?" + 
					IDENTIFIER_PATTERN + ARGUMENT_TYPER + "(?i)[" + join("", DATA_TYPES) + "](?-i))|" +
							"(" + UNKNOWN_PATTERN + "(?=\\s*\\)))");
	
	public static String firstIdentifierFrom(String particle) {
		MatchResult mr = findFirstIn(particle, IDENTIFIER_PATTERN);
		if (mr != null)
			return mr.group();
		else
			return null;
	}
	
	public static String firstMethodFrom(String particle) {
		MatchResult mr = findFirstIn(particle, IDENTIFIER_PATTERN2);
		if (mr != null)
			return mr.group();
		else
			return null;
	}
	
	public static ValList argumentsFrom(String signature) {
		ValList args = vlist();
		for (MatchResult m : findAllIn(signature, ARGUMENT_PATTERN)) 
			args.add(m.group());
		return args;
	}
	
	public static String assemble(String signature, ValList list) {
		StringBuffer sb = new StringBuffer();
		Matcher matcher = ARGUMENT_PATTERN.matcher(signature);
		int sep = 0;
	    int index = 0;
	    while (matcher.find()) {
	    	String found = matcher.toMatchResult().group();
	    	boolean ellipsis = UNKNOWN.equals(found);
	    	String replacement = "";
	    	if (index < list.size()) {
				if (ellipsis) {
					do {
						if (replacement.length() > 0)
							replacement += ARGUMENT_SEPARATORS[sep];
						replacement += list.get(index).toString();
						index++;
					} while (index < list.size());
				}
				else
					replacement = list.get(index).toString();
				replacement = replacement.replaceAll("\\$", "\\\\\\$");
			}
	    	else if (ellipsis)
	    		replacement = ARGUMENT_SEPARATORS[sep];
	    	matcher.appendReplacement(sb, replacement);
			index++;
	    }
	    matcher.appendTail(sb);
	    String s = sb.toString();
	    s = s.replaceAll("(" + ARGUMENT_SEPARATORS[sep] + ")+(?=\\s*?\\))", "");
	    return s;
	}
	
    public static Map<String,String> signatures() {
    	Map<String,String> map = new LinkedHashMap<String,String>();
    	map.put("set", "#set( $reference_R = $expression_X )");
    	map.put("foreach", "#foreach( $reference_R in $list_L )" + BLOCK_END);
    	map.put("break", "#{break}");
    	map.put("if", "#if( $condition_X )" + BLOCK_END);
    	map.put("elseif", "#elseif( $condition_X )" + UNKNOWN);
    	map.put("else", "#{else}" + UNKNOWN);
    	map.put("stop", "#{stop}");
    	map.put("macro", "#macro( $name_N _$argument_R " + UNKNOWN + ")" + BLOCK_END);
    	map.put("include", "#include( $file_S " + UNKNOWN + ")");
    	map.put("parse", "#parse( $file_S )");
    	map.put("evaluate", "#evaluate( $expression_X )");
    	map.put("define", "#define( $reference_R )" + BLOCK_END);
    	map.put("line comment", "## " + UNKNOWN);
    	map.put("block comment", "#*" + UNKNOWN + "*#");
    	map.put("unparsed content", "#[[" + UNKNOWN + "]]#");
    	return map;
    }
	
	public static final String MATH_TOOL = VRI + "math";

    public static Map<String,String> math_tool() {
    	Map<String,String> map = new LinkedHashMap<String,String>();
    	map.put("random", "$math.random( [$lower_F], [$upper_F] )");
    	map.put("round", "$math.roundTo( $decimals_I, $number_F )");
    	map.put("roundToInt", "$math.roundToInt( $number_F )");
    	map.put("add", "$math.add( $summand1_F, $summand2_F )");
    	map.put("sub", "$math.sub( $minuend_F, $subtrahend_F )");
    	map.put("mul", "$math.mul( $factor1_F, $factor2_F )");
    	map.put("div", "$math.div( $dividend_F, $divisor_F )");
    	map.put("pow", "$math.pow( $basis_F, $exponent_F )");
    	map.put("min", "$math.min( $number1_F, $number2_F )");
    	map.put("max", "$math.max( $number1_F, $number2_F )");
    	return map;
    }

	public static final String NODE = "node";
	public static final String TOKEN = "token";
	public static final int DELETE = 0;
	public static final int INSERT = 1;
	public static final int MODIFY = 2;
	
	public static ValMap nodeMap(Object node, int indents, Object token) {
		ValMap map = vmap();
		map.put(NODE, node);
		map.put(INDENTS, indents);
		if (token != null)
			map.put(TOKEN, token);
		return map;
	}
	
	public static boolean hasEndToken(ValMap map) {
		return map.containsKey(TOKEN) && Visitor.isEndToken((Token)map.get(TOKEN));
	}
	
	public static boolean isPossible(int actionCode, ValMap map) {
		int group = Visitor.nodeGroup(map.get(NODE));
		boolean endToken = hasEndToken(map);
		switch (actionCode) {
		case DELETE:
			return group == DIRECTIVE && !endToken;
		case INSERT:
			return group == DIRECTIVE;
		case MODIFY:
			return (group == DIRECTIVE && !endToken) || group == REFERENCE || group == EXPRESSION;
		default:
			return false;
		}
	}
	
	public static final String INDENTS = "indents";
	public static final String BLOCKS = "blocks";
	
	public static Function<String> indentor = new Function<String>() {
		public String apply(Object... params) {
			int indents = param(0, 0, params);
			int blocks = param(0, 1, params);
			char indentChar = param(' ', 2, params);
			char blockChar = param(' ', 3, params);
			int indentLength = param(2, 4, params);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < indents * indentLength; i++)
				if (blocks > i) 
					sb.append(blockChar);
				else
					sb.append(indentChar);
			return sb.toString();
		}
	};
}
