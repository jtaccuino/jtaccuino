@ECHO OFF

rem ------ ENVIRONMENT --------------------------------------------------------

rem See https://github.com/dlemmermann/JPackageScriptFX

rem The script depends on various environment variables to exist in order to
rem run properly:
rem
rem PROJECT_VERSION=1.1.0-SNAPSHOT
rem APP_VERSION=1.0.0

set JAVA_VERSION=23
set MAIN_JAR="app.jar"
set APP_VERSION=1.0.0

rem Set desired installer type: "app-image" "msi" "exe".
set INSTALLER_TYPE=msi

rem ------ SETUP DIRECTORIES AND FILES ----------------------------------------
rem Remove previously generated java runtime and installers. Copy all required
rem jar files into the input/libs folder.

IF EXIST target\java-runtime rmdir /S /Q target\java-runtime
IF EXIST target\installer rmdir /S /Q target\installer

mkdir target\installer\input\libs

tar -xf app\build\distributions\app.zip -C target\installer\input
xcopy /S /Q target\installer\input\app\lib\* target\installer\input\libs\

rem ------ REQUIRED MODULES ---------------------------------------------------
rem Use jlink to detect all modules that are required to run the application.
rem Starting point for the jdep analysis is the set of jars being used by the
rem application.

echo detecting required modules

"%JAVA_HOME%\bin\jdeps" ^
  -q ^
  --multi-release %JAVA_VERSION% ^
  --ignore-missing-deps ^
  --class-path "target\installer\input\libs\*" ^
  --print-module-deps app\build\classes\java\main\org\jtaccuino\app\StudioLauncher.class > temp.txt

set /p detected_modules=<temp.txt

echo detected modules: %detected_modules%

rem ------ MANUAL MODULES -----------------------------------------------------
rem jdk.crypto.ec has to be added manually bound via --bind-services or
rem otherwise HTTPS does not work.
rem
rem See: https://bugs.openjdk.java.net/browse/JDK-8221674
rem
rem In addition we need jdk.localedata if the application is localized.
rem This can be reduced to the actually needed locales via a jlink parameter,
rem e.g., --include-locales=en,de.
rem
rem Do not forget the leading ','!

set manual_modules=,java.desktop,java.naming,jdk.unsupported,jdk.jshell,java.logging,java.net.http,java.sql,java.sql.rowset,java.transaction.xa,java.xml,jdk.localedata
echo manual modules: %manual_modules%

rem ------ RUNTIME IMAGE ------------------------------------------------------
rem Use the jlink tool to create a runtime image for our application. We are
rem doing this in a separate step instead of letting jlink do the work as part
rem of the jpackage tool. This approach allows for finer configuration and also
rem works with dependencies that are not fully modularized, yet.

echo creating java runtime image

call "%JAVA_HOME%\bin\jlink" ^
  --strip-native-commands ^
  --no-header-files ^
  --no-man-pages ^
  --compress=2 ^
  --strip-debug ^
  --add-modules %detected_modules%%manual_modules% ^
  --include-locales=en ^
  --output target/java-runtime

rem ------ PACKAGING ----------------------------------------------------------
rem In the end we will find the package inside the target/installer directory.

echo Creating installer

call "%JAVA_HOME%\bin\jpackage" ^
  --type %INSTALLER_TYPE% ^
  --dest target/installer ^
  --input target/installer/input/libs ^
  --name JTaccuinoStudio ^
  --main-class org.jtaccuino.app.StudioLauncher ^
  --main-jar %MAIN_JAR% ^
  --java-options "-Xmx2048m --enable-preview" ^
  --runtime-image target/java-runtime ^
  --icon app/src/main/logo/windows/notebook.ico ^
  --app-version %APP_VERSION% ^
  --vendor "JTaccuino" ^
  --copyright "Copyright © 2024 JTaccuino Project" ^
  --win-dir-chooser ^
  --win-shortcut ^
  --win-per-user-install ^
  --win-menu ^
  --verbose
