@ECHO OFF
SETLOCAL
SET L4J_LOG_NAME=launch4j.log
SET EDITOR_LOG_NAME=ftl-editor-log.txt
SET L4J_LOG_PATH=%~dp0launch4j.log
SET EDITOR_LOG_PATH=%~dp0ftl-editor-log.txt

IF EXIST "%L4J_LOG_PATH%" DEL /Q "%L4J_LOG_PATH%"
IF EXIST "%EDITOR_LOG_PATH%" DEL /Q "%EDITOR_LOG_PATH%"

ECHO.
ECHO.
ECHO This script will try to run the Profile/SavedGame Editor.
ECHO.
ECHO When it exits, logs will be presented in multiple notepad windows.
ECHO.
ECHO.
ECHO.


ECHO Some users reported interface glitches if they had custom Windows themes.
ECHO Setting 'use_default_ui=true' in ftl-editor.cfg is a workaround.
ECHO.
SET /P YN="Create a new ftl-editor.cfg to do this? (y/N): "
ECHO.

IF /I [%YN%]==[y] (
  ECHO Creating a new config.
  ECHO use_default_ui=true>ftl-editor.cfg
  ECHO.
)
ECHO.


ECHO Running the Editor...
ECHO.
"%~dp0FTLProfileEditor.exe" --l4j-debug

ECHO Spawning notepad...
ECHO.
IF EXIST "%L4J_LOG_PATH%" (
  ECHO For Java issues, see "%L4J_LOG_NAME%"
  ECHO.
  START /B notepad.exe "%L4J_LOG_PATH%"
) ELSE (
  ECHO Missing log: "%L4J_LOG_NAME%"
)

IF EXIST "%EDITOR_LOG_PATH%" (
  ECHO For Editor issues, see "%EDITOR_LOG_NAME%"
  ECHO.
  START /B notepad.exe "%EDITOR_LOG_PATH%"
) ELSE (
  ECHO Missing log: "%EDITOR_LOG_NAME%"
)

ECHO.
ECHO.
ECHO Interesting logs can be pasted on the FTL forum between [code] [/code] tags.
ECHO.
ECHO.
ECHO This script will now exit.
ECHO.
PAUSE
ENDLOCAL & EXIT /B
