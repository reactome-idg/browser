package org.reactome.web.pwp.client.details.tabs.analysis.widgets.results.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.pwp.client.details.tabs.analysis.widgets.results.events.InteractorsPathwaySelectedEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface InteractorsPathwaySelectedHandler extends EventHandler {

    void onPathwayFoundInteractorsSelected(InteractorsPathwaySelectedEvent event);

}
