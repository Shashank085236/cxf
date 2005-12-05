@echo off
@setlocal

cd ..

set BASEDON=Time
set AMOUNT=3
set OPERATION=sendReceiveData
set PACKETSIZE=1
set THREADS=1

:Loop
IF "%1"=="" GOTO Continue
IF "%1"=="-BasedOn" (set BASEDON=%2)
IF "%1"=="-Amount" (set AMOUNT=%2)
IF "%1"=="-Operation" (set OPERATION=%2)
IF "%1"=="-Threads" (set THREADS=%2)
IF "%1"=="-PacketSize" (set PACKETSIZE=%2)
SHIFT
GOTO Loop

:Continue

ant client -Dceltix.running.time=%AMOUNT% -Dceltix.operation=%OPERATION% -Dceltix.basedon=%BASEDON% -Dceltix.packet.size=%PACKETSIZE% -Dceltix.threads=%THREADS%

@endlocal