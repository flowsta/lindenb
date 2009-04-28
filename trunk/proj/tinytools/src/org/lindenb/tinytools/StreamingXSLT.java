package org.lindenb.tinytools;


import java.io.InputStreamReader;
import java.io.Reader;


import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.lindenb.io.IOUtils;
import org.lindenb.util.Compilation;
import org.lindenb.util.StringUtils;
import org.lindenb.xml.XMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



public class StreamingXSLT
{
private Transformer transformer=null;
private int depth=-1;
private String qNameTarget=null;

private StreamingXSLT() throws Exception
	{
	}

private class Sax2dom
	extends DefaultHandler
	{
	private Document dom;
	private Node currentNode=null;
	private List<String> nsMapping=new ArrayList<String>();
	private int indexInStream=0;
	
	Sax2dom() throws Exception
		{
		DocumentBuilderFactory domFactory= DocumentBuilderFactory.newInstance();
		DocumentBuilder builder= domFactory.newDocumentBuilder();
		this.dom= builder.newDocument();
		}
	@Override
	public void startDocument() throws SAXException {
		this.currentNode=this.dom;
		this.indexInStream=0;
		}
	
	@Override
	public void endDocument() throws SAXException {
		this.currentNode=null;
		}
	
	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
		this.currentNode.appendChild(this.dom.createProcessingInstruction(target, data));
		}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		final String text = new String(ch, start, length);
		this.currentNode.appendChild(this.dom.createTextNode(text));
		}
	
	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		this.nsMapping.add(prefix);
		this.nsMapping.add(uri);
		}
	
	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException
		{
		
		org.w3c.dom.Element e= null;
		
		if(uri!=null)
			{
			e=this.dom.createElementNS(uri, name);
			}
		else
			{
			e=this.dom.createElement(name);
			}
		currentNode.appendChild(e);
		currentNode=e;
		
		for(int i=0; i+1< nsMapping.size();i+=2)
			{
			String prefix= nsMapping.get(i);
			String ns = nsMapping.get(i+1);
			 if (StringUtils.isEmpty(prefix))
			 	{
				e.setAttributeNS(XMLConstants.XML_NS_URI, "xmlns",ns);
				}
			else
				{
				e.setAttributeNS(XMLConstants.XML_NS_URI, "xmlns:"+prefix,ns);
				}
			}
		
		final int nAtt= attributes.getLength();
		for(int i=0; i< nAtt;++i)
			{
			 if (attributes.getLocalName(i) == null)
			 	{
				e.setAttribute(attributes.getQName(i), attributes.getValue(i));
				}
			else
				{
				e.setAttributeNS(attributes.getURI(i), attributes.getQName(i), attributes.getValue(i));
				}
			}
		
		nsMapping.clear();
		}
	
	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException
		{
		boolean b=process(this.dom,Element.class.cast(this.currentNode),this.indexInStream);
		this.currentNode=this.currentNode.getParentNode();
		if(b)
			{
			++this.indexInStream;
			while(this.currentNode.hasChildNodes())
				{
				this.currentNode.removeChild(this.currentNode.getFirstChild());
				}
			}
		}
	
	}

private boolean process(
		Document dom,
		Element node,
		int indexInStream) throws SAXException
	{
	if(this.depth!=-1 && depth!=XMLUtilities.getLevel(node)) return false;
	if(this.qNameTarget!=null && node.getTagName().equals(this.qNameTarget)==false) return false;
	
	Source src= new DOMSource(dom);
	Result result= new StreamResult(System.out);
	try {
		this.transformer.setParameter("nodeIndex", indexInStream);
		this.transformer.transform(src, result);
		} 
	catch (TransformerException e)
		{
		throw new SAXException(e);
		}
	return true;
	}


private void parse(Reader in) throws Exception
	{
	SAXParserFactory saxFactory= SAXParserFactory.newInstance();
	SAXParser parser= saxFactory.newSAXParser();
	parser.parse(new InputSource(in), new Sax2dom());
	}

public static void main(String[] args) {
	try {
		StreamingXSLT app= new StreamingXSLT();
		int optind=0;
	    String stylesheet=null;
	    List<String> parameters= new ArrayList<String>();
		while(optind<args.length)
			{
			if(args[optind].equals("-h"))
				{
				System.err.println("Pierre Lindenbaum PhD.");
				System.err.println(Compilation.getLabel());
				System.err.println("-h this screen");
				System.err.println("-x <xslt-stylesheet> required");
				System.err.println("-p <param-name> <param-value> (add parameter to the xslt engine)");
				System.err.println("-d depth (0 based) default:-1");
				System.err.println("-q qName target default:null" );
				return;
				}
			 else if (args[optind].equals("-x"))
			     {
				 stylesheet=args[++optind];
			     }
			 else if (args[optind].equals("-d"))
			     {
				 app.depth= Integer.parseInt(args[++optind]);
			     }
			 else if (args[optind].equals("-p"))
			     {
				 parameters.add(args[++optind]);
				 parameters.add(args[++optind]);
			     }
			
			 else if (args[optind].equals("-q"))
			     {
				 app.qNameTarget= args[++optind];
			     }
			 else if (args[optind].equals("--"))
			     {
			     ++optind;
			     break;
			     }
			else if (args[optind].startsWith("-"))
			     {
			     System.err.println("bad argument " + args[optind]);
			     System.exit(-1);
			     }
			else
			     {
			     break;
			     }
			++optind;
			}
		if(stylesheet==null)
			{
			System.err.println("Undefined Stylesheet");
			return ;
			}
		TransformerFactory transformerFactory=TransformerFactory.newInstance();
		Templates templates = transformerFactory.newTemplates(new StreamSource(
				IOUtils.openReader(stylesheet)
			));
		app.transformer= templates.newTransformer();
		for(int i=0;i+1< parameters.size();i+=2)
			{
			app.transformer.setParameter(parameters.get(i), parameters.get(i+1));
			}
		if(optind==args.length)
			{
			app.parse(new InputStreamReader(System.in));
			}
		else
			{
			while(optind< args.length)
				{
				Reader r = IOUtils.openReader(args[optind++]);
				app.parse(r);
				r.close();
				}
			}
	} catch (Exception e) {
		e.printStackTrace();
	}
}
}