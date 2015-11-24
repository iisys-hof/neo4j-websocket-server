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
package de.hofuniversity.iisys.neo4j.websock.procedures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.graphdb.GraphDatabaseService;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.WebsockContextHandler;
import de.hofuniversity.iisys.neo4j.websock.calls.IStoredProcedure;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Creates a loader for native procedures using Guice for dependency injection,
 * based on a list of definition files specified in the configuration.
 *
 * Definitions are read in the format:
 * <guice module class>
 * <provider name 1>=<IProcedureProvider class 1>
 * ...
 * <provider name n>=<IProcedureProvider class n>
 * <empty line>
 * <next name>
 *
 */
public class GuiceProcedureLoader extends AbstractModule
    implements IProcedureProvider
{
    public static final String NATIVE_FILES = "websocket.stored.native";

    private final GraphDatabaseService fDb;
    private final GraphConfig fConfig;
    private final ImplUtil fImpl;

    private final Logger fLogger;

    private Injector fInjector;

    private String[] fFiles;

    /**
     * Creates a native procedure loader creating procedures using the given
     * graph database.
     * The given database and implementation utility must not be null.
     *
     * @param database database to execute queries on
     * @param impl implementation utility to bind
     */
    public GuiceProcedureLoader(GraphDatabaseService database, ImplUtil impl)
    {
        if(database == null)
        {
            throw new NullPointerException("database service was null");
        }
        if(impl == null)
        {
            throw new NullPointerException("implementation utility was null");
        }

        fDb = database;

        fConfig = WebsockContextHandler.getInstance().getConfig();
        String filesString = fConfig.getProperty(NATIVE_FILES);
        fImpl = impl;

        if(filesString != null && !filesString.isEmpty())
        {
            fFiles = filesString.split(";");
        }

        fLogger = Logger.getLogger(this.getClass().getName());

        //create initial injector
        fInjector = Guice.createInjector(this);
    }

    @Override
    protected void configure()
    {
        bind(GraphDatabaseService.class).toInstance(fDb);
        bind(ImplUtil.class).toInstance(fImpl);
        bind(GraphConfig.class).toInstance(fConfig);
    }

    @Override
    public Map<String, IStoredProcedure> getProcedures()
    {
        final Map<String, IStoredProcedure> procedures =
            new HashMap<String, IStoredProcedure>();

        if(fFiles != null && fFiles.length > 0)
        {
            for(String file : fFiles)
            {
                try
                {
                    List<String> providers = getProviders(file);
                    addProcedures(procedures, providers);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    fLogger.log(Level.SEVERE, "could not load native " +
                        "procedure definition file: " + file, e);
                }
            }
        }

        return procedures;
    }

    private List<String> getProviders(String file) throws Exception
    {
        //load all guice modules, return list of named providers
        final BufferedReader reader = new BufferedReader(
            new FileReader(new File(file)));

        Module mod = null;

        final List<Module> modules = new LinkedList<Module>();
        final List<String> providers = new LinkedList<String>();

        String line = reader.readLine();

        //load modules
        while(line != null)
        {
            //skip comments
            if(line.startsWith("#"))
            {
                line = reader.readLine();
                continue;
            }
            else if(line.isEmpty())
            {
                mod = null;
            }
            else if(mod == null)
            {
                mod = getModule(line);
                modules.add(mod);
            }
            else
            {
                providers.add(line);
            }

            line = reader.readLine();
        }

        //extend injector
        fInjector = fInjector.createChildInjector(modules);

        return providers;
    }

    @SuppressWarnings("unchecked")
    private Module getModule(String className)
    {
        Module mod = null;

        try
        {
            Class<? extends Module> modClass =
                (Class<? extends Module>) Class.forName(className);

            mod = modClass.newInstance();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fLogger.log(Level.SEVERE, "failed to load guice module :"
                + className, e);
        }


        return mod;
    }

    private void addProcedures(final Map<String, IStoredProcedure> procedures,
        List<String> providers)
    {
        Map<String, IStoredProcedure> newProcs = null;

        IProcedureProvider provider = null;
        Key<IProcedureProvider> key = null;

        for(String provName : providers)
        {
            try
            {
                key = Key.get(IProcedureProvider.class, Names.named(provName));
                provider = fInjector.getInstance(key);
            }
            catch(Exception e)
            {
                e.printStackTrace();
                fLogger.log(Level.SEVERE, "failed to load procedure provider :"
                    + provName, e);
            }

            if(provider != null)
            {
                newProcs = provider.getProcedures();
                addAll(procedures, newProcs, provName);

                provider = null;
            }
        }
    }

    private void addAll(final Map<String, IStoredProcedure> procedures,
        final Map<String, IStoredProcedure> newProcs, String provName)
    {
        String key = null;
        IStoredProcedure proc = null;

        for(Entry<String, IStoredProcedure> procE : newProcs.entrySet())
        {
            key = procE.getKey();
            proc = procE.getValue();

            proc = procedures.put(key, proc);

            if(proc != null)
            {
                fLogger.log(Level.WARNING, "provider " + provName
                    + " overwrites existing procedure " + key);
            }
        }
    }
}
