package net.wohlfart.charts;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.jboss.seam.annotations.Transactional;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * not yet used...
 * 
 * @author michael
 *
 */
public class DoubleBarChart implements IChartData {

    private final Session session;

    private final String title;
    private final String query;

    protected static final String NAME     = "name";
    protected static final String VALUE    = "value";
    protected static final String CATEGORY = "category";

    protected DoubleBarChart(final Session session, final String query, final String title) {
        this.session = session;
        this.title = title;
        this.query = query;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Transactional
    public byte[] getChartData() {
        // create the empty dataset we need
        final DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();
        // execute the query
        final List<Map> list = session.createQuery(query)
            .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();

        // put the result from the query into the dataset
        for (final Map map : list) {
            final String category = (String) map.get(CATEGORY);
            final String name = (String) map.get(NAME);
            final Long value = (Long) map.get(VALUE);
            categoryDataset.setValue(value, category, name + "(" + value + ")");
        }

        final JFreeChart chart = ChartFactory.createBarChart(
                title, // Title
                "", //"Bereich und Maschine", // Y-Axis
                "Anzahl", // X-Axis
                categoryDataset, 
                //PlotOrientation.HORIZONTAL, 
                PlotOrientation.VERTICAL,
                true, // Show  legend
                true, 
                true);

        try {
            int width = 60 + list.size() * 20;
            return ChartUtilities.encodeAsPNG(chart.createBufferedImage(width, 600));
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
