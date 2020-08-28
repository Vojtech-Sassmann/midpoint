/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.PrismValue;

import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumnPanel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author katka
 *
 */
public abstract class ItemPanel<VW extends PrismValueWrapper<?>, IW extends ItemWrapper> extends BasePanel<IW> implements RefreshableTabPanel {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ItemPanel.class);

    private static final String DOT_CLASS = ItemPanel.class.getName() + "";

    private static final String ID_VALUES = "values";

    private ItemPanelSettings itemPanelSettings;


    public ItemPanel(String id, IModel<IW> model, ItemPanelSettings itemPanelSettings) {
        super(id, model);
        this.itemPanelSettings = itemPanelSettings;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {

        //ugly hack TODO FIME - prism context is lost during serialization/deserialization.. find better way how to do it.
        if (getModelObject() != null) {
            getModelObject().revive(getPrismContext());
        }

        Component headerPanel = createHeaderPanel();
        headerPanel.add(new VisibleBehaviour(() -> getHeaderVisibility()));
        add(headerPanel);

        Component valuesPanel = createValuesPanel();
        add(valuesPanel);

    }

    protected boolean getHeaderVisibility() {
        if (!isHeaderVisible()) {
            return false;
        }
        return getParent().findParent(AbstractItemWrapperColumnPanel.class) == null;
    }

    protected abstract Component createHeaderPanel();

    protected Component createValuesPanel() {

        ListView<VW> values = new ListView<VW>(ID_VALUES, createValuesModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<VW> item) {


                Component panel = createValuePanel(item);
                panel.add(new VisibleBehaviour(() -> item.getModelObject().isVisible()));
            }

        };

        return values;
    }

    protected IModel<List<VW>> createValuesModel() {
        return new PropertyModel<>(getModel(), "values");
    }

    protected void removeValue(VW valueToRemove, AjaxRequestTarget target) throws SchemaException {
        LOGGER.debug("Removing value of {}", valueToRemove);

        getModelObject().remove(valueToRemove, getPageBase());
        target.add(ItemPanel.this);
    }

    // VALUE REGION

     protected abstract Component createValuePanel(ListItem<VW> item);


    protected abstract <PV extends PrismValue> PV createNewValue(IW itemWrapper);

        public ItemVisibilityHandler getVisibilityHandler() {
            if (itemPanelSettings == null) {
                return null;
            }
            return itemPanelSettings.getVisibilityHandler();
        }

        public ItemEditabilityHandler getEditabilityHandler() {
            if (itemPanelSettings == null) {
                return null;
            }
            return itemPanelSettings.getEditabilityHandler();
        }

        protected boolean isHeaderVisible() {
             if (itemPanelSettings == null) {
                 return true;
            }

             return itemPanelSettings.isHeaderVisible();
        }

    public ItemPanelSettings getSettings() {
         return itemPanelSettings;
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.singleton(this);
    }
}
