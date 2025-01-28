/*
 * Copyright 2025 JTaccuino Contributors
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

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import org.jtaccuino.core.ui.spi.StatusDisplayerSPI;

public class StatusDisplayerSPIImpl extends StatusDisplayerSPI{

    @Override
    public void display(String displayText) {
        Platform.runLater(() -> {
            var displayNode = StatusLine.getDefault().getStatusLabel();
            displayNode.setText(displayText);
            displayNode.setOpacity(0);

            FadeTransition fadeIn = new FadeTransition(new Duration(500), displayNode);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            // in
            fadeIn.setOnFinished(e -> {
                // out
                FadeTransition fadeOut = new FadeTransition(new Duration(2000), displayNode);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setDelay(Duration.millis(5000));
                fadeOut.play();
            });
            fadeIn.play();
        });
    }
}
