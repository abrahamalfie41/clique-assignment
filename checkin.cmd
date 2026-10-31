@echo off
setlocal

rem Run from the project folder so Maven and the data files can be found.
pushd "%~dp0" || exit /b 1
call mvnw.cmd -q compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=Main "-Dexec.args=%*"
set "CHECKIN_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %CHECKIN_EXIT_CODE%
