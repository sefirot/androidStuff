package com.applang;
 
import java.io.InputStream;
 
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
 
import android.content.res.Resources;
 
public class AndroidResourceLoader extends FileResourceLoader {
	private Resources resources;
	private String packageName;
 
	public void commonInit(RuntimeServices rs, ExtendedProperties configuration) {
		super.commonInit(rs,configuration);
		this.resources = (Resources)rs.getProperty("android.content.res.Resources");
		this.packageName = (String)rs.getProperty("packageName");
	}
 
	public long getLastModified(Resource resource) {
		return 0;
	}
 
	public InputStream getResourceStream(String templateName) {
		int id = resources.getIdentifier(templateName, "raw", this.packageName);
		return resources.openRawResource(id);
	}
 
	public boolean	isSourceModified(Resource resource) {
		return false;
	}
 
	public boolean	resourceExists(String templateName) {
		return resources.getIdentifier(templateName, "raw", this.packageName) != 0;
	}
}