package org.jtaccuino.jshell.extensions.fx;

import java.util.List;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;

public final class ChartsFx {

    public static ScatterChart<?,?> scatterFx(int[] x, double[] y, String xLabel, String yLabel, String title) {
        var xAxis = new NumberAxis();
        xAxis.setLabel(xLabel);

        var yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);

        var scatterChart = new ScatterChart<Number, Number>(xAxis, yAxis);

        var series = new XYChart.Series<Number,Number>();
        series.setName(title);

        var intStream = IntStream.range(0, x.length);
        var data = intStream.mapToObj(i -> new XYChart.Data<Number, Number>(x[i], y[i])).toList();

        series.getData().addAll(data);

        scatterChart.getData().setAll(List.of(series));
        return scatterChart;
    }
    
    public static LineChart<?,?> lineFx(int[] x, double[] y, String xLabel, String yLabel, String title) {
        var xAxis = new NumberAxis();
        xAxis.setLabel(xLabel);

        var yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);

        var lineChart = new LineChart<Number, Number>(xAxis, yAxis);

        var series = new XYChart.Series<Number,Number>();
        series.setName(title);

        var intStream = IntStream.range(0, x.length);
        var data = intStream.mapToObj(i -> new XYChart.Data<Number, Number>(x[i], y[i])).toList();

        series.getData().addAll(data);

        lineChart.getData().setAll(List.of(series));
        return lineChart;
    }

    public static LineChart<?,?> plotFx(Function<Double,Double> function, double from, double to, double step, String xLabel, String yLabel, String title) {
        var xAxis = new NumberAxis();
        xAxis.setLabel(xLabel);

        var yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);

        var lineChart = new LineChart<Number, Number>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);

        var series = new XYChart.Series<Number,Number>();
        series.setName(title);

        var xStream = DoubleStream.iterate(from, n -> n + step)
                .limit((long)((to -from) / step));
        var data = xStream.mapToObj(x -> new XYChart.Data<Number, Number>(x, function.apply(x))).toList();

        series.getData().addAll(data);

        lineChart.getData().setAll(List.of(series));
        return lineChart;
    }    
    
}
