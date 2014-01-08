/*
 * Copyright (c) 2012-2013 Institute of Information Systems, Hof University
 *
 * This file is part of "Neo4j WebSocket Server".
 *
 * "Neo4j WebSocket Server" is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.hofuniversity.iisys.neo4j.websock.neo4j;

import org.neo4j.graphdb.RelationshipType;

/**
 * Relationship types for the graph database.
 */
public enum Neo4jRelTypes implements RelationshipType
{
    KNOWS,
    FRIEND_OF,
    DELETED,
    MEMBER_OF,
    OWNER_OF,
    OWNS,
    ACCOUNT,
    CAME_FROM,
    SENT,
    SENT_TO,
    REPLY_TO,
    LOCATED_AT,
    CURRENTLY_AT,
    HAS_DATA,
    USED_BY,
    TAGGED,
    HAS_TAG,
    ACTED,
    CONTAINS,
    AFFILIATED,
    ACTOR,
    GENERATOR,
    OBJECT,
    PROVIDER,
    TARGET,
    HAS_ICON,
    ATTACHED,
    AUTHOR,
    EMAILS,
    IMS,
    PHONE_NUMS,
    PHOTOS,
    FRIEND_REQUEST,
    UPLOADED;
}
