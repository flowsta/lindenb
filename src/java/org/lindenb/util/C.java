package org.lindenb.util;

public class C {
	protected C() {}
	 /**
     * escape a C string
     * @param s a word to convert
     * @return the converted string
     */
    static public String escape(CharSequence s)
        {
    	if(s==null) return null;
        int needed=-1;
        for(int i=0;i< s.length();++i)
	        {
	        switch(s.charAt(i))
	            {
	            case('\"'): 
	            case('\''): 
	            case('\n'): 
	            case('\t'):
	            case('\\'): needed=i; break;
	            default: break;
	            }
	        if(needed!=-1) break;
	        }
        if(needed==-1) return s.toString();
        
        StringBuilder buffer=new StringBuilder(s.subSequence(0,needed));
        
        for(int i=needed;i< s.length();++i)
            {
            switch(s.charAt(i))
                {
                case('\"'): buffer.append("\\\"");break;
                case('\''): buffer.append("\\\'");break;
                case('\n'): buffer.append("\\n");break;
                case('\t'): buffer.append("\\t");break;
                case('\\'): buffer.append("\\\\");break;
                default: buffer.append(s.charAt(i)); break;
                }
            }
        return buffer.toString();
        }

}
