package com.applang;

import static com.applang.Util.*;
import static com.applang.Util2.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.applang.Util.Job;

public class ZipUtil
{
	static String basePath(Object path) throws Exception {
		return param_File(new File(""), 0, path).getCanonicalPath();
	}
	
	static String entryPath(Object file, String base) throws Exception {
		return pathDivide(basePath(file), base);
	}
	 
	public static Object[] iterateFiles(boolean includeDirs, File dir, Job<Object> job, Object...params) throws Exception {
		params = reduceDepth(params);
		if (dir != null && dir.isDirectory()) {
			for (File file : dir.listFiles())
				if (file.isDirectory())
					iterateFiles(includeDirs, file, job, params);
				else if (file.isFile()) {
					job.perform(file, params);
					Integer n = param_Integer(null, 0, params);
					if (n != null)
						params[0] = n + 1;
				}
			if (includeDirs) {
				job.perform(dir, params);
				Integer n = param_Integer(null, 1, params);
				if (n != null)
					params[1] = n + 1;
			}
		}
		return params;
	} 

	public static class ZipJob implements Job<Object>
	{
		public ZipJob(Object base) throws Exception {
			this.base = basePath(base);
		}
		
		String base;
		
		@Override
		public void perform(Object o, Object[] parms) throws Exception {
			File file = param_File(null, 0, o);

			String path = o.toString();
			if (file != null) 
				path = entryPath(file, base);
			
			ZipOutputStream out = param(null, 1, parms);
			
			ZipEntry entry = new ZipEntry(path);
			entry.setTime(file.lastModified());
			out.putNextEntry(entry);
			
//			System.out.println(entry);
			
			if (file != null) {
				FileInputStream in = new FileInputStream(file);
				copyContents(in, out);
				in.close();
			}
			
			out.closeEntry();
		}
	};

	public static class UnzipJob implements Job<ZipEntry>
	{
		public UnzipJob(Object base) throws Exception {
			path = basePath(base);
		}
		
		String path;
		
		@Override
		public void perform(ZipEntry entry, Object[] parms) throws Exception {
			ZipInputStream in = param(null, 0, parms);
			if (in == null)
				return;
			
			File file = fileOf(path, entry.getName());
			file.getParentFile().mkdirs();
			file.createNewFile();
			
			OutputStream out = new FileOutputStream(file);
			copyContents(in, out);
			out.close();
		}
	}

	public static int zipArchive(Object archive, Object base, Object... params) {
		int cnt = 0;
		
		try {
			ZipOutputStream out;
			if (archive instanceof File) 
				out = new ZipOutputStream(new FileOutputStream((File)archive));
			else if (archive instanceof ZipOutputStream)
				out = (ZipOutputStream)archive;
			else
				throw new Exception("invalid archive");
			
			ZipJob append = new ZipJob(base);
			
			for (Object o : params) {
				File file = param_File(null, 0, o);
				if (file == null || !file.exists())
					continue;
				else if (file.isDirectory())
					cnt += (Integer)iterateFiles(false, file, append, 0, out)[0];
				else { 
					append.perform(file, new Object[] {0, out});
					cnt++;
				}
			}
			
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return cnt;
	}
	
	public static int unzipArchive(File archive, Job<ZipEntry> extract, boolean exclude, String... names) {
		List<String> filter = asList(names);
		
		int cnt = 0;
		
	    try {
	    	FileInputStream stream = new FileInputStream(archive);
			ZipInputStream in = new ZipInputStream(stream);
			
			ZipEntry entry = null;
			while ((entry = in.getNextEntry()) != null) {
		    	String name = entry.getName();
		    	if (filter.size() < 1 || 
		    		(exclude ? 
		    			!filter.contains(name) : 
		    			filter.contains(name)))
		    	{
		    		extract.perform(entry, new Object[] {in});
		    		cnt++;
		    	}
		    	
		    	in.closeEntry();
		    }
		    
		    in.close();
		    stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return cnt;
	}

	public static int updateArchive(File archive, Object base, int nDelete, Object... params) {
		if (nDelete > params.length || nDelete < 0)
			return -1;
		
		int cnt = 0;
		
		try {
			String path = basePath(base);
			ArrayList<String> filter = alist();
			for (int i = 0; i < nDelete; i++) 
				filter.add(entryPath(params[i], path));
			
			File temp = File.createTempFile("zip", ".zip", new File(tempPath()));
			final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(temp));
			
			cnt = unzipArchive(archive, new Job<ZipEntry>() {
				public void perform(ZipEntry zipEntry, Object[] parms) throws Exception {
					ZipInputStream in = param(null, 0, parms);
					out.putNextEntry(zipEntry);
			        if (!zipEntry.isDirectory())
			        	copyContents(in, out);
				}
			}, true, toStrings(filter));
			
			cnt += zipArchive(out, base, 
					arrayslice(params, nDelete, params.length - nDelete));

			if (archive.delete())
				temp.renameTo(archive);
			else
				throw new Exception(String.format("zip-file '%s' not updated", archive.getPath()));
		} catch (Exception e) {
			e.printStackTrace();
			return -2;
		}
		
		return cnt;
	}

}
