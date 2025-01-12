/*
 * Copyright 2024 JTaccuino Contributors
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
package org.jtaccuino.app.studio;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventType;
import org.jtaccuino.core.ui.Sheet;

public class SheetEvent extends Event {

    private static final long serialVersionUID = 42042042L;

    public static final EventType<SheetEvent> SHEET_OPENED = new EventType<>("SHEET_OPENED");

    public SheetEvent(@NamedArg(value = "source") Sheet sheet, @NamedArg(value = "eventType") EventType<? extends Event> et) {
       super(sheet, null, et);
    }

    @Override
    public Sheet getSource() {
        return (Sheet) super.getSource();
    }

    public EventType<SheetEvent> getEventType() {
        return getEventType();
    }
}
