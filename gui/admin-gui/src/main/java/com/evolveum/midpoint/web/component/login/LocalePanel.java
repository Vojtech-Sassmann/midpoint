/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.login;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.form.select.IOptionRenderer;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.extensions.markup.html.form.select.SelectOptions;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * @author lazyman
 */
public class LocalePanel extends Panel {

    private static final Trace LOGGER = TraceManager.getTrace(LocalePanel.class);
    private static final String LOCALIZATION_DESCRIPTOR = "Messages.localization";
    private static final List<LocaleDescriptor> AVAILABLE_LOCALES;

    private static final String ID_SELECT = "select";
    private static final String ID_OPTIONS = "options";

    static {
        List<LocaleDescriptor> locales = new ArrayList<LocaleDescriptor>();
        try {
            ClassLoader classLoader = LocalePanel.class.getClassLoader();
            Enumeration<URL> urls = classLoader.getResources(LOCALIZATION_DESCRIPTOR);
            while (urls.hasMoreElements()) {
                final URL url = urls.nextElement();
                LOGGER.debug("Found localization descriptor {}.", new Object[]{url.toString()});

                Properties properties = new Properties();
                Reader reader = null;
                try {
                    reader = new InputStreamReader(url.openStream(), "utf-8");
                    properties.load(reader);

                    LocaleDescriptor descriptor = new LocaleDescriptor(properties);
                    if (descriptor != null) {
                        locales.add(descriptor);
                    }
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't load localization", ex);
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            }

            Collections.sort(locales);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load locales", ex);
        }

        AVAILABLE_LOCALES = Collections.unmodifiableList(locales);
    }

    public LocalePanel(String id) {
        super(id);

        setRenderBodyOnly(true);

        final IModel<LocaleDescriptor> model = new Model(getSelectedLocaleDescriptor());
        Select<LocaleDescriptor> select = new Select<LocaleDescriptor>(ID_SELECT, model);
        select.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                System.out.println("asdf");
                changeLocale(target, model.getObject());
            }
        });
        select.setOutputMarkupId(true);
        add(select);
        SelectOptions<LocaleDescriptor> options = new SelectOptions<LocaleDescriptor>(ID_OPTIONS, AVAILABLE_LOCALES,
                new IOptionRenderer<LocaleDescriptor>() {

                    @Override
                    public String getDisplayValue(LocaleDescriptor object) {
                        return object.getName();
                    }

                    @Override
                    public IModel<LocaleDescriptor> getModel(LocaleDescriptor value) {
                        return new Model<LocaleDescriptor>(value);
                    }
                }) {

            @Override
            protected SelectOption newOption(String text, IModel<LocaleDescriptor> model) {
                SelectOption option = super.newOption("&nbsp;" + text, model);
                option.add(new AttributeModifier("data-icon", "flag-" + model.getObject().getFlag()));

                return option;
            }
        };
        select.add(options);
    }

    private LocaleDescriptor getSelectedLocaleDescriptor() {
        Locale locale = getSession().getLocale();
        if (locale == null) {
            return null;
        }

        for (LocaleDescriptor desc : AVAILABLE_LOCALES) {
            if (locale.equals(desc.getLocale())) {
                return desc;
            }
        }

        return null;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        String selectId = get(ID_SELECT).getMarkupId();
        response.render(OnDomReadyHeaderItem.forScript("$('#" + selectId + "').selectpicker({});"));
    }

    private void changeLocale(AjaxRequestTarget target, LocaleDescriptor descriptor) {
        LOGGER.info("Changing locale to {}.", new Object[]{descriptor.getLocale()});
        getSession().setLocale(descriptor.getLocale());

        target.add(getPage());
    }
}
