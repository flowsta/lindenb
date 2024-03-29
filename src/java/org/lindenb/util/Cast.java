package org.lindenb.util;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;


import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Utilities to cast String to another java type
 */
public class Cast<T>
{
/** archetype class */
private Class<T> clazz=null;
/** constructor with one string as argument */
private Constructor<T> cstor=null;
/** constructor */
public Cast(Class<T> clazz) { this.clazz=clazz;}
/** @return the archetype class */
public Class<T> getJavaClass() { return this.clazz;}
/** @return a name for the class */
public String getName() { return getJavaClass().getName();}
/** should I trim the input string */
protected String trim(String s) { return s==null?null:s.trim();}
/** can this string  be casted ? calls <code>cast</code> */
public boolean isA(String s)
	{
	return cast(s)!=null;
	}
/** transform a string to T */
public T cast(String s)
	{
	if(s==null) return null;
	try { return getConstructor().newInstance(trim(s));}
	catch(Throwable err) { return null;}
	}
/** verify this string isA<T> else throws a IllegalArgumentException */
public void verify(String s)
	{
	if(!isA(s)) throw new IllegalArgumentException("Not a "+getName()+" : "+s);
	}
public final Constructor<T> getConstructor()
	{
	if(this.cstor!=null) return this.cstor;
	try { this.cstor= getJavaClass().getConstructor(String.class); return this.cstor;}
	catch(NoSuchMethodException err)
		{
		throw new RuntimeException(err);
		}
	}

private static class CastBoolean extends Cast<Boolean>
	{
	public CastBoolean() {
		super(Boolean.class);
		}
	@Override
	public Boolean cast(java.lang.String s) {
		if(s==null) return null;
		s=trim(s).toLowerCase();
		if(s.equals("yes") || s.equals("y") || s.equals("true") || s.equals("t") || s.equals("1")) return java.lang.Boolean.TRUE;
		if(s.equals("no") || s.equals("n") || s.equals("false") || s.equals("f") || s.equals("0")) return java.lang.Boolean.FALSE;
		return null;
		}
	}

private static class CastULong extends Cast<Long>
	{
	public CastULong() {
		super(Long.class);
		}
	@Override
	public java.lang.String getName() {
		return "unsigned long";
		}
	@Override
	public Long cast(java.lang.String s)
		{
		Long L= super.cast(s);
		return (L==null || L< 0L ? null:L);
		}
	}

private static class CastUInt extends Cast<Integer>
	{
	public CastUInt() {
		super(Integer.class);
		}
	@Override
	public java.lang.String getName() {
		return "unsigned int";
		}
	@Override
	public Integer cast(java.lang.String s)
		{
		Integer i= super.cast(s);
		return (i==null || i< 0 ? null:i);
		}
	}

	
private static class CasterOpaqueURI extends Cast<java.net.URI>
	{
	public CasterOpaqueURI(){ super(java.net.URI.class);}
	@Override
	public java.lang.String getName() { return "opaque uri"; }
	@Override
	public java.net.URI cast(String s)
		{
		java.net.URI uri= super.cast(s);
		return (uri==null || !uri.isOpaque() ? null:uri);
		}
	}

private static class CasterAbsoluteURI extends Cast<java.net.URI>
	{
	public CasterAbsoluteURI(){ super(java.net.URI.class);}
	@Override
	public java.lang.String getName() { return "absolute uri"; }	
	@Override
	public java.net.URI cast(String s)
		{
		java.net.URI uri= super.cast(s);
		return (uri==null || !uri.isAbsolute() ? null:uri);
		}
	}

private static class CasterXML extends Cast<String>
	{
	private SAXParser parser=null;
	/** namespace aware ?*/
	private boolean nsAware;
	public CasterXML(boolean nsAware){ super(String.class);this.nsAware=nsAware;}
	protected SAXParserFactory getParserFactory() {
		SAXParserFactory f= SAXParserFactory.newInstance();
		f.setNamespaceAware(this.nsAware);
		f.setValidating(false);
		return f;
		}
	protected SAXParser getParser()
		{
		if(this.parser==null)
			{
			try { this.parser=getParserFactory().newSAXParser(); }
			catch(Throwable err) { throw new RuntimeException(err);}
			}
		return this.parser;
		}
	@Override
	public java.lang.String cast(java.lang.String s) {
		if(s==null) return null;
		s= trim(s);
		try {
			getParser().parse(new InputSource(new StringReader(s)), new DefaultHandler());
			return s;
			}
		catch(IOException err) { return null;}
		catch(SAXException err) { return null;}
		}
	}

private static class CasterPattern extends Cast<java.util.regex.Pattern>
	{
	public CasterPattern() {
		super(java.util.regex.Pattern.class);
		}
	@Override
	public java.util.regex.Pattern cast(java.lang.String s) {
		if(s==null) return null;
		return java.util.regex.Pattern.compile(trim(s));
		}
	}
private static class CharPattern extends Cast<Character>
	{
	protected CharPattern() {super(Character.class);}
	@Override
	public Character cast(java.lang.String s) {
		if(s==null || s.length()!=1) return null;
		return s.charAt(0);
		}
	}

public final static Cast<java.net.URL> URL= new Cast<java.net.URL>(java.net.URL.class);
public final static Cast<java.net.URI> URI= new Cast<java.net.URI>(java.net.URI.class);
public final static Cast<java.net.URI> OpaqueURI= new CasterOpaqueURI();
public final static Cast<java.net.URI> AbsoluteURI= new CasterAbsoluteURI();
public final static Cast<BigInteger> BigInteger= new Cast<BigInteger>(BigInteger.class);
public final static Cast<BigDecimal> BigDecimal= new Cast<BigDecimal>(BigDecimal.class);
public final static Cast<java.lang.String> String= new Cast<java.lang.String>(java.lang.String.class);
public final static Cast<Character> Character= new CharPattern();
public final static Cast<java.lang.Long> Long= new Cast<java.lang.Long>(Long.class);
public final static Cast<java.lang.Long> ULong= new CastULong();
public final static Cast<java.lang.Double> Double= new Cast<java.lang.Double>(java.lang.Double.class);
public final static Cast<java.lang.Float> Float= new Cast<java.lang.Float>(java.lang.Float.class);
public final static Cast<java.lang.Integer> Integer= new Cast<java.lang.Integer>(java.lang.Integer.class);
public final static Cast<java.lang.Integer> UInteger= new CastUInt();
public final static Cast<java.lang.Short> Short= new Cast<java.lang.Short>(java.lang.Short.class);
public final static Cast<java.lang.Boolean> Boolean = new CastBoolean();
public final static Cast<String> XMLNS = new CasterXML(true);
public final static Cast<String> XML = new CasterXML(false);
public final static Cast<java.util.regex.Pattern> Pattern = new CasterPattern();
}
