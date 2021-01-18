package org.reactome.web.pwp.client.manager.state;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import org.reactome.web.analysis.client.AnalysisClient;
import org.reactome.web.analysis.client.AnalysisHandler;
import org.reactome.web.analysis.client.filter.ResultFilter;
import org.reactome.web.analysis.client.model.AnalysisSummary;
import org.reactome.web.analysis.client.model.ResourceSummary;
import org.reactome.web.diagram.events.DiagramObjectsFlagResetEvent;
import org.reactome.web.diagram.handlers.DiagramObjectsFlagResetHandler;
import org.reactome.web.pwp.client.common.Selection;
import org.reactome.web.pwp.client.common.events.*;
import org.reactome.web.pwp.client.common.handlers.*;
import org.reactome.web.pwp.client.common.module.BrowserModule;
import org.reactome.web.pwp.client.details.tabs.DetailsTabType;
import org.reactome.web.pwp.client.details.tabs.analysis.widgets.summary.events.AnalysisFilterChangedEvent;
import org.reactome.web.pwp.client.details.tabs.analysis.widgets.summary.handlers.AnalysisFilterChangedHandler;
import org.reactome.web.pwp.client.manager.state.token.Token;
import org.reactome.web.pwp.client.manager.state.token.TokenMalformedException;
import org.reactome.web.pwp.client.manager.title.TitleManager;
import org.reactome.web.pwp.model.client.classes.*;
import org.reactome.web.pwp.model.client.util.Path;

import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class StateManager implements BrowserModule.Manager, ValueChangeHandler<String>,
        State.StateLoadedHandler, StateChangedHandler, DatabaseObjectSelectedHandler, DetailsTabChangedHandler,
        DiagramObjectsFlagResetHandler, AnalysisCompletedHandler, AnalysisResetHandler, AnalysisFilterChangedHandler,
        ToolSelectedHandler, TermFlaggedHandler, SpeciesListRetrievedHandler {

    private EventBus eventBus;

    private State currentState;

    private List<Species> speciesList;

    public StateManager(EventBus eventBus) {
        this.eventBus = eventBus;
        new TitleManager(eventBus);
        History.addValueChangeHandler(this);

        //The orthology manager can also fire state changed events
        this.eventBus.addHandler(StateChangedEvent.TYPE, this);
        this.eventBus.addHandler(DatabaseObjectSelectedEvent.TYPE, this);
        this.eventBus.addHandler(DetailsTabChangedEvent.TYPE, this);
        this.eventBus.addHandler(ToolSelectedEvent.TYPE, this);
        this.eventBus.addHandler(AnalysisCompletedEvent.TYPE, this);
        this.eventBus.addHandler(AnalysisResetEvent.TYPE, this);
        this.eventBus.addHandler(DiagramObjectsFlagResetEvent.TYPE, this);
        this.eventBus.addHandler(AnalysisFilterChangedEvent.TYPE, this);
        this.eventBus.addHandler(TermFlaggedEvent.TYPE, this);
        this.eventBus.addHandler(SpeciesListRetrievedEvent.TYPE, this);
    }

    @Override
    public void onValueChange(ValueChangeEvent<String> event) {
        String value = event.getValue();
        try {
            Token token = new Token(value);
            if (token.isWellFormed()) {
                new State(token, this);
            } else {
                this.tokenError(value);
            }
        } catch (TokenMalformedException e) {
            this.tokenError(value);
        }
    }

    @Override
    public void onAnalysisCompleted(AnalysisCompletedEvent event) {
        AnalysisSummary summary = event.getAnalysisResult().getSummary();
        AnalysisClient.addValidToken(summary.getToken());

        List<ResourceSummary> resources = event.getAnalysisResult().getResourceSummary();
        ResourceSummary resource = resources.size() == 2 ? resources.get(1) : resources.get(0); //IMPORTANT!

        State desiredState = new State(this.currentState);
        desiredState.setDetailsTab(DetailsTabType.ANALYSIS);

        ResultFilter filter = new ResultFilter();
        filter.setResource(resource.getResource());
        filter.setIncludeDisease(event.isGsaIncludeDisease());

        desiredState.setAnalysisParameters(summary.getToken(), filter);
        this.eventBus.fireEventFromSource(new StateChangedEvent(desiredState), this);
    }

    @Override
    public void onAnalysisReset() {
        State desiredState = new State(this.currentState);
        desiredState.resetAnalysisParameters();
        this.eventBus.fireEventFromSource(new StateChangedEvent(desiredState), this);
    }

    @Override
    public void onSpeciesListRetrieved(SpeciesListRetrievedEvent event) {
        this.speciesList = event.getSpeciesList();
    }

    @Override
    public void onDatabaseObjectSelected(DatabaseObjectSelectedEvent event) {
        Selection newSelection = event.getSelection();
        Selection currentSelection = new Selection(currentState);
        if (!currentSelection.equals(newSelection)) {
            Pathway diagram = newSelection.getDiagram();
            DatabaseObject selected = newSelection.getDatabaseObject();
            Path path = newSelection.getPath();

            //The following code assumes that PE or Events of different species
            //are selected when showing them, so it is highly recommended to do
            //some further checking and detect when an odd case happens
            currentState.setSelected(selected);
            if (diagram == null) {
                if (selected == null) {
                    currentState.setEvent(null);
                    currentState.setPath(null);
                } else {
                    Species species = null;
                    if (selected instanceof PhysicalEntity) {
                        PhysicalEntity pe = (PhysicalEntity) selected;
                        if (pe.getSpecies() != null && !pe.getSpecies().isEmpty()) {
                            species = pe.getSpecies().get(pe.getSpecies().size() - 1);
                        }
                    } else if (selected instanceof Event) {
                        Event e = (Event) selected;
                        if (e.getSpecies() != null && !e.getSpecies().isEmpty()) {
                            species = e.getSpecies().get(e.getSpecies().size() - 1);
                        }
                        currentState.setEvent(e);
                        currentState.setPath(path);
                    }
                    if (species != null && !species.equals(currentState.getSpecies()) && speciesList.contains(species)) {
                        eventBus.fireEventFromSource(new SpeciesSelectedEvent(species), this);
                        return;
                    }
                }
            } else {
                currentState.setEvent(diagram);
                currentState.setPath(path);
            }

            currentState.doConsistencyCheck(state -> {
                currentState = state;
                eventBus.fireEventFromSource(new StateChangedEvent(currentState), StateManager.this);
                History.newItem(currentState.toString(), false);
            });
        }
    }

    @Override
    public void onDetailsTabChanged(DetailsTabChangedEvent event) {
        State desiredState = new State(this.currentState);
        desiredState.setDetailsTab(event.getDetailsTab());
        this.eventBus.fireEventFromSource(new StateChangedEvent(desiredState), this);
    }

    @Override
    public void onDiagramObjectsFlagReset(DiagramObjectsFlagResetEvent event) {
        State desiredState = new State(this.currentState);
        desiredState.setFlag(null);
        desiredState.setFlagIncludeInteractors(false); //want to set this to false regardless to remove it from url on Flag close.
        this.eventBus.fireEventFromSource(new StateChangedEvent(desiredState), this);
    }

    @Override
    public void onAnalysisFilterChanged(AnalysisFilterChangedEvent event) {
        State desiredState = new State(this.currentState);
        desiredState.setAnalysisParameters(this.currentState.getAnalysisStatus().getToken(), event.getFilter());
        this.eventBus.fireEventFromSource(new StateChangedEvent(desiredState), this);
    }

    @Override
    public void onStateChanged(StateChangedEvent event) {
        State state = event.getState();
        if (!this.currentState.equals(state)) {
            this.currentState = state;
            History.newItem(state.toString(), false);
        }
    }

    @Override
    public void onStateLoaded(final State state) {
        if (state == null) {
            this.eventBus.fireEventFromSource(new ErrorMessageEvent("The data in the URL can not be fit into a state"), this);
        } else if (!state.equals(this.currentState)) {
            String token = state.getAnalysisStatus().getToken();
            AnalysisClient.checkTokenAvailability(token, new AnalysisHandler.Token() {
                @Override
                public void onTokenAvailabilityChecked(boolean available, String message) {
                    if (!available) {
                        eventBus.fireEventFromSource(new ErrorMessageEvent(message), StateManager.this);
                        state.resetAnalysisParameters();
                        History.newItem(state.toString(), false);
                    }
                    currentState = state;
                    eventBus.fireEventFromSource(new StateChangedEvent(state), StateManager.this);
                }

                @Override
                public void onAnalysisServerException(String message) {
                    //TODO
                }

                @Override
                public void onTokenAvailabilityCheckedWithSummary(AnalysisSummary summary, boolean available, String message) {
                    if (!available) {
                        eventBus.fireEventFromSource(new ErrorMessageEvent(message), StateManager.this);
                        state.resetAnalysisParameters();
                        History.newItem(state.toString(), false);
                    }
                    currentState = state;

                    State desired = new State(currentState);
                    ResultFilter filter = state.getAnalysisStatus().getResultFilter();
                    if (filter != null) {
                        if (summary != null && !summary.isIncludeDisease()) {
                            filter.setIncludeDisease(summary.isIncludeDisease());
                        }
                    }
                    desired.setAnalysisParameters(token, filter);
                    eventBus.fireEventFromSource(new StateChangedEvent(desired), StateManager.this);
                }
            });
        }
    }

    @Override
    public void onToolSelected(ToolSelectedEvent event) {
        State desiredState = new State(this.currentState);
        desiredState.setTool(event.getTool());
        this.eventBus.fireEventFromSource(new StateChangedEvent(desiredState), this);
    }

    private void tokenError(String token) {
        eventBus.fireEventFromSource(new TokenErrorEvent(token), this);
        try {
            new State(new Token(""), this); //Forcing to load the initial state at least
        } catch (TokenMalformedException e) {/*Nothing here*/}
    }

    @Override
    public void onTermFlagged(TermFlaggedEvent event) {
        State desiredState = new State(this.currentState);
        desiredState.setFlag(event.getTerm());
        desiredState.setFlagIncludeInteractors(event.getIncludeInteractors());
        this.eventBus.fireEventFromSource(new StateChangedEvent(desiredState), this);
    }
}
