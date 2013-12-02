<?xml version="1.0" ?>
<?xml-stylesheet type="text/xsl" href="control.xsl"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:util="com.applang.Util"
	extension-element-prefixes="util">
	
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />

	<xsl:param name="dbfile" select="//dbfile" />
	<xsl:param name="dbfile2" select="//dbfile2" />
	<xsl:param name="year" select="//year" />
	<xsl:param name="weekInYear" select="//weekInYear" />
	<xsl:param name="dayInWeek" select="//dayInWeek" />
	
	<xsl:param name="Bericht"><![CDATA[(?i)(.*bericht.*|berufsschule|lehrgang.*)]]></xsl:param>
	<xsl:param name="Bemerkung"><![CDATA[(?i).*bemerk.*]]></xsl:param>
	<xsl:param name="dateFormat" select="'dd.MM.yyyy'" />
	<xsl:param name="weekFormat" select="'w/yy'" />
	
	<xsl:variable name="start" select="util:timeInMillis($year, $weekInYear, 1)" />
	<xsl:variable name="monday" select="util:timeInMillis($year, $weekInYear, 2)" />
	<xsl:variable name="tuesday" select="util:timeInMillis($year, $weekInYear, 3)" />
	<xsl:variable name="wednesday" select="util:timeInMillis($year, $weekInYear, 4)" />
	<xsl:variable name="thursday" select="util:timeInMillis($year, $weekInYear, 5)" />
	<xsl:variable name="friday" select="util:timeInMillis($year, $weekInYear, 6)" />
	<xsl:variable name="saturday" select="util:timeInMillis($year, $weekInYear, 7)" />
	<xsl:variable name="sunday" select="util:timeInMillis($year, $weekInYear, 8)" />
	
	<xsl:variable name="monday2" select="util:timeInMillis($year, $weekInYear, 2, 1) - 1" />
	<xsl:variable name="tuesday2" select="util:timeInMillis($year, $weekInYear, 3, 1) - 1" />
	<xsl:variable name="wednesday2" select="util:timeInMillis($year, $weekInYear, 4, 1) - 1" />
	<xsl:variable name="thursday2" select="util:timeInMillis($year, $weekInYear, 5, 1) - 1" />
	<xsl:variable name="friday2" select="util:timeInMillis($year, $weekInYear, 6, 1) - 1" />
	<xsl:variable name="saturday2" select="util:timeInMillis($year, $weekInYear, 7, 1) - 1" />
	<xsl:variable name="sunday2" select="util:timeInMillis($year, $weekInYear, 8, 1) - 1" />
	
	<xsl:variable name="_monday" select="util:formatDate($monday,$dateFormat)" />
	<xsl:variable name="_tuesday" select="util:formatDate($tuesday,$dateFormat)" />
	<xsl:variable name="_wednesday" select="util:formatDate($wednesday,$dateFormat)" />
	<xsl:variable name="_thursday" select="util:formatDate($thursday,$dateFormat)" />
	<xsl:variable name="_friday" select="util:formatDate($friday,$dateFormat)" />
	<xsl:variable name="_saturday" select="util:formatDate($saturday,$dateFormat)" />
	<xsl:variable name="_sunday" select="util:formatDate($sunday,$dateFormat)" />
	
	<xsl:template match="/">
<control>
	<DBINFO>
		<dbdriver>org.sqlite.JDBC</dbdriver>
		<dburl><xsl:value-of select="concat('jdbc:sqlite:', util:canonicalPath($dbfile))" /></dburl>
		<user />
		<password/>
	</DBINFO>
	<DBINFO>
		<dbdriver>org.sqlite.JDBC</dbdriver>
		<dburl><xsl:value-of select="concat('jdbc:sqlite:', util:canonicalPath($dbfile2))" /></dburl>
		<user />
		<password/>
	</DBINFO>
	<QUERY 
		dbinfo="1"
		statement="SELECT note FROM notes where title regexp ? and created between ? and ?" 
		typeinfo="string,integer,integer">
	</QUERY>
	<QUERY 
		dbinfo="2"
		statement="SELECT precipitation FROM weathers where created between ? and ?" 
		typeinfo="integer,integer">
	</QUERY>
	<QUERY 
		dbinfo="2"
		statement="SELECT maxtemp FROM weathers where created between ? and ?" 
		typeinfo="integer,integer">
	</QUERY>
	<QUERY 
		dbinfo="2"
		statement="SELECT mintemp FROM weathers where created between ? and ?" 
		typeinfo="integer,integer">
	</QUERY>
	<QUERY 
		dbinfo="2"
		statement="SELECT description FROM weathers where created between ? and ?" 
		typeinfo="integer,integer">
	</QUERY>
	<DATE 
		year="{$year}"
		weekInYear="{$weekInYear}"
		dayInWeek="{$dayInWeek}" />
	<textelement formid="control1" query="0"><xsl:value-of select="util:formatDate($start,$weekFormat)" /></textelement>
	<textelement formid="control2" query="0"><xsl:value-of select="$_monday" /></textelement>
	<textelement formid="control3" query="0"><xsl:value-of select="$_sunday" /></textelement>
	<textelement formid="control4"  query="2" param2="{$monday}" date="{$_monday}" param3="{$monday2}" />
	<textelement formid="control8"  query="2" param2="{$tuesday}" date="{$_tuesday}" param3="{$tuesday2}" />
	<textelement formid="control12" query="2" param2="{$wednesday}" date="{$_wednesday}" param3="{$wednesday2}" />
	<textelement formid="control16" query="2" param2="{$thursday}" date="{$_thursday}" param3="{$thursday2}" />
	<textelement formid="control20" query="2" param2="{$friday}" date="{$_friday}" param3="{$friday2}" />
	<textelement formid="control24" query="2" param2="{$saturday}" date="{$_saturday}" param3="{$saturday2}" />
	<textelement formid="control28" query="2" param2="{$sunday}" date="{$_sunday}" param3="{$sunday2}" />
	<textelement formid="control5"  query="3" param2="{$monday}" date="{$_monday}" param3="{$monday2}" />
	<textelement formid="control9"  query="3" param2="{$tuesday}" date="{$_tuesday}" param3="{$tuesday2}" />
	<textelement formid="control13" query="3" param2="{$wednesday}" date="{$_wednesday}" param3="{$wednesday2}" />
	<textelement formid="control17" query="3" param2="{$thursday}" date="{$_thursday}" param3="{$thursday2}" />
	<textelement formid="control21" query="3" param2="{$friday}" date="{$_friday}" param3="{$friday2}" />
	<textelement formid="control25" query="3" param2="{$saturday}" date="{$_saturday}" param3="{$saturday2}" />
	<textelement formid="control29" query="3" param2="{$sunday}" date="{$_sunday}" param3="{$sunday2}" />
	<textelement formid="control6"  query="4" param2="{$monday}" date="{$_monday}" param3="{$monday2}" />
	<textelement formid="control10" query="4" param2="{$tuesday}" date="{$_tuesday}" param3="{$tuesday2}" />
	<textelement formid="control14" query="4" param2="{$wednesday}" date="{$_wednesday}" param3="{$wednesday2}" />
	<textelement formid="control18" query="4" param2="{$thursday}" date="{$_thursday}" param3="{$thursday2}" />
	<textelement formid="control22" query="4" param2="{$friday}" date="{$_friday}" param3="{$friday2}" />
	<textelement formid="control26" query="4" param2="{$saturday}" date="{$_saturday}" param3="{$saturday2}" />
	<textelement formid="control30" query="4" param2="{$sunday}" date="{$_sunday}" param3="{$sunday2}" />
	<textelement formid="control7"  query="5" param2="{$monday}" date="{$_monday}" param3="{$monday2}" />
	<textelement formid="control11" query="5" param2="{$tuesday}" date="{$_tuesday}" param3="{$tuesday2}" />
	<textelement formid="control15" query="5" param2="{$wednesday}" date="{$_wednesday}" param3="{$wednesday2}" />
	<textelement formid="control19" query="5" param2="{$thursday}" date="{$_thursday}" param3="{$thursday2}" />
	<textelement formid="control23" query="5" param2="{$friday}" date="{$_friday}" param3="{$friday2}" />
	<textelement formid="control27" query="5" param2="{$saturday}" date="{$_saturday}" param3="{$saturday2}" />
	<textelement formid="control31" query="5" param2="{$sunday}" date="{$_sunday}" param3="{$sunday2}" />
	<textelement formid="control32" query="1" param1="{$Bericht}" param2="{$monday}" date="{$_monday}" param3="{$monday2}" />
	<textelement formid="control33" query="1" param1="{$Bericht}" param2="{$tuesday}" date="{$_tuesday}" param3="{$tuesday2}" />
	<textelement formid="control34" query="1" param1="{$Bericht}" param2="{$wednesday}" date="{$_wednesday}" param3="{$wednesday2}" />
	<textelement formid="control35" query="1" param1="{$Bericht}" param2="{$thursday}" date="{$_thursday}" param3="{$thursday2}" />
	<textelement formid="control36" query="1" param1="{$Bericht}" param2="{$friday}" date="{$_friday}" param3="{$friday2}" />
	<textelement formid="control37" query="1" param1="{$Bericht}" param2="{$saturday}" date="{$_saturday}" param3="{$saturday2}" />
	<textelement formid="control41" query="0"><xsl:value-of select="util:formatDate($start,$weekFormat)" /></textelement>
	<textelement formid="control42" query="0"><xsl:value-of select="$_monday" /></textelement>
	<textelement formid="control43" query="0"><xsl:value-of select="$_sunday" /></textelement>
	<textelement formid="control44" query="1" param1="{$Bemerkung}" param2="{$monday}" date="{$_monday}" param3="{$monday2}" />
	<textelement formid="control45" query="1" param1="{$Bemerkung}" param2="{$tuesday}" date="{$_tuesday}" param3="{$tuesday2}" />
	<textelement formid="control46" query="1" param1="{$Bemerkung}" param2="{$wednesday}" date="{$_wednesday}" param3="{$wednesday2}" />
	<textelement formid="control47" query="1" param1="{$Bemerkung}" param2="{$thursday}" date="{$_thursday}" param3="{$thursday2}" />
	<textelement formid="control48" query="1" param1="{$Bemerkung}" param2="{$friday}" date="{$_friday}" param3="{$friday2}" />
	<textelement formid="control49" query="1" param1="{$Bemerkung}" param2="{$saturday}" date="{$_saturday}" param3="{$saturday2}" />
</control>
	</xsl:template>


</xsl:stylesheet>

