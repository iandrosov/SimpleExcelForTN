package com.wm.app.tn.doc;

import com.wm.app.tn.err.ActivityLogEntry;
import com.wm.app.tn.err.SystemLog2;
import com.wm.app.tn.tspace.Reservation;
import com.wm.app.tn.tspace.ReservationAgent;
import com.wm.app.tn.util.*;
import com.wm.data.*;
import com.wm.lang.ns.NSName;
import java.io.*;
import java.text.ParseException;
import java.util.Enumeration;

// Referenced classes of package com.wm.app.tn.doc:
//            BizDocType, BizDocTypeException, BizDocAttributeTransform, BizDocEnvelope,
//            BizDocContentPart, BizDocAttribute, BizDocErrorSet

public class ExcelDocType extends BizDocType
{

    private static final NSName EXCEL_VALIDATION_SVC = NSName.create("SimpleExcelForTN.tn.doc.excel:validate");
    private static final String VERSION_NO = TNBuild.getVersion();
    private String parsingSchema;
    private String contentType;
    private IData validationInputs;
    private IData systemAttrs;

    public ExcelDocType()
    {
        validationInputs = null;
        systemAttrs = null;
        setValidationService(EXCEL_VALIDATION_SVC);
    }

    public static boolean s_isType(IData in)
    {
        Object o = ValuesEmulatorUtil.get(in, "ffdata");
        return o != null && (o instanceof InputStream);
    }

    public boolean isType(IData in)
    {
        return s_isType(in);
    }

    public boolean recognize(IData in)
    {
        SystemLog2.log(9, "TRNSERV.000019.000064", getName());
        IData pipeMatch = getPipelineMatchIData();
        if(pipeMatch == null)
            return false;
        if(ValuesEmulator.size(pipeMatch) == 0)
            return false;
        else
            return pipelineMatch(in);
    }

    public BizDocEnvelope createEnvelope(IData in)
        throws BizDocTypeException
    {
        BizDocEnvelope bizdoc = newEnvelope();
        setContentType(in);
        setDocumentContent(in, bizdoc);
        IDataCursor cur = in.getCursor();
        collectAttributes(cur, bizdoc);
        setReceiveSvc(in, bizdoc);
        cur.destroy();
        return bizdoc;
    }

    public static byte[] getDocumentContent(IData in, BizDocEnvelope env)
        throws BizDocTypeException
    {
        String enc;
        ByteArrayOutputStream bos;
        enc = null;
        IData tndata = (IData)ValuesEmulator.get(in, "TN_parms");
        if(tndata != null)
        {
            IDataCursor cur = tndata.getCursor();
            if(cur.first("$contentEncoding"))
                enc = (String)cur.getValue();
            cur.destroy();
        }
        if(enc == null)
            enc = "UTF8";
        Object o = ValuesEmulatorUtil.get(in, "ffdata");
        if(o == null || !(o instanceof InputStream))
            return null;
        int READ_BUFFER_SIZE = 4096;
        bos = new ByteArrayOutputStream();
        BufferedInputStream bis = new BufferedInputStream((InputStream)o, READ_BUFFER_SIZE);
        byte data[] = new byte[READ_BUFFER_SIZE];
        try
        {
            do
            {
                int bytesRead = bis.read(data, 0, READ_BUFFER_SIZE);
                if(bytesRead < 0)
                    break;
                bos.write(data, 0, bytesRead);
            } while(true);
            bos.flush();
            bos.close();
        }
        catch(IOException ioe)
        {
            if(env != null)
                logError(env, SystemLog2.format("TRNSERV.000019.000070", env.getDocType().getName()), SystemLog2.format("TRNSERV.000019.000071", env.getDocType().getName(), SystemLog2.getMessage(ioe), SystemLog2.getStackTrace(ioe)));
            else
                throw new BizDocTypeException(ioe.getLocalizedMessage(), ioe);
        }
        if(enc == null || enc.length() == 0 || enc.equals("UTF8"))
            return bos.toByteArray();
        String str = new String(bos.toByteArray(), enc);
        return str.getBytes("UTF8");
        UnsupportedEncodingException uee;
        //uee;
        if(env != null)
        {
            SystemLog2.log(3, "TRNSERV.000019.000085", env.getInternalId());
            SystemLog2.log(3, uee.toString());
            logError(env, "Encountered an unsupported encoding", "Error turning stream into bytes: " + uee.toString());
            return null;
        } else
        {
            throw new BizDocTypeException(uee.getLocalizedMessage(), uee);
        }
    }

    public static IData s_createPipeline(BizDocEnvelope bizdoc)
        throws BizDocTypeException
    {
        BizDocContentPart part = bizdoc.getContentPart("ffdata");
        if(part == null)
            return null;
        IData pipe = IDataFactory.create();
        IDataCursor cur = pipe.getCursor();
        if(!part.isLargePart())
            cur.insertAfter("ffdata", new ByteArrayInputStream(part.getBytes()));
        else
            try
            {
                cur.insertAfter("ffdata", ((Reservation)part.getStorageRef()).getInputStream());
            }
            catch(IOException ioe)
            {
                throw new BizDocTypeException(SystemLog2.format("TRNSERV.000019.000084", bizdoc.getDocType().getName(), bizdoc.getInternalId(), ioe));
            }
        cur.destroy();
        return pipe;
    }

    public IData createPipeline(BizDocEnvelope bizdoc)
        throws BizDocTypeException
    {
        if(!(bizdoc.getDocType() instanceof FFDocType))
            return IDataFactory.create();
        IData pipe = s_createPipeline(bizdoc);
        if(pipe == null)
            pipe = IDataFactory.create();
        IDataCursor cur = pipe.getCursor();
        Enumeration e = bizdoc.getAttributeNames();
        if(e != null)
        {
            String a_name;
            for(; e.hasMoreElements(); cur.insertAfter(a_name, bizdoc.getStringValue(a_name)))
                a_name = (String)e.nextElement();

        }
        cur.destroy();
        return pipe;
    }

    public BizDocType copy()
    {
        FFDocType ffdt = new FFDocType();
        ffdt.setIData(getIData());
        ffdt.setHidden(false);
        return ffdt;
    }

    public byte[] getContentBytes(BizDocEnvelope bizdoc)
    {
        BizDocContentPart part = bizdoc.getContentPart("ffdata");
        if(part != null)
            return part.getBytes();
        else
            return null;
    }

    public void setContentBytes(BizDocEnvelope env, byte data[])
    {
        BizDocContentPart part = env.getContentPart("ffdata");
        if(part != null)
            part.setBytes(data);
        else
            env.addContentPart("ffdata", contentType, data, 0);
    }

    public Object getDeliveryContent(BizDocEnvelope bizdoc)
        throws IOException
    {
        BizDocContentPart part = bizdoc.getContentPart("ffdata");
        if(part != null)
            return part.getContent();
        else
            return null;
    }

    public void setSystemAttribute(BizDocAttributeTransform bdat)
        throws BizDocTypeException
    {
        BizDocAttribute bda = bdat.getAttribute();
        if(bda == null)
            throw new BizDocTypeException(SystemLog2.format("TRNSERV.000019.000086", bdat.getFunctionName()));
        if(systemAttrs == null)
            systemAttrs = IDataFactory.create(7);
        ValuesEmulator.put(systemAttrs, bda.getId(), bdat);
    }

    public BizDocAttributeTransform getSystemAttribute(String id)
    {
        if(systemAttrs == null)
            return null;
        else
            return (BizDocAttributeTransform)ValuesEmulator.get(systemAttrs, id);
    }

    public void removeAllAttributes()
    {
        super.removeAllAttributes();
        systemAttrs = null;
    }

    public String getFtpFileExtension(BizDocEnvelope bizdoc)
    {
        return "dat";
    }

    public String getDisplayName()
    {
        return "Excel file";
    }

    public String getEditorName()
    {
        return "com.wm.app.tn.client.editor.excelfile.UIExcelfileEditor";
    }

    public String getContentType(BizDocEnvelope bizdoc)
    {
        BizDocContentPart part = bizdoc.getContentPart("ffdata");
        if(part != null)
            return part.getMimeType();
        else
            return null;
    }

    public String getContentEncoding(BizDocEnvelope bizdoc)
    {
        String type = getContentType(bizdoc);
        if(type == null)
            return null;
        int i = type.indexOf(';');
        if(i == -1)
            return "UTF8";
        else
            return type.substring(i + 1).trim();
    }

    public String getParsingSchema()
    {
        return parsingSchema;
    }

    public void setParsingSchema(String s)
    {
        parsingSchema = s;
    }

    public IData getValidationInputs()
    {
        return validationInputs;
    }

    public void setValidationInputs(IData in)
    {
        validationInputs = in;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Type: " + getName() + "  ID: " + getId() + "\n");
        sb.append("Last Modified: " + getLastModifiedTime() + "\n");
        sb.append("Validation Service: " + getValidationService() + "\n");
        sb.append("Validation Inputs: " + getValidationInputs() + "\n");
        sb.append("Verification Service: " + getVerificationService() + "\n");
        sb.append("Signing Service: " + getSigningService() + "\n");
        //sb.append("Parsing Schema: " + getParsingSchema() + "\n");
        sb.append("Attributes: \n");
        Enumeration e = getAttributeIds();
        if(e == null)
        {
            sb.append("  none\n");
        } else
        {
            String id;
            BizDocAttribute attr;
            for(; e.hasMoreElements(); sb.append("  " + attr.getName() + " (" + id + ") = " + getAttributeTransform(id) + "\n"))
            {
                id = (String)e.nextElement();
                attr = getAttribute(id);
            }

        }
        return sb.toString();
    }

    public IData getIData()
    {
        IData idata = super.getIData();
        //ValuesEmulator.put(idata, "ParsingSchema", parsingSchema);
        ValuesEmulator.put(idata, "ValidationInputs", validationInputs);
        ValuesEmulator.put(idata, "SystemAttributes", systemAttrs);
        ValuesEmulator.put(idata, "VERSION_NO", VERSION_NO);
        return idata;
    }

    public void setIData(IData idata)
    {
        super.setIData(idata);
        //setParsingSchema((String)ValuesEmulator.get(idata, "ParsingSchema"));
        setValidationInputs((IData)ValuesEmulator.get(idata, "ValidationInputs"));
        systemAttrs = (IData)ValuesEmulator.get(idata, "SystemAttributes");
    }

    public static IData create()
    {
        return new FFDocType();
    }

    protected static String getContentTypeFromPipe(IData in)
    {
        String type = null;
        String enc = null;
        IData tndata = (IData)ValuesEmulator.get(in, "TN_parms");
        if(tndata != null)
        {
            IDataCursor cur = tndata.getCursor();
            if(cur.first("$contentType"))
                type = (String)cur.getValue();
            if(cur.first("$contentEncoding"))
                enc = (String)cur.getValue();
            cur.destroy();
        }
        if(type == null)
            type = "text/plain";
        if(enc == null)
            enc = "UTF8";
        return type + "; " + enc;
    }

    public void setContentType(IData in)
    {
        contentType = getContentTypeFromPipe(in);
    }

    private void collectAttributes(IDataCursor cur, BizDocEnvelope bizdoc)
    {
        IDataCursor tncur = null;
        if(cur.first("TN_parms"))
        {
            IData tndata = (IData)cur.getValue();
            tncur = tndata.getCursor();
        } else
        {
            return;
        }
        if(systemAttrs != null)
        {
            IDataCursor scur = systemAttrs.getCursor();
            do
            {
                if(!scur.next())
                    break;
                BizDocAttributeTransform bdat = (BizDocAttributeTransform)scur.getValue();
                Object attr_val = collectSystemAttribute(tncur, bizdoc, bdat);
                if(attr_val != null)
                {
                    String attr_nm = bdat.getAttribute().getName();
                    if(attr_nm.equals("SenderID"))
                        BizDocAttributeTransform.validateAndSetSender(bizdoc, (String)attr_val, (String)tncur.getValue());
                    else
                    if(attr_nm.equals("ReceiverID"))
                        BizDocAttributeTransform.validateAndSetReceiver(bizdoc, (String)attr_val, (String)tncur.getValue());
                    else
                    if("DocumentID".equals(attr_nm))
                        bizdoc.setDocumentId((String)attr_val);
                    else
                    if("GroupID".equals(attr_nm))
                        bizdoc.setGroupId((String)attr_val);
                    else
                    if("ConversationID".equals(attr_nm))
                        bizdoc.setConversationId((String)attr_val);
                    else
                    if("UserStatus".equals(attr_nm))
                        bizdoc.setUserStatus((String)attr_val);
                    else
                        SystemLog2.log(5, "TRNSERV.000019.000078", attr_nm, attr_val);
                }
            } while(true);
            scur.destroy();
        }
        Enumeration e = getAttributeIds();
        if(e == null)
            return;
        BizDocAttributeTransform bdat;
        for(; e.hasMoreElements(); collectCustomAttribute(tncur, bizdoc, bdat))
        {
            String id = (String)e.nextElement();
            bdat = getAttributeTransform(id);
        }

        tncur.destroy();
    }

    private Object collectSystemAttribute(IDataCursor cur, BizDocEnvelope bizdoc, BizDocAttributeTransform bdat)
    {
        BizDocAttribute attr = bdat.getAttribute();
        String id = attr.getId();
        String nm = attr.getName();
        Object temp = null;
        String val[] = null;
        Object xform_val = null;
        if(cur.first(nm))
        {
            temp = cur.getValue();
            if(temp == null)
                return null;
            if(temp instanceof String[])
                val = (String[])temp;
            else
            if(temp instanceof String)
            {
                val = new String[1];
                val[0] = (String)temp;
            } else
            {
                logError(bizdoc, SystemLog2.format("TRNSERV.000019.000065", getName()), SystemLog2.format("TRNSERV.000019.000066", getName(), bdat.getFunctionName(), nm));
                return null;
            }
        } else
        {
            return null;
        }
        if(bdat == null || bdat.getFunction() == -1)
            xform_val = val[0];
        else
            try
            {
                xform_val = bdat.apply(val);
            }
            catch(Exception e)
            {
                logError(bizdoc, SystemLog2.format("TRNSERV.000019.000068", getName()), SystemLog2.format("TRNSERV.000019.000069", getName(), bdat.getFunctionName(), nm, SystemLog2.getMessage(e), SystemLog2.getStackTrace(e)));
                return null;
            }
        if(xform_val != null)
            return xform_val;
        if(isAttributeRequired(id))
            logError(bizdoc, SystemLog2.format("TRNSERV.000019.000067", nm, getName()), SystemLog2.format("TRNSERV.000019.000067", nm, getName()));
        return null;
    }

    private void collectCustomAttribute(IDataCursor cur, BizDocEnvelope bizdoc, BizDocAttributeTransform bdat)
    {
        BizDocAttribute attr = bdat.getAttribute();
        String id = attr.getId();
        String nm = attr.getName();
        String type_name = bizdoc.getDocType().getName();
        Object temp = null;
        String val[] = null;
        Object xform_val = null;
        if(cur.first(nm))
        {
            temp = cur.getValue();
            if(temp == null)
                val = null;
            else
            if(temp instanceof String[])
                val = (String[])temp;
            else
            if(temp instanceof String)
            {
                val = new String[1];
                val[0] = (String)temp;
            } else
            {
                logError(bizdoc, SystemLog2.format("TRNSERV.000019.000065", getName()), SystemLog2.format("TRNSERV.000019.000066", getName(), bdat.getFunctionName(), nm));
                return;
            }
        } else
        {
            val = null;
        }
        if(val == null)
        {
            if("STRING LIST".equals(attr.getType()))
                bizdoc.setStringListValue(nm, null);
            else
            if("NUMBER LIST".equals(attr.getType()))
                bizdoc.setNumberListValue(nm, null);
            else
            if("DATETIME LIST".equals(attr.getType()))
                bizdoc.setDateListValue(nm, null);
            else
                bizdoc.setStringValue(nm, null);
            if(isAttributeRequired(id))
                logError(bizdoc, SystemLog2.format("TRNSERV.000019.000067", nm, getName()), SystemLog2.format("TRNSERV.000019.000067", nm, getName()));
            return;
        }
        if(bdat.getFunction() == -1)
        {
            if("STRING LIST".equals(attr.getType()))
            {
                BizDocAttributeTransform.convertEmptyStringsToNull(val);
                bizdoc.setStringListValue(nm, (String[])val);
            } else
            if("NUMBER LIST".equals(attr.getType()))
            {
                Double numberList[] = new Double[val.length];
                boolean error = false;
                for(int i = 0; i < val.length; i++)
                {
                    if(val[i] == null || val[i].trim().length() == 0)
                    {
                        numberList[i] = null;
                        continue;
                    }
                    try
                    {
                        numberList[i] = new Double(I18NUtil.parseDouble(val[i]));
                    }
                    catch(ParseException pe)
                    {
                        error = true;
                        logError(bizdoc, SystemLog2.format("TRNSERV.000019.000057"), SystemLog2.format("TRNSERV.000008.000021", nm, type_name, SystemLog2.getMessage(pe)));
                        break;
                    }
                    numberList[i] = new Double(val[i]);
                }

                if(!error)
                    bizdoc.setNumberListValue(nm, numberList);
            } else
            if("DATETIME LIST".equals(attr.getType()))
            {
                if(isAttributeRequired(id))
                    logError(bizdoc, SystemLog2.format("TRNSERV.000019.000057"), SystemLog2.format("TRNSERV.000008.000021", nm, type_name, SystemLog2.format("TRNSERV.000008.000019", attr.getName())));
                else
                    logWarning(bizdoc, SystemLog2.format("TRNSERV.000019.000057"), SystemLog2.format("TRNSERV.000008.000021", nm, type_name, SystemLog2.format("TRNSERV.000008.000019", attr.getName())));
            } else
            {
                xform_val = val[0];
                if(xform_val != null)
                {
                    bizdoc.setStringValue(nm, xform_val.toString());
                } else
                {
                    bizdoc.setStringValue(nm, null);
                    if(isAttributeRequired(id))
                        logError(bizdoc, SystemLog2.format("TRNSERV.000019.000067", nm, getName()), SystemLog2.format("TRNSERV.000019.000067", nm, getName()));
                }
            }
            return;
        }
        try
        {
            xform_val = bdat.apply(val);
        }
        catch(Exception e)
        {
            logError(bizdoc, SystemLog2.format("TRNSERV.000019.000068", getName()), SystemLog2.format("TRNSERV.000019.000069", getName(), bdat.getFunctionName(), nm, SystemLog2.getMessage(e), SystemLog2.getStackTrace(e)));
            return;
        }
        if(xform_val != null)
        {
            if("STRING LIST".equals(attr.getType()))
            {
                BizDocAttributeTransform.convertEmptyStringsToNull((String[])xform_val);
                bizdoc.setStringListValue(nm, (String[])xform_val);
            } else
            if("NUMBER LIST".equals(attr.getType()))
                bizdoc.setNumberListValue(nm, (Double[])xform_val);
            else
            if("DATETIME LIST".equals(attr.getType()))
                bizdoc.setDateListValue(nm, (java.sql.Timestamp[])xform_val);
            else
                bizdoc.setStringValue(nm, xform_val.toString());
        } else
        {
            if("STRING LIST".equals(attr.getType()))
                bizdoc.setStringListValue(nm, null);
            else
            if("NUMBER LIST".equals(attr.getType()))
                bizdoc.setNumberListValue(nm, null);
            else
            if("DATETIME LIST".equals(attr.getType()))
                bizdoc.setDateListValue(nm, null);
            else
                bizdoc.setStringValue(nm, null);
            if(isAttributeRequired(id))
                logError(bizdoc, SystemLog2.format("TRNSERV.000019.000067", nm, getName()), SystemLog2.format("TRNSERV.000019.000067", nm, getName()));
        }
    }

    private void setDocumentContent(Reservation reservation, BizDocEnvelope env)
        throws BizDocTypeException
    {
        env.addContentPart("ffdata", contentType, 0, -1, "tspace", reservation);
        env.setContent(null);
    }

    private void setDocumentContent(InputStream is, BizDocEnvelope env)
        throws BizDocTypeException
    {
        byte bytes[] = inputStreamToBytes(is, env);
        env.addContentPart("ffdata", contentType, bytes, 0);
        env.setContent(bytes);
    }

    private void setDocumentContent(byte bytes[], BizDocEnvelope env)
        throws BizDocTypeException
    {
        env.addContentPart("ffdata", contentType, bytes, 0);
        env.setContent(bytes);
    }

    private void setDocumentContent(IData in, BizDocEnvelope env)
        throws BizDocTypeException
    {
        InputStream ffdata = (InputStream)ValuesEmulator.get(in, "ffdata");
        if(ffdata == null)
        {
            logError(env, SystemLog2.format("TRNSERV.000019.000070", getName()), SystemLog2.format("TRNSERV.000019.000083", getName()));
            return;
        }
        int largeDocThreshold = Config.getLargeDocThreshold();
        if(largeDocThreshold < 0)
        {
            setDocumentContent(ffdata, env);
            return;
        }
        try
        {
            int READ_BUFFER_SIZE = 40000;
            BufferedInputStream bis = new BufferedInputStream(ffdata, 40000);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesRead = 0;
            byte data[] = new byte[40000];
            bytesRead = bis.read(data, 0, largeDocThreshold + 1);
            int read = 0;
            do
            {
                read = bis.read(data, 0, 40000);
                if(read < 0)
                    break;
                bytesRead += read;
                baos.write(data, 0, read);
            } while(bytesRead < largeDocThreshold);
            if(bytesRead <= largeDocThreshold)
            {
                setDocumentContent(baos.toByteArray(), env);
            } else
            {
                ReservationAgent ra = ReservationAgent.current();
                int size = bytesRead;
                Reservation r = ra.createReservation(size);
                BufferedOutputStream bos = new BufferedOutputStream(r.getOutputStream(), 40000);
                bos.write(baos.toByteArray());
                byte content[] = new byte[40000];
                do
                {
                    bytesRead = bis.read(content, 0, 40000);
                    if(bytesRead < 0)
                        break;
                    size += bytesRead;
                    ra.resizeReservation(r, size);
                    bos.write(content, 0, bytesRead);
                } while(true);
                bos.flush();
                bos.close();
                setDocumentContent(r, env);
            }
        }
        catch(Throwable t)
        {
            logError(env, SystemLog2.format("TRNSERV.000019.000070", getName()), SystemLog2.format("TRNSERV.000019.000071", getName(), SystemLog2.getMessage(t), SystemLog2.getStackTrace(t)));
        }
    }

    private byte[] inputStreamToBytes(InputStream is, BizDocEnvelope env)
    {
        ByteArrayOutputStream bos;
        String enc;
        if(is == null)
            return null;
        int READ_BUFFER_SIZE = 4096;
        bos = new ByteArrayOutputStream();
        BufferedInputStream bis = new BufferedInputStream(is, READ_BUFFER_SIZE);
        byte data[] = new byte[READ_BUFFER_SIZE];
        try
        {
            do
            {
                int bytesRead = bis.read(data, 0, READ_BUFFER_SIZE);
                if(bytesRead < 0)
                    break;
                bos.write(data, 0, bytesRead);
            } while(true);
            bos.flush();
            bos.close();
        }
        catch(IOException ioe)
        {
            logError(env, SystemLog2.format("TRNSERV.000019.000070", getName()), SystemLog2.format("TRNSERV.000019.000071", getName(), SystemLog2.getMessage(ioe), SystemLog2.getStackTrace(ioe)));
            return null;
        }
        enc = null;
        int i = contentType.indexOf(';');
        if(i != -1)
            enc = contentType.substring(i + 1).trim();
        if(enc == null || enc.length() == 0 || enc.equals("UTF8"))
            return bos.toByteArray();
        String str = new String(bos.toByteArray(), enc);
        return str.getBytes("UTF8");
        UnsupportedEncodingException uee;
        //uee;
        SystemLog2.log(3, "TRNSERV.000019.000085", env.getInternalId());
        SystemLog2.log(3, uee.toString());
        logError(env, SystemLog2.format("TRNSERV.000019.000091"), SystemLog2.format("TRNSERV.000019.000092", uee.toString()));
        return null;
    }

    private static void logError(BizDocEnvelope bizdoc, String brief, String full)
    {
        ActivityLogEntry err = ActivityLogEntry.createError("Recognition", brief, full);
        bizdoc.getErrorSet().addError(err);
    }

    private static void logWarning(BizDocEnvelope bizdoc, String brief, String full)
    {
        ActivityLogEntry err = ActivityLogEntry.createWarning("Recognition", brief, full);
        bizdoc.getErrorSet().addError(err);
    }

}