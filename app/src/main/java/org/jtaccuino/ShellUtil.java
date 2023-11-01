package org.jtaccuino;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import jdk.jshell.JShell;
import org.jtaccuino.deps.Dependencies;

public class ShellUtil {

    public static ShellUtil INSTANCE = new ShellUtil();

    private final Map<UUID, VBox> sheets = new HashMap<>();
    private final Map<UUID, JShell> shells = new HashMap<>();
    private VBox activeOutput;

    private ShellUtil() {
    }

    public void setActiveOutput(VBox vbox) {
        this.activeOutput = vbox;
    }

    public void display(Node node, UUID uuid) {
        Platform.runLater(() -> activeOutput.getChildren().add(node));
    }

    public void resolve(String mavenCoordinates, UUID uuid) {
        var shell = shells.get(uuid);
        Dependencies.resolve(mavenCoordinates).forEach(p -> shell.addToClasspath(p.toString()));
    }

    public void register(VBox vbox, JShell jshell, UUID uuid) {
        sheets.put(uuid, vbox);
        shells.put(uuid, jshell);
    }

}
