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
package org.jtaccuino.app;

import java.io.IOException;
import java.util.Properties;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.jtaccuino.app.ui.WindowSystem;

public class Studio extends Application {

    private static final String TITLE = "JTaccuino Studio - A scientific notebook powered by Java";
    private static final Properties PROPS = new Properties();

    static {

        try (var input = Studio.class.getResourceAsStream("/app-info.properties")) {
            PROPS.load(input);
        } catch (IOException ex) {
            System.getLogger(Studio.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    public static String getVersion() {
        return PROPS.getProperty("application.version");
    }

    public static void main(String[] args) {
        System.out.println("JTaccuino Studio\n\t Version: " + getVersion());
        System.out.println("================");
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        var node = WindowSystem.getDefault().getNode();
        var scene = new Scene(node);
        scene.getStylesheets().add(this.getClass().getResource("/jtaccuino.css").toExternalForm());
        stage.setScene(scene);
        stage.setHeight(Screen.getPrimary().getBounds().getHeight() * 0.75);
        stage.setWidth(Screen.getPrimary().getBounds().getWidth() * 0.6);
        stage.setTitle(TITLE);
        stage.getIcons().add(new Image("notebook-svgrepo-com_256.png"));
        stage.setOnCloseRequest((event) -> {
            WindowSystem.getDefault().shutdown();
        });
        stage.show();
    }
}
