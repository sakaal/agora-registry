/*
 *  Agora Exchange for Online Managed Services
 *
 *  Copyright (C) 2012 Sakari A. Maaranen
 */
package org.agora_exchange.ws.rs.supply;

import static javax.ejb.TransactionManagementType.BEAN;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

import java.net.HttpURLConnection;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
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
@TransactionManagement(BEAN)
public class EffectiveResourceService extends Service<EffectiveResource> {

    @PersistenceContext(unitName = "org.agora_exchange.xml",
        type=PersistenceContextType.TRANSACTION)
    private EntityManager manager;

    @Resource
    private UserTransaction tx;

    @Override
    protected UserTransaction getTransaction() {
        return tx;
    }

    @Override
    public EffectiveResource createRecord() {
        return new EffectiveResource();
    }

    @Override
    public void copy(EffectiveResource source, EffectiveResource target, EntityManager em) {
        // TODO:
    }

    @Override
    protected EffectiveResource find(String id, EntityManager manager) {
        return manager.find(
                org.agora_exchange.xml.supply.EffectiveResource.class, id);
    }

    @GET
    @Path("{id}")
    @Produces({ APPLICATION_XML, APPLICATION_JSON })
    public Response get(@PathParam("id") String id) {
        return super.get(id, manager);
    }
}

