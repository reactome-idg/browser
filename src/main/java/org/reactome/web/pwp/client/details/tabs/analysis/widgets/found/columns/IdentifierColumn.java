package org.reactome.web.pwp.client.details.tabs.analysis.widgets.found.columns;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Style;
import org.reactome.web.analysis.client.model.PathwayEntity;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class IdentifierColumn extends AbstractColumn<PathwayEntity, String> {

    private static final String explanation = "The submitted identifier";

    public IdentifierColumn(String group, String title) {
        super(new TextCell(), Style.TextAlign.LEFT, group, title, explanation);
        setHorizontalAlignment(ALIGN_LEFT);
    }

    @Override
    public String getValue(PathwayEntity object) {
        return object.getId();
    }
}
