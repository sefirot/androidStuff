<?xml version="1.0" ?>
<xsl:stylesheet version = '1.0'
	xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
	xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" 
	xmlns:draw='urn:oasis:names:tc:opendocument:xmlns:drawing:1.0'
	xmlns:svg='urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0'
	xmlns:text='urn:oasis:names:tc:opendocument:xmlns:text:1.0'
	xmlns:xlink='http://www.w3.org/1999/xlink'
	xmlns:java="http://xml.apache.org/xalan/java"
	xmlns:redirect="http://xml.apache.org/xalan/redirect"
	xmlns:util="com.applang.Util"
	xmlns:util2="com.applang.Util2"
	xmlns:exslt="http://exslt.org/common"
	extension-element-prefixes="exslt util util2 redirect">
	
	<xsl:include href = "debug.xsl"/>
	
	<xsl:output method="xml" indent="no" omit-xml-declaration="yes" />
	
	<xsl:param name="mode" select="0" />
	<xsl:param name="debug" select="'no'" />
	<xsl:param name="filter" />
	
	<xsl:variable name="table.header">
		<tr>
			<th></th>
			<th>control</th>
			<th>x</th>
			<th>y</th>
			<th>width</th>
			<th>height</th>
		</tr>
	</xsl:variable>
	<xsl:variable name="table.row1">
		<tr>
			<th></th>
			<th>Input :</th>
			<th><input type="text" name="x" value="" /></th>
			<th><input type="text" name="y" value="" /></th>
			<th><input type="text" name="width" value="" /></th>
			<th><input type="text" name="height" value="" /></th>
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
		<xsl:variable name="filename">
			<xsl:call-template name="mask">
				<xsl:with-param name="pos" select="$pos" />
			</xsl:call-template>
		</xsl:variable>
		<redirect:write select="$filename" append="false">
			<html>
				<body>
					<form method="POST" action="{$page}">
						<table>
							<tr>
								<xsl:call-template name="submit"><xsl:with-param name="num" select="1" /><xsl:with-param name="nam" select="'update'" /></xsl:call-template>
								<xsl:call-template name="submit"><xsl:with-param name="num" select="2" /><xsl:with-param name="nam" select="'add'" /></xsl:call-template>
								<xsl:call-template name="submit"><xsl:with-param name="num" select="3" /><xsl:with-param name="nam" select="'delete'" /></xsl:call-template>
								<xsl:if test="not($first)">
									<td>
										<xsl:call-template name="pageswitch">
											<xsl:with-param name="pos" select="number($pos)-1" />
											<xsl:with-param name="switch" select="util2:getSetting('mask.action4', 'previous')" />
										</xsl:call-template>
									</td>
								</xsl:if>
								<xsl:if test="not($last)">
									<td>
										<xsl:call-template name="pageswitch">
											<xsl:with-param name="pos" select="number($pos)+1" />
											<xsl:with-param name="switch" select="util2:getSetting('mask.action5', 'next')" />
										</xsl:call-template>
									</td>
								</xsl:if>
							</tr>
						</table>
						<table id="controls">
							<xsl:copy-of select="exslt:node-set($table.row1)/*" />
							<xsl:copy-of select="exslt:node-set($table.header)/*" />
							<xsl:apply-templates mode="filter" />
							<tr>
								<td><input type="checkbox" name="frame" /></td>
								<td>frame</td>
								<xsl:variable name="prefix" select="concat('frame',$pos,'_')" />
								<xsl:variable name="name" select="concat($prefix,'image')" />
								<xsl:variable name="image" select="draw:frame[1]/draw:image/@xlink:href" />
								<td colspan="2" align="center">image : <input type="text" name="{$name}" value="{$image}" /></td>
								<xsl:variable name="nix" select="util:setMapping($name,$image)" />
								
								<xsl:variable name="name1" select="concat($prefix,'width')" />
								<xsl:variable name="width">
									<xsl:apply-templates select="draw:frame[1]/@svg:width" mode="units">
										<xsl:with-param name="strip" select="true()" />
									</xsl:apply-templates>
								</xsl:variable>
								<td name="{$name1}" align="center"><xsl:value-of select="$width" /></td>
								<xsl:variable name="nix1" select="util:setMapping($name1,$width)" />
								
								<xsl:variable name="name2" select="concat($prefix,'height')" />
								<xsl:variable name="height">
									<xsl:apply-templates select="draw:frame[1]/@svg:height" mode="units">
										<xsl:with-param name="strip" select="true()" />
									</xsl:apply-templates>
								</xsl:variable>
								<td name="{$name2}" align="center"><xsl:value-of select="$height" /></td>
								<xsl:variable name="nix2" select="util:setMapping($name2,$height)" />
							</tr>
						</table>
					</form>
				</body>
			</html>
		</redirect:write>
	</xsl:template>
	
	<xsl:template name="mask">
		<xsl:param name="pos" />
		<xsl:param name="fullpath" select="true()" />
		<xsl:variable name="name" select="concat('page',$pos,'.html')" />
		<xsl:choose>
			<xsl:when test="$fullpath">
				<xsl:value-of select="util2:tempPath(util2:getSetting('temp.subdir','/tmp'),$name)" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$name" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="submit">
		<xsl:param name="num" />
		<xsl:param name="nam" />
		<xsl:variable name="action" select="util2:getSetting(concat('mask.action',$num), $nam)" />
		<xsl:if test="$action != $nam">
			<xsl:variable name="name" select="concat('action',$num)" />
			<td><input type='SUBMIT' name="{$name}" value="{$action}" /></td>
		</xsl:if>
	</xsl:template>

	<xsl:template name="pageswitch">
		<xsl:param name="pos" />
		<xsl:param name="switch" />
		<xsl:variable name="page">
			<xsl:call-template name="mask">
				<xsl:with-param name="pos" select="$pos" />
				<xsl:with-param name="fullpath" select="false()" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="pageswitch" select="util2:getSetting('mask.pageswitch', '')" />
		<xsl:choose>
			<xsl:when test="$pageswitch = 'hyperlink'">
				<a href="{$page}"><xsl:value-of select="$switch" /></a>
			</xsl:when>
			<xsl:when test="$pageswitch = 'submit'">
				<input type='SUBMIT' name="{$switch}" value="{$switch}" />
				<input type='HIDDEN' name="page" value="{$page}" />
			</xsl:when>
			<xsl:otherwise>
				<a href="{$page}"><input type='BUTTON' value="{$switch}" /></a>
			</xsl:otherwise>
		</xsl:choose>
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
					<td><input type="checkbox" name="{$ctrl}" /></td>
					<xsl:for-each select="exslt:node-set($table.header)/tr/th">
						<xsl:variable name="th" select="." />
						<xsl:for-each select="$this/@*">
							<xsl:if test="substring-after(name(),':') = $th">
								<xsl:variable name="value">
									<xsl:apply-templates select="." mode="units">
										<xsl:with-param name="strip" select="true()" />
									</xsl:apply-templates>
								</xsl:variable>
								<xsl:element name="td">
									<xsl:choose>
										<xsl:when test="$th = 'control'">
											<xsl:value-of select="." />
										</xsl:when>
										<xsl:otherwise>
											<xsl:variable name="name" select="concat($ctrl,'_',$th)" />
											<xsl:attribute name="name"><xsl:value-of select="$name" /></xsl:attribute>
											<xsl:attribute name="align"><xsl:value-of select="'center'" /></xsl:attribute>
											<xsl:value-of select="$value" />
											<xsl:variable name="nix" select="util:setMapping($name,$value)" />
										</xsl:otherwise>
									</xsl:choose>
								</xsl:element>
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
	
	<xsl:template match="text:p">
		<xsl:variable name="filename">
			<xsl:call-template name="mask">
				<xsl:with-param name="pos" select="1+count(preceding-sibling::*[name()='text:p'])" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:call-template name="render">
			<xsl:with-param name="param" select="document($filename)" />
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template match="draw:control">
		<xsl:param name="param" />
		<xsl:variable name="ctrl" select="string(@draw:control)" />
		<xsl:variable name="table" select="exslt:node-set($param)//table[@id='controls']" />
		<xsl:variable name="tr">
			<xsl:for-each select="$table/*">
				<xsl:if test="./td=$ctrl">
					<xsl:copy-of select="." />
				</xsl:if>
			</xsl:for-each>
		</xsl:variable>
		<xsl:copy>
			<xsl:for-each select="@*">
				<xsl:variable name="name" select="name()" />
				<xsl:attribute name="{$name}">
					<xsl:variable name="localname" select="substring-after($name,':')" />
					<xsl:choose>
						<xsl:when test="exslt:node-set($table.header)/tr/th[text()=$localname]">
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
											<xsl:variable name="td" select="exslt:node-set($tr)/tr/td[$pos]" />
											<xsl:variable name="name" select="string($td/@name)" />
											<xsl:variable name="value" select="string($td)" />
											<xsl:variable name="value3" select="util:getMapping($name)" />
<xsl:call-template name="debug.out">
	<xsl:with-param name="hide" select="true()" />
	<xsl:with-param name="text" select="concat($name,$tab,$tab,$tab,$tab)" />
	<xsl:with-param name="object" select="$value" />
	<xsl:with-param name="text2" select="concat($tab,$tab,'oldValue')" />
	<xsl:with-param name="object2" select="$oldValue" />
	<xsl:with-param name="deep" select="false()" />
</xsl:call-template>
											<xsl:choose>
												<xsl:when test="$mode = 2 and $value">
													<xsl:value-of select="concat($value,$units)" />
												</xsl:when>
												<xsl:when test="$mode = 3 and $value3">
													<xsl:value-of select="concat($value3,$units)" />
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
