<%@page buffer="none" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="cms" uri="http://www.opencms.org/taglib/cms" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<cms:formatter var="content" val="value" rdfa="rdfa">
<div>
	<%-- Concatenate the configured container types. --%>
	<c:forEach var="type" items="${value.Types.valueList.ContainerType}"><c:set var="types" value="${types}${type.stringValue}," /></c:forEach>
	<cms:container name="modelcontainer" type="${types}" />
</div>
</cms:formatter>