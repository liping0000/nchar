package net.wohlfart.charts;

import java.util.ArrayList;
import java.util.List;

import net.wohlfart.changerequest.entities.ChangeRequestData;
import net.wohlfart.refdata.entities.ChangeRequestProduct;

import org.hibernate.Session;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Transactional;

/**
 * 
 * see: http://www.elegando.com/2007/07/charts-in-seam/
 * http://www.java2s.com/Code/Java/Chart/JFreeChartGanttDemo1.htm
 * 
 * @author Michael Wohlfart
 * 
 */
@Name("chartGenerator")
public class ChartGenerator {

    @In(value = "hibernateSession")
    private Session hibernateSession; // Entity Manager to access the database

    @SuppressWarnings("unchecked")
    @Factory(value = "chartArray")
    @Transactional
    public List<byte[]> createCharts() {
        final List<byte[]> result = new ArrayList<byte[]>();

        // a simple PieChart: products by request count
        result.add(
                new SimplePieChart(hibernateSession,
                        "select " 
                        + " p.defaultName as " + SimplePieChart.NAME 
                        + " , count(p) as  " + SimplePieChart.VALUE 
                        + " from " + ChangeRequestData.class.getName() + " b " 
                        + " join b.changeRequestProduct p" 
                        + " where b.businessKey is not null" 
                        + " group by p.defaultName "                        
                , "Anträge pro Maschinentyp"
        ).getChartData());
       
        // one chart for each machine:
        
        
        
        
        // a simple BarChart units by request count
        result.add(
                new SimpleBarChart(hibernateSession,
                        "select " 
                        + " u.defaultName as " + SimpleBarChart.NAME 
                        + " , 'group' as  "  + SimpleBarChart.CATEGORY 
                        + " , count(u) as  "  + SimpleBarChart.VALUE
                        + " from " 
                        + ChangeRequestData.class.getName() 
                        + " b " 
                        + " join b.changeRequestUnit u" 
                        + " where b.businessKey is not null"
                        + " group by u.defaultName "
                , "Anträge pro Bereich"
        ).getChartData());
        
        
        // list of machines:
        List<ChangeRequestProduct> products = hibernateSession.createQuery("select distinct p " 
                        + " from " 
                        + ChangeRequestData.class.getName() 
                        + " b " 
                        + " join b.changeRequestProduct p").list();
        // a chart for each machine:
        //System.err.println("machines: " + machines);
        for (ChangeRequestProduct product : products) {
            result.add(
                    new SimpleBarChart(hibernateSession,
                            "select " 
                            + " u.defaultName as " + SimpleBarChart.NAME 
                            + " , 'group' as  "  + SimpleBarChart.CATEGORY 
                            + " , count(u) as  "  + SimpleBarChart.VALUE
                            + " from " 
                            + ChangeRequestData.class.getName() 
                            + " b " 
                            + " join b.changeRequestUnit u" 
                            + " join b.changeRequestProduct p" 
                            + " where b.businessKey is not null"
                            + " and p.defaultName like '" + product.getDefaultName() + "'"
                            + " group by u.defaultName "
                    , "Anträge für " + product.getDefaultName()
            ).getChartData());            
        }
        
        
        
/*
        // a simple BarChart: units & products by request count
        result.add(
                new DoubleBarChart(hibernateSession,
                        "select " 
                        + " u.defaultName as " + SimpleBarChart.NAME 
                        + " , p.defaultName as  "  + SimpleBarChart.CATEGORY 
                        + " , count(u) as  "  + SimpleBarChart.VALUE
                        + " from " + ChangeRequestData.class.getName() + " b " 
                        + " join b.changeRequestUnit u" 
                        + " join b.changeRequestProduct p" 
                        + " where b.businessKey is not null"
                        + " group by u.defaultName, p.defaultName "
                , "Anträge pro Bereich und Maschine"
        ).getChartData());



        // a simple BarChart: products & units  by request count
        result.add(
                new DoubleBarChart(hibernateSession,
                        "select " 
                        + " p.defaultName as " + SimpleBarChart.NAME 
                        + " , u.defaultName as "  + SimpleBarChart.CATEGORY 
                        + " , count(u) as  "  + SimpleBarChart.VALUE
                        + " from " + ChangeRequestData.class.getName() + " b " 
                        + " join b.changeRequestUnit u" 
                        + " join b.changeRequestProduct p" 
                        + " where b.businessKey is not null"
                        + " group by p.defaultName , u.defaultName "
                , "Anträge pro Maschine und Bereich "
        ).getChartData());
*/
        
        // another simple BarChart codes by request count
        result.add(
                new SimplePieChart(hibernateSession,
                        "select " 
                        + " e.defaultName as " + SimplePieChart.NAME 
                        + " , count(e) as  " + SimplePieChart.VALUE 
                        + " from " 
                        + ChangeRequestData.class.getName()
                        + " b " 
                        + " join b.changeRequestCode e" 
                        + " where b.businessKey is not null" 
                        + " group by e.defaultName "
                , "Anträge pro Codierung"
        ).getChartData());

        return result;
    }

}
