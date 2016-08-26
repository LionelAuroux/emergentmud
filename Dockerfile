#    EmergentMUD - A modern MUD with a procedurally generated world.
#    Copyright (C) 2016 BoneVM, LLC
#
#    This file is part of EmergentMUD.
#
#    EmergentMUD is free software: you can redistribute it and/or modify
#    it under the terms of the GNU Affero General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    EmergentMUD is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU Affero General Public License for more details.
#
#    You should have received a copy of the GNU Affero General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.

FROM ingensi/oracle-jdk
MAINTAINER Peter Keeler <scion@emergentmud.com>
EXPOSE 8080
RUN ./gradlew clean build
COPY build/libs/emergentmud-*.jar /opt/mud/app.jar
CMD ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005","-jar","/opt/mud/app.jar"]