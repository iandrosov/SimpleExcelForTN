package SimpleExcelForTN.tn.doc;

// -----( IS Java Code Template v1.2
// -----( CREATED: 2005-03-30 11:44:20 JST
// -----( ON-HOST: xiandros-c640

import com.wm.data.*;
import com.wm.util.Values;
import com.wm.app.b2b.server.Service;
import com.wm.app.b2b.server.ServiceException;
// --- <<IS-START-IMPORTS>> ---
import com.wm.app.b2b.server.*;
import com.wm.app.tn.doc.*;
import com.wm.app.tn.err.*;
import com.wm.app.tn.util.IDataUtilExtension;
import com.wm.data.*;
import java.util.StringTokenizer;
import com.wm.excel.parse.ParseError;
// --- <<IS-END-IMPORTS>> ---

public final class excel

{
	// ---( internal utility methods )---

	final static excel _instance = new excel();

	static excel _newInstance() { return new excel(); }

	static excel _cast(Object o) { return (excel)o; }

	// ---( server methods )---




	public static final void registerContentTypes (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(registerContentTypes)>> ---
		// @subtype unknown
		// @sigtype java 3.5

		try
		{
			String s = System.getProperty("tn.excel.contenttypes", "application/vnd.ms-excel");
			StringTokenizer stringtokenizer = new StringTokenizer(s, ",");
			String as[] = new String[stringtokenizer.countTokens()];
			int i = 0;
			StringBuffer stringbuffer = new StringBuffer();
			for(; stringtokenizer.hasMoreTokens(); stringbuffer.append(as[i++]))
			{
				as[i] = stringtokenizer.nextToken();
				if(i > 0)
					stringbuffer.append(",");
			}

			if(as == null || as.length == 0)
				return;
			IData idata1 = IDataFactory.create(1);
			ValuesEmulator.put(idata1, "contentTypes", as);
			try
			{
				Service.doInvoke("SimpleExcelForTN.server.msexcel", "registerContentHandler", idata1);
				SystemLog2.log(6, "Registred content type {0} for ContentHandler_ExcelFile", stringbuffer.toString());
			}
			catch(UnknownServiceException _ex)
			{
				SystemLog2.log(4, "Unable to register content types with the MS Excel File Content Handler.");
			}
		}
		catch(Throwable throwable)
		{
			throw new EXMLException("SimpleExcelForTN.tn.doc.excel:registerContentTypes", throwable);
		}
		// --- <<IS-END>> ---

                
	}



	public static final void validate (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(validate)>> ---
		// @specification wm.tn.rec:BizDocValidationService
		// @subtype unknown
		// @sigtype java 3.5
		IDataCursor idatacursor = pipeline.getCursor();

		BizDocEnvelope bizdocenvelope = (BizDocEnvelope)ValuesEmulator.get(pipeline, "bizdoc");
		if(bizdocenvelope == null)
			throw new ServiceException(SystemLog2.format("TRNSERV.000000.000008", "bizdoc"));
		BizDocType bizdoctype = bizdocenvelope.getDocType();

		//System.out.println("### Name - "+bizdoctype.getName()+" Editor - "+ bizdoctype.getEditorName());

		//if(!(bizdoctype instanceof ExcelDocType))
		//	throw new ServiceException(SystemLog2.format("Excel file validation failed. Document type is not Microsoft Excel file"));
		if (!(bizdoctype.getDisplayName().equals("Excel file")))
		     throw new ServiceException(SystemLog2.format("Excel file validation failed. Document type is not Microsoft Excel file"));
		IDataCursor idatacursor1 = null;
		try
		{
			String s = bizdocenvelope.getInternalId();
			/*
			Object obj = bizdocenvelope.getDeliveryContent();
			System.out.println("### Class - "+obj.getClass().getName());
			if(obj == null)
				throw new ServiceException(SystemLog2.format("Excel file validation failed: no document content available"));
			*/
			byte bin_array[] = bizdocenvelope.getContentBytes();
			if (bin_array == null)
  				throw new ServiceException(SystemLog2.format("Excel file validation failed: no document content available"));

			//ExcelDocType xls_doctype = (ExcelDocType)bizdoctype;
			String s1 = bizdoctype.getName(); //xls_doctype.getName();
			String s2 = "UTF8"; //xls_doctype.getContentEncoding(bizdocenvelope);

			//IData idata1 = IDataUtilExtension.smartClone(xls_doctype.getValidationInputs());
			IData idata1 = bizdoctype.getIData();
			idatacursor1 = idata1.getCursor();
			//System.out.println("### Fill up inputs");

			//idatacursor1.insertAfter("xlsData", obj);
			idatacursor1.insertAfter("bindata", bin_array);
			idatacursor1.insertAfter("encoding", s2);
			idatacursor1.insertAfter("validate", "true");
			idatacursor1.insertAfter("returnErrors", "asArray");
			//System.out.println("### Call service");
			Service.doInvoke("SimpleExcelForTN.util", "MSExcelWorkSheetToRecord", idata1);
			int i = 0;
			IData aidata[] = null;
			if(idatacursor1.first("errors"))
			{
				aidata = (IData[])idatacursor1.getValue();
				if(max_errs == -2)
					max_errs = get_max_errs();
				if(max_errs < 0)
					i = aidata.length;
				else
					i = aidata.length >= i ? i : aidata.length;
			}
			if(i == 0)
			{
				idatacursor.insertAfter("errorCount", "0");
			} 
			else
			{
				idatacursor.insertAfter("errorCount", Integer.toString(i));
				ActivityLogEntry aactivitylogentry[] = new ActivityLogEntry[i];
				for(int j = 0; j < i; j++)
					aactivitylogentry[j] = ActivityLogEntry.createError("Validation", SystemLog2.format("TRNSERV.000002.000022"), SystemLog2.format("TRNSERV.000002.000023", ParseError.formatErrorMessage(aidata[j])));

				idatacursor.insertAfter("errors", aactivitylogentry);
			}
		}
		catch(Throwable throwable)
		{
			throw new EXMLException("SimpleExcelForTN.tn.doc.excel:validate", throwable);
		}
		finally
		{
			idatacursor.destroy();
			if(idatacursor1 != null)
				idatacursor1.destroy();
		}
	
		// --- <<IS-END>> ---

                
	}

	// --- <<IS-START-SHARED>> ---
	static final String PROP_MAX_ERRS = "tn.doc.validate.max_errs";
	static int max_errs = -2;
	
	    static int get_max_errs()
	    {
	        try
	        {
	            String s = System.getProperty("tn.doc.validate.max_errs", "10");
	            return Integer.parseInt(s);
	        }
	        catch(Exception _ex)
	        {
	            return -1;
	        }
	    }
	
	// --- <<IS-END-SHARED>> ---
}

