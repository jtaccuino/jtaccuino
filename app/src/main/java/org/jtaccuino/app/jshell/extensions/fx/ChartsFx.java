/*
 * Copyright 2024-2025 JTaccuino Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jtaccuino.app.jshell.extensions.fx;

import java.util.List;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;

public final class ChartsFx {

    public static record DataPoint(int x, double y) {}

    public static ScatterChart<?, ?> scatterFx(IntStream x, DoubleStream y, String xLabel, String yLabel, String title) {
        return scatterFx(x.toArray(), y.toArray(), xLabel, yLabel, title);
    }

    public static ScatterChart<?, ?> scatterFx(int[] x, double[] y, String xLabel, String yLabel, String title) {
        var xAxis = new NumberAxis();
        xAxis.setLabel(xLabel);

        var yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);

        var scatterChart = new ScatterChart<Number, Number>(xAxis, yAxis);

        var series = new XYChart.Series<Number, Number>();
        series.setName(title);

        var intStream = IntStream.range(0, x.length);
        var data = intStream.mapToObj(i -> new XYChart.Data<Number, Number>(x[i], y[i])).toList();

        series.getData().addAll(data);

        scatterChart.getData().setAll(List.of(series));
        return scatterChart;
    }

    public static ScatterChart<?, ?> scatterPlot(List<double[]> xData, List<double[]> yData, String xLabel, String yLabel, String[] title) {
        var xAxis = new NumberAxis();
        xAxis.setLabel(xLabel);

        var yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);

        var scatterChart = new ScatterChart<Number, Number>(xAxis, yAxis);

        for (int i = 0; i < xData.size(); i++) {
            var x = xData.get(i);
            var y = yData.get(i);
            var intStream = IntStream.range(0, x.length);
            var data = intStream.mapToObj(j -> new XYChart.Data<Number, Number>(x[j], y[j])).toList();
            var series = new XYChart.Series<Number, Number>();
            series.setName(title[i]);
            series.getData().addAll(data);
            scatterChart.getData().add(series);
        }
        return scatterChart;
    }

    public static LineChart<?, ?> lineFx(int[] x, double[] y, String xLabel, String yLabel, String title) {
        var xAxis = new NumberAxis();
        xAxis.setLabel(xLabel);

        var yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);

        var lineChart = new LineChart<Number, Number>(xAxis, yAxis);

        var series = new XYChart.Series<Number, Number>();
        series.setName(title);

        var intStream = IntStream.range(0, x.length);
        var data = intStream.mapToObj(i -> new XYChart.Data<Number, Number>(x[i], y[i])).toList();

        series.getData().addAll(data);

        lineChart.getData().setAll(List.of(series));
        return lineChart;
    }

    public static LineChart<?, ?> plotFx(Function<Double, Double> function, double from, double to, double step, String xLabel, String yLabel, String title) {
        var xAxis = new NumberAxis();
        xAxis.setLabel(xLabel);

        var yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);

        var lineChart = new LineChart<Number, Number>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);

        var series = new XYChart.Series<Number, Number>();
        series.setName(title);

        var xStream = DoubleStream.iterate(from, n -> n + step)
                .limit((long) ((to - from) / step));
        var data = xStream.mapToObj(x -> new XYChart.Data<Number, Number>(x, function.apply(x))).toList();

        series.getData().addAll(data);

        lineChart.getData().setAll(List.of(series));
        return lineChart;
    }

    public static double[][] convertToDoubles(List<DataPoint> dataPoints) {
        var values = new double[2][dataPoints.size()];
        for (int i = 0; i < dataPoints.size(); i++) {
            values[0][i] = dataPoints.get(i).x();
            values[1][i] = dataPoints.get(i).y();
        }
        return values;
    }
}
