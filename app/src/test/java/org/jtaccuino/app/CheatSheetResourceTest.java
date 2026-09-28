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
package org.jtaccuino.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CheatSheetResourceTest {

    @Test
    void cheatSheetIsBundledInTheApplication() throws IOException {
        try (var in = CheatSheetResourceTest.class.getResourceAsStream("/cheat-sheet.pdf")) {
            assertNotNull(in, "cheat-sheet.pdf should be bundled in the application");
            assertEquals("%PDF", new String(in.readNBytes(4), StandardCharsets.US_ASCII));
        }
    }
}
