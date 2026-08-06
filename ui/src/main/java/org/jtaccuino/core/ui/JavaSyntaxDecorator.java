/*
 * Copyright 2026 JTaccuino Contributors
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
package org.jtaccuino.core.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import jdk.jshell.SourceCodeAnalysis;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.SyntaxDecorator;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import org.jtaccuino.jshell.ReactiveJShell;

class JavaSyntaxDecorator implements SyntaxDecorator {

    private static final int STYLE_KEYWORD = 1;
    private static final int STYLE_DECLARATION = 2;
    private static final int STYLE_ERROR = 3;

    private record JShellHighlight(int start, int end, SourceCodeAnalysis.Attribute attribute) {
    }

    private final ReactiveJShell shell;
    private final PauseTransition debounce;
    private final Tooltip errorTooltip = new Tooltip();
    private ReactiveJShell.ErrorRange tooltipError;
    private StyleAttributeMap baseStyle;
    private StyleAttributeMap keywordStyle;
    private StyleAttributeMap declarationStyle;
    private StyleAttributeMap errorStyle;
    private volatile List<JShellHighlight> highlights = List.of();
    private volatile List<ReactiveJShell.ErrorRange> staticErrors = List.of();
    private final List<ReactiveJShell.ErrorRange> executionErrors = new ArrayList<>();
    private volatile List<ReactiveJShell.ErrorRange> errors = List.of();
    private GutterDecorator gutter;
    private volatile CodeTextModel model;
    private long generation = 0;
    private boolean refreshing = false;

    JavaSyntaxDecorator(ReactiveJShell shell, Font font) {
        this.shell = shell;
        applyFont(font);
        debounce = new PauseTransition(Duration.millis(150));
        debounce.setOnFinished(e -> highlight());
    }

    void applyFont(Font font) {
        baseStyle = style(font, false, false);
        keywordStyle = style(font, true, false);
        declarationStyle = style(font, false, true);
        errorStyle = StyleAttributeMap.builder()
                .setFontFamily(font.getFamily())
                .setFontSize(font.getSize())
                .setTextColor(Color.RED)
                .build();
    }

    void refresh() {
        var currentModel = model;
        if (null != currentModel) {
            refreshing = true;
            try {
                currentModel.fireStyleChangeEvent(TextPos.ZERO, currentModel.getDocumentEnd());
            } finally {
                refreshing = false;
            }
        }
    }

    void installErrorTooltip(CodeArea area) {
        errorTooltip.setAutoHide(true);
        area.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            var textPos = area.getTextPosition(event.getScreenX(), event.getScreenY());
            Optional<ReactiveJShell.ErrorRange> error = Optional.empty();
            if (null != textPos) {
                error = errorAt(globalCharIndex(area, textPos));
            }
            if (error.isPresent()) {
                if (errorTooltip.isShowing()) {
                    errorTooltip.setX(event.getScreenX() + 12);
                    errorTooltip.setY(event.getScreenY() + 12);
                    if (!error.get().equals(tooltipError)) {
                        errorTooltip.setText(error.get().message());
                        tooltipError = error.get();
                    }
                } else {
                    tooltipError = error.get();
                    errorTooltip.setText(error.get().message());
                    errorTooltip.show(area, event.getScreenX() + 12, event.getScreenY() + 12);
                }
            } else {
                tooltipError = null;
                errorTooltip.hide();
            }
        });
        area.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            tooltipError = null;
            errorTooltip.hide();
        });
    }

    private static int globalCharIndex(CodeArea area, TextPos pos) {
        var charIndex = pos.charIndex();
        for (var i = 0; i < pos.index(); i++) {
            charIndex += area.getPlainText(i).length() + 1;
        }
        return charIndex;
    }

    boolean isErrorTooltipShowing() {
        return errorTooltip.isShowing();
    }

    Optional<ReactiveJShell.ErrorRange> errorAt(int offset) {
        return errors.stream()
                .filter(error -> error.start() <= offset && offset < Math.max(error.start() + 1, error.end()))
                .findFirst();
    }

    void setExecutionErrors(List<ReactiveJShell.ErrorRange> ranges) {
        executionErrors.clear();
        executionErrors.addAll(ranges);
        updateErrors();
    }

    void attachGutter(GutterDecorator gutter) {
        this.gutter = gutter;
        updateGutter();
    }

    private void updateErrors() {
        var merged = new ArrayList<ReactiveJShell.ErrorRange>(staticErrors.size() + executionErrors.size());
        merged.addAll(staticErrors);
        merged.addAll(executionErrors);
        errors = List.copyOf(merged);
        updateGutter();
        refresh();
    }

    private void updateGutter() {
        if (null == gutter) {
            return;
        }
        var currentModel = model;
        if (null == currentModel) {
            gutter.setMarkers(Map.of());
            return;
        }
        var byLine = new HashMap<Integer, List<GutterDecorator.GutterMarker>>();
        for (var error : errors) {
            for (var i = 0; i < currentModel.size(); i++) {
                var start = paragraphStart(currentModel, i);
                var end = start + currentModel.getPlainText(i).length();
                if (error.start() <= start && error.end() >= end) {
                    byLine.computeIfAbsent(i, key -> new ArrayList<>())
                            .add(new GutterDecorator.GutterMarker(
                                    GutterDecorator.MarkerKind.ERROR, error.message(), null));
                    break;
                }
            }
        }
        gutter.setMarkers(byLine);
    }

    private static StyleAttributeMap style(Font font, boolean bold, boolean underline) {
        return StyleAttributeMap.builder()
                .setFontFamily(font.getFamily())
                .setFontSize(font.getSize())
                .setBold(bold)
                .setUnderline(underline)
                .build();
    }

    @Override
    public RichParagraph createRichParagraph(CodeTextModel model, int index) {
        var text = model.getPlainText(index);
        var paragraphStart = paragraphStart(model, index);
        var paragraphEnd = paragraphStart + text.length();
        var paragraphStyles = new int[text.length()];
        for (var highlight : highlights) {
            var start = Math.max(highlight.start(), paragraphStart);
            var end = Math.min(highlight.end(), paragraphEnd);
            for (var i = start; i < end; i++) {
                if (SourceCodeAnalysis.Attribute.KEYWORD == highlight.attribute()) {
                    paragraphStyles[i - paragraphStart] = STYLE_KEYWORD;
                } else if (SourceCodeAnalysis.Attribute.DECLARATION == highlight.attribute()) {
                    paragraphStyles[i - paragraphStart] = STYLE_DECLARATION;
                }
            }
        }
        for (var error : errors) {
            var start = Math.max(error.start(), paragraphStart);
            var end = Math.min(error.end(), paragraphEnd);
            if (end < start) {
                continue;
            }
            if (end == start) {
                var position = error.end();
                if (position < paragraphStart || position >= paragraphEnd) {
                    continue;
                }
                end = start + 1;
            }
            for (var i = start; i < end; i++) {
                paragraphStyles[i - paragraphStart] = STYLE_ERROR;
            }
        }
        var builder = RichParagraph.builder();
        var runStart = 0;
        var runStyle = runStyle(paragraphStyles, 0);
        for (var i = 1; i <= paragraphStyles.length; i++) {
            var style = runStyle(paragraphStyles, i);
            if (style != runStyle) {
                builder.addSegment(text.substring(runStart, i), styleMap(runStyle));
                runStart = i;
                runStyle = style;
            }
        }
        for (var error : errors) {
            var start = Math.max(error.start(), paragraphStart);
            var end = Math.min(error.end(), paragraphEnd);
            if (end < start) {
                continue;
            }
            if (end == start) {
                var position = error.end();
                if (position < paragraphStart || position >= paragraphEnd) {
                    continue;
                }
                end = start + 1;
            }
            if (error.start() <= paragraphStart && error.end() >= paragraphEnd) {
                continue;
            }
            builder.addWavyUnderline(start - paragraphStart, end - paragraphStart, Color.RED);
        }
        return builder.build();
    }

    private static int runStyle(int[] styles, int index) {
        return index < styles.length ? styles[index] : -1;
    }

    private StyleAttributeMap styleMap(int style) {
        return switch (style) {
            case STYLE_KEYWORD -> keywordStyle;
            case STYLE_DECLARATION -> declarationStyle;
            case STYLE_ERROR -> errorStyle;
            default -> baseStyle;
        };
    }

    @Override
    public void handleChange(CodeTextModel model, TextPos start, TextPos end, int charsTop, int linesAdded, int charsBottom) {
        this.model = model;
        if (!refreshing) {
            executionErrors.clear();
            debounce.playFromStart();
        }
    }

    private void highlight() {
        var currentModel = model;
        if (null == currentModel) {
            return;
        }
        var text = currentModel.getPlainText(0);
        var paragraphCount = currentModel.size();
        var fullText = new StringBuilder(text);
        for (var i = 1; i < paragraphCount; i++) {
            fullText.append('\n').append(currentModel.getPlainText(i));
        }
        var currentGeneration = ++generation;
        shell.highlightingAsync(fullText.toString(), result -> {
            Platform.runLater(() -> {
                if (currentGeneration == generation) {
                    highlights = result.stream()
                            .filter(h -> h.attributes().contains(SourceCodeAnalysis.Attribute.KEYWORD)
                                    || h.attributes().contains(SourceCodeAnalysis.Attribute.DECLARATION))
                            .map(h -> {
                                var attribute = h.attributes().contains(SourceCodeAnalysis.Attribute.KEYWORD)
                                        ? SourceCodeAnalysis.Attribute.KEYWORD
                                        : SourceCodeAnalysis.Attribute.DECLARATION;
                                return new JShellHighlight(h.start(), h.end(), attribute);
                            })
                            .toList();
                    refresh();
                }
            });
        });
        if (shell.hasExecutedUserCode()) {
            shell.parseErrorsAsync(fullText.toString(), result -> {
                Platform.runLater(() -> {
                    if (currentGeneration == generation) {
                        staticErrors = result;
                        updateErrors();
                    }
                });
            });
        } else {
            Platform.runLater(() -> {
                if (currentGeneration == generation && !staticErrors.isEmpty()) {
                    staticErrors = List.of();
                    updateErrors();
                }
            });
        }
    }

    void analyzeNow() {
        Platform.runLater(this::highlight);
    }

    private static int paragraphStart(CodeTextModel model, int index) {
        var start = 0;
        for (var i = 0; i < index; i++) {
            start += model.getPlainText(i).length() + 1;
        }
        return start;
    }
}
