package org.reactome.web.pwp.client.details.tabs.analysis.widgets.found.columns;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Style;
import org.reactome.web.analysis.client.model.PathwayInteractor;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class InteractorColumn extends AbstractColumn<PathwayInteractor, String> {

    private static final String explanation = "Entities interacting with the submitted identifier";

    public InteractorColumn(String group, String title) {
        super(new TextCell(), Style.TextAlign.LEFT, group, title, explanation);
        setHorizontalAlignment(ALIGN_LEFT);
    }

    @Override
    public String getValue(PathwayInteractor object) {
        return object.getIdentifier();
    }
}
