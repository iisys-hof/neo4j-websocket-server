#!/bin/bash
# Copyright (c) 2012-2013 Institute of Information Systems, Hof University
#
# This file is part of "Neo4j WebSocket Server".
#
# "Neo4j WebSocket Server" is free software: you can redistribute it and/or
# modify it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or (at your
# option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.

PID=""
APP_NAME="Neo4j WebSocket Server"
APP_CLASS="de.hofuniversity.iisys.neo4j.websock.ServiceWebSocket"
APP_COMMAND="java -cp ./*:./dependency-jars/* $APP_CLASS"

function getPID
{
  PID=`ps ax | grep java | grep $APP_CLASS | cut -d " " -f 2`
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

