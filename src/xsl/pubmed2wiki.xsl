<?xml version='1.0' ?>
<xsl:stylesheet
	xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
	version='1.0'
	>
<xsl:param name="layout">ref</xsl:param>
<!--

This stylesheet transforms one or more Pubmed
Article in xml format into a set of citations for wikipedia

Author: Pierre Lindenbaum PhD plindenbaum@yahoo.fr

see: http://en.wikipedia.org/wiki/Template:Citation

-->
<xsl:output method='text'/>


<xsl:template match="/">
&lt;!-- 
Generated with pubmed2wiki.xsl
Author: Pierre Lindenbaum PhD.
plindenbaum@yahoo.fr 
http://en.wikipedia.org/wiki/User:Plindenbaum
--&gt;
<xsl:apply-templates/>
</xsl:template>

<xsl:template match="PubmedArticleSet">
<xsl:apply-templates select="PubmedArticle"/>
</xsl:template>



<xsl:template match="PubmedArticle"><xsl:choose><xsl:when test="$layout='ref'">&lt;ref&gt;</xsl:when><xsl:otherwise>*</xsl:otherwise></xsl:choose>{{cite journal
| quotes = no
<xsl:apply-templates select=".//AuthorList"/>
<xsl:apply-templates select=".//PubDate"/>
<xsl:apply-templates select=".//ArticleTitle"/>
<xsl:apply-templates select=".//ISOAbbreviation"/>
<xsl:if test="not(.//ISOAbbreviation)"><xsl:call-template name="periodical">
<xsl:with-param name="J" select=".//Journal/Title"/>
</xsl:call-template></xsl:if>
<xsl:apply-templates select=".//JournalIssue"/>
<xsl:apply-templates select=".//Pagination"/>
| publisher = 
| location = 
| issn = 
<xsl:apply-templates select=".//PMID"/>
<xsl:apply-templates select=".//ArticleId[@IdType=&apos;doi&apos;]"/>
| bibcode = 
| oclc =
| id = 
| url = 
| language = 
| format = 
| accessdate = 
| laysummary = 
| laysource = 
| laydate = 
| quote = 
 }}<xsl:choose><xsl:when test="$layout='ref'">&lt;/ref&gt;</xsl:when><xsl:otherwise><xsl:text>
</xsl:text></xsl:otherwise></xsl:choose></xsl:template>








<xsl:template match="PMID">| pmid = <xsl:value-of select="."/>
</xsl:template>

<xsl:template match="ArticleId[@IdType=&apos;doi&apos;]">
|doi = <xsl:value-of select="."/>
</xsl:template>

<xsl:template match="AuthorList">
<xsl:if test="count(Author)&gt;0">|last=<xsl:value-of select="Author[1]/LastName"/>
|first=<xsl:value-of select="Author[1]/ForeName"/>
|authorlink=</xsl:if>
<xsl:if test="count(Author)&gt;1">
|coauthors=<xsl:for-each select="Author">
<xsl:if test="position()&gt;1">
<xsl:value-of select="LastName"/><xsl:text> </xsl:text><xsl:value-of select="ForeName"/>
<xsl:if test="position()!=last()"><xsl:text>, </xsl:text></xsl:if>
</xsl:if>
</xsl:for-each>
</xsl:if>
</xsl:template>

<xsl:template match="PubDate">
|year=<xsl:value-of select="Year"/>
|month=<xsl:value-of select="Month"/>
</xsl:template>


<xsl:template match="ArticleTitle">
|title=<xsl:choose>
<xsl:when test="substring(.,string-length(.))='.'">
<xsl:value-of select="substring(.,1,string-length(.)-1)"/></xsl:when>
<xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
</xsl:choose>
</xsl:template>

<xsl:template match="ISOAbbreviation"><xsl:call-template name="periodical">
<xsl:with-param name="J" select="."/>
</xsl:call-template>
</xsl:template>

<xsl:template name="periodical">
<xsl:param name="J"/>
|journal=<xsl:choose>
<xsl:when test="$J='JAMA'">[[Journal of the American Medical Association|JAMA]]</xsl:when>
<xsl:when test="$J='Science'">[[Science (journal)|Science]]</xsl:when>
<xsl:when test="$J='Nature'">[[Nature (journal)|Nature]]</xsl:when>
<xsl:when test="$J='Endocrinology'">[[Endocrinology (journal)|Endocrinology]]</xsl:when>
<xsl:when test="$J='Genetics'">[[Genetics (journal)|Genetics]]</xsl:when>
<xsl:when test="$J='Lancet'">[[The_Lancet|Lancet]]</xsl:when>
<xsl:when test="$J='Proc. Natl. Acad. Sci. U.S.A.'">[[PNAS|Proc. Natl. Acad. Sci. U.S.A.]]</xsl:when>
<xsl:when test="$J='Journal of the Royal Society of Medicine'">[[Journal of the Royal Society of Medicine]]</xsl:when>
<xsl:otherwise><xsl:value-of select="$J"/></xsl:otherwise>
</xsl:choose>
</xsl:template>



<xsl:template match="JournalIssue">
|volume=<xsl:value-of select="Volume"/>
|issue=<xsl:value-of select="Issue"/>
</xsl:template>


<xsl:template match="Pagination">
|pages=<xsl:value-of select="MedlinePgn"/>
</xsl:template>


</xsl:stylesheet>