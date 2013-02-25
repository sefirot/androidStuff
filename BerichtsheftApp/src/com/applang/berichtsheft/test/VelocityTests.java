package com.applang.berichtsheft.test;

import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.AbstractContext;

import junit.framework.TestCase;

public class VelocityTests extends TestCase {

	protected static void setUpBeforeClass() throws Exception {
	}

	protected static void tearDownAfterClass() throws Exception {
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testContext() {
	    Properties p = new Properties();
	    p.setProperty(
	    		"file.resource.loader.path", 
	    		"/home/lotharla/work/Niklas/androidStuff/BerichtsheftApp/src/com/applang/berichtsheft/test");
	    Velocity.init( p );

		VelocityContext context = new VelocityContext();
		context.put("vormittag", "sonnig");
		context.put("nachmittag", "stark bewölkt");
		
		StringWriter w;
		
		w = new StringWriter();
		String s = "v.m. $vormittag, n.m. $nachmittag";
        Velocity.evaluate( context, w, "mystring", s );
        String s1 = w.toString();
        
		w = new StringWriter();
		Velocity.mergeTemplate("description.vm", "UTF-8", context, w );
        String s2 = w.toString();
        
        assertEquals(s1, s2);
	}
	
	public class WeatherContext extends AbstractContext
	{

		@Override
		public Object internalGet(String key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object internalPut(String key, Object value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean internalContainsKey(Object key) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Object[] internalGetKeys() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object internalRemove(Object key) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
