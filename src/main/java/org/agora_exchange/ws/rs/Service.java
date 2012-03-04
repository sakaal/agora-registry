/*
 *  Agora Exchange for Online Managed Services
 *
 *  Copyright (C) 2012 Sakari A. Maaranen
 */
package org.agora_exchange.ws.rs;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.agora_exchange.xml.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS web service for managing a JAXB/JPA entity.
 * 
 * @author Sakari A. Maaranen
 * 
 * @param <R>
 *            JAXB/JPA annotated Record subclass
 */
public abstract class Service<R extends Record> {
    protected static final String JNDI_ENTITY_MANAGER_FACTORY =
            "java:/persistence/AgoraEntityManagerFactory";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected abstract R createRecord();

    protected abstract R find(String id, EntityManager manager);

    protected abstract void copy(R source, R target, EntityManager manager);

    private R createRecord(EntityManager manager) {
        R record = createRecord();
        for (int retry = 0; retry < 4; retry++) { // sanity check
            try {
                record.setId(UUID.randomUUID().toString());
                manager.persist(record);
                return record;
            } catch (EntityExistsException ex) {
                // Shouldn't repeat unless UUID generation is broken.
            }
        }
        throw new IllegalStateException("Repeated Record UUID collisions.");
    }

    /**
     * The returned entity manager must be closed when no longer needed.
     * 
     * @return An <code>EntityManager</code> object.
     */
    private EntityManager createEntityManager() {
        try {
            EntityManagerFactory factory =
                    (EntityManagerFactory) new InitialContext()
                            .lookup(JNDI_ENTITY_MANAGER_FACTORY);
            if (null == factory) {
                throw new WebApplicationException(new NullPointerException(
                        "EntityManagerFactory not found"), HTTP_INTERNAL_ERROR);
            }
            return factory.createEntityManager();
        } catch (NamingException e) {
            throw new WebApplicationException(e, HTTP_INTERNAL_ERROR);
        }
    }

    @Context
    private UriInfo uriInfo;

    private URI getURI(Record entity) {
        return uriInfo.getRequestUriBuilder().path(entity.getId()).build();
    }

    /**
     * HTTP/1.1 POST method implementation.
     * 
     * @param jaxb
     * @return
     */
    @POST
    @Consumes({ APPLICATION_XML, APPLICATION_JSON })
    @Produces({ APPLICATION_XML, APPLICATION_JSON })
    public final Response post(R jaxb) {
        EntityTransaction tx = null;
        EntityManager manager = createEntityManager();
        String id = jaxb.getId();
        try {
            tx = manager.getTransaction();
            tx.begin();

            R record = isNotEmpty(id) ? find(id, manager) : null;

            ResponseBuilder builder;
            if (null == record) {
                if (isNotEmpty(id)) {
                    record = createRecord();
                    record.setId(id);
                    manager.persist(record);
                } else {
                    record = createRecord(manager);
                    id = record.getId();
                }
                logger.debug("POST created " + record.getClass().getSimpleName()
                        + ".id=" + id);
                builder = Response.created(getURI(record));
            } else {
                logger.debug("POST updating " + record);
                builder = Response.seeOther(getURI(record));
            }
            copy(jaxb, record, manager);
            record.setUpdated(new Date());
            record = manager.merge(record);

            builder.lastModified(record.getUpdated());
            builder.entity(record);
            Response response = builder.build();

            tx.commit();
            tx = null;
            return response;

        } finally {
            if (null != tx) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                tx = null;
            }
            manager.close();
            manager = null;
        }
    }

    /**
     * HTTP/1.1 GET method implementation.
     * 
     * @param id
     * @return
     */
    @GET
    @Path("{id}")
    @Produces({ APPLICATION_XML, APPLICATION_JSON })
    public final Response get(@PathParam("id") String id) {
        EntityTransaction tx = null;
        EntityManager manager = createEntityManager();
        try {
            tx = manager.getTransaction();
            tx.begin();
            R record = find(id, manager);
            if (null == record) {
                return Response.status(HTTP_NOT_FOUND).build();
            }
            ResponseBuilder builder = Response.ok(record);
            builder.lastModified(record.getUpdated());
            Response response = builder.build();

            tx.commit();
            tx = null;
            return response;

        } finally {
            if (null != tx) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                tx = null;
            }
            manager.close();
            manager = null;
        }
    }

    /**
     * HTTP/1.1 PUT method implementation.
     * 
     * @param id
     * @param jaxb
     * @return
     */
    @PUT
    @Path("{id}")
    @Consumes({ APPLICATION_XML, APPLICATION_JSON })
    @Produces({ APPLICATION_XML, APPLICATION_JSON })
    public Response put(@PathParam("id") String id, R jaxb) {
        EntityTransaction tx = null;
        EntityManager manager = createEntityManager();
        try {
            tx = manager.getTransaction();
            tx.begin();

            R record = find(id, manager);

            ResponseBuilder builder;

            if (null == record) {
                record = createRecord();
                record.setId(id);
                manager.persist(record);
                logger.debug("PUT created " + record.getClass().getSimpleName()
                        + ".id=" + id);
                builder = Response.created(getURI(record));
            } else {
                logger.debug("PUT updating " + record);
                builder = Response.ok();
            }
            copy(jaxb, record, manager);
            record.setUpdated(new Date());
            record = manager.merge(record);

            builder.lastModified(record.getUpdated());
            builder.entity(jaxb);
            Response response = builder.build();

            tx.commit();
            tx = null;
            return response;

        } finally {
            if (null != tx) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                tx = null;
            }
            manager.close();
            manager = null;
        }
    }

    /**
     * HTTP/1.1 DELETE method implementation.
     * 
     * @param id
     * @return
     */
    @DELETE
    @Path("{id}")
    @Produces({ APPLICATION_XML, APPLICATION_JSON })
    public Response delete(@PathParam("id") String id) {
        EntityTransaction tx = null;
        EntityManager manager = createEntityManager();
        try {
            tx = manager.getTransaction();
            tx.begin();
            R record = find(id, manager);
            if (null == record) {
                return Response.status(HTTP_NOT_FOUND).build();
            }
            manager.remove(record);

            logger.debug("DELETE removed " + record);
            ResponseBuilder builder = Response.ok(record);
            builder.lastModified(record.getUpdated());
            Response response = builder.build();

            tx.commit();
            tx = null;
            return response;

        } finally {
            if (null != tx) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                tx = null;
            }
            manager.close();
            manager = null;
        }
    }
}
