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
package de.hofuniversity.iisys.neo4j.websock.calls;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import org.junit.Test;

import de.hofuniversity.iisys.neo4j.websock.result.AResultSet;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;

/**
 * Test for the native procedure wrapper around native Java methods.
 */
public class NativeProcedureTest
{
    private static final String PARAM_1 = "param1";
    private static final String PARAM_2 = "param2";
    private static final String PARAM_3 = "param3";

    private static final int INT_1 = 42;
    private static final int INT_2 = 27;

    private static final String NO_PARAM_METHOD = "testMethod1";
    private static final String THREE_PARAM_METHOD = "testMethod2";

    /**
     * Tests the valid calling of methods.
     */
    @Test
    public void testCall() throws Exception
    {
        NativeTestObject host = new NativeTestObject();

        //method with no parameters
        Method method = host.getClass().getMethod(NO_PARAM_METHOD);
        NativeProcedure procedure = new NativeProcedure(NO_PARAM_METHOD, host,
            method, null);

        //call, check and reset
        AResultSet<?> set = procedure.call(null);
        Assert.assertNotNull(set);

        Assert.assertTrue(host.noParamCalled());

        host.clear();

        //method with multiple parameters
        method = host.getClass().getMethod(THREE_PARAM_METHOD, Integer.TYPE,
            List.class, Map.class);
        List<String> paramList = new ArrayList<String>();
        paramList.add(PARAM_1);
        paramList.add(PARAM_2);
        paramList.add(PARAM_3);
        procedure = new NativeProcedure(THREE_PARAM_METHOD, host,
            method, paramList);

        //call with all parameters
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(PARAM_1, INT_1);
        List<Object> listParam = new ArrayList<Object>();
        parameters.put(PARAM_2, listParam);
        Map<String, Object> mapParam = new HashMap<String, Object>();
        parameters.put(PARAM_3, mapParam);

        set = procedure.call(parameters);
        Assert.assertNull(set);

        Assert.assertTrue(host.ThreeParamCalled());
        Assert.assertEquals(INT_1, host.getParam1());
        Assert.assertEquals(listParam, host.getParam2());
        Assert.assertEquals(mapParam, host.getParam3());

        host.clear();

        //call without optional parameter
        parameters = new HashMap<String, Object>();
        parameters.put(PARAM_1, INT_2);
        listParam = new ArrayList<Object>();
        parameters.put(PARAM_2, listParam);

        set = procedure.call(parameters);
        Assert.assertNull(set);

        Assert.assertTrue(host.ThreeParamCalled());
        Assert.assertEquals(INT_2, host.getParam1());
        Assert.assertEquals(listParam, host.getParam2());
        Assert.assertNull(host.getParam3());

        host.clear();
    }

    /**
     * Tests faulty calls to working methods.
     */
    @Test
    public void testFaultyCall() throws Exception
    {
        NativeTestObject host = new NativeTestObject();

        //method with multiple parameters
        Method method = host.getClass().getMethod(THREE_PARAM_METHOD,
            Integer.TYPE, List.class, Map.class);
        List<String> paramList = new ArrayList<String>();
        paramList.add(PARAM_1);
        paramList.add(PARAM_2);
        paramList.add(PARAM_3);
        NativeProcedure procedure = new NativeProcedure(THREE_PARAM_METHOD,
            host, method, paramList);

        //call with non-optional parameter misspelled
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("pram1", INT_1);
        List<Object> listParam = new ArrayList<Object>();
        parameters.put(PARAM_2, listParam);
        Map<String, Object> mapParam = new HashMap<String, Object>();
        parameters.put(PARAM_3, mapParam);

        boolean fail = false;
        try
        {
            procedure.call(parameters);
        }
        catch(Exception e)
        {
            fail = true;
        }
        Assert.assertTrue(fail);

        host.clear();

        //call with parameters with a wrong type
        parameters = new HashMap<String, Object>();
        parameters.put(PARAM_1, INT_1);
        listParam = new ArrayList<Object>();
        mapParam = new HashMap<String, Object>();
        parameters.put(PARAM_2, mapParam);
        parameters.put(PARAM_3, listParam);

        fail = false;
        try
        {
            procedure.call(parameters);
        }
        catch(Exception e)
        {
            fail = true;
        }
        Assert.assertTrue(fail);
    }

    public class NativeTestObject
    {
        private boolean fNoParamCalled = false;
        private boolean fThreeParamCalled = false;

        private int fParam1 = 0;
        private List<Object> fParam2 = null;
        private Map<String, Object> fParam3 = null;

        public AResultSet<?> testMethod1()
        {
            fNoParamCalled = true;

            return new SingleResult(new HashMap<String, Object>());
        }

        public void testMethod2(int param1, List<Object> param2,
            Map<String, Object> param3)
        {
            fThreeParamCalled = true;
            fParam1 = param1;
            fParam2 = param2;
            fParam3 = param3;
        }

        public boolean noParamCalled()
        {
            return fNoParamCalled;
        }

        public boolean ThreeParamCalled()
        {
            return fThreeParamCalled;
        }

        public int getParam1()
        {
            return fParam1;
        }

        public List<Object> getParam2()
        {
            return fParam2;
        }

        public Map<String, Object> getParam3()
        {
            return fParam3;
        }

        public void clear()
        {
            fNoParamCalled = false;
            fThreeParamCalled = false;

            fParam1 = 0;
            fParam2 = null;
            fParam3 = null;
        }
    }
}
