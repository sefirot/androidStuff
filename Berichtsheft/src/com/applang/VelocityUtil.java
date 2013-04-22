package com.applang;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.AbstractContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.ASTBlock;
import org.apache.velocity.runtime.parser.node.Node;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import static com.applang.Util.*;
 
public class VelocityUtil
{
	public static String BLOCK_SYMBOL = "\n...\n";
   /**
     * A base class for custom directives.
     */
    public static abstract class CustomDirective extends Directive
    {
    	protected String[] arguments = null;
    	
    	public String signature(boolean full) {
    		boolean block = BLOCK == getType();
			return VELOCITY_DIRECTIVE_IDENTIFICATOR + getName() + 
    				(full ? "(" + join(" ", arguments) + ")" : "()") + 
    				(block ? BLOCK_SYMBOL + VELOCITY_DIRECTIVE_END : "");
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
	
	public static String evaluation(Context context, String template, String logTag) {
		StringWriter w = new StringWriter();
		Velocity.evaluate( context, w, logTag, template );
		return w.toString();
	}
	
	public static String merge(Context context, Template template) {
		StringWriter w = new StringWriter();
		template.merge( context, w );
		return w.toString();
	}

	public static class MapContext extends AbstractContext
	{
		protected ValMap map;
		
		public MapContext(ValMap map) {
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

	@SuppressWarnings("unchecked")
	public static Object walkJSON(Object[] path, Object json, Function<Object> filter, Object...params) throws Exception {
		Object object = json;
		
		if (path == null)
			path = new Object[0];
		
		if (json instanceof JSONObject) {
			JSONObject jo = (JSONObject) json;
			Util.ValMap map = new Util.ValMap();
			Iterator<String> it = jo.keys();
			while (it.hasNext()) {
				String key = it.next();
				Object value = jo.get(key);
				Object[] path2 = Util.arrayappend(path, key);
				if (value.toString().startsWith("[")) 
					value = walkJSON(path2, jo.getJSONArray(key), filter, params);
				else if (value.toString().startsWith("{"))
					value = walkJSON(path2, jo.getJSONObject(key), filter, params);
				else
					value = walkJSON(path2, value, filter, params);
				map.put(key, value);
			}
			object = map;
		}
		else if (json instanceof JSONArray) {
			JSONArray ja = (JSONArray) json;
			Util.ValList list = new Util.ValList();
			for (int i = 0; i < ja.length(); i++) 
				list.add(walkJSON(Util.arrayappend(path, i), ja.get(i), filter, params));
			object = list;
		}
		else if (filter != null)
			object = filter.apply(path, json, params);
		
		return object;
	}

	public static Object member(Object[] path, Object object) {
		for (int i = 0; i < path.length; i++) {
			if (path[i] instanceof Integer) 
				object = ((Util.ValList)object).get((Integer) path[i]);
			else
				object = ((Util.ValMap)object).get(path[i].toString());
		}
		return object;
	}

	@SuppressWarnings("rawtypes")
	public static void toJSON(JSONStringer stringer, String string, Object object, Function<Object> filter, Object...params) throws Exception {
		if (Util.notNullOrEmpty(string))
			stringer.key(string);
		
		if (object instanceof Map) {
			stringer.object();
			Map map = (Map) object;
			for (Object key : map.keySet()) 
				toJSON(stringer, key.toString(), map.get(key), filter, params);
			stringer.endObject();
		}
		else if (object instanceof Collection) {
			stringer.array();
			Iterator it = ((Collection) object).iterator();
			while (it.hasNext()) 
				toJSON(stringer, "", it.next(), filter, params);
			stringer.endArray();
		}
		else {
			if (filter != null)
				object = filter.apply(object, params);
			
			if (object != null) 
				stringer.value(object);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static String[] arrayOfStrings(Object value) {
		Object[] values;
		if (value == null) 
			return new String[0];
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
		
		return arraycast(values, new String[0]);
	}

	public static final Character VELOCITY_REFERENCE_IDENTIFICATOR = '$';
	public static final Pattern VELOCITY_REFERENCE_PATTERN = 
			Pattern.compile("\\" + VELOCITY_REFERENCE_IDENTIFICATOR + "\\!?\\{?([a-zA-Z]+[a-zA-Z0-9_\\-]*)\\}?");
	
	public static List<String> referencesIn(String template) {
		ArrayList<String> list = new ArrayList<String>();
		for (MatchResult m : findAllIn(template, VELOCITY_REFERENCE_PATTERN))
			list.add(m.group());
		return list;
	}
    
	public static final Character VELOCITY_DIRECTIVE_IDENTIFICATOR = '#';
	public static final String VELOCITY_DIRECTIVE_END = VELOCITY_DIRECTIVE_IDENTIFICATOR + "end";
	
    public static Map<String,String> directives() {
    	LinkedHashMap<String,String> map = new LinkedHashMap<String,String>();
    	map.put("set", VELOCITY_DIRECTIVE_IDENTIFICATOR + "set( $ref = arg )");
    	map.put("foreach", enclose(VELOCITY_DIRECTIVE_IDENTIFICATOR + "foreach( $ref in arg )", BLOCK_SYMBOL, VELOCITY_DIRECTIVE_END));
    	map.put("break", VELOCITY_DIRECTIVE_IDENTIFICATOR + "break");
    	map.put("if", enclose(VELOCITY_DIRECTIVE_IDENTIFICATOR + "if( [condition] )", BLOCK_SYMBOL, VELOCITY_DIRECTIVE_END));
    	map.put("elseif", enclose(VELOCITY_DIRECTIVE_IDENTIFICATOR + "elseif( [condition] )", BLOCK_SYMBOL, ""));
    	map.put("else", enclose(VELOCITY_DIRECTIVE_IDENTIFICATOR + "else", BLOCK_SYMBOL, ""));
    	map.put("stop", VELOCITY_DIRECTIVE_IDENTIFICATOR + "stop");
    	map.put("macro", VELOCITY_DIRECTIVE_IDENTIFICATOR + "macro( vmname $arg1 [= def1] [ $arg2 [= def2] ... $argn [= defn] ] ) [ vmcode... ] " + VELOCITY_DIRECTIVE_END);
    	map.put("include", VELOCITY_DIRECTIVE_IDENTIFICATOR + "include( arg [arg2 ... argn] )");
    	map.put("parse", VELOCITY_DIRECTIVE_IDENTIFICATOR + "parse( arg )");
    	map.put("evaluate", VELOCITY_DIRECTIVE_IDENTIFICATOR + "evaluate( arg )");
    	map.put("define", enclose(VELOCITY_DIRECTIVE_IDENTIFICATOR + "define( $ref )", BLOCK_SYMBOL, VELOCITY_DIRECTIVE_END));
    	map.put("line comment", VELOCITY_DIRECTIVE_IDENTIFICATOR + VELOCITY_DIRECTIVE_IDENTIFICATOR + " ");
    	map.put("block comment", enclose(VELOCITY_DIRECTIVE_IDENTIFICATOR + "*", BLOCK_SYMBOL, "*" + VELOCITY_DIRECTIVE_IDENTIFICATOR));
    	map.put("content (unparsed)", enclose(VELOCITY_DIRECTIVE_IDENTIFICATOR + "[[", BLOCK_SYMBOL, "]]" + VELOCITY_DIRECTIVE_IDENTIFICATOR));
    	return map;
    }
}
