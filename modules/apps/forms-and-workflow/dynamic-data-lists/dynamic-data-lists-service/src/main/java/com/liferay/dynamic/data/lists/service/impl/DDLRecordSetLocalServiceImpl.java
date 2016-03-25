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

package com.liferay.dynamic.data.lists.service.impl;

import com.liferay.dynamic.data.lists.exception.RecordSetDDMStructureIdException;
import com.liferay.dynamic.data.lists.exception.RecordSetDuplicateRecordSetKeyException;
import com.liferay.dynamic.data.lists.exception.RecordSetNameException;
import com.liferay.dynamic.data.lists.model.DDLRecordSet;
import com.liferay.dynamic.data.lists.model.DDLRecordSetSettings;
import com.liferay.dynamic.data.lists.service.base.DDLRecordSetLocalServiceBaseImpl;
import com.liferay.dynamic.data.mapping.io.DDMFormValuesJSONDeserializer;
import com.liferay.dynamic.data.mapping.io.DDMFormValuesJSONSerializer;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMStructureLink;
import com.liferay.dynamic.data.mapping.service.DDMStructureLinkLocalService;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.util.DDMFormFactory;
import com.liferay.dynamic.data.mapping.util.DDMFormInstanceFactory;
import com.liferay.dynamic.data.mapping.validator.DDMFormValuesValidator;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.SystemEventConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.systemevent.SystemEvent;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.spring.extender.service.ServiceReference;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides the local service for accessing, adding, deleting, and updating
 * dynamic data list (DDL) record sets.
 *
 * @author Brian Wing Shun Chan
 * @author Marcellus Tavares
 */
public class DDLRecordSetLocalServiceImpl
	extends DDLRecordSetLocalServiceBaseImpl {

	/**
	 * Adds a record set referencing the DDM structure.
	 *
	 * @param  userId the primary key of the record set's creator/owner
	 * @param  groupId the primary key of the record set's group
	 * @param  ddmStructureId the primary key of the record set's DDM structure
	 * @param  recordSetKey the record set's mnemonic primary key. If
	 *         <code>null</code>, the record set key will be autogenerated.
	 * @param  nameMap the record set's locales and localized names
	 * @param  descriptionMap the record set's locales and localized
	 *         descriptions
	 * @param  minDisplayRows the record set's minimum number of rows to be
	 *         displayed in spreadsheet view.
	 * @param  scope the record set's scope, used to scope the record set's
	 *         data. For more information search
	 *         <code>DDLRecordSetConstants</code> in the
	 *         <code>dynamic.data.lists.api</code> module for constants starting
	 *         with the "SCOPE_" prefix.
	 * @param  serviceContext the service context to be applied. Can set the
	 *         UUID, guest permissions, and group permissions for the record
	 *         set.
	 * @return the record set
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public DDLRecordSet addRecordSet(
			long userId, long groupId, long ddmStructureId, String recordSetKey,
			Map<Locale, String> nameMap, Map<Locale, String> descriptionMap,
			int minDisplayRows, int scope, ServiceContext serviceContext)
		throws PortalException {

		// Record set

		User user = userPersistence.findByPrimaryKey(userId);

		if (Validator.isNull(recordSetKey)) {
			recordSetKey = String.valueOf(counterLocalService.increment());
		}

		validate(groupId, ddmStructureId, recordSetKey, nameMap);

		long recordSetId = counterLocalService.increment();

		DDLRecordSet recordSet = ddlRecordSetPersistence.create(recordSetId);

		recordSet.setUuid(serviceContext.getUuid());
		recordSet.setGroupId(groupId);
		recordSet.setCompanyId(user.getCompanyId());
		recordSet.setUserId(user.getUserId());
		recordSet.setUserName(user.getFullName());
		recordSet.setDDMStructureId(ddmStructureId);
		recordSet.setRecordSetKey(recordSetKey);
		recordSet.setNameMap(nameMap);
		recordSet.setDescriptionMap(descriptionMap);
		recordSet.setMinDisplayRows(minDisplayRows);
		recordSet.setScope(scope);

		ddlRecordSetPersistence.update(recordSet);

		// Resources

		if (serviceContext.isAddGroupPermissions() ||
			serviceContext.isAddGuestPermissions()) {

			addRecordSetResources(
				recordSet, serviceContext.isAddGroupPermissions(),
				serviceContext.isAddGuestPermissions());
		}
		else {
			addRecordSetResources(
				recordSet, serviceContext.getGroupPermissions(),
				serviceContext.getGuestPermissions());
		}

		// Dynamic data mapping structure link

		long classNameId = classNameLocalService.getClassNameId(
			DDLRecordSet.class);

		ddmStructureLinkLocalService.addStructureLink(
			classNameId, recordSetId, ddmStructureId);

		return recordSet;
	}

	/**
	 * Adds the resources to the record set.
	 *
	 * @param  recordSet the record set
	 * @param  addGroupPermissions whether to add group permissions
	 * @param  addGuestPermissions whether to add guest permissions
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public void addRecordSetResources(
			DDLRecordSet recordSet, boolean addGroupPermissions,
			boolean addGuestPermissions)
		throws PortalException {

		resourceLocalService.addResources(
			recordSet.getCompanyId(), recordSet.getGroupId(),
			recordSet.getUserId(), DDLRecordSet.class.getName(),
			recordSet.getRecordSetId(), false, addGroupPermissions,
			addGuestPermissions);
	}

	/**
	 * Adds the model resources with the permissions to the record set.
	 *
	 * @param  recordSet the record set
	 * @param  groupPermissions whether to add group permissions
	 * @param  guestPermissions whether to add guest permissions
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public void addRecordSetResources(
			DDLRecordSet recordSet, String[] groupPermissions,
			String[] guestPermissions)
		throws PortalException {

		resourceLocalService.addModelResources(
			recordSet.getCompanyId(), recordSet.getGroupId(),
			recordSet.getUserId(), DDLRecordSet.class.getName(),
			recordSet.getRecordSetId(), groupPermissions, guestPermissions);
	}

	/**
	 * Deletes the record set and its resources.
	 *
	 * @param  recordSet the record set to be deleted
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	@SystemEvent(
		action = SystemEventConstants.ACTION_SKIP,
		type = SystemEventConstants.TYPE_DELETE
	)
	public void deleteRecordSet(DDLRecordSet recordSet) throws PortalException {

		// Record set

		ddlRecordSetPersistence.remove(recordSet);

		// Resources

		resourceLocalService.deleteResource(
			recordSet.getCompanyId(), DDLRecordSet.class.getName(),
			ResourceConstants.SCOPE_INDIVIDUAL, recordSet.getRecordSetId());

		// Records

		ddlRecordLocalService.deleteRecords(recordSet.getRecordSetId());

		// Dynamic data mapping structure link

		ddmStructureLinkLocalService.deleteStructureLinks(
			classNameLocalService.getClassNameId(DDLRecordSet.class),
			recordSet.getRecordSetId());

		// Workflow

		workflowDefinitionLinkLocalService.deleteWorkflowDefinitionLink(
			recordSet.getCompanyId(), recordSet.getGroupId(),
			DDLRecordSet.class.getName(), recordSet.getRecordSetId(), 0);
	}

	/**
	 * Deletes the record set and its resources.
	 *
	 * @param  recordSetId the primary key of the record set to be deleted
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public void deleteRecordSet(long recordSetId) throws PortalException {
		DDLRecordSet recordSet = ddlRecordSetPersistence.findByPrimaryKey(
			recordSetId);

		ddlRecordSetLocalService.deleteRecordSet(recordSet);
	}

	/**
	 * Deletes the record set and its resources.
	 *
	 * <p>
	 * This operation updates the record set matching the group and
	 * recordSetKey.
	 * </p>
	 *
	 * @param  groupId the primary key of the record set's group
	 * @param  recordSetKey the record set's mnemonic primary key
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public void deleteRecordSet(long groupId, String recordSetKey)
		throws PortalException {

		DDLRecordSet recordSet = ddlRecordSetPersistence.findByG_R(
			groupId, recordSetKey);

		ddlRecordSetLocalService.deleteRecordSet(recordSet);
	}

	/**
	 * Deletes all the record sets matching the group.
	 *
	 * @param  groupId the primary key of the record set's group
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public void deleteRecordSets(long groupId) throws PortalException {
		List<DDLRecordSet> recordSets = ddlRecordSetPersistence.findByGroupId(
			groupId);

		for (DDLRecordSet recordSet : recordSets) {
			ddlRecordSetLocalService.deleteRecordSet(recordSet);
		}
	}

	/**
	 * Returns the record set with the ID.
	 *
	 * @param  recordSetId the primary key of the record set
	 * @return the record set with the ID, or <code>null</code> if a matching
	 *         record set could not be found
	 */
	@Override
	public DDLRecordSet fetchRecordSet(long recordSetId) {
		return ddlRecordSetPersistence.fetchByPrimaryKey(recordSetId);
	}

	/**
	 * Returns the record set matching the group and record set key.
	 *
	 * @param  groupId the primary key of the record set's group
	 * @param  recordSetKey the record set's mnemonic primary key
	 * @return the record set matching the group and record set key, or
	 *         <code>null</code> if a matching record set could not be found
	 */
	@Override
	public DDLRecordSet fetchRecordSet(long groupId, String recordSetKey) {
		return ddlRecordSetPersistence.fetchByG_R(groupId, recordSetKey);
	}

	/**
	 * Returns the record set with the ID.
	 *
	 * @param  recordSetId the primary key of the record set
	 * @return the record set with the ID
	 * @throws PortalException if the the matching record set could not be found
	 */
	@Override
	public DDLRecordSet getRecordSet(long recordSetId) throws PortalException {
		return ddlRecordSetPersistence.findByPrimaryKey(recordSetId);
	}

	/**
	 * Returns the record set matching the group and record set key.
	 *
	 * @param  groupId the primary key of the record set's group
	 * @param  recordSetKey the record set's mnemonic primary key
	 * @return the record set matching the group and record set key
	 * @throws PortalException if the the matching record set could not be found
	 */
	@Override
	public DDLRecordSet getRecordSet(long groupId, String recordSetKey)
		throws PortalException {

		return ddlRecordSetPersistence.findByG_R(groupId, recordSetKey);
	}

	/**
	 * Returns all the record sets belonging the group.
	 *
	 * @return the record sets belonging to the group
	 */
	@Override
	public List<DDLRecordSet> getRecordSets(long groupId) {
		return ddlRecordSetPersistence.findByGroupId(groupId);
	}

	/**
	 * Returns the number of all the record sets belonging the group.
	 *
	 * @param  groupId the primary key of the record set's group
	 * @return the number of record sets belonging to the group
	 */
	@Override
	public int getRecordSetsCount(long groupId) {
		return ddlRecordSetPersistence.countByGroupId(groupId);
	}

	/**
	 * Returns the record set's settings as a DDMFormValues object. For more
	 * information see <code>DDMFormValues</code> in the
	 * <code>dynamic.data.mapping.api</code> module.
	 *
	 * @param  recordSet the record set
	 * @return the record set settings as a DDMFormValues object
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public DDMFormValues getRecordSetSettingsDDMFormValues(
			DDLRecordSet recordSet)
		throws PortalException {

		DDMForm ddmForm = DDMFormFactory.create(DDLRecordSetSettings.class);

		return ddmFormValuesJSONDeserializer.deserialize(
			ddmForm, recordSet.getSettings());
	}

	/**
	 * Returns the record set's settings.
	 *
	 * @param  recordSet the record set
	 * @return the record set settings
	 * @throws PortalException if a portal exception occurred
	 * @see    {#getRecordSetSettingsDDMFormValues(DDLRecordSet)}
	 */
	@Override
	public DDLRecordSetSettings getRecordSetSettingsModel(
			DDLRecordSet recordSet)
		throws PortalException {

		DDMFormValues ddmFormValues = getRecordSetSettingsDDMFormValues(
			recordSet);

		return DDMFormInstanceFactory.create(
			DDLRecordSetSettings.class, ddmFormValues);
	}

	/**
	 * Returns a range of all record sets matching the parameters, including a
	 * keywords parameter for matching string values to the record set's name or
	 * description.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end -
	 * start</code> instances. <code>start</code> and <code>end</code> are not
	 * primary keys, they are indexes in the result set. Thus, <code>0</code>
	 * refers to the first result in the set. Setting both <code>start</code>
	 * and <code>end</code> to <code>QueryUtil.ALL_POS</code> will return the
	 * full result set.
	 * </p>
	 *
	 * @param  companyId the primary key of the record set's company
	 * @param  groupId the primary key of the record set's group
	 * @param  keywords the keywords (space separated) to look for and match in
	 *         the record set name or description (optionally
	 *         <code>null</code>). If the keywords value is not
	 *         <code>null</code>, the search uses the OR operator in connecting
	 *         query criteria; otherwise it uses the AND operator.
	 * @param  scope the record set's scope. A constant used to scope the record
	 *         set's data. For more information search the
	 *         <code>dynamic.data.lists.api</code> module's
	 *         <code>DDLRecordSetConstants</code> class for constants prefixed
	 *         with "SCOPE_".
	 * @param  start the lower bound of the range of record sets to return
	 * @param  end the upper bound of the range of recor sets to return (not
	 *         inclusive)
	 * @param  orderByComparator the comparator to order the record sets
	 * @return the range of matching record sets ordered by the comparator
	 */

	@Override
	public List<DDLRecordSet> search(
		long companyId, long groupId, String keywords, int scope, int start,
		int end, OrderByComparator<DDLRecordSet> orderByComparator) {

		return ddlRecordSetFinder.findByKeywords(
			companyId, groupId, keywords, scope, start, end, orderByComparator);
	}

	/**
	 * Returns an ordered range of record sets. Company ID and group ID must be
	 * matched. If the and operator is set to <code>true</code>, only record
	 * sets with a matching name, description, and scope are returned. If the
	 * and operator is set to <code>false</code>, only one parameter of name,
	 * description, and scope is needed to return matching record sets.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end -
	 * start</code> instances. <code>start</code> and <code>end</code> are not
	 * primary keys, they are indexes in the result set. Thus, <code>0</code>
	 * refers to the first result in the set. Setting both <code>start</code>
	 * and <code>end</code> to <code>QueryUtil.ALL_POS</code> will return the
	 * full result set.
	 * </p>
	 *
	 * @param  companyId the primary key of the record set's company
	 * @param  groupId the primary key of the record set's group
	 * @param  name the name keywords (space separated, optionally
	 *         <code>null</code>)
	 * @param  description the description keywords (space separated, optionally
	 *         <code>null</code>)
	 * @param  scope the record set's scope. A constant used to scope the record
	 *         set's data. For more information search the
	 *         <code>dynamic.data.lists.api</code> module's
	 *         <code>DDLRecordSetConstants</code> class for constants prefixed
	 *         with "SCOPE_".
	 * @param  andOperator whether every field must match its value or keywords,
	 *         or just one field must match. Company and group must match their
	 *         values.
	 * @param  start the lower bound of the range of record sets to return
	 * @param  end the upper bound of the range of recor sets to return (not
	 *         inclusive)
	 * @param  orderByComparator the comparator to order the record sets
	 * @return the range of matching record sets ordered by the comparator
	 */
	@Override
	public List<DDLRecordSet> search(
		long companyId, long groupId, String name, String description,
		int scope, boolean andOperator, int start, int end,
		OrderByComparator<DDLRecordSet> orderByComparator) {

		return ddlRecordSetFinder.findByC_G_N_D_S(
			companyId, groupId, name, description, scope, andOperator, start,
			end, orderByComparator);
	}

	/**
	 * Returns the number of record sets matching the parameters. The keywords
	 * parameter is used for matching the record set's name or description
	 *
	 * @param  companyId the primary key of the record set's company
	 * @param  groupId the primary key of the record set's group.
	 * @param  keywords the keywords (space separated) to look for and match in
	 *         the record set name or description (optionally
	 *         <code>null</code>). If the keywords value is not
	 *         <code>null</code>, the OR operator is used in connecting query
	 *         criteria; otherwise it uses the AND operator.
	 * @param  scope the record set's scope. A constant used to scope the record
	 *         set's data. For more information search the
	 *         <code>dynamic.data.lists.api</code> module's
	 *         <code>DDLRecordSetConstants</code> class for constants prefixed
	 *         with "SCOPE_".
	 * @return the number of matching record sets
	 */
	@Override
	public int searchCount(
		long companyId, long groupId, String keywords, int scope) {

		return ddlRecordSetFinder.countByKeywords(
			companyId, groupId, keywords, scope);
	}

	/**
	 * Returns the number of all record sets matching the parameters. name and
	 * description keywords. Company ID and group ID must be matched. If the and
	 * operator is set to <code>true</code>, only record sets with a matching
	 * name, description, and scope are counted. If the and operator is set to
	 * <code>false</code>, only one parameter of name, description, and scope is
	 * needed to count matching record sets.
	 *
	 * @param  companyId the primary key of the record set's company
	 * @param  groupId the primary key of the record set's group
	 * @param  name the name keywords (space separated). This can be
	 *         <code>null</code>.
	 * @param  description the description keywords (space separated). This can
	 *         be <code>null</code>.
	 * @param  scope the record set's scope. A constant used to scope the record
	 *         set's data. For more information search the
	 *         <code>dynamic.data.lists.api</code> module's
	 *         <code>DDLRecordSetConstants</code> class for constants prefixed
	 *         with "SCOPE_".
	 * @param  andOperator whether every field must match its value or keywords,
	 *         or just one field must match. Company and group must match their
	 *         values.
	 * @return the number of matching record sets
	 */
	@Override
	public int searchCount(
		long companyId, long groupId, String name, String description,
		int scope, boolean andOperator) {

		return ddlRecordSetFinder.countByC_G_N_D_S(
			companyId, groupId, name, description, scope, andOperator);
	}

	/**
	 * Updates the number of minimum rows to display for the record set. Useful
	 * when the record set is being displayed in spreadsheet.
	 *
	 * @param  recordSetId the primary key of the record set
	 * @param  minDisplayRows the record set's minimum number of rows to be
	 *         displayed in spreadsheet view
	 * @param  serviceContext the service context to be applied. This can set
	 *         the record set modified date.
	 * @return the record set
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public DDLRecordSet updateMinDisplayRows(
			long recordSetId, int minDisplayRows, ServiceContext serviceContext)
		throws PortalException {

		DDLRecordSet recordSet = ddlRecordSetPersistence.findByPrimaryKey(
			recordSetId);

		recordSet.setMinDisplayRows(minDisplayRows);

		ddlRecordSetPersistence.update(recordSet);

		return recordSet;
	}

	/**
	 * Updates the the record set's settings.
	 *
	 * @param  recordSetId the primary key of the record set
	 * @param  settingsDDMFormValues the record set's settings. For more
	 *         information see <code>DDMFormValues</code> in the
	 *         <code>dynamic.data.mapping.api</code> the module.
	 * @return the record set
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public DDLRecordSet updateRecordSet(
			long recordSetId, DDMFormValues settingsDDMFormValues)
		throws PortalException {

		Date now = new Date();

		ddmFormValuesValidator.validate(settingsDDMFormValues);

		DDLRecordSet recordSet = ddlRecordSetPersistence.findByPrimaryKey(
			recordSetId);

		recordSet.setModifiedDate(now);
		recordSet.setSettings(
			ddmFormValuesJSONSerializer.serialize(settingsDDMFormValues));

		return ddlRecordSetPersistence.update(recordSet);
	}

	/**
	 * Updates the DDM structure, name, description, and minimum number of
	 * display rows for the record set matching the record set ID.
	 *
	 * @param  recordSetId the primary key of the record set
	 * @param  ddmStructureId the primary key of the record set's DDM structure
	 * @param  nameMap the record set's locales and localized names
	 * @param  descriptionMap the record set's locales and localized
	 *         descriptions
	 * @param  minDisplayRows the record set's minimum number of rows to be
	 *         displayed in spreadsheet view
	 * @param  serviceContext the service context to be applied. This can set
	 *         the record set modified date.
	 * @return the record set
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public DDLRecordSet updateRecordSet(
			long recordSetId, long ddmStructureId, Map<Locale, String> nameMap,
			Map<Locale, String> descriptionMap, int minDisplayRows,
			ServiceContext serviceContext)
		throws PortalException {

		DDLRecordSet recordSet = ddlRecordSetPersistence.findByPrimaryKey(
			recordSetId);

		return doUpdateRecordSet(
			ddmStructureId, nameMap, descriptionMap, minDisplayRows,
			serviceContext, recordSet);
	}

	/**
	 * Updates the DDM strucutre, name, description, and minimum number of
	 * display rows for the record set matching the record set key and group ID.
	 *
	 * @param  groupId the primary key of the record set's group
	 * @param  ddmStructureId the primary key of the record set's DDM structure
	 * @param  recordSetKey the record set's mnemonic primary key
	 * @param  nameMap the record set's locales and localized names
	 * @param  descriptionMap the record set's locales and localized
	 *         descriptions
	 * @param  minDisplayRows the record set's minimum number of rows to be
	 *         displayed in spreadsheet view
	 * @param  serviceContext the service context to be applied. This can set
	 *         the record set modified date.
	 * @return the record set
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public DDLRecordSet updateRecordSet(
			long groupId, long ddmStructureId, String recordSetKey,
			Map<Locale, String> nameMap, Map<Locale, String> descriptionMap,
			int minDisplayRows, ServiceContext serviceContext)
		throws PortalException {

		DDLRecordSet recordSet = ddlRecordSetPersistence.findByG_R(
			groupId, recordSetKey);

		return doUpdateRecordSet(
			ddmStructureId, nameMap, descriptionMap, minDisplayRows,
			serviceContext, recordSet);
	}

	protected DDLRecordSet doUpdateRecordSet(
			long ddmStructureId, Map<Locale, String> nameMap,
			Map<Locale, String> descriptionMap, int minDisplayRows,
			ServiceContext serviceContext, DDLRecordSet recordSet)
		throws PortalException {

		// Record set

		validateDDMStructureId(ddmStructureId);
		validateName(nameMap);

		long oldDDMStructureId = recordSet.getDDMStructureId();

		recordSet.setDDMStructureId(ddmStructureId);
		recordSet.setNameMap(nameMap);
		recordSet.setDescriptionMap(descriptionMap);
		recordSet.setMinDisplayRows(minDisplayRows);

		ddlRecordSetPersistence.update(recordSet);

		if (oldDDMStructureId != ddmStructureId) {

			// Records

			ddlRecordLocalService.deleteRecords(recordSet.getRecordSetId());

			// Dynamic data mapping structure link

			long classNameId = classNameLocalService.getClassNameId(
				DDLRecordSet.class);

			DDMStructureLink ddmStructureLink =
				ddmStructureLinkLocalService.getUniqueStructureLink(
					classNameId, recordSet.getRecordSetId());

			ddmStructureLinkLocalService.updateStructureLink(
				ddmStructureLink.getStructureLinkId(), classNameId,
				recordSet.getRecordSetId(), ddmStructureId);
		}

		return recordSet;
	}

	protected void validate(
			long groupId, long ddmStructureId, String recordSetKey,
			Map<Locale, String> nameMap)
		throws PortalException {

		validateDDMStructureId(ddmStructureId);

		if (Validator.isNotNull(recordSetKey)) {
			DDLRecordSet recordSet = ddlRecordSetPersistence.fetchByG_R(
				groupId, recordSetKey);

			if (recordSet != null) {
				RecordSetDuplicateRecordSetKeyException rsdrske =
					new RecordSetDuplicateRecordSetKeyException();

				rsdrske.setRecordSetKey(recordSet.getRecordSetKey());

				throw rsdrske;
			}
		}

		validateName(nameMap);
	}

	protected void validateDDMStructureId(long ddmStructureId)
		throws PortalException {

		DDMStructure ddmStructure = ddmStructureLocalService.fetchStructure(
			ddmStructureId);

		if (ddmStructure == null) {
			throw new RecordSetDDMStructureIdException(
				"No DDM structure exists with the DDM structure ID " +
					ddmStructureId);
		}
	}

	protected void validateName(Map<Locale, String> nameMap)
		throws PortalException {

		Locale locale = LocaleUtil.getSiteDefault();

		String name = nameMap.get(locale);

		if (Validator.isNull(name)) {
			throw new RecordSetNameException(
				"Name is null for locale " + locale.getDisplayName());
		}
	}

	@ServiceReference(type = DDMFormValuesJSONDeserializer.class)
	protected DDMFormValuesJSONDeserializer ddmFormValuesJSONDeserializer;

	@ServiceReference(type = DDMFormValuesJSONSerializer.class)
	protected DDMFormValuesJSONSerializer ddmFormValuesJSONSerializer;

	@ServiceReference(type = DDMFormValuesValidator.class)
	protected DDMFormValuesValidator ddmFormValuesValidator;

	@ServiceReference(type = DDMStructureLinkLocalService.class)
	protected DDMStructureLinkLocalService ddmStructureLinkLocalService;

	@ServiceReference(type = DDMStructureLocalService.class)
	protected DDMStructureLocalService ddmStructureLocalService;

}