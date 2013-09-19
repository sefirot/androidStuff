package com.applang;

import static com.applang.Util.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class ZipUtil
{
	static String basePath(Object path) throws Exception {
		return Util.paramFile(new File(""), 0, path).getCanonicalPath();
	}
	
	static String entryPath(Object file, String base) throws Exception {
		return Util2.pathDivide(basePath(file), base);
	}

	public static class ZipJob implements Util.Job<Object>
	{
		public ZipJob(Object base) throws Exception {
			this.base = basePath(base);
		}
		
		String base;
		
		@Override
		public void perform(Object o, Object[] parms) throws Exception {
			File file = Util.paramFile(null, 0, o);

			String path = o.toString();
			if (file != null) 
				path = entryPath(file, base);
			
			ZipOutputStream out = Util.param(null, 1, parms);
			
			ZipEntry entry = new ZipEntry(path);
			entry.setTime(file.lastModified());
			out.putNextEntry(entry);
			
//			System.out.println(entry);
			
			if (file != null) {
				FileInputStream in = new FileInputStream(file);
				Util.copyContents(in, out);
				in.close();
			}
			
			out.closeEntry();
		}
	};

	public static class UnzipJob implements Util.Job<ZipEntry>
	{
		public UnzipJob(Object base) throws Exception {
			path = basePath(base);
		}
		
		String path;
		
		@Override
		public void perform(ZipEntry entry, Object[] parms) throws Exception {
			ZipInputStream in = Util.param(null, 0, parms);
			if (in == null)
				return;
			
			File file = Util.fileOf(path, entry.getName());
			file.getParentFile().mkdirs();
			file.createNewFile();
			
			OutputStream out = new FileOutputStream(file);
			Util.copyContents(in, out);
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
				File file = Util.paramFile(null, 0, o);
				if (file == null || !file.exists())
					continue;
				else if (file.isDirectory())
					cnt += (Integer)Util.iterateFiles(false, file, append, 0, out)[0];
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
	
	public static int unzipArchive(File archive, Util.Job<ZipEntry> extract, boolean exclude, String... names) {
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
			ArrayList<String> filter = new ArrayList<String>();
			for (int i = 0; i < nDelete; i++) 
				filter.add(entryPath(params[i], path));
			
			File temp = File.createTempFile("zip", ".zip", new File(Util2.tempPath()));
			final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(temp));
			
			cnt = unzipArchive(archive, new Util.Job<ZipEntry>() {
				public void perform(ZipEntry zipEntry, Object[] parms) throws Exception {
					ZipInputStream in = Util.param(null, 0, parms);
					out.putNextEntry(zipEntry);
			        if (!zipEntry.isDirectory())
			        	Util.copyContents(in, out);
				}
			}, true, filter.toArray(new String[0]));
			
			cnt += zipArchive(out, base, 
					Util2.arrayreduce(params, nDelete, params.length - nDelete));

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
