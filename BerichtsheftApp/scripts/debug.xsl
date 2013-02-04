<?xml version="1.0" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:redirect="http://xml.apache.org/xalan/redirect"
	xmlns:exslt="http://exslt.org/common"
	xmlns:java="http://xml.apache.org/xalan/java"
	xmlns:util="com.applang.Util"
	extension-element-prefixes="redirect exslt java util">
	
	<xsl:include href = "common.xsl"/>

	<xsl:output method="text"/>
	
	<xsl:param name="debug_out">/tmp/debug.out</xsl:param>
	
	<xsl:template name="debug.out">
		<xsl:param name="append" select="true()" />
		<xsl:param name="text" select="''" />
		<xsl:param name="text2" select="''" />
		<xsl:param name="textOnly" select="false()" />
		<xsl:param name="object" />
		<xsl:param name="object2" />
		<xsl:param name="descendants" select="true()" />
		<redirect:write select="$debug_out" append="{$append}">
			<xsl:choose>
				<xsl:when test="$textOnly">
					<xsl:if test="$text">
						<xsl:value-of select="$text" />	
					</xsl:if>
					<xsl:if test="$text2">
						<xsl:value-of select="$text2" />	
					</xsl:if>
				</xsl:when>
				<xsl:otherwise>
					<xsl:if test="$object">
						<xsl:call-template name="debug.object">
							<xsl:with-param name="text" select="$text" />
							<xsl:with-param name="object" select="$object" />
							<xsl:with-param name="descendants" select="$descendants" />
						</xsl:call-template>
					</xsl:if>
					<xsl:if test="$object2">
						<xsl:call-template name="debug.object">
							<xsl:with-param name="text" select="$text2" />
							<xsl:with-param name="object" select="$object2" />
							<xsl:with-param name="descendants" select="$descendants" />
						</xsl:call-template>
					</xsl:if>
				</xsl:otherwise>
			</xsl:choose>
		</redirect:write>
	</xsl:template>
	
	<xsl:template name="debug.object">
		<xsl:param name="text" />
		<xsl:param name="indent" select="0"/>
		<xsl:param name="object" />
		<xsl:param name="descendants" select="true()" />
		<xsl:param name="type" select="exslt:object-type($object)" />
		<xsl:variable name="info">
			<xsl:if test="string-length($text) > 0">
				<xsl:value-of select="concat($text,' : ')" />
			</xsl:if>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$type = 'node-set'">
				<xsl:call-template name="println">
					<xsl:with-param name="indent" select="$indent"/>
					<xsl:with-param name="line" select="concat($info,$type,'(',count($object/*),')')"/>
				</xsl:call-template>
				<xsl:choose>
					<xsl:when test="string-length(name($object)) > 0">
						<xsl:apply-templates select="$object" mode="debug.element">
							<xsl:with-param name="indent" select="1 + $indent" />
							<xsl:with-param name="descendants" select="$descendants" />
						</xsl:apply-templates>
					</xsl:when>
					<xsl:otherwise>
						<xsl:for-each select="$object/*">
							<xsl:call-template name="debug.object">
								<xsl:with-param name="object" select="." />
								<xsl:with-param name="indent" select="1 + $indent" />
								<xsl:with-param name="descendants" select="$descendants" />
							</xsl:call-template>
						</xsl:for-each>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="$type = 'RTF'"><!-- result tree fragment -->
				<xsl:variable name="node_set" select="exslt:node-set($object)" />
				<xsl:choose>
					<xsl:when test="count($node_set/*) > 0">
						<xsl:call-template name="debug.object">
							<xsl:with-param name="text" select="concat($info,$type)"/>
							<xsl:with-param name="indent" select="$indent" />
							<xsl:with-param name="object" select="$node_set/*" />
							<xsl:with-param name="descendants" select="$descendants" />
						</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="debug.object">
							<xsl:with-param name="text" select="concat($info,$type)"/>
							<xsl:with-param name="indent" select="$indent" />
							<xsl:with-param name="object" select="$node_set" />
							<xsl:with-param name="descendants" select="$descendants" />
						</xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="println">
					<xsl:with-param name="indent" select="$indent"/>
					<xsl:with-param name="line" select="concat($info,$type,' : ',$object)"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="@*" mode="debug.element">
		<xsl:param name="indent" />
		<xsl:call-template name="println">
			<xsl:with-param name="indent" select="$indent"/>
			<xsl:with-param name="line" select="concat(name(.),' = ',.)"/>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template match="*" mode="debug.element">
		<xsl:param name="indent" />
		<xsl:param name="descendants" select="true()" />
		<xsl:call-template name="println">
			<xsl:with-param name="indent" select="$indent"/>
			<xsl:with-param name="line" select="name()"/>
		</xsl:call-template>
		<xsl:if test="not(util:isWhiteSpace(text()))">
			<xsl:call-template name="println">
				<xsl:with-param name="indent" select="$indent + 1"/>
				<xsl:with-param name="line" select="text()"/>
			</xsl:call-template>
		</xsl:if>
		<xsl:for-each select="attribute::node()">
			<xsl:apply-templates select="." mode="debug.element">
				<xsl:with-param name="indent" select="$indent + 1"/>
			</xsl:apply-templates>
		</xsl:for-each>
		<xsl:if test="$descendants">
			<xsl:for-each select="*">
				<xsl:if test="string-length(name(.)) > 0">
					<xsl:apply-templates select="." mode="debug.element">
						<xsl:with-param name="indent" select="$indent + 1" />
						<xsl:with-param name="descendants" select="$descendants" />
					</xsl:apply-templates>
				</xsl:if>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="debug">
		<xsl:call-template name="debug.out">
			<xsl:with-param name="text" select="name()"/>
			<xsl:with-param name="object" select="."/>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:variable name="content">
		<debug>
			<one type="number">1</one>
			<two>two</two>
			<three>
				<one>2</one>
				<two>one</two>
				<three>true</three>
				<four>3.1415</four>
			</three>
			<four>false</four>
		</debug> 
	</xsl:variable>
	
	<xsl:template match="/">
		<xsl:call-template name="debug.out">
			<xsl:with-param name="append" select="false()"/>
			<xsl:with-param name="text" select="concat('now : ', util:now(), $newline)"/>
			<xsl:with-param name="textOnly" select="true()"/>
		</xsl:call-template>
		<xsl:call-template name="debug.out">
			<xsl:with-param name="object" select="$content"/>
		</xsl:call-template>
	</xsl:template>
	
</xsl:stylesheet>

