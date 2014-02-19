package com.applang;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.ReferenceInsertionEventHandler;
import org.apache.velocity.runtime.parser.node.Node;
//import org.apache.velocity.tools.generic.MathTool;

import com.applang.Util.Function;
import com.applang.Util.Job;
import com.applang.Util.ValMap;
import com.applang.VelocityUtil.Visitor;
import com.applang.berichtsheft.R;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;

public class UserContext extends CustomContext implements Serializable
{
	private static final long serialVersionUID = 1704932233683596747L;
	
//	protected void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
//	}
//
//	protected void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
//	}

	public static String toPlainText(String script, Object...params) {
		UserContext context = param(new UserContext(), 0, params);
		return evaluate(context, script, TAG);
	}
	
	public UserContext() {
		this(vmap());
	}

	public UserContext(ValMap map) {
		super(map);
		if (map != null) {
	//		map.put(MATH_TOOL.substring(1), new MathTool());
		}
	}

	@Override
	public String toString() {
		return map.toString();
	}
	
	List<Object> allReferences() {
		return asList(getKeys());
	}
    
	public ValList suggestions() {
    	ValList list = vlist();
    	for (Object item : allReferences()) {
    		String listItem = VRI + item.toString();
    		list.add(listItem);
    	}
    	return list;
    }
    
	public Collection<Object> suggestions(Predicate<Object> isIncluded, boolean negate) {
    	return filter(suggestions(), negate, isIncluded);
    }

	public static final String[] USER_DIRECTIVE_CLASSES = {
		"com.applang.PromptDirective", 
  		"com.applang.SpinnerDirective",
  		"com.applang.CursorDirective",
  	};
    
    public static void setupVelocity(Activity activity, boolean force, Object... params) {
    	String logClassname = "com.applang.VelocityLogger";
		if (!force && logClassname.equals(Velocity.getProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS)))
    		return;
    	
  		if (activity != null && activity.getIntent() != null) {
  			String packageName = param(getPackageNameByClass(R.class), 0, params);
			Velocity.setProperty("packageName", packageName);
			Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS,
					logClassname);
			Velocity.setProperty(Velocity.RESOURCE_LOADER, "android");
			Velocity.setProperty("android.resource.loader.class",
					"com.applang.VelocityResourceLoader");
			Velocity.setProperty("android.content.res.Resources",
					activity.getResources());
		}
  		
		Velocity.setProperty("userdirective", join(",", USER_DIRECTIVE_CLASSES));
  		
  		Velocity.init();
  	};
    
    public static Map<String,String> directives(Object...params) {
    	Map<String,String> direct = signatures();
    	ValMap map = param(null, 0, params);
    	if (map != null) {
    		Object[] elses = new Object[] { "else", "elseif" };
			boolean endToken = hasEndToken(map);
			Node node = (Node) map.get(NODE);
			int partOfIF = Visitor.partOfIfStatement(node);
			String tokens = Visitor.tokens(node);
			switch (partOfIF) {
			case 1:
				if (endToken) {
					boolean hasElse = findFirstIn(tokens, directivePattern(elses[0] + "(?!if)")) != null;
					if (!hasElse)
						break;
				}
				for (Object key : elses)
					direct.remove(key);
				break;
			case 2:
			case 3:
				direct.remove(elses[0]);
				break;
			default:
				if (endToken) {
					String key = firstIdentifierFrom(tokens);
					if (!direct.containsKey(key))
						direct.clear();
				}
				else
					for (Object key : elses)
						direct.remove(key);
				break;
			}
		}
		for (String userClass : USER_DIRECTIVE_CLASSES) 
			try {
				Class<?> c = Class.forName(userClass);
				Object inst = c.newInstance();
				String name = (String) c.getMethod("getName").invoke(inst);
				Method m = c.getMethod("signature", Boolean.TYPE);
				String value = (String) m.invoke(inst, true);
				direct.put(name, value);
			} catch (Exception e) {
				log.error(TAG, e);
			}
    	return direct;
    }
    
    public Object buildTerm(String image) {
    	String identifier = firstIdentifierFrom(image);
    	if (notNullOrEmpty(identifier))
			try {
				String baseClass = "com.applang.BaseDirective";
				for (String userClass : arrayappend(USER_DIRECTIVE_CLASSES, baseClass)) {
					Class<?> c = Class.forName(userClass);
					Object inst = c.newInstance();
					Method method = c.getMethod("construct", String.class, UserContext.class);
					if (baseClass.equals(userClass)) {
						return method.invoke(inst, image, this);
					}
					else {
						String name = (String) c.getMethod("getName").invoke(inst);
						if (identifier.equals(name)) {
							Object o = c.getMethod("signature", Boolean.TYPE).invoke(inst, true);
							return method.invoke(inst, o, this);
						}
					}
				} 
			} catch (Exception e) {
				log.error(TAG, e);
			}
    	return image;
    }
	
	public static boolean isHidden(String key) {
		return isOptionalArgument(key);
	}
	
	public static String hidden(String key) {
		return optionalize(key);
	}

	public Node modify(Node node) {
		this.put(hidden(NODE), node);
		switch (Visitor.nodeGroup(node)) {
		case DIRECTIVE:
			String image = Visitor.tokens(node, VDI);
			String name = firstIdentifierFrom(image);
			String signature = directives().get(name);
			buildTerm(signature);
			break;

		default:
			break;
		}
		return node;
	}
	
    static ValMap nonNullMap(ValMap m) {
        return (m != null) ? m : vmap();
    }

	public UserContext(ValMap map, EvaluationTask evaluationTask) {
		this(nonNullMap(map));
		this.evaluationTask = evaluationTask;
	}
	
	transient private EvaluationTask evaluationTask;

	public EvaluationTask getEvaluationTask() {
		return evaluationTask;
	}
        
	public static class EvaluationTask extends Task<Object>
    {
		public EvaluationTask(Activity activity, ValMap refMap, 
				Handler progressHandler, Job<Void> progressJob, 
				Job<Object> followUp, Object... params)
		{
			super(activity, followUp, params);
			setupVelocity(activity, true);
    		this.progressHandler = progressHandler;
    		this.progressJob = progressJob;
			this.userContext = new UserContext(refMap, this);
			for (Object authority : contentAuthorities(providerPackages)) {
				String auth = String.valueOf(authority);
				String key = split(auth, DOT_REGEX).get(-1) + "Uri";
				userContext.put(key, contentUri(auth, null).toString());
			}
    	}

		private Handler progressHandler;
		private Job<Void> progressJob;
    	private UserContext userContext = null;
    	private List<String> refs = null;
    	private int size = 0;

    	@Override
		protected void onPreExecute() {
    		super.onPreExecute();
    		if (progressHandler != null) {
    		    EventCartridge ec = new EventCartridge();
    		    ec.addEventHandler(new ReferenceInsertionEventHandler() {
					@Override
					public Object referenceInsert(final String reference, Object data) {
						if (refs != null) {
							int pos = refs.indexOf(reference);
							if (pos > -1)
								refs.remove(pos);
							if (size > 0) {
								final float percent = 100 * Float.valueOf(size - refs.size()) / size;
								progressHandler.post(new Runnable() {
			                        public void run() {
			                        	try {
											progressJob.perform(null, new Object[]{(int)percent, reference});
										} catch (Exception e) {
											log.error(TAG, e);
										}
			                        }
			                    });
							}
						}
						return data;
					}
    		    });
    		    ec.attachToContext(userContext);
    		}
		}
    	
		@Override
		protected Object doInBackground(Object... params) {
    		if (isAvailable(0, params)) {
    			Object param = params[0];
    			if (param instanceof String) {
					String template = (String) param;
					refs = referencesIn(template);
					size = refs.size();
					try {
						return evaluate(userContext, template, TAG);
					} catch (Exception e) {
						publishProgress(
							new Intent(Dialogs.PROMPT_ACTION)
								.putExtra(BaseDirective.PROMPT, param_String("", 1, params))
								.putExtra(BaseDirective.VALUES, strings(e.getMessage()))
								.putExtra(BaseDirective.TYPE, Dialogs.DIALOG_TEXT_INFO));
					}
    			}
    			else if (param instanceof Function<?>) {
    				params[0] = userContext;
    				return ((Function<?>)param).apply(params);
    			}
			}
    		return null;
		}
    	
    	public void doInForeground(Intent intent) {
    		publishProgress(intent);
		}
	}
	
	public static void buildDirective(final String key, 
			Activity activity, ValMap refMap, 
			Job<Object> followUp, Object...params)
	{
		new EvaluationTask(activity, refMap, null, null, followUp, params)
			.execute(new Function<Object>() {
				public Object apply(Object... params) {
					UserContext userContext = (UserContext) params[0];
					return userContext.buildTerm(key);
				}
			});
	}
	
	public static void modifyNode(final Node node, 
			Activity activity, ValMap refMap, 
			Job<Object> followUp, Object...params) {
		new EvaluationTask(activity, refMap, null, null, followUp, params)
			.execute(new Function<Node>() {
				public Node apply(Object... params) {
					UserContext userContext = (UserContext) params[0];
					return userContext.modify(node);
				}
			});
	}
}
