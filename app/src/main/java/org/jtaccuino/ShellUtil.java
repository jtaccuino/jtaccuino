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
package org.jtaccuino;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.layout.VBox;
import javax.imageio.ImageIO;
import jdk.jshell.JShell;
import org.jtaccuino.deps.Dependencies;

public class ShellUtil {

    public static ShellUtil INSTANCE = new ShellUtil();

    private final Map<UUID, VBox> sheets = new HashMap<>();
    private final Map<UUID, JShell> shells = new HashMap<>();
    private VBox activeOutput;
    private CellData activeCellData;

    private ShellUtil() {
    }

    public void setActiveOutput(VBox vbox) {
        this.activeOutput = vbox;
    }

    public void display(Node node, UUID uuid) {
        if (null != node) {
            Platform.runLater(() -> {
                try {
                    activeOutput.getChildren().add(node);
                    var snapshot = node.snapshot(new SnapshotParameters(), null);
                    var baos = new ByteArrayOutputStream();
                    var eos = Base64.getEncoder().wrap(baos);
                    try {
                        ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png",eos);
                        baos.flush();
                        baos.close();
                        var displayData = new String(baos.toByteArray());
                        activeCellData.getOutputData().add(CellData.OutputData.of(CellData.OutputData.OutputType.DISPLAY_DATA, Map.of("image/png", displayData)));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }

    public void resolve(String mavenCoordinates, UUID uuid) {
        try {
            var shell = shells.get(uuid);
            Dependencies.resolve(mavenCoordinates).forEach(p -> shell.addToClasspath(p.toString()));
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    public void register(VBox vbox, JShell jshell, UUID uuid) {
        sheets.put(uuid, vbox);
        shells.put(uuid, jshell);
    }

    void setCurrentCellData(CellData cellData) {
        this.activeCellData = cellData;
    }

}
