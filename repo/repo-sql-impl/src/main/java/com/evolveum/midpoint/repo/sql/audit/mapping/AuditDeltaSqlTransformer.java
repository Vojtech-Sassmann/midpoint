/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit.mapping;

import com.querydsl.sql.SQLServerTemplates;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.audit.SqlTransformerBase;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditDelta;
import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditDelta;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sqlbase.SqlConfiguration;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Simple class with methods for audit event transformation between repo and Prism world.
 */
public class AuditDeltaSqlTransformer
        extends SqlTransformerBase<ObjectDeltaOperationType, QAuditDelta, MAuditDelta> {

    public AuditDeltaSqlTransformer(PrismContext prismContext,
            QAuditDeltaMapping mapping, SqlConfiguration sqlConfiguration) {
        super(prismContext, mapping, sqlConfiguration);
    }

    public ObjectDeltaOperationType toSchemaObject(MAuditDelta row) throws SchemaException {
        ObjectDeltaOperationType odo = new ObjectDeltaOperationType();
        boolean usingSqlServer = sqlConfiguration.getQuerydslTemplates() instanceof SQLServerTemplates;
        if (row.delta != null) {
            String serializedDelta =
                    RUtil.getSerializedFormFromBytes(row.delta, usingSqlServer);

            ObjectDeltaType delta = prismContext.parserFor(serializedDelta)
                    .parseRealValue(ObjectDeltaType.class);
            odo.setObjectDelta(delta);
        }
        if (row.fullResult != null) {
            String serializedResult =
                    RUtil.getSerializedFormFromBytes(row.fullResult, usingSqlServer);

            OperationResultType resultType = prismContext.parserFor(serializedResult)
                    .parseRealValue(OperationResultType.class);
            odo.setExecutionResult(resultType);
        }
        if (row.objectNameOrig != null || row.objectNameNorm != null) {
            odo.setObjectName(new PolyStringType(
                    new PolyString(row.objectNameOrig, row.objectNameNorm)));
        }
        odo.setResourceOid(row.resourceOid);
        if (row.resourceNameOrig != null || row.resourceNameNorm != null) {
            odo.setResourceName(new PolyStringType(
                    new PolyString(row.resourceNameOrig, row.resourceNameNorm)));
        }

        return odo;
    }
}
