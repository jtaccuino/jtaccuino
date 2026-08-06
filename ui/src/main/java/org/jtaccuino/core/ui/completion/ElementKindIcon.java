/*
 * Copyright 2025-2026 JTaccuino Contributors
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
package org.jtaccuino.core.ui.completion;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;
import javax.lang.model.element.ElementKind;

/**
 * A 16x16 iconographic glyph for a Java element, rendered in the abstract
 * shape language used by the Apache NetBeans completion popup: classes are a
 * blue square with an amber diamond and a pink circle, methods a pink circle,
 * fields two overlapping blue squares, constructors an amber diamond, and so
 * on. The shape outlines are drawn in a translucent black as in NetBeans.
 */
public class ElementKindIcon extends Group {

    private static final Color CLASS_BLUE = Color.web("#a7cdea");
    private static final Color CLASS_AMBER = Color.web("#f0c373");
    private static final Color CLASS_PINK = Color.web("#ffb0a9");
    private static final Color CONSTANT_RED = Color.web("#ff7063");
    private static final Color INTERFACE_SURFACE = Color.web("#e5e5e5");
    private static final Color INTERFACE_BORDER = Color.web("#9a9a9a");
    private static final Color VARIABLE_SURFACE = Color.web("#dde5ef");
    private static final Color VARIABLE_BORDER = Color.web("#65727f");
    private static final Color FOLDER_BODY = Color.web("#ecc66b");
    private static final Color FOLDER_LINE = Color.web("#af9447");
    private static final Color FOLDER_BORDER = Color.web("#775e20");
    private static final Color FOLDER_HIGHLIGHT = Color.rgb(234, 212, 167, 0.5);
    private static final Color FOLDER_LABEL = Color.web("#f5eedc");
    private static final Color ANNOTATION_GREEN = Color.web("#6fae42");
    private static final Color ANNOTATION_DOT = Color.web("#f5eedc");
    private static final Color KEYWORD_RED = Color.web("#f7768e");
    private static final Color UNKNOWN_GRAY = Color.web("#9a9a9a");
    private static final Color SHAPE_BORDER = Color.rgb(0, 0, 0, 0.33);

    @SuppressWarnings("this-escape")
    public ElementKindIcon(ElementKind elementKind, boolean keyword, boolean staticMember) {
        getStyleClass().add("completion-icon");
        Rectangle canvas = new Rectangle(0, 0, 16, 16);
        canvas.setFill(Color.TRANSPARENT);
        canvas.setStroke(Color.TRANSPARENT);
        getChildren().add(canvas);
        if (keyword) {
            addBadge(KEYWORD_RED);
        } else if (elementKind == null) {
            addBadge(UNKNOWN_GRAY);
        } else {
            switch (elementKind) {
                case CLASS, RECORD -> classShape();
                case INTERFACE -> interfaceShape();
                case ENUM -> enumShape();
                case ENUM_CONSTANT, FIELD, BINDING_VARIABLE -> fieldShape(staticMember);
                case ANNOTATION_TYPE -> annotationShape();
                case METHOD -> methodShape(staticMember);
                case CONSTRUCTOR -> constructorShape();
                case PACKAGE -> packageShape();
                case MODULE, PARAMETER, LOCAL_VARIABLE, RESOURCE_VARIABLE,
                        EXCEPTION_PARAMETER, TYPE_PARAMETER -> variableShape();
                case null, default -> addBadge(UNKNOWN_GRAY);
            }
        }
    }

    private void classShape() {
        addShape(new Rectangle(0, 6, 9, 9), CLASS_BLUE);
        addShape(new Polygon(2, 5.5, 7.5, 0, 13, 5.5, 7.5, 11), CLASS_AMBER);
        addShape(new Circle(11.5, 10.5, 4.5), CLASS_PINK);
    }

    private void interfaceShape() {
        addShape(new Rectangle(3.5, 6.5, 7, 2), INTERFACE_SURFACE, INTERFACE_BORDER);
        addShape(new Circle(12.5, 7.5, 3), INTERFACE_SURFACE, INTERFACE_BORDER);
        addShape(new Circle(2.5, 7.5, 2), INTERFACE_SURFACE, INTERFACE_BORDER);
    }

    private void enumShape() {
        addShape(new Rectangle(0, 5.864, 9, 9), CLASS_BLUE);
        addShape(new Circle(11.5, 10.5, 4.5), CLASS_PINK);
        addShape(new Polygon(11.5, 0, 7, 8, 16, 8), CONSTANT_RED);
        addShape(new Polygon(0.034, 4.5, 4.534, 0, 9.034, 4.5, 4.534, 9), CLASS_AMBER);
    }

    private void methodShape(boolean staticMember) {
        addShape(new Circle(8, 9.5, 5.5), CLASS_PINK);
        if (staticMember) {
            addStaticBar(CLASS_PINK);
        }
    }

    private void constructorShape() {
        addShape(new Polygon(2.5, 9.5, 8, 4, 13.5, 9.5, 8, 15), CLASS_AMBER);
    }

    private void fieldShape(boolean staticMember) {
        addShape(new Rectangle(2.5, 4, 11, 11), CLASS_BLUE);
        addShape(new Rectangle(2.5, 4, 11, 11), CLASS_BLUE);
        if (staticMember) {
            addStaticBar(CLASS_BLUE);
        }
    }

    private void variableShape() {
        addShape(new Rectangle(3.5, 2.5, 10, 10), VARIABLE_SURFACE, VARIABLE_BORDER);
    }

    private void annotationShape() {
        addShape(new Rectangle(3.5, 3.5, 9, 9), ANNOTATION_GREEN);
        addShape(new Circle(8, 8, 2.2), ANNOTATION_DOT, null);
    }

    private void packageShape() {
        addShape(new Polygon(1, 15, 16, 15, 16, 4, 14, 1, 3, 1, 1, 4), FOLDER_BODY, FOLDER_BORDER);
        addShape(new Polygon(16, 4, 14, 1, 3, 1, 1, 4, 2, 4.303, 15, 4.303), FOLDER_HIGHLIGHT, null);
        addShape(new Rectangle(7.5, 4.3, 1, 10.7), FOLDER_LINE, null);
        addShape(new Rectangle(1, 8.5, 15, 1), FOLDER_LINE, null);
        addShape(new Rectangle(2, 4.303, 13, 0.697), FOLDER_LINE, null);
        addShape(new Rectangle(11, 6, 3, 2), FOLDER_LABEL, null);
    }

    private void addBadge(Color fill) {
        var badge = new Rectangle(4, 4, 8, 8);
        badge.setArcWidth(2);
        badge.setArcHeight(2);
        addShape(badge, fill);
    }

    private void addStaticBar(Color fill) {
        addShape(new Rectangle(6.5, 3, 3, 13), fill);
    }

    private void addShape(Shape shape, Color fill) {
        addShape(shape, fill, SHAPE_BORDER);
    }

    private void addShape(Shape shape, Color fill, Color stroke) {
        shape.setFill(fill);
        shape.setStroke(stroke);
        shape.setStrokeWidth(1);
        shape.setStrokeType(StrokeType.INSIDE);
        getChildren().add(shape);
    }
}
