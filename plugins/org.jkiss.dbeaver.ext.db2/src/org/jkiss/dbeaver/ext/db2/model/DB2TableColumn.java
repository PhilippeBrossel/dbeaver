/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.editors.DB2ColumnDataTypeListProvider;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2ColumnHiddenState;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableColumnCompression;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableColumnGenerated;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.impl.DBPositiveNumberTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectExt4;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Types;

/**
 * DB2 Table Column
 * 
 * @author Denis Forveille
 */
public class DB2TableColumn extends JDBCTableColumn<DB2TableBase>
    implements DBSTableColumn, DBSTypedObjectEx, DBPHiddenObject, DBSTypedObjectExt4<DB2DataType> {

    private static final Log log = Log.getLog(DB2TableColumn.class);

    private DB2DataType dataType;
    private DB2Schema dataTypeSchema;
    private String remarks;
    private DB2ColumnHiddenState hidden;
    private Boolean identity;
    private Boolean lobCompact;
    private DB2TableColumnGenerated generated;
    private String generatedText;
    private DB2TableColumnCompression compress;
    private String rowBegin;
    private String rowEnd;
    private String transactionStartId;
    private String collationSchema;
    private String collationName;
    private int codePage;

    private String typeStringUnits;
    private Integer stringUnitsLength;
    private String stringLength;

    private Integer keySeq;
    private Integer partKeySeq;

    private Long colcard;
    private String high2key;
    private String low2key;
    private Integer avgLength;
    private Integer nbQuantiles;
    private Integer nbMostFreq;
    private Long nbNulls;
    private Integer pctInlined;
    private Integer pctEncoded;

    private boolean hiddenState;

    // -----------------
    // Constructors
    // -----------------

    public DB2TableColumn(DBRProgressMonitor monitor, DB2TableBase tableBase, ResultSet dbResult) throws DBException
    {
        super(tableBase, true);

        DB2DataSource db2DataSource = tableBase.getDataSource();

        setName(JDBCUtils.safeGetString(dbResult, "COLNAME"));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, "COLNO"));
        setRequired(JDBCUtils.safeGetBoolean(dbResult, "NULLS", DB2YesNo.N.name()));
        setDefaultValue(JDBCUtils.safeGetString(dbResult, "DEFAULT"));
        setMaxLength(JDBCUtils.safeGetInt(dbResult, "LENGTH"));
        setScale(JDBCUtils.safeGetInteger(dbResult, "SCALE"));

        this.hidden = CommonUtils.valueOf(DB2ColumnHiddenState.class, JDBCUtils.safeGetString(dbResult, "HIDDEN"));
        this.identity = JDBCUtils.safeGetBoolean(dbResult, "IDENTITY", DB2YesNo.Y.name());
        this.lobCompact = JDBCUtils.safeGetBoolean(dbResult, "COMPACT", DB2YesNo.Y.name());
        this.generated = CommonUtils.valueOf(DB2TableColumnGenerated.class, JDBCUtils.safeGetString(dbResult, "GENERATED"));
        this.generatedText = JDBCUtils.safeGetString(dbResult, "TEXT");
        this.compress = CommonUtils.valueOf(DB2TableColumnCompression.class, JDBCUtils.safeGetString(dbResult, "COMPRESS"));
        this.colcard = JDBCUtils.safeGetLong(dbResult, "COLCARD");
        this.high2key = JDBCUtils.safeGetString(dbResult, "HIGH2KEY");
        this.low2key = JDBCUtils.safeGetString(dbResult, "LOW2KEY");
        this.avgLength = JDBCUtils.safeGetInteger(dbResult, "AVGCOLLEN");
        this.nbNulls = JDBCUtils.safeGetLong(dbResult, "NUMNULLS");
        this.keySeq = JDBCUtils.safeGetInteger(dbResult, "KEYSEQ");
        this.partKeySeq = JDBCUtils.safeGetInteger(dbResult, "PARTKEYSEQ");

        this.remarks = JDBCUtils.safeGetString(dbResult, DB2Constants.SYSCOLUMN_REMARKS);

        this.codePage = JDBCUtils.safeGetInt(dbResult, "CODEPAGE");
        if (db2DataSource.isAtLeastV9_5()) {
            this.collationSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATIONSCHEMA");
            this.collationName = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME");
            this.nbQuantiles = JDBCUtils.safeGetInteger(dbResult, "NQUANTILES");
            this.nbMostFreq = JDBCUtils.safeGetInteger(dbResult, "NMOSTFREQ");
        }
        if (db2DataSource.isAtLeastV9_7()) {
            this.pctInlined = JDBCUtils.safeGetInteger(dbResult, "PCTINLINED");
        }
        if (db2DataSource.isAtLeastV10_1()) {
            this.rowBegin = JDBCUtils.safeGetString(dbResult, "ROWBEGIN");
            this.rowEnd = JDBCUtils.safeGetString(dbResult, "ROWEND");
            this.transactionStartId = JDBCUtils.safeGetStringTrimmed(dbResult, "TRANSACTIONSTARTID");
        }
        if (db2DataSource.isAtLeastV10_5()) {
            this.typeStringUnits = JDBCUtils.safeGetStringTrimmed(dbResult, "TYPESTRINGUNITS");
            this.stringUnitsLength = JDBCUtils.safeGetInteger(dbResult, "STRINGUNITSLENGTH");
            this.pctEncoded = JDBCUtils.safeGetInteger(dbResult, "PCTENCODED");

            if (typeStringUnits == null) {
                stringLength = "";
            } else {
                stringLength = stringUnitsLength + " " + typeStringUnits;
            }
        }

        hiddenState = this.hidden == null ? false : hidden.isHidden();

        // Set DataTypes data
        // Search for DataType
        // Look first in Standards type
        String typeName = JDBCUtils.safeGetString(dbResult, "TYPENAME");
        this.dataType = tableBase.getDataSource().getDataTypeCache().getObject(monitor, getTable().getDataSource(), typeName);
        if (this.dataType == null) {
            String typeSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TYPESCHEMA");
            this.dataTypeSchema = getDataSource().getSchema(monitor, typeSchemaName);
            this.dataType = this.dataTypeSchema.getUDT(monitor, typeName);
        } else {
            this.dataTypeSchema = dataType.getSchema();
        }
        if (this.dataType == null) {
            log.debug("Data type '" + typeName + "' wasn't resolved");
            setTypeName(typeName);
            setValueType(Types.OTHER);
        } else {
            setTypeName(dataType.getFullyQualifiedName(DBPEvaluationContext.DML));
            setValueType(dataType.getTypeID());
        }
    }

    public DB2TableColumn(DB2TableBase tableBase) throws DBException
    {
        super(tableBase, false);

        setMaxLength(50L);
        setOrdinalPosition(-1);
        this.dataType = tableBase.getDataSource().getDataTypeCache().getCachedObject("VARCHAR");
        if (dataType != null) {
            this.dataTypeSchema = dataType.getSchema();
            setTypeName(dataType.getFullyQualifiedName(DBPEvaluationContext.DML));
            setValueType(dataType.getTypeID());
        }
        setRequired(false);
    }

    // -----------------
    // Business Contract
    // -----------------

    @NotNull
    @Override
    public DB2DataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    public boolean isAutoGenerated()
    {
        // DF: This method is used when the user uses the "insert row" function
        // in the data table editor or for generating INSERT INTO statements (not yet 2013-12-17)
        // GENERATED ALWAYS columns must not be included in such scenario
        if (generated != null) {
            if (generated.equals(DB2TableColumnGenerated.A)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isHidden()
    {
        return hiddenState;
    }

    @Override
    public DBPDataKind getDataKind()
    {
        DBPDataKind dataKind = dataType.getDataKind();
        if (dataKind == DBPDataKind.STRING && this.codePage == 0) {
            return DBPDataKind.CONTENT; // FOR BIT DATA
        }
        return dataKind;
    }

    @Override
    public String getTypeName()
    {
        return super.getTypeName();
    }

    // -----------------
    // Properties
    // -----------------
    @Property(viewable = true, editable = false, order = 19, category = DB2Constants.CAT_OWNER)
    public DB2TableBase getOwner()
    {
        return getTable();
    }

    @Property(viewable = true, editable = false, order = 20, category = DB2Constants.CAT_OWNER)
    public DB2Schema getTypeSchema()
    {
        return dataTypeSchema;
    }

    @Nullable
    @Property(viewable = true, editable = true, updatable = true, order = 21, listProvider = DB2ColumnDataTypeListProvider.class)
    public DBSDataType getDataType()
    {
        return dataType;
    }

    @Override
    public void setDataType(DB2DataType dataType) {
        onChangeDataType(this.dataType, dataType);
        this.dataType = dataType;
        this.typeName = dataType.getTypeName();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 38)
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Override
    @Property(viewable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 39)
    public Integer getScale()
    {
        return super.getScale();
    }

    @Property(viewable = true, order = 40)
    public String getStringLength()
    {
        return stringLength;
    }

    // @Property(viewable = true, order = 40)
    // public Integer getStringUnitsLength()
    // {
    // return stringUnitsLength;
    // }
    //
    // @Property(viewable = true, order = 41)
    // public String getTypeStringUnits()
    // {
    // return typeStringUnits;
    // }

    @Override
    @Property(viewable = false, valueRenderer = DBPositiveNumberTransformer.class, order = 42)
    public Integer getPrecision()
    {
        return super.getPrecision();
    }

    @Override
    @Property(viewable = true, order = 43, editable = true, updatable = true)
    public boolean isRequired()
    {
        return super.isRequired();
    }

    @Override
    @Property(viewable = true, order = 44, editable = true)
    public String getDefaultValue()
    {
        return super.getDefaultValue();
    }

    @Property(viewable = true, order = 45)
    public Boolean getIdentity()
    {
        return identity;
    }

    @Property(viewable = false, order = 46)
    public DB2TableColumnGenerated getGenerated()
    {
        return generated;
    }

    @Property(viewable = false, order = 47)
    public String getGeneratedText()
    {
        return generatedText;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 999, editable = true, updatable = true, length = PropertyLength.MULTILINE)
    public String getDescription()
    {
        return remarks;
    }

    public void setDescription(String remarks)
    {
        this.remarks = remarks;
    }

    @Property(viewable = false, order = 134)
    public Boolean getLobCompact()
    {
        return lobCompact;
    }

    @Property(viewable = false, order = 120)
    public Integer getKeySeq()
    {
        return keySeq;
    }

    @Property(viewable = false, order = 121)
    public Integer getPartKeySeq()
    {
        return partKeySeq;
    }

    @Property(viewable = false, order = 136)
    public DB2TableColumnCompression getCompress()
    {
        return compress;
    }

    @Property(viewable = false, order = 137)
    public DB2ColumnHiddenState getHidden()
    {
        return hidden;
    }

    // Temporal

    @Property(viewable = false, order = 138, category = DB2Constants.CAT_TEMPORAL)
    public String getRowBegin()
    {
        return rowBegin;
    }

    @Property(viewable = false, order = 139, category = DB2Constants.CAT_TEMPORAL)
    public String getRowEnd()
    {
        return rowEnd;
    }

    @Property(viewable = false, order = 140, category = DB2Constants.CAT_TEMPORAL)
    public String getTransactionStartId()
    {
        return transactionStartId;
    }

    @Property(viewable = false, order = 150, category = DBConstants.CAT_STATISTICS)
    public Long getColcard()
    {
        return colcard;
    }

    @Property(viewable = false, order = 152, category = DBConstants.CAT_STATISTICS)
    public Integer getAvgLength()
    {
        return avgLength;
    }

    @Property(viewable = false, order = 153, category = DBConstants.CAT_STATISTICS)
    public String getLow2key()
    {
        return low2key;
    }

    @Property(viewable = false, order = 154, category = DBConstants.CAT_STATISTICS)
    public String getHigh2key()
    {
        return high2key;
    }

    @Property(viewable = false, order = 155, category = DBConstants.CAT_STATISTICS)
    public Integer getPctInlined()
    {
        return pctInlined;
    }

    @Property(viewable = false, order = 156, category = DBConstants.CAT_STATISTICS)
    public Integer getPctEncoded()
    {
        return pctEncoded;
    }

    @Property(viewable = false, order = 157, category = DBConstants.CAT_STATISTICS)
    public Integer getNbQuantiles()
    {
        return nbQuantiles;
    }

    @Property(viewable = false, order = 158, category = DBConstants.CAT_STATISTICS)
    public Integer getNbMostFreq()
    {
        return nbMostFreq;
    }

    @Property(viewable = false, order = 159, category = DBConstants.CAT_STATISTICS)
    public Long getNbNulls()
    {
        return nbNulls;
    }

    @Property(viewable = false, order = 180, category = DB2Constants.CAT_COLLATION)
    public String getCollationSchema()
    {
        return collationSchema;
    }

    @Property(viewable = false, order = 181, category = DB2Constants.CAT_COLLATION)
    public String getcollationName()
    {
        return collationName;
    }

}
