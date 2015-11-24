#!/bin/bash

PID=""
APP_NAME="Neo4j WebSocket Server"
APP_CLASS="de.hofuniversity.iisys.neo4j.websock.ServiceWebSocket"
APP_COMMAND="java -cp ./:./*:./dependency-jars/* $APP_CLASS"

function getPID
{
  PID=`ps axf | grep java | grep $APP_CLASS | grep -v grep | awk '{print $1}'`
}

function start
{
  getPID

  if [ -z $PID ]; then
    echo -n "Starting $APP_NAME ... "
    nohup $APP_COMMAND > server.log 2>&1 &

    getPID
    echo "DONE"
    echo "see server.log for console output"

  else
    echo "$APP_NAME is already running."
  fi
}

function stop
{
  getPID

  if [ -z $PID ]; then
    echo "$APP_NAME is not running."
    exit 1

  else
    echo -n "Shutting Down $APP_NAME ... "
    kill $PID
    sleep 1
    echo "DONE"
  fi
}

function restart
{
  echo  "Restarting $APP_NAME ..."
  getPID
  if [ -z $PID ]; then
    start
  else
    stop
    start
  fi
}

function status
{
  getPID
  if [ -z  $PID ]; then
    echo "$APP_NAME is not running."
  else
    echo "$APP_NAME is running"
  fi
}

case "$1" in
  start)
    start
    ;;

  stop)
    stop
    ;;

  restart)
    restart
    ;;

  status)
    status
    ;;

  *)
    echo "Commands: $0 {start|stop|restart|status}"
    ;;
esac

