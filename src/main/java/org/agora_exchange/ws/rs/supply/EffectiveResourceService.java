/*
 *  Agora Exchange for Online Managed Services
 *
 *  Copyright (C) 2012 Sakari A. Maaranen
 */
package org.agora_exchange.ws.rs.supply;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

import java.net.HttpURLConnection;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.agora_exchange.ws.rs.Service;
import org.agora_exchange.xml.supply.EffectiveResource;

/**
 * @author Sakari Maaranen
 */
@Path("resources")
@Stateless
public class EffectiveResourceService extends Service<EffectiveResource>
{
    @PersistenceContext(unitName = "org.agora_exchange.xml")
    private EntityManager manager;

    public EffectiveResourceService() {
        super(EffectiveResource.class);
    }

    @Override
    public void copy(EffectiveResource source, EffectiveResource target,
            EntityManager em) {
        // TODO:
    }

    @POST
    @Consumes({ APPLICATION_XML, APPLICATION_JSON })
    @Produces({ APPLICATION_XML, APPLICATION_JSON })
    public Response post(EffectiveResource jaxb) {
        return post(jaxb, manager);
    }

    @PUT
    @Path("{id}")
    @Consumes({ APPLICATION_XML, APPLICATION_JSON })
    @Produces({ APPLICATION_XML, APPLICATION_JSON })
    public Response put(@PathParam("id") String id, EffectiveResource jaxb) {
        return put(id, jaxb, manager);
    }

    @GET
    @Path("{id}")
    @Produces({ APPLICATION_XML, APPLICATION_JSON })
    public Response get(@PathParam("id") String id) {
        return get(id, manager);
    }

    @DELETE
    @Path("{id}")
    @Produces({ APPLICATION_XML, APPLICATION_JSON })
    public Response delete(@PathParam("id") String id) {
        return delete(id, manager);
    }

    @GET
    @Path("/")
    @Produces({ APPLICATION_XML, APPLICATION_JSON })
    public Response getByReservation(
            @QueryParam("reservation") String reservationId) {

        TypedQuery<EffectiveResource> query =
                manager.createQuery("SELECT i FROM EffectiveResource i "
                        + "WHERE i.reservationId = :reservationId",
                        EffectiveResource.class);
        query.setParameter("reservationId", reservationId);
        List<EffectiveResource> resources = query.getResultList();

        if (null == resources || resources.isEmpty()) {
            ResponseBuilder response =
                    Response.status(HttpURLConnection.HTTP_NOT_FOUND);
            return response.build();
        }

        Date lastModified = new Date(Long.MIN_VALUE);
        for (EffectiveResource entity : resources) {
            if (lastModified.before(entity.getUpdated())) {
                lastModified = entity.getUpdated();
            }
        }

        ResponseBuilder response = Response.ok(resources);
        response.lastModified(lastModified);
        return response.build();

    }
}
