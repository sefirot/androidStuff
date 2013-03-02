package com.applang;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBaseConnection
{
	public boolean open(String dbPath, Object... params) {
		boolean retval = false;
		ResultSet rs = null;
		try {
			close();
			
			String driver = Util.paramString("org.sqlite.JDBC", 2, params);
			Class.forName(driver);
			
			scheme = Util.paramString("sqlite", 1, params);
			boolean memoryDb = "sqlite".equals(scheme) && dbPath == null;
			
			String url = "jdbc:" + scheme + ":" + (memoryDb ? "" : dbPath);
			con = DriverManager.getConnection(url);
			stmt = con.createStatement();
			
			postConnect();
			
			String database = Util.paramString("sqlite_master", 3, params);
			if ("sqlite".equals(scheme))
				rs = stmt.executeQuery("select name from " + database + " where type = 'table'");
			else if ("mysql".equals(scheme)) {
				rs = stmt.executeQuery("show databases;");
				boolean exists = false;
			    while (rs.next()) 
			        if (rs.getString(1).equals(database)) {
			        	exists = true;
			        	break;
			        }
	        	rs.close();
	        	if (!exists)
	        		throw new Exception(String.format("database '%s' not found", database));
	        	else
	        		stmt.execute(String.format("use %s;", database));
	        	
				rs = stmt.executeQuery("show tables in " + database + ";");
			}
			
			String tableName = Util.paramString(null, 0, params);
			if (tableName == null)
				return true;
			
		    while (rs.next()) 
		        if (rs.getString(1).equals(tableName)) 
		        	return true;
		    
		    return false;
		} catch (Exception e) {
			SwingUtil.handleException(e);
			return retval;
		} 
		finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					SwingUtil.handleException(e);
					retval = false;
				}
			}
		}
	}
	
	public void postConnect() throws Exception {
	}

	public void close() {
		try {
			if (con != null && !con.isClosed())
				con.close();
		} catch (SQLException e) {
			SwingUtil.handleException(e);
		}
	}
	
	public String scheme = null;
	public Connection con = null;
	public Statement stmt = null;
}