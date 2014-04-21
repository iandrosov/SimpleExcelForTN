package SimpleExcelForTN.server;

// -----( IS Java Code Template v1.2
// -----( CREATED: 2005-03-29 17:22:32 JST
// -----( ON-HOST: xiandros-c640

import com.wm.data.*;
import com.wm.util.Values;
import com.wm.app.b2b.server.Service;
import com.wm.app.b2b.server.ServiceException;
// --- <<IS-START-IMPORTS>> ---
import com.wm.excel.net.ContentHandlerFactory_ExcelFile;
import java.util.*;
import com.wm.app.b2b.server.*;
import com.wm.data.*;
import java.io.*;
// --- <<IS-END-IMPORTS>> ---

public final class msexcel

{
	// ---( internal utility methods )---

	final static msexcel _instance = new msexcel();

	static msexcel _newInstance() { return new msexcel(); }

	static msexcel _cast(Object o) { return (msexcel)o; }

	// ---( server methods )---




	public static final void init (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(init)>> ---
		// @subtype unknown
		// @sigtype java 3.5
        try
        {
	    // Register content handler	
            ServerAPI.registerContentHandler("application/vnd.ms-excel", new ContentHandlerFactory_ExcelFile());
 
            // Register Excel file Category for TN
	    IData idata1 = IDataFactory.create(1);
	    ValuesEmulator.put(idata1, "displayName", "Excel file");
	    ValuesEmulator.put(idata1, "className", "com.wm.app.tn.doc.ExcelDocType");
	    Service.doInvoke("wm.tn.doctype", "registerCategory", idata1);

            //File f = new File(Server.getResources().getPackageConfigDir("SimpleExcelForTN"), "excel.cnf");
            //Config.init(f);

	    // Update WmTN properties to add content type
	    //"tn.excel.contenttypes=application/vnd.ms-excel"
	    String pkg = "WmTN";	
	    File fl = ServerAPI.getPackageConfigDir(pkg);
	    if (fl != null)
	    {
	    	String config_dir = fl.getPath();

		// Set WmTN Properties
		String prop_file = config_dir + File.separator + "properties.cnf";
		
		InputStream in_stream = (InputStream) new FileInputStream(prop_file);
		if (in_stream != null)
		{
		    Properties config = new Properties();
		    config.load(in_stream);
		    config.setProperty("tn.excel.contenttypes", "application/vnd.ms-excel");
		    // Stor properties to file
		    OutputStream out = (OutputStream) new FileOutputStream(prop_file);
		    config.store(out, "WmTN property");
		    out.flush();
		    out.close();
		    in_stream.close();
		}
		// Set WmTN Default Properties
		String def_prop_file = config_dir + File.separator + "default_properties.cnf";	
		in_stream = (InputStream) new FileInputStream(def_prop_file);
		if (in_stream != null)
		{
		    Properties config = new Properties();
		    config.load(in_stream);
		    config.setProperty("tn.excel.contenttypes", "application/vnd.ms-excel");
		    // Stor properties to file
		    OutputStream out = (OutputStream) new FileOutputStream(def_prop_file);
		    config.store(out, "WmTN property");
		    out.flush();
		    out.close();
		    in_stream.close();
		}
	    }
	
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
		// --- <<IS-END>> ---

                
	}



	public static final void registerContentHandler (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(registerContentHandler)>> ---
		// @subtype unknown
		// @sigtype java 3.5
		// [i] field:1:required contentTypes
		String types[];
		     int i;
		        try
		        {
		            types = (String[])ValuesEmulator.get(pipeline, "contentTypes");
		            if(types == null || types.length == 0)
		                return;
		        }
		        catch(Throwable t)
		        {
		            throw new ServiceException(t);
		        }
		        for(i = 0; i < types.length; i++)
			{
		            if(!types[i].equals("$wm_default") && !types[i].equals("application/x-www-form-urlencoded") && !types[i].equals("text/xml") && !types[i].equals("application/x-wmrpc2") && !types[i].equals("application/x-x509v3-bin") && !types[i].equals("application/x-wmrpc") && !types[i].equals("application/x-wmrpc-bin") && !types[i].equals("application/x-wmidatabin") && !types[i].equals("application/wm-soap") && !types[i].equals("application/soap") && !types[i].equals("application/soap+xml"))
		            {
		                ServerAPI.registerContentHandler(types[i], new ContentHandlerFactory_ExcelFile(types[i]));
		                registeredContentTypes.addElement(types[i]);
		            }
			}
		// --- <<IS-END>> ---

                
	}



	public static final void setACLs (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(setACLs)>> ---
		// @subtype unknown
		// @sigtype java 3.5

        for(int i = 0; i < ADMIN_SERVICES.length; i++)
            ACLManager.setAclGroup(ADMIN_SERVICES[i], "Developers");

        for(int i = 0; i < DEVEL_SERVICES.length; i++)
            ACLManager.setAclGroup(DEVEL_SERVICES[i], "Developers");


		// --- <<IS-END>> ---

                
	}



	public static final void shutdown (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(shutdown)>> ---
		// @subtype unknown
		// @sigtype java 3.5
        try
        {
            ServerAPI.removeContentHandler("application/vnd.ms-excel");
            for(int i = 0; i < registeredContentTypes.size(); i++)
                ServerAPI.removeContentHandler((String)registeredContentTypes.elementAt(i));

            // UnRegister Excel file Category for TN
	    IData idata1 = IDataFactory.create(1);
	    ValuesEmulator.put(idata1, "displayName", "Excel file");
	    Service.doInvoke("wm.tn.doctype", "unregisterCategory", idata1);

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
		// --- <<IS-END>> ---

                
	}

	// --- <<IS-START-SHARED>> ---
	protected static Properties _props;
	protected static Vector registeredContentTypes = new Vector();
	
	public static final String ADMIN_SERVICES[] = {
	        "SimpleExcelForTN.server.msexcel:init", "SimpleExcelForTN.server.msexcel:registerContentHandler", "SimpleExcelForTN.server.msexcel:setACLs", 
	        "SimpleExcelForTN.server.msexcel:shutdown"
	    };
	public static final String DEVEL_SERVICES[] = {
	        "SimpleExcelForTN.util:MSExcelWorksheetToRecord"
	    };
	
	// --- <<IS-END-SHARED>> ---
}

