package org.jtaccuino;

import java.util.List;
import java.util.Map;

public record Notebook(Map<String, Object> metadata, int nbformat, int nbformat_minor, List<Cell> cells) {

    public static record Cell(String cell_type, Map<String, Object> metadata, String source, List<Output> outputs) {

    }

    public static record Output(String output_type, Map<String, Object> data, Map<String, Object> metadata) {

    }
}
