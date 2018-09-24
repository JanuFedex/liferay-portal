<%--
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
--%>

<%@ include file="/init.jsp" %>

<%
OrganizationScreenNavigationDisplayContext organizationScreenNavigationDisplayContext = (OrganizationScreenNavigationDisplayContext)request.getAttribute(UsersAdminWebKeys.ORGANIZATION_SCREEN_NAVIGATION_DISPLAY_CONTEXT);

Organization organization = organizationScreenNavigationDisplayContext.getOrganization();
%>

<aui:input name="<%= Constants.CMD %>" type="hidden" value="<%= (organization == null) ? Constants.ADD : Constants.UPDATE %>" />

<div class="sheet-section">
	<h3 class="sheet-subtitle"><liferay-ui:message key="organization-information" /></h3>

	<liferay-util:include page="/organization/details.jsp" servletContext="<%= application %>" />
</div>

<div class="sheet-section">
	<h3 class="sheet-subtitle"><liferay-ui:message key="more-information" /></h3>

	<liferay-util:include page="/organization/categorization.jsp" servletContext="<%= application %>" />

	<liferay-util:include page="/organization/comments.jsp" servletContext="<%= application %>" />
</div>

<c:if test="<%= CustomFieldsUtil.hasVisibleCustomFields(company.getCompanyId(), User.class) %>">
	<div class="sheet-section">
		<h4 class="sheet-tertiary-title"><liferay-ui:message key="custom-fields" /></h4>

		<liferay-util:include page="/organization/custom_fields.jsp" servletContext="<%= application %>" />
	</div>
</c:if>

<aui:script>
	function <portlet:namespace />createURL(href, value, onclick) {
		return '<a href="' + href + '"' + (onclick ? ' onclick="' + onclick + '" ' : '') + '>' + value + '</a>';
	}
</aui:script>