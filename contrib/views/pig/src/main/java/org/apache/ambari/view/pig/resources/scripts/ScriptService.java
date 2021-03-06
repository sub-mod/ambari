/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.pig.resources.scripts;

import com.google.inject.Inject;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.pig.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;
import org.apache.ambari.view.pig.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.pig.resources.scripts.models.PigScript;
import org.apache.ambari.view.pig.services.BaseService;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;

/**
 * Servlet for scripts
 * API:
 * GET /:id
 *      read script
 * POST /
 *      create new script
 *      Required: title, pigScript
 * GET /
 *      get all scripts of current user
 */
public class ScriptService extends BaseService {
    @Inject
    ViewResourceHandler handler;

    protected ScriptResourceManager resourceManager = null;
    protected final static Logger LOG =
            LoggerFactory.getLogger(ScriptService.class);

    protected synchronized PersonalCRUDResourceManager<PigScript> getResourceManager() {
        if (resourceManager == null) {
            resourceManager = new ScriptResourceManager(context);
        }
        return resourceManager;
    }

    /**
     * Get single item
     */
    @GET
    @Path("{scriptId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getScript(@PathParam("scriptId") String scriptId) {
        PigScript script = null;
        try {
            script = getResourceManager().read(scriptId);
        } catch (ItemNotFound itemNotFound) {
            return Response.status(404).build();
        }
        JSONObject object = new JSONObject();
        object.put("script", script);
        return Response.ok(object).build();
    }

    /**
     * Delete single item
     */
    @DELETE
    @Path("{scriptId}")
    public Response deleteScript(@PathParam("scriptId") String scriptId) {
        try {
            getResourceManager().delete(scriptId);
        } catch (ItemNotFound itemNotFound) {
            return Response.status(404).build();
        }
        return Response.status(204).build();
    }

    /**
     * Get all scripts
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getScriptList() {
        LOG.debug("Getting all scripts");
        List allScripts = getResourceManager().readAll(
                new OnlyOwnersFilteringStrategy(this.context.getUsername()));

        JSONObject object = new JSONObject();
        object.put("scripts", allScripts);
        return Response.ok(object).build();
    }

    /**
     * Update item
     */
    @PUT
    @Path("{scriptId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateScript(PigScriptRequest request,
                                 @PathParam("scriptId") String scriptId) {
        try {
            getResourceManager().update(request.script, scriptId);
        } catch (ItemNotFound itemNotFound) {
            return Response.status(404).build();
        }
        return Response.status(204).build();
    }

    /**
     * Create script
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveScript(PigScriptRequest request, @Context HttpServletResponse response,
                               @Context UriInfo ui) {
        getResourceManager().create(request.script);

        PigScript script = null;

        try {
            script = getResourceManager().read(request.script.getId());
        } catch (ItemNotFound itemNotFound) {
            return Response.status(404).build();
        }

        response.setHeader("Location",
                String.format("%s/%s", ui.getAbsolutePath().toString(), request.script.getId()));

        JSONObject object = new JSONObject();
        object.put("script", script);
        return Response.ok(object).status(201).build();
    }

    public static class PigScriptRequest {
        public PigScript script;
    }
}
