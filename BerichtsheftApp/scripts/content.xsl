<xsl:stylesheet version = '1.0'
	xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
	xmlns:form='urn:oasis:names:tc:opendocument:xmlns:form:1.0'
	xmlns:java="http://xml.apache.org/xalan/java"
	xmlns:sql="org.apache.xalan.lib.sql.XConnection"
	xmlns:util="com.applang.Util"
	xmlns:exslt="http://exslt.org/common"
	extension-element-prefixes="sql util exslt">
	
	<xsl:include href = "debug.xsl"/>
	
	<xsl:output method="xml" indent="no" omit-xml-declaration="no" />
 
	<xsl:param name="inputfile" />
	<xsl:param name="input" select="document($inputfile)/*"/>
 
	<xsl:param name="controlinfo" select="/*"/>
 
	<xsl:param name="dbfile" />
	<xsl:param name="dbinfo">
		<xsl:choose>
			<xsl:when test="$dbfile">
				<dbdriver>org.sqlite.JDBC</dbdriver>
				<dburl><xsl:value-of select="concat('jdbc:sqlite:', $dbfile)" /></dburl>
				<user />
				<password/>
			</xsl:when>
			<xsl:when test="$controlinfo">
				<xsl:copy-of select="$controlinfo//DBINFO/*" />
			</xsl:when>
		</xsl:choose>
	</xsl:param>
	
	<xsl:param name="year">
		<xsl:if test="$controlinfo">
			<xsl:value-of select="$controlinfo//DATE/@year" />
		</xsl:if>
	</xsl:param>
	<xsl:param name="weekInYear">
		<xsl:if test="$controlinfo">
			<xsl:value-of select="$controlinfo//DATE/@weekInYear" />
		</xsl:if>
	</xsl:param>
	<xsl:param name="dayInWeek">
		<xsl:if test="$controlinfo">
			<xsl:value-of select="$controlinfo//DATE/@dayInWeek" />
		</xsl:if>
	</xsl:param>
	
	<xsl:param name="debug" select="no" />
	<xsl:param name="filter" />
	
	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="$debug = 'yes'">
				<xsl:call-template name="debug.out">
					<xsl:with-param name="text" select="'CONTROL'" />
					<xsl:with-param name="object" select="$controlinfo" />
					<xsl:with-param name="append" select="false()" />
				</xsl:call-template>
				<xsl:call-template name="debug.out">
					<xsl:with-param name="text" select="'filter'" />
					<xsl:with-param name="object" select="$filter" />
					<xsl:with-param name="append" select="true()" />
				</xsl:call-template>
				
				<xsl:apply-templates select="$input" mode="filter" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="$input" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="place_value">
		<xsl:param name="params" />
		<xsl:if test="$dbinfo and $controlinfo">
			<xsl:variable name="db" select="sql:new()"/>
			<xsl:if test="not(sql:connect($db, $dbinfo))" >
				<xsl:message>Error connecting to the database</xsl:message>
				<xsl:copy-of select="sql:getError($db)/ext-error" />
			</xsl:if>
		
			<xsl:value-of select="sql:addParameterFromElement($db, $params)"/>
			<xsl:variable name="query" select="$controlinfo//QUERY/@statement"/>
			<xsl:variable name="typeinfo" select="$controlinfo//QUERY/@typeinfo" />
			<xsl:variable name="table" select='sql:pquery($db, $query, $typeinfo )'/>
			<xsl:if test="not($table)" >
				<xsl:message>Error in query</xsl:message>
				<xsl:copy-of select="sql:getError($db)/ext-error" />
			</xsl:if>
			
			<xsl:apply-templates select="$table/sql/row-set"/>
			
			<xsl:value-of select="sql:close($db)"/>
		</xsl:if>
	</xsl:template>

	<xsl:template match="row-set">
		<xsl:apply-templates select="./row"/>
	</xsl:template>

	<xsl:template match="row">
		<xsl:apply-templates select="./col"/>
	</xsl:template>

	<xsl:template match="col">
		<xsl:value-of select="text()"/>
	</xsl:template>

	<xsl:template match="form:*">
		<xsl:variable name="id" select="@form:id" />
		<xsl:choose>
			<xsl:when test="$controlinfo and $id and $controlinfo//textelement[@formid = $id]">
				<xsl:call-template name="render_with_contents">
					<xsl:with-param name="formid" select="$id" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="render" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="kind_of_value">
		<xsl:param name="textelement" />
		<xsl:variable name="weekday" select="$textelement/@weekday" />
		<xsl:choose>
			<xsl:when test="$textelement and not($weekday)">
				<xsl:value-of select="1" />
			</xsl:when>
			<xsl:when test="$textelement and $weekday and $dayInWeek and util:matches($weekday,$dayInWeek)">
				<xsl:value-of select="2" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="0" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="render_with_contents">
		<xsl:param name="formid" />
		<xsl:variable name="textelement" select="$controlinfo//textelement[@formid = $formid]" />
		<xsl:copy>
			<xsl:variable name="kind">
				<xsl:call-template name="kind_of_value">
					<xsl:with-param name="textelement" select="$textelement" />
				</xsl:call-template>
			</xsl:variable>
			<xsl:for-each select="@*">
				<xsl:if test="not(name(.) = 'form:id') and not(name(.) = 'form:current-value') or number($kind) &lt; 1">
					<xsl:copy-of select="." />
				</xsl:if>
			</xsl:for-each>
			<xsl:copy-of select="@form:id" />
			<xsl:choose>
				<xsl:when test="number($kind) = 2">
					<xsl:variable name="millis" select="util:dateInMillis($year,$weekInYear,$textelement/@weekday)" />
					<xsl:attribute name="form:current-value">
						<xsl:call-template name="place_value">
							<xsl:with-param name="params">
								<title><xsl:value-of select="$textelement/@title" /></title>
								<created><xsl:value-of select="$millis" /></created>
							</xsl:with-param>
						</xsl:call-template>
					</xsl:attribute>
				</xsl:when>
				<xsl:when test="number($kind) = 1">
					<xsl:attribute name="form:current-value">
						<xsl:value-of select="$textelement" />
					</xsl:attribute>
				</xsl:when>
			</xsl:choose>
			<xsl:copy-of select="node()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template name="debug.formelement">
		<xsl:param name="id" />
		<xsl:variable name="formelement">
			<xsl:call-template name="render_with_contents">
				<xsl:with-param name="formid" select="$id" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:call-template name="debug.out">
			<xsl:with-param name="text" select="name()" />
			<xsl:with-param name="object" select="$formelement" />
			<xsl:with-param name="descendants" select="false()" />
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="form:*" mode="filter">
		<xsl:variable name="id" select="@form:id" />
		<xsl:choose>
			<xsl:when test="$filter">
				<xsl:if test="util:matches(name(),$filter)">
					<xsl:call-template name="debug.formelement">
						<xsl:with-param name="id" select="$id" />
					</xsl:call-template>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<xsl:if test="$id and $controlinfo and $controlinfo//textelement[@formid = $id]">
					<xsl:call-template name="debug.formelement">
						<xsl:with-param name="id" select="$id" />
					</xsl:call-template>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:apply-templates mode="filter" />
	</xsl:template>

</xsl:stylesheet> 
