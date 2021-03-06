/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.dispatchToObjectDetailsPage;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.button.CsvDownloadButtonPanel;
import com.evolveum.midpoint.gui.impl.GuiChannel;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.web.component.search.DateSearchItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;

import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.AuditLogStorage;
import com.evolveum.midpoint.web.session.PageStorage;

import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.reports.PageAuditLogDetails;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

/**
 * Created by honchar
 */
public class AuditLogViewerPanel extends BasePanel {
    private static final long serialVersionUID = 1L;

    static final Trace LOGGER = TraceManager.getTrace(AuditLogViewerPanel.class);

    private static final String ID_AUDIT_LOG_VIEWER_TABLE = "auditLogViewerTable";

    public AuditLogViewerPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ContainerableListPanel<AuditEventRecordType, SelectableBean<AuditEventRecordType>> auditLogViewerTable =
                new ContainerableListPanel<AuditEventRecordType, SelectableBean<AuditEventRecordType>>(ID_AUDIT_LOG_VIEWER_TABLE, AuditEventRecordType.class) {

            @Override
            protected List<IColumn<SelectableBean<AuditEventRecordType>, String>> createDefaultColumns() {
                return AuditLogViewerPanel.this.createColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return null;
            }

            @Override
            protected IColumn<SelectableBean<AuditEventRecordType>, String> createNameColumn(IModel<String> columnNameModel,
                    String itemPath, ExpressionType expression) {
                return AuditLogViewerPanel.this.createNameColumn();
            }

            @Override
            protected IColumn<SelectableBean<AuditEventRecordType>, String> createCheckboxColumn() {
                return null;
            }

            @Override
            protected IColumn<SelectableBean<AuditEventRecordType>, String> createIconColumn() {
                return null;
            }

            @Override
            protected Search createSearch() {
                AuditLogStorage storage = (AuditLogStorage) getPageStorage();
                Search search = SearchFactory.createContainerSearch(getType(), AuditEventRecordType.F_TIMESTAMP, getPageBase());
                DateSearchItem timestampItem = (DateSearchItem) search.findPropertySearchItem(AuditEventRecordType.F_TIMESTAMP);
                if (timestampItem.getFromDate() == null && timestampItem.getToDate() == null && !isCollectionViewPanelForWidget()) {
                    Date todayDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
                    timestampItem.setFromDate(MiscUtil.asXMLGregorianCalendar(todayDate));
                }
                return search;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER;
            }

            @Override
            protected String getStorageKey() {
                if (isCollectionViewPanelForCompiledView()) {
                    StringValue collectionName = getCollectionNameParameterValue();
                    String collectionNameValue = collectionName != null ? collectionName.toString() : "";
                    return WebComponentUtil.getObjectListPageStorageKey(collectionNameValue);
                }

                return SessionStorage.KEY_AUDIT_LOG;
            }

            @Override
            protected ISelectableDataProvider createProvider() {
                PageStorage pageStorage = getPageStorage();
                SelectableBeanContainerDataProvider<AuditEventRecordType> provider = new SelectableBeanContainerDataProvider<AuditEventRecordType>(
                        AuditLogViewerPanel.this, AuditEventRecordType.class, null, false) {

                    @Override
                    protected PageStorage getPageStorage() {
                        return pageStorage;
                    }

                    @Override
                    public ObjectQuery getQuery() {
                        return createQuery();
                    }

                    @Override
                    protected Integer countObjects(Class<? extends AuditEventRecordType> type, ObjectQuery query,
                            Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result) {
                        return getPage().getAuditService().countObjects(query, currentOptions, result);
                    }

                    @Override
                    protected List<AuditEventRecordType> searchObjects(Class<? extends AuditEventRecordType> type, ObjectQuery query,
                            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
                            throws SchemaException {
                        return getPage().getAuditService().searchObjects(query, options, result);
                    }

                    @NotNull
                    @Override
                    protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                        if (sortParam != null && sortParam.getProperty() != null) {
                            OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
                            return Collections.singletonList(
                                    getPrismContext().queryFactory().createOrdering(
                                            ItemPath.create(new QName(AuditEventRecordType.COMPLEX_TYPE.getNamespaceURI(), sortParam.getProperty())), order));
                        } else {
                            return Collections.emptyList();
                        }
                    }
                };
                provider.setSort(AuditEventRecordType.F_TIMESTAMP.getLocalPart(), SortOrder.DESCENDING);
                return provider;
            }

            @Override
            protected List<Component> createToolbarButtonsList(String idButton) {
                List<Component> buttonsList = new ArrayList<>();
                CsvDownloadButtonPanel exportDataLink = new CsvDownloadButtonPanel(idButton) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String getFilename() {
                        return "AuditLogViewer_" + createStringResource("MainObjectListPanel.exportFileName").getString();
                    }

                    @Override
                    protected DataTable<?, ?> getDataTable() {
                        return getTable().getDataTable();
                    }
                };
                exportDataLink.add(new VisibleEnableBehaviour() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_URI);
                    }
                });
                buttonsList.add(exportDataLink);


                AjaxCompositedIconButton createReport = new AjaxCompositedIconButton(idButton, WebComponentUtil.createCreateReportIcon(),
                        getPageBase().createStringResource("MainObjectListPanel.createReport")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        createReportPerformed(target);
                    }
                };
                createReport.add(AttributeAppender.append("class", "btn btn-default btn-sm btn-margin-right"));
                buttonsList.add(createReport);
                return buttonsList;
            }
        };
        auditLogViewerTable.setOutputMarkupId(true);
        add(auditLogViewerTable);
    }

    private ContainerableListPanel getAuditLogViewerTable() {
        return (ContainerableListPanel) get(ID_AUDIT_LOG_VIEWER_TABLE);
    }

    protected List<IColumn<SelectableBean<AuditEventRecordType>, String>> createColumns() {
        List<IColumn<SelectableBean<AuditEventRecordType>, String>> columns = new ArrayList<>();
        LinkColumn<SelectableBean<AuditEventRecordType>> initiatorRefColumn =
                new LinkColumn<SelectableBean<AuditEventRecordType>>(createStringResource("AuditEventRecordType.initiatorRef"),
                        SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_INITIATOR_REF.getLocalPart()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected IModel<String> createLinkModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                        AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                        return Model.of(WebModelServiceUtils.resolveReferenceName(auditEventRecordType.getInitiatorRef(), getPageBase(), true));
                    }

                    @Override
                    public void onClick(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                        AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                        dispatchToObjectDetailsPage(auditEventRecordType.getInitiatorRef(), getPageBase(), false);
                    }
                };
        columns.add(initiatorRefColumn);

        if (!isObjectHistoryPanel()) {
            IColumn<SelectableBean<AuditEventRecordType>, String> eventStageColumn =
                    new PropertyColumn<SelectableBean<AuditEventRecordType>, String>(
                            createStringResource("PageAuditLogViewer.eventStageLabel"), AuditEventRecordType.F_EVENT_STAGE.getLocalPart(),
                            SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_EVENT_STAGE.getLocalPart()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public IModel<String> getDataModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                            return WebComponentUtil.createLocalizedModelForEnum(unwrapModel(rowModel).getEventStage(),
                                    AuditLogViewerPanel.this);
                        }
                    };
            columns.add(eventStageColumn);
        }

        IColumn<SelectableBean<AuditEventRecordType>, String> eventTypeColumn =
                new PropertyColumn<SelectableBean<AuditEventRecordType>, String>(createStringResource("PageAuditLogViewer.eventTypeLabel"), AuditEventRecordType.F_EVENT_TYPE.getLocalPart(),
                        SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_EVENT_TYPE.getLocalPart()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                        return WebComponentUtil.createLocalizedModelForEnum(unwrapModel(rowModel).getEventType(), AuditLogViewerPanel.this);
                    }
                };
        columns.add(eventTypeColumn);

        if (!isObjectHistoryPanel()) {
            LinkColumn<SelectableBean<AuditEventRecordType>> targetRefColumn =
                    new LinkColumn<SelectableBean<AuditEventRecordType>>(createStringResource("AuditEventRecordType.targetRef"),
                            SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_TARGET_REF.getLocalPart()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected IModel<String> createLinkModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                            AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                            return Model.of(WebModelServiceUtils.resolveReferenceName(auditEventRecordType.getTargetRef(), getPageBase(), true));
                        }

                        @Override
                        public void onClick(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                            AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                            dispatchToObjectDetailsPage(auditEventRecordType.getTargetRef(), getPageBase(), false);
                        }
                    };
            columns.add(targetRefColumn);
        }

        if (!isObjectHistoryPanel()) {
            LinkColumn<SelectableBean<AuditEventRecordType>> targetOwnerRefColumn =
                    new LinkColumn<SelectableBean<AuditEventRecordType>>(createStringResource("AuditEventRecordType.targetOwnerRef"),
                            SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_TARGET_OWNER_REF.getLocalPart()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected IModel<String> createLinkModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                            AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                            return Model.of(WebModelServiceUtils.resolveReferenceName(auditEventRecordType.getTargetOwnerRef(), getPageBase(), true));
                        }

                        @Override
                        public void onClick(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                            AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                            dispatchToObjectDetailsPage(auditEventRecordType.getTargetOwnerRef(), getPageBase(), false);
                        }
                    };
            columns.add(targetOwnerRefColumn);
        }
        IColumn<SelectableBean<AuditEventRecordType>, String> channelColumn =
                new PropertyColumn<SelectableBean<AuditEventRecordType>, String>(
                        createStringResource("AuditEventRecordType.channel"), AuditEventRecordType.F_CHANNEL.getLocalPart(),
                        SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_CHANNEL.getLocalPart()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<AuditEventRecordType>>> item, String componentId,
                            IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                        AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                        String channel = auditEventRecordType.getChannel();
                        GuiChannel channelValue = null;
                        for (GuiChannel chan : GuiChannel.values()) {
                            if (chan.getUri().equals(channel)) {
                                channelValue = chan;
                                break;
                            }
                        }
                        if (channelValue != null) {
                            item.add(new Label(componentId, WebComponentUtil.createLocalizedModelForEnum(channelValue, AuditLogViewerPanel.this)));
                        } else {
                            item.add(new Label(componentId, ""));
                        }
                        item.add(new AttributeModifier("style", new Model<>("width: 10%;")));
                    }
                };
        columns.add(channelColumn);

        IColumn<SelectableBean<AuditEventRecordType>, String> outcomeColumn =
                new PropertyColumn<SelectableBean<AuditEventRecordType>, String>(
                        createStringResource("PageAuditLogViewer.outcomeLabel"), AuditEventRecordType.F_OUTCOME.getLocalPart(),
                        SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_OUTCOME.getLocalPart()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                        return WebComponentUtil.createLocalizedModelForEnum(unwrapModel(rowModel).getOutcome(), AuditLogViewerPanel.this);
                    }
                };
        columns.add(outcomeColumn);

        return columns;
    }

    private IColumn<SelectableBean<AuditEventRecordType>, String> createNameColumn() {
        return new LinkColumn<SelectableBean<AuditEventRecordType>>(createStringResource("AuditEventRecordType.timestamp"), AuditEventRecordType.F_TIMESTAMP.getLocalPart(),
                SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_TIMESTAMP.getLocalPart()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                AuditEventRecordType record = unwrapModel(rowModel);
                return Model.of(WebComponentUtil.formatDate(record.getTimestamp()));
            }

            @Override
            public boolean isEnabled(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                return unwrapModel(rowModel) != null;
            }

            @Override
            public void onClick(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                AuditEventRecordType record = unwrapModel(rowModel);
                try {
                    AuditEventRecord.adopt(record, getPageBase().getPrismContext());
                } catch (SchemaException e) {
                    throw new SystemException("Couldn't adopt event record: " + e, e);
                }
                getPageBase().navigateToNext(new PageAuditLogDetails(record));
            }
        };
    }

    protected AuditEventRecordType unwrapModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        if (rowModel == null || rowModel.getObject() == null) {
            return null;
        }
        return rowModel.getObject().getValue();
    }

    protected boolean isObjectHistoryPanel() {
        return false;
    }

    protected AuditLogStorage getAuditLogViewerStorage(){
        return getPageBase().getSessionStorage().getAuditLog();
    }

}
