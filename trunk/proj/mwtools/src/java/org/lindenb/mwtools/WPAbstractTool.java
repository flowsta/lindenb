package org.lindenb.mwtools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

public abstract class WPAbstractTool
	{
	/** logger */
	protected static final Logger LOG= Logger.getLogger("fr.lindenb.mwtools");
	/** xml parser factory */
	protected XMLInputFactory xmlInputFactory;
	/** WP base URP */
	protected String base_api="http://en.wikipedia.org/w/api.php";
	
	protected WPAbstractTool()
		{
		xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
		}
	/** escapes WP title */
	protected String escape(String entry) throws IOException
		{
		return entry.replace(' ', '_');
		}
	
	protected String attr(StartElement e, String attName)
		{
		Attribute att=e.getAttributeByName(new QName(attName));
		return att==null?"":att.getValue();
		}
	
	/**
	 * Open a URL to the given stream, retry 10 times if it fails
	 * @param url
	 * @return
	 * @throws IOException
	 */
	protected InputStream openStream(String url) throws IOException
		{
		final int tryNumber=10;
		IOException lastError=null;
		URL net = new URL(url);
		for(int i=0;i< tryNumber;++i)
			{
			try
				{
				InputStream in=net.openStream();
				return in;
				}
			catch(IOException err)
				{
				lastError=err;
				LOG.info("Trying "+i+" "+err.getMessage());
				try {
					Thread.sleep(10000);//sleep 10secs
				} catch (Exception e) {
					
				}
				continue;
				}
			}
		throw lastError;
		}
	
	}
