package org.reactome.web.pwp.client.tools.analysis.gsa.steps;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.reactome.web.pwp.client.common.utils.Console;
import org.reactome.web.pwp.client.details.common.widgets.button.IconButton;
import org.reactome.web.pwp.client.tools.analysis.gsa.client.model.raw.Parameter;
import org.reactome.web.pwp.client.tools.analysis.gsa.common.GSAWizardContext;
import org.reactome.web.pwp.client.tools.analysis.gsa.common.GSAWizardEventBus;
import org.reactome.web.pwp.client.tools.analysis.gsa.common.widgets.parameters.*;
import org.reactome.web.pwp.client.tools.analysis.gsa.events.StepSelectedEvent;
import org.reactome.web.pwp.client.tools.analysis.gsa.handlers.StepSelectedHandler;
import org.reactome.web.pwp.client.tools.analysis.gsa.style.GSAStyleFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is the last step before the analysis is submitted.
 * It presents the list of parameters that have a 'common' scope, allowing
 * the user to change them.
 *
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
@SuppressWarnings("Duplicates")
public class OptionsStep extends AbstractGSAStep implements StepSelectedHandler, BooleanParameter.Handler<Boolean>  {
    private IconButton nextBtn;
    private IconButton previousBtn;

    private List<Parameter> parameters;
    private FlowPanel parametersPanel;

    private List<BooleanParameter> booleanParameters;

    public OptionsStep(GSAWizardEventBus wizardEventBus, GSAWizardContext wizardContext) {
        super(wizardEventBus, wizardContext);
        init();
        initHandlers();
    }

    @Override
    public void onStepSelected(StepSelectedEvent event) {
        if (event.getSource().equals(this) || event.getStep() != GSAStep.OPTIONS)  {
            return;
        }
        updateUI();
    }

    private void init() {
        FlowPanel container = new FlowPanel();
        container.setStyleName(GSAStyleFactory.getStyle().container());

        SimplePanel title = new SimplePanel();
        title.setStyleName(GSAStyleFactory.getStyle().title());
        title.getElement().setInnerHTML("Step 3: Analysis options");
        container.add(title);

        container.add(getAnalysisParametersPanel());

        container.add(getDisclaimer());

        addNavigationButtons();

        add(new ScrollPanel(container));

    }

    private void initHandlers() {
        wizardEventBus.addHandler(StepSelectedEvent.TYPE, this);
    }

    private Widget getAnalysisParametersPanel() {
        parametersPanel = new FlowPanel();
        parametersPanel.setStyleName(GSAStyleFactory.getStyle().optionsPanel());
        return parametersPanel;
    }

    private void populateParametersPanel() {
        if (booleanParameters == null || booleanParameters.isEmpty()) {
            booleanParameters = new ArrayList<>(5);
        }

        parametersPanel.clear();
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        for (Parameter par : parameters) {
            AbstractParameterWidget widget = null;
            switch (par.getType()) {
                case "string":
                    widget = par.getValues() == null ? new TextBoxParameter(par) : new DropDownParameter(par);
                    break;
                case "int":
                    widget = new IntParameter(par);
                    break;
                case "float":
                    widget = new FloatParameter(par);
                    break;
                case "bool":
                    widget = new BooleanParameter(par);
                    widget.setParameterChangeHandler(this);
                    booleanParameters.add((BooleanParameter) widget);
                    break;
                default:
                    Console.error("Unknown parameter type [" + par.getType() + "]");
                    break;
            }
            if(widget != null) {
                String prop = par.getName();
                if (prop != null && prop.equals("create_reports")) {
                    // Request from 14/10/2020 - default value from the API is FALSE, setting to TRUE for PWB
                    widget.setValue("True");
                } else {
                    widget.setValue(wizardContext.getParameters().getOrDefault(par.getName(), par.getDefault()));
                }
                parametersPanel.add(widget);
            }

        }
    }

    private boolean validateAllParameters() {
        boolean rtn = true;
        for (Widget widget : parametersPanel) {
            AbstractParameterWidget parameterWidget = (AbstractParameterWidget) widget;
            if (!parameterWidget.validate()) {
                rtn = false;
            }
        }
        return rtn;
    }

    private Map<String, String> getParameterValues() {
        Map<String, String> rtn = new HashMap<>();
        for (Widget widget : parametersPanel) {
            AbstractParameterWidget parameterWidget = (AbstractParameterWidget) widget;
            rtn.put(parameterWidget.getName(), parameterWidget.getValue());
        }

        return rtn;
    }

    private Widget getDisclaimer() {
        FlowPanel disclaimerPanel = new FlowPanel();
        disclaimerPanel.setStyleName(GSAStyleFactory.getStyle().disclaimerPanel());
        disclaimerPanel.add(new HTML(GSAStyleFactory.RESOURCES.emailDisclaimer().getText()));
        return disclaimerPanel;
    }

    private void setReactomeAnalysisServer() {
        String hostName = Window.Location.getHostName();
        String server = "production";
        if (hostName.equalsIgnoreCase("dev.reactome.org")) {
            server = "dev";
        } else if (hostName.equals("localhost") || hostName.equals("127.0.0.1")) {
            server = "dev";
        } else if (hostName.equalsIgnoreCase("release.reactome.org")) {
            server = "release";
        }
        wizardContext.setParameter("reactome_server", server);
    }


    private void addNavigationButtons() {
        nextBtn = new IconButton(
                "GO",
                GSAStyleFactory.RESOURCES.nextIcon(),
                GSAStyleFactory.getStyle().navigationBtn(),
                "Submit datasets for analysis",
                event -> {
                    if (validateAllParameters()) {
                        Map<String, String> selectedParameters = getParameterValues();
                        for (Map.Entry<String, String> entry : selectedParameters.entrySet()) {
                            wizardContext.setParameter(entry.getKey(), entry.getValue());
                        }
                        // Important Note: the following method has to set the analysis server
                        // so that gsa.reactome.org stores the analysis token at the correct server.
                        setReactomeAnalysisServer();
                        wizardEventBus.fireEventFromSource(new StepSelectedEvent(GSAStep.ANALYSIS), this);
                    }
                });
        nextBtn.setEnabled(true);
        addRightButton(nextBtn);

        previousBtn = new IconButton(
                "Back",
                GSAStyleFactory.RESOURCES.previousIcon(),
                GSAStyleFactory.getStyle().navigationBtn(),
                "Go back to dataset overview",
                event -> wizardEventBus.fireEventFromSource(new StepSelectedEvent(GSAStep.DATASETS), this));
        addLeftButton(previousBtn);
    }

    private void updateUI() {
        /*
         * We only show the properties with 'common' scope to the user at this step
         * Note: We also hide the reactome server property from the user but we silently set its value
         * using the setReactomeAnalysisServer() method
         */
        parameters = wizardContext.getMethod().getParameters().stream()
                .filter(parameter -> parameter.getScope().equalsIgnoreCase("common"))
                .filter(parameter -> !parameter.getName().equalsIgnoreCase("reactome_server"))
                .collect(Collectors.toList());

        populateParametersPanel();
    }

    @Override
    public void onParameterChange(Boolean value) {
        nextBtn.setEnabled(false);
        if (booleanParameters.stream().anyMatch(booleanParameter -> Boolean.parseBoolean(booleanParameter.getValue()))) {
            nextBtn.setEnabled(true);
        }
    }
}
