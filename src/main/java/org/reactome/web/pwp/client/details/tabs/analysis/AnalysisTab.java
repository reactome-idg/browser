package org.reactome.web.pwp.client.details.tabs.analysis;

import org.reactome.web.analysis.client.model.AnalysisResult;
import org.reactome.web.pwp.client.details.tabs.DetailsTab;
import org.reactome.web.pwp.client.details.tabs.analysis.widgets.filtering.Filter;
import org.reactome.web.pwp.client.details.tabs.analysis.widgets.filtering.events.FilterAppliedEvent;
import org.reactome.web.pwp.client.tools.analysis.gsa.client.model.raw.Report;
import org.reactome.web.pwp.model.client.classes.Pathway;

import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface AnalysisTab {

    interface Presenter extends DetailsTab.Presenter {
        void onPathwayHovered(Long dbId);
        void onPathwayHoveredReset();
        void onPathwaySelected(Long dbId);
        void onFilterChanged(FilterAppliedEvent event);
    }

    interface Display extends DetailsTab.Display<Presenter>{
        void clearSelection();
        void selectPathway(Pathway pathway);
        void showResult(AnalysisResult analysisResult, Filter filter);
        void setSpecies(String species);
        void showGsaReports(List<Report> reportList);
    }
}
