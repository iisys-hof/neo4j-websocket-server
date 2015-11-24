/*
 * Copyright (c) 2012-2015 Institute of Information Systems, Hof University
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
package de.hofuniversity.iisys.neo4j.websock.calls;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import de.hofuniversity.iisys.neo4j.websock.result.AResultSet;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;

/**
 * Wrapper for a stored procedure, pointing to a Java method that can be
 * executed with different sets of named parameters.
 */
public class NativeProcedure implements IStoredProcedure
{
    private final String fName;
    private final Object fObject;
    private final Method fMethod;
    private final List<String> fParamNames;

    private final int fParamCount;

    /**
     * Creates a named native procedure, which calls the given method on the
     * given object, mapping the sequence of parameters using the given list of
     * parameter names in that order.
     * Name, object and method may not be null.
     *
     * @param name name of the procedure
     * @param object object to call the method on
     * @param method method to call
     * @param paramNames ordered list of parameter names
     */
    public NativeProcedure(String name, Object object, Method method,
        List<String> paramNames)
    {
        if(name == null)
        {
            throw new NullPointerException("name was null");
        }
        if(object == null)
        {
            throw new NullPointerException("object was null");
        }
        if(method == null)
        {
            throw new NullPointerException("method was null");
        }

        fName = name;
        fObject = object;
        fMethod = method;
        fParamNames = paramNames;

        if(fParamNames == null)
        {
            fParamCount = 0;
        }
        else
        {
            fParamCount = fParamNames.size();
        }
    }

    @Override
    public String getName()
    {
        return fName;
    }

    @Override
    public AResultSet<?> call(final Map<String, Object> parameters)
    {
        AResultSet<?> result = null;
        final Object[] args = new Object[fParamCount];

        //extract matching parameters
        String name = null;
        Object param = null;
        for(int i = 0; i < fParamCount; ++i)
        {
            name = fParamNames.get(i);

            if(!WebsockConstants.OPTIONS_MAP.equals(name))
            {
                param = parameters.get(name);
            }
            else
            {
                param = parameters;
            }

            args[i] = param;
        }

        try
        {
            Object resObj = fMethod.invoke(fObject, args);

            if(resObj != null)
            {
                result = (AResultSet<?>) resObj;
            }
        }
        //TODO: verbose error messages
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public boolean isNative()
    {
        return true;
    }
}
