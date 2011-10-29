package net.wohlfart.charts;

import java.io.IOException;

import org.jboss.seam.annotations.Transactional;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

public class DummyChart implements IChartData {

    protected DummyChart() {

    }

    @Override
    @Transactional
    public byte[] getChartData() {
        final DefaultPieDataset pieDataset = new DefaultPieDataset();
        pieDataset.setValue("A", Integer.valueOf(75));
        pieDataset.setValue("B", Integer.valueOf(25));
        pieDataset.setValue("C", Integer.valueOf(45));
        pieDataset.setValue("D", Integer.valueOf(35));
        pieDataset.setValue("E", Integer.valueOf(65));
        final JFreeChart chart = ChartFactory.createPieChart("Sample Pie Chart", // Title
                pieDataset, // Dataset
                true, // Show legend
                false, false);
        try {
            return ChartUtilities.encodeAsPNG(chart.createBufferedImage(600, 400));
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }

    }

}
