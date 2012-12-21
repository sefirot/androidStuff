<?xml version="1.0" ?>
<?xml-stylesheet type="text/xsl" href="control.xsl"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:util="com.applang.Util"
	extension-element-prefixes="util">
	
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />

	<xsl:param name="dbfile" select="//dbfile" />
	<xsl:param name="year" select="//year" />
	<xsl:param name="weekInYear" select="//weekInYear" />
	<xsl:param name="dayInWeek" select="//dayInWeek" />
	<xsl:param name="Bericht"><![CDATA[(?i).*bericht.*]]></xsl:param>
	<xsl:param name="Bemerkung"><![CDATA[(?i).*bemerk.*]]></xsl:param>
	
	<xsl:template match="/">
<control>
	<DBINFO>
		<dbdriver>org.sqlite.JDBC</dbdriver>
		<dburl><xsl:value-of select="concat('jdbc:sqlite:', $dbfile)" /></dburl>
		<user />
		<password/>
	</DBINFO>
	<QUERY 
		statement="SELECT note FROM notes where title regexp ? and created=?" 
		typeinfo="string,integer">
	</QUERY>
	<DATE 
		year="{$year}"
		weekInYear="{$weekInYear}"
		dayInWeek="{$dayInWeek}" />
	<textelement formid="control1"><xsl:value-of select="util:formatDate(util:dateInMillis($year, $weekInYear, 1),'w/yy')" /></textelement>
	<textelement formid="control2"><xsl:value-of select="util:formatDate(util:dateInMillis($year, $weekInYear, 2),'dd.MM.yyyy')" /></textelement>
	<textelement formid="control3"><xsl:value-of select="util:formatDate(util:dateInMillis($year, $weekInYear, 7),'dd.MM.yyyy')" /></textelement>
	<textelement formid="control32" title="{$Bericht}" weekday="2" />
	<textelement formid="control33" title="{$Bericht}" weekday="3" />
	<textelement formid="control34" title="{$Bericht}" weekday="4" />
	<textelement formid="control35" title="{$Bericht}" weekday="5" />
	<textelement formid="control36" title="{$Bericht}" weekday="6" />
	<textelement formid="control37" title="{$Bericht}" weekday="7" />
	<textelement formid="control41"><xsl:value-of select="util:formatDate(util:dateInMillis($year, $weekInYear, 1),'w/yy')" /></textelement>
	<textelement formid="control42"><xsl:value-of select="util:formatDate(util:dateInMillis($year, $weekInYear, 2),'dd.MM.yyyy')" /></textelement>
	<textelement formid="control43"><xsl:value-of select="util:formatDate(util:dateInMillis($year, $weekInYear, 7),'dd.MM.yyyy')" /></textelement>
	<textelement formid="control44" title="{$Bemerkung}" weekday="2" />
	<textelement formid="control45" title="{$Bemerkung}" weekday="3" />
	<textelement formid="control46" title="{$Bemerkung}" weekday="4" />
	<textelement formid="control47" title="{$Bemerkung}" weekday="5" />
	<textelement formid="control48" title="{$Bemerkung}" weekday="6" />
	<textelement formid="control49" title="{$Bemerkung}" weekday="7" />
</control>
	</xsl:template>


</xsl:stylesheet>

