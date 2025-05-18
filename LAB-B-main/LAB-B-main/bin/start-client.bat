@echo off
REM Script per avviare il client tramite Maven su Windows

REM Determina la directory in cui si trova lo script
set "SCRIPT_DIR=%~dp0"
REM Rimuove la barra finale
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

REM Calcola la directory principale del progetto (un livello superiore rispetto a bin)
set "PROJECT_DIR=%SCRIPT_DIR%\.."

REM Vai alla directory del progetto
cd /d "%PROJECT_DIR%"

REM Mostra informazioni
echo Directory dello script: %SCRIPT_DIR%
echo Directory del progetto: %PROJECT_DIR%
echo Avvio del client con Maven senza variabili d'ambiente problematiche...

REM Avvia il client usando Maven
REM Imposta le opzioni Java direttamente nella riga di comando di Maven
call mvn clean javafx:run ^
  -e ^
  -Djavafx.mainClass=book_recommender.lab_b.Client ^
  -Dprism.order=sw ^
  -Dmonocle.platform=Headless ^
  -Dprism.verbose=true ^
  -Djavafx.verbose=true

pause