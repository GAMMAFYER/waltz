/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017 Waltz open source project
 * See README.md for more information
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.khartec.waltz.service.measurable;

import com.khartec.waltz.common.DateTimeUtilities;
import com.khartec.waltz.data.EntityReferenceNameResolver;
import com.khartec.waltz.data.measurable.MeasurableDao;
import com.khartec.waltz.data.measurable.MeasurableIdSelectorFactory;
import com.khartec.waltz.data.measurable.search.MeasurableSearchDao;
import com.khartec.waltz.model.*;
import com.khartec.waltz.model.changelog.ImmutableChangeLog;
import com.khartec.waltz.model.entity_search.EntitySearchOptions;
import com.khartec.waltz.model.measurable.Measurable;
import com.khartec.waltz.service.changelog.ChangeLogService;
import org.jooq.Record1;
import org.jooq.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.khartec.waltz.common.Checks.checkNotNull;
import static com.khartec.waltz.model.EntityReference.mkRef;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;


@Service
public class MeasurableService {
    
    private final MeasurableDao measurableDao;
    private final MeasurableIdSelectorFactory measurableIdSelectorFactory;
    private final MeasurableSearchDao measurableSearchDao;
    private final ChangeLogService changeLogService;
    private final EntityReferenceNameResolver nameResolver;


    @Autowired
    public MeasurableService(MeasurableDao measurableDao,
                             MeasurableIdSelectorFactory measurableIdSelectorFactory,
                             MeasurableSearchDao measurableSearchDao,
                             EntityReferenceNameResolver nameResolver,
                             ChangeLogService changeLogService) {
        checkNotNull(measurableDao, "measurableDao cannot be null");
        checkNotNull(measurableIdSelectorFactory, "measurableIdSelectorFactory cannot be null");
        checkNotNull(measurableSearchDao, "measurableSearchDao cannot be null");
        checkNotNull(nameResolver, "nameResolver cannot be null");
        checkNotNull(changeLogService, "changeLogService cannot be null");

        this.measurableDao = measurableDao;
        this.measurableIdSelectorFactory = measurableIdSelectorFactory;
        this.measurableSearchDao = measurableSearchDao;
        this.nameResolver = nameResolver;
        this.changeLogService = changeLogService;
    }


    public List<Measurable> findAll() {
        return measurableDao.findAll();
    }


    /**
     * Includes parents, this should probably be deprecated and rolled into findByMeasureableIdSelector
     * @param ref Entity reference of item to search against
     * @return List of measurable related to the given entity `ref`
     */
    public List<Measurable> findMeasurablesRelatedToEntity(EntityReference ref) {
        checkNotNull(ref, "ref cannot be null");
        return measurableDao.findMeasuresRelatedToEntity(ref);
    }


    public List<Measurable> findByMeasurableIdSelector(IdSelectionOptions options) {
        checkNotNull(options, "options cannot be null");
        Select<Record1<Long>> selector = measurableIdSelectorFactory.apply(options);
        return measurableDao.findByMeasurableIdSelector(selector);
    }


    public Collection<Measurable> search(String query) {
        return search(query, EntitySearchOptions.mkForEntity(EntityKind.MEASURABLE));
    }


    public Collection<Measurable> search(String query, EntitySearchOptions options) {
        return measurableSearchDao.search(query, options);
    }


    public Collection<Measurable> findByExternalId(String extId) {
        return measurableDao.findByExternalId(extId);
    }


    public Measurable getById(long id) {
        return measurableDao.getById(id);
    }


    public boolean updateConcreteFlag(Long id, boolean newValue, String userId) {
        logUpdate(id, "concrete flag", Boolean.toString(newValue), m -> Optional.of(Boolean.toString(m.concrete())), userId);

        return measurableDao.updateConcreteFlag(id, newValue, userId);
    }


    public boolean updateName(long id, String newValue, String userId) {
        logUpdate(id, "name", newValue, m -> ofNullable(m.name()), userId);
        return measurableDao.updateName(id, newValue, userId);
    }


    public boolean updateDescription(long id, String newValue, String userId) {
        logUpdate(id, "description", newValue, m -> ofNullable(m.description()), userId);
        return measurableDao.updateDescription(id, newValue, userId);
    }


    public boolean updateExternalId(long id, String newValue, String userId) {
        logUpdate(id, "externalId", newValue, m -> m.externalId(), userId);
        return measurableDao.updateExternalId(id, newValue, userId);
    }


    public boolean create(Measurable measurable, String userId) {
        return measurableDao.create(measurable);
    }


    public int deleteByIdSelector(IdSelectionOptions selectionOptions) {
        Select<Record1<Long>> selector = measurableIdSelectorFactory
                .apply(selectionOptions);
        return measurableDao
                .deleteByIdSelector(selector);
    }


    /**
     * Changes the parentId of the given measurable to the new parent specified
     * by destinationId.  If destination id is null the measurable will be a new
     * root node in the tree.
     *
     * @param measurableId  measurable id of item to move
     * @param destinationId new parent id (or null if root)
     * @param userId who initiated this move
     */
    public boolean updateParentId(Long measurableId, Long destinationId, String userId) {
        checkNotNull(measurableId, "Cannot updateParentId a measurable with a null id");

        writeAuditMessage(
                measurableId,
                userId,
                format("Measurable: [%s] moved to new parent: [%s]",
                        resolveName(measurableId),
                        destinationId == null
                                ? "<root of tree>"
                                : resolveName(destinationId)));

        return measurableDao.updateParentId(measurableId, destinationId, userId);
    }


    // --- helpers ---

    private void logUpdate(long id, String valueName, String newValue, Function<Measurable, Optional<String>> valueExtractor, String userId) {
        String existingValue = ofNullable(measurableDao.getById(id))
                .flatMap(valueExtractor)
                .orElse("<null>");

        writeAuditMessage(
                id,
                userId,
                format("Measurable: [%s] %s updated, from: [%s] to: [%s]",
                        resolveName(id),
                        valueName,
                        existingValue,
                        newValue));
    }


    private String resolveName(long id) {
        return nameResolver
                .resolve(mkRef(EntityKind.MEASURABLE, id))
                .flatMap(EntityReference::name)
                .orElse("UNKNOWN");
    }


    private void writeAuditMessage(Long measurableId, String userId, String msg) {
        changeLogService.write(ImmutableChangeLog.builder()
                .severity(Severity.INFORMATION)
                .userId(userId)
                .operation(Operation.UPDATE)
                .parentReference(mkRef(EntityKind.MEASURABLE, measurableId))
                .createdAt(DateTimeUtilities.nowUtc())
                .message(msg)
                .build());
    }



}
