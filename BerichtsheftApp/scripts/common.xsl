<?xml version="1.0" ?>
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">
	
	<xsl:variable name="tab"><xsl:text>&#x0009;</xsl:text></xsl:variable>
	<xsl:variable name="newline"><xsl:text>&#x000A;</xsl:text></xsl:variable>
	
	<xsl:template name="println">
		<xsl:param name="line"/>
		<xsl:param name="indent" select="0"/>
		<xsl:call-template name="repeat">
			<xsl:with-param name="times" select="$indent"/>
			<xsl:with-param name="str" select="$tab"/>
		</xsl:call-template>
		<xsl:value-of select="concat($line, $newline)" />
	</xsl:template>
	
	<xsl:template name="repeat">
		<xsl:param name="times" select="1"/>
		<xsl:param name="str" select="'.'"/>
		<xsl:if test="$times > 0">
			<!-- <xsl:value-of select="concat($times, $str)" /> -->
			<xsl:value-of select="$str" />
			<xsl:call-template name="repeat">
				<xsl:with-param name="times" select="$times - 1"/>
				<xsl:with-param name="str" select="$str"/>
			</xsl:call-template>
        </xsl:if>
	</xsl:template>

	<xsl:template name="render" match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="/">
		<xsl:apply-templates />
	</xsl:template>

</xsl:stylesheet>

