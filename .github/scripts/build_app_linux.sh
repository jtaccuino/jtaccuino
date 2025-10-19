#!/bin/bash

# ------ ENVIRONMENT --------------------------------------------------------
# The script depends on various environment variables to exist in order to
# run properly. The java version we want to use, the location of the java
# binaries (java home), and the project version as defined inside the pom.xml
# file, e.g. 1.0-SNAPSHOT.
#
# VERSION: version used in pom.xml, e.g. 1.0-SNAPSHOT , shown in "about" dialog
# APP_VERSION: the application version, e.g. 1.0.0 

JAVA_VERSION=25
MAIN_JAR="app-${VERSION}.jar"

echo "java home: $JAVA_HOME"
echo "project version: $VERSION"
echo "app version: $APP_VERSION"
echo "main JAR file: $MAIN_JAR"

# ------ SETUP DIRECTORIES AND FILES ----------------------------------------
# Remove previously generated java runtime and installers. Copy all required
# jar files into the input/libs folder.

rm -rfd ./target/java-runtime/
rm -rfd target/installer/input

mkdir -p target/installer/input/libs/

unzip app/build/distributions/app-${VERSION}.zip -d target/installer/input
cp target/installer/input/app-${VERSION}/lib/* target/installer/input/libs

# ------ REQUIRED MODULES ---------------------------------------------------
# Use jlink to detect all modules that are required to run the application.
# Starting point for the jdep analysis is the set of jars being used by the
# application.

echo "detecting required modules"
detected_modules=`$JAVA_HOME/bin/jdeps \
  -q \
  --multi-release ${JAVA_VERSION} \
  --ignore-missing-deps \
  --print-module-deps \
  --class-path "target/installer/input/libs/*" \
    app/build/classes/java/main/org/jtaccuino/app/StudioLauncher.class`
echo "detected modules: ${detected_modules}"


# ------ MANUAL MODULES -----------------------------------------------------
# jdk.crypto.ec has to be added manually bound via --bind-services or
# otherwise HTTPS does not work.
#
# See: https://bugs.openjdk.java.net/browse/JDK-8221674
#
# In addition we need jdk.localedata if the application is localized.
# This can be reduced to the actually needed locales via a jlink parameter,
# e.g., --include-locales=en,de.
#
# Don't forget the leading ','!

manual_modules=,java.desktop,java.naming,jdk.unsupported,jdk.jshell,java.logging,java.net.http,java.sql,java.sql.rowset,java.transaction.xa,java.xml,jdk.localedata
incubating_modules=,jdk.incubator.vector
echo "manual modules: ${manual_modules}"
echo "incubating modules: ${incubating_modules}"

# ------ RUNTIME IMAGE ------------------------------------------------------
# Use the jlink tool to create a runtime image for our application. We are
# doing this in a separate step instead of letting jlink do the work as part
# of the jpackage tool. This approach allows for finer configuration and also
# works with dependencies that are not fully modularized, yet.

echo "creating java runtime image"
$JAVA_HOME/bin/jlink \
  --strip-native-commands \
  --no-header-files \
  --no-man-pages  \
  --compress=2  \
  --strip-debug \
  --add-modules "${detected_modules}${manual_modules}${incubating_modules}" \
  --include-locales=en,de \
  --output target/java-runtime

# ------ PACKAGING ----------------------------------------------------------
# In the end we will find the package inside the target/installer directory.

echo "Creating installer of type $INSTALLER_TYPE"

$JAVA_HOME/bin/jpackage \
--verbose \
--dest target/installer \
--input target/installer/input/libs \
--name JTaccuinoStudio \
--main-class org.jtaccuino.app.StudioLauncher \
--main-jar ${MAIN_JAR} \
--java-options "--enable-preview --add-modules jdk.incubator.vector --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --enable-native-access=ALL-UNNAMED" \
--runtime-image target/java-runtime \
--icon app/src/main/logo/linux/notebook.png \
--app-version ${APP_VERSION} \
--copyright "Copyright © 2025 JTaccuino Project" \
--vendor JTaccuino \
"$@"
