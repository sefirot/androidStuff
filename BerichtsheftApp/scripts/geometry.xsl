<?xml version="1.0" ?>
<xsl:stylesheet version = '1.0'
	xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
	xmlns:draw='urn:oasis:names:tc:opendocument:xmlns:drawing:1.0'
	xmlns:svg='urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0'
	xmlns:text='urn:oasis:names:tc:opendocument:xmlns:text:1.0'
	xmlns:java="http://xml.apache.org/xalan/java"
	xmlns:redirect="http://xml.apache.org/xalan/redirect"
	xmlns:util="com.applang.Util"
	xmlns:exslt="http://exslt.org/common"
	extension-element-prefixes="exslt util redirect">
	
	<xsl:include href = "debug.xsl"/>
	
	<xsl:output method="xml" indent="no" omit-xml-declaration="yes" />
 
	<xsl:param name="controlfile" />
	<xsl:param name="controlinfo" select="document($controlfile)/*"/>
	
	<xsl:param name="mode" select="0" />
	<xsl:param name="debug" select="'no'" />
	<xsl:param name="filter" />
	
	<xsl:variable name="table.header">
		<tr>
			<th>control</th>
			<th>x</th>
			<th>y</th>
			<th>width</th>
			<th>height</th>
		</tr>
	</xsl:variable>

	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="$debug = 'yes'">
				<xsl:call-template name="begin.debug" />
				<xsl:call-template name="debug.out">
					<xsl:with-param name="text" select="'filter'" />
					<xsl:with-param name="object" select="$filter" />
				</xsl:call-template>
				<xsl:apply-templates mode="filter" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="$mode = 1">
						<xsl:for-each select="//text:p">
							<xsl:call-template name="page" />
						</xsl:for-each>
					</xsl:when>
					<xsl:otherwise>
						<xsl:apply-templates />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="page">
		<xsl:variable name="pos" select="position()" />
		<xsl:variable name="first" select="$pos = 1" />
		<xsl:variable name="last" select="$pos = last()" />
		<xsl:variable name="page" select="concat('page',$pos)" />
		<xsl:variable name="filename" select="util:tempPath('berichtsheft',concat($page,'.html'))" />
		<redirect:write select="$filename" append="false">
			<html>
				<body>
					<form method="POST" action="{$page}">
						<table>
							<tr>
								<td><input type='SUBMIT' value='Accept' /></td>
								<xsl:if test="not($first)">
									<td><a href="previous">previous</a></td>
								</xsl:if>
								<xsl:if test="not($last)">
									<td><a href="next">next</a></td>
								</xsl:if>
							</tr>
						</table>
						<table id="controls" border="1">
							<xsl:copy-of select="exslt:node-set($table.header)/*" />
							<xsl:apply-templates mode="filter" />
						</table>
					</form>
				</body>
			</html>
		</redirect:write>
	</xsl:template>

	<xsl:template match="draw:control" mode="filter">
		<xsl:choose>
			<xsl:when test="$debug = 'yes'">
				<xsl:apply-templates select="@*" mode="filter" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="this" select="." />
				<xsl:variable name="ctrl" select="@draw:control" />
				<tr>
					<xsl:for-each select="exslt:node-set($table.header)/tr/th">
						<xsl:variable name="th" select="." />
						<xsl:for-each select="$this/@*">
							<xsl:if test="substring-after(name(),':') = $th">
								<xsl:variable name="value">
									<xsl:apply-templates select="." mode="units">
										<xsl:with-param name="strip" select="true()" />
									</xsl:apply-templates>
								</xsl:variable>
								<td>
									<xsl:choose>
										<xsl:when test="$th = 'control'">
											<xsl:value-of select="." />
										</xsl:when>
										<xsl:otherwise>
											<xsl:variable name="id" select="concat($ctrl,'_',$th)" />
											<input id="{$id}" name="{$id}" type="text" value="{$value}" />
											<xsl:variable name="nix" select="util:setMapping($id,$value)" />
										</xsl:otherwise>
									</xsl:choose>
								</td>
							</xsl:if>
						</xsl:for-each>
					</xsl:for-each>
				</tr>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:apply-templates mode="filter" />
	</xsl:template>
	
	<xsl:template match="@svg:*|@draw:control" mode="filter">
		<xsl:call-template name="debug" />
	</xsl:template>
	
	<xsl:template match="draw:control">
		<xsl:variable name="ctrl" select="string(@draw:control)" />
		<xsl:variable name="tr" select="exslt:node-set($controlinfo)//table[@id='controls']/tr[contains(string(.),$ctrl)]" />
		<xsl:copy>
			<xsl:for-each select="@*">
				<xsl:variable name="name" select="name()" />
				<xsl:attribute name="{$name}">
					<xsl:variable name="localname" select="substring-after($name,':')" />
					<xsl:choose>
						<xsl:when test="contains(string(exslt:node-set($table.header)/tr),$localname)">
							<xsl:variable name="oldValue" select="." />
							<xsl:choose>
								<xsl:when test="$localname = 'control'">
									<xsl:value-of select="$oldValue" />
								</xsl:when>
								<xsl:otherwise>
									<xsl:variable name="units">
										<xsl:apply-templates select="." mode="units" />
									</xsl:variable>
									<xsl:for-each select="exslt:node-set($table.header)/tr/th">
										<xsl:variable name="th" select="." />
										<xsl:if test="$localname = $th">
											<xsl:variable name="pos" select="position()" />
											<xsl:variable name="id" select="exslt:node-set($tr)/td[$pos]/input/@id" />
											<xsl:variable name="value" select="concat(util:getMapping($id),$units)" />
<xsl:if test="$value != $oldValue">
	<xsl:call-template name="debug.out">
		<xsl:with-param name="text" select="$id" />
		<xsl:with-param name="object" select="$value" />
		<xsl:with-param name="text2" select="$localname" />
		<xsl:with-param name="object2" select="string($oldValue)" />
	</xsl:call-template>
</xsl:if>
											<xsl:choose>
												<xsl:when test="$value">
													<xsl:value-of select="$value" />
												</xsl:when>
												<xsl:otherwise>
													<xsl:value-of select="$oldValue" />
												</xsl:otherwise>
											</xsl:choose>
										</xsl:if>
									</xsl:for-each>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="." />
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
			</xsl:for-each>
			<xsl:apply-templates select="node()" />
		</xsl:copy>
	</xsl:template>
	
</xsl:stylesheet> 
