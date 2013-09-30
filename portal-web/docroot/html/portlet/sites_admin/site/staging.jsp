<%--
/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
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

<%@ include file="/html/portlet/sites_admin/init.jsp" %>

<%
Group liveGroup = (Group)request.getAttribute("site.liveGroup");
long liveGroupId = ((Long)request.getAttribute("site.liveGroupId")).longValue();
UnicodeProperties liveGroupTypeSettings = (UnicodeProperties)request.getAttribute("site.liveGroupTypeSettings");

LayoutSet privateLayoutSet = LayoutSetLocalServiceUtil.getLayoutSet(liveGroup.getGroupId(), true);
LayoutSet publicLayoutSet = LayoutSetLocalServiceUtil.getLayoutSet(liveGroup.getGroupId(), false);

boolean stagedLocally = liveGroup.isStaged() && !liveGroup.isStagedRemotely();
boolean stagedRemotely = liveGroup.isStaged() && !stagedLocally;

Group stagingGroup = null;
long stagingGroupId = 0;

if (stagedLocally) {
	stagingGroup = liveGroup.getStagingGroup();
	stagingGroupId = stagingGroup.getGroupId();
}
%>

<c:if test="<%= stagedLocally && (BackgroundTaskLocalServiceUtil.getBackgroundTasksCount(liveGroupId, LayoutStagingBackgroundTaskExecutor.class.getName(), false) > 0) %>">
	<liferay-portlet:renderURL portletName="<%= PortletKeys.LAYOUTS_ADMIN %>" var="publishProcessesURL" windowState="<%= LiferayWindowState.POP_UP.toString() %>">
		<portlet:param name="<%= Constants.CMD %>" value="view_processes" />
		<portlet:param name="struts_action" value="/layouts_admin/publish_layouts" />
		<portlet:param name="<%= SearchContainer.DEFAULT_CUR_PARAM %>" value="<%= ParamUtil.getString(request, SearchContainer.DEFAULT_CUR_PARAM) %>" />
		<portlet:param name="<%= SearchContainer.DEFAULT_DELTA_PARAM %>" value="<%= ParamUtil.getString(request, SearchContainer.DEFAULT_DELTA_PARAM) %>" />
		<portlet:param name="groupId" value="<%= String.valueOf(stagingGroupId) %>" />
		<portlet:param name="liveGroupId" value="<%= String.valueOf(liveGroupId) %>" />
		<portlet:param name="localPublishing" value="<%= String.valueOf(stagedLocally) %>" />
	</liferay-portlet:renderURL>

	<div class="alert alert-block">
		<liferay-ui:message key="an-inital-staging-publication-is-in-progress" />

		<a id="<portlet:namespace />publishProcessesLink"><liferay-ui:message key="the-status-of-the-publication-can-be-checked-on-the-publish-screen" /></a>
	</div>

	<aui:script use="aui-base">
		var publishProcessesLink = A.one('#<portlet:namespace />publishProcessesLink');

		publishProcessesLink.on(
			'click',
			function(event) {
				Liferay.Util.openWindow(
					{
						id: 'publishProcesses',
						title: Liferay.Language.get('initial-publication'),
						uri: '<%= HtmlUtil.escapeJS(publishProcessesURL.toString()) %>'
					}
				);
			}
		);
	</aui:script>
</c:if>

<liferay-ui:error-marker key="errorSection" value="staging" />

<c:choose>
	<c:when test="<%= privateLayoutSet.isLayoutSetPrototypeLinkActive() || publicLayoutSet.isLayoutSetPrototypeLinkActive() %>">
		<div class="alert alert-info">
			<liferay-ui:message key="staging-cannot-be-used-for-this-site-because-the-propagation-of-changes-from-the-site-template-is-enabled" />
			<c:choose>
				<c:when test="<%= PortalPermissionUtil.contains(permissionChecker, ActionKeys.UNLINK_LAYOUT_SET_PROTOTYPE) %>">
					<liferay-ui:message key="change-the-configuration-in-the-details-section" />
				</c:when>
				<c:otherwise>
					<liferay-ui:message key="contact-your-administrator-to-change-the-configuration" />
				</c:otherwise>
			</c:choose>
		</div>
	</c:when>
	<c:when test="<%= GroupPermissionUtil.contains(permissionChecker, liveGroupId, ActionKeys.MANAGE_STAGING) %>">

		<liferay-ui:error exception="<%= LocaleException.class %>">

			<%
			LocaleException le = (LocaleException)errorException;
			%>

			<c:if test="<%= le.getType() == LocaleException.TYPE_EXPORT_IMPORT %>">
				<liferay-ui:message arguments="<%= new String[] {StringUtil.merge(le.getSourceAvailableLocales(), StringPool.COMMA_AND_SPACE), StringUtil.merge(le.getTargetAvailableLocales(), StringPool.COMMA_AND_SPACE)} %>" key="the-default-language-x-does-not-match-the-portal's-available-languages-x" />
			</c:if>
		</liferay-ui:error>

		<liferay-ui:error exception="<%= SystemException.class %>">

			<%
			SystemException se = (SystemException)errorException;
			%>

			<liferay-ui:message key="<%= se.getMessage() %>" />
		</liferay-ui:error>

		<div class="staging-types" id="<portlet:namespace />stagingTypes">
			<aui:field-wrapper label="staging-type">
				<aui:input checked="<%= !liveGroup.isStaged() %>" id="none" label="none" name="stagingType" type="radio" value="<%= StagingConstants.TYPE_NOT_STAGED %>" />

				<aui:input checked="<%= stagedLocally %>" helpMessage="staging-type-local" id="local" label="local-live" name="stagingType" type="radio" value="<%= StagingConstants.TYPE_LOCAL_STAGING %>" />

				<aui:input checked="<%= stagedRemotely %>" helpMessage="staging-type-remote" id="remote" label="remote-live" name="stagingType" type="radio" value="<%= StagingConstants.TYPE_REMOTE_STAGING %>" />
			</aui:field-wrapper>
		</div>

		<%
		boolean showRemoteOptions = stagedRemotely;

		int stagingType = ParamUtil.getInteger(request, "stagingType");

		if (stagingType == StagingConstants.TYPE_REMOTE_STAGING) {
			showRemoteOptions = true;
		}
		%>

		<div class="<%= showRemoteOptions ? StringPool.BLANK : "hide" %> staging-section" id="<portlet:namespace />remoteStagingOptions">
			<br />

			<liferay-ui:error exception="<%= AuthException.class %>">

				<%
				AuthException ae = (AuthException)errorException;
				%>

				<c:if test="<%= ae instanceof RemoteAuthException %>">

					<%
					RemoteAuthException rae = (RemoteAuthException)errorException;
					%>

					<liferay-ui:message arguments='<%= "<em>" + rae.getURL() + "</em>" %>' key="an-unexpected-error-occurred-in-the-remote-server-at-x" />
				</c:if>

				<c:if test="<%= ae.getType() == AuthException.INTERNAL_SERVER_ERROR %>">
					<liferay-ui:message key="internal-server-error" />
				</c:if>

				<c:if test="<%= ae.getType() == AuthException.NO_SHARED_SECRET %>">
					<liferay-ui:message key="the-tunneling-servlet-shared-secret-is-not-set" />
				</c:if>

				<c:if test="<%= ae.getType() == AuthException.INVALID_SHARED_SECRET %>">
					<liferay-ui:message key="the-tunneling-servlet-shared-secret-must-be-16,-32-or-64-characters-long" />
				</c:if>

				<c:if test="<%= ae.getType() == RemoteAuthException.WRONG_SHARED_SECRET %>">
					<liferay-ui:message key="the-tunneling-servlet-shared-secrets-do-not-match" />
				</c:if>

			</liferay-ui:error>

			<liferay-ui:error exception="<%= RemoteExportException.class %>">

				<%
				RemoteExportException ree = (RemoteExportException)errorException;
				%>

				<c:if test="<%= ree.getType() == RemoteExportException.BAD_CONNECTION %>">
					<liferay-ui:message arguments="<%= ree.getURL() %>" key="there-was-a-bad-connection-with-the-remote-server-at-x" />
				</c:if>

				<c:if test="<%= ree.getType() == RemoteExportException.NO_GROUP %>">
					<liferay-ui:message arguments="<%= ree.getGroupId() %>" key="no-site-exists-on-the-remote-server-with-site-id-x" />
				</c:if>

				<c:if test="<%= ree.getType() == RemoteExportException.NO_PERMISSIONS %>">
					<liferay-ui:message arguments="<%= ree.getGroupId() %>" key="you-do-not-have-permissions-to-edit-the-site-with-id-x-on-the-remote-server" />
				</c:if>
			</liferay-ui:error>

			<aui:fieldset label="remote-live-connection-settings">
				<liferay-ui:error exception="<%= RemoteOptionsException.class %>">

					<%
					RemoteOptionsException roe = (RemoteOptionsException)errorException;
					%>

					<c:if test="<%= roe.getType() == RemoteOptionsException.REMOTE_ADDRESS %>">
						<liferay-ui:message arguments="<%= roe.getRemoteAddress() %>" key="the-remote-address-x-is-not-valid" />
					</c:if>

					<c:if test="<%= roe.getType() == RemoteOptionsException.REMOTE_GROUP_ID %>">
						<liferay-ui:message arguments="<%= roe.getRemoteGroupId() %>" key="the-remote-site-id-x-is-not-valid" />
					</c:if>

					<c:if test="<%= roe.getType() == RemoteOptionsException.REMOTE_PATH_CONTEXT %>">
						<liferay-ui:message arguments="<%= roe.getRemotePathContext() %>" key="the-remote-path-context-x-is-not-valid" />
					</c:if>

					<c:if test="<%= roe.getType() == RemoteOptionsException.REMOTE_PORT %>">
						<liferay-ui:message arguments="<%= roe.getRemotePort() %>" key="the-remote-port-x-is-not-valid" />
					</c:if>
				</liferay-ui:error>

				<div class="alert alert-info">
					<liferay-ui:message key="remote-publish-help" />
				</div>

				<aui:input label="remote-host-ip" name="remoteAddress" size="20" type="text" value='<%= liveGroupTypeSettings.getProperty("remoteAddress") %>' />

				<aui:input label="remote-port" name="remotePort" size="10" type="text" value='<%= liveGroupTypeSettings.getProperty("remotePort") %>' />

				<aui:input label="remote-path-context" name="remotePathContext" size="10" type="text" value='<%= liveGroupTypeSettings.getProperty("remotePathContext") %>' />

				<aui:input label='<%= LanguageUtil.get(pageContext, "remote-site-id" ) %>' name="remoteGroupId" size="10" type="text" value='<%= liveGroupTypeSettings.getProperty("remoteGroupId") %>' />

				<aui:input label="use-a-secure-network-connection" name="secureConnection" type="checkbox" value='<%= liveGroupTypeSettings.getProperty("secureConnection") %>' />
			</aui:fieldset>
		</div>

		<div class="<%= (liveGroup.isStaged() ? StringPool.BLANK : "hide") %> staging-section" id="<portlet:namespace />stagedPortlets">
			<br />

			<c:if test="<%= !liveGroup.isCompany() %>">
				<aui:fieldset helpMessage="page-versioning-help" label="page-versioning">
					<aui:input label="enabled-on-public-pages" name="branchingPublic" type="checkbox" value='<%= GetterUtil.getBoolean(liveGroupTypeSettings.getProperty("branchingPublic")) %>' />

					<aui:input label="enabled-on-private-pages" name="branchingPrivate" type="checkbox" value='<%= GetterUtil.getBoolean(liveGroupTypeSettings.getProperty("branchingPrivate")) %>' />
				</aui:fieldset>
			</c:if>

			<aui:fieldset helpMessage="staged-portlets-help" label="staged-content">
				<div class="alert alert-block">
					<liferay-ui:message key="staged-portlets-alert" />
				</div>

				<%
				Set<String> portletDataHandlerClasses = new HashSet<String>();

				List<Portlet> dataSiteLevelPortlets = LayoutExporter.getDataSiteLevelPortlets(company.getCompanyId());

				dataSiteLevelPortlets = ListUtil.sort(dataSiteLevelPortlets, new PortletTitleComparator(application, locale));

				for (Portlet curPortlet : dataSiteLevelPortlets) {
					String portletDataHandlerClass = curPortlet.getPortletDataHandlerClass();

					if (!portletDataHandlerClasses.contains(portletDataHandlerClass)) {
						portletDataHandlerClasses.add(portletDataHandlerClass);
					}
					else {
						continue;
					}

					PortletDataHandler portletDataHandler = curPortlet.getPortletDataHandlerInstance();

					boolean staged = GetterUtil.getBoolean(liveGroupTypeSettings.getProperty(StagingUtil.getStagedPortletId(curPortlet.getRootPortletId())), portletDataHandler.isPublishToLiveByDefault());
				%>

					<aui:input label="<%= PortalUtil.getPortletTitle(curPortlet, application, locale) %>" name="<%= StagingUtil.getStagedPortletId(curPortlet.getRootPortletId()) %>" type="checkbox" value="<%= staged %>" />

				<%
				}
				%>

			</aui:fieldset>
		</div>

		<aui:script use="aui-base">
			var remoteStagingOptions = A.one('#<portlet:namespace />remoteStagingOptions');
			var stagedPortlets = A.one('#<portlet:namespace />stagedPortlets');

			var stagingTypes = A.all('#<portlet:namespace />stagingTypes input');

			stagingTypes.on(
				'change',
				function(event) {
					var value = event.currentTarget.val();

					stagedPortlets.toggle(value != '<%= StagingConstants.TYPE_NOT_STAGED %>');

					remoteStagingOptions.toggle(value == '<%= StagingConstants.TYPE_REMOTE_STAGING %>');
				}
			);
		</aui:script>
	</c:when>
	<c:otherwise>
		<div class="alert alert-info">
			<liferay-ui:message key="you-do-not-have-permission-to-manage-settings-related-to-staging" />
		</div>
	</c:otherwise>
</c:choose>