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
import org.jfree.data.general.DefaultPieDataset;

public class SimplePieChart implements IChartData {

    private final Session session;

    private final String title;
    private final String query;

    protected static final String NAME  = "name";
    protected static final String VALUE = "value";

    protected SimplePieChart(final Session session, final String query, final String title) {
        this.session = session;
        this.title = title;
        this.query = query;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Transactional
    public byte[] getChartData() {
        final DefaultPieDataset pieDataset = new DefaultPieDataset();
        final List<Map> list = session.createQuery(query).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();

        for (final Map map : list) {
            final String name = (String) map.get(NAME);
            final Long value = (Long) map.get(VALUE);
            pieDataset.setValue(name + "(" + value + ")", value);
        }

        final JFreeChart chart = ChartFactory.createPieChart(title, // Title
                pieDataset, // Dataset
                false, // Show legend
                true, true);

        try {
            return ChartUtilities.encodeAsPNG(chart.createBufferedImage(600, 400));
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
