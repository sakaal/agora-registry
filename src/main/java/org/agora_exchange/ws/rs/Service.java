/*
 *  Agora Exchange for Online Managed Services
 *
 *  Copyright (C) 2012 Sakari A. Maaranen
 */
package org.agora_exchange.ws.rs;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.ws.rs.PathParam;
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
public abstract class Service<R extends Record>
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected abstract R createRecord();

    protected abstract R find(String id, EntityManager manager);

    /**
     * Copies data from the source record to the target record without
     * overwriting the target timestamps and identity, if it is set
     * <code>(target.getId() != null)</code>.
     * 
     * @param source
     *            A record of type <code>R</code> to read the data from.
     * @param target
     *            A record of type <code>R</code> to write the data to.
     * @param manager
     */
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
    protected Response post(R jaxb, EntityManager manager) {
        String id = jaxb.getId();

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

        return response;

    }

    /**
     * HTTP/1.1 GET method implementation.
     * 
     * @param id
     * @return
     */
    protected Response get(String id, EntityManager manager) {
        R record = find(id, manager);
        if (null == record) {
            return Response.status(HTTP_NOT_FOUND).build();
        }
        ResponseBuilder builder = Response.ok(record);
        builder.lastModified(record.getUpdated());
        Response response = builder.build();

        return response;
    }

    /**
     * HTTP/1.1 PUT method implementation.
     * 
     * @param id
     * @param jaxb
     * @return
     */
    protected Response put(@PathParam("id") String id, R jaxb,
            EntityManager manager) {

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

        return response;
    }

    /**
     * HTTP/1.1 DELETE method implementation.
     * 
     * @param id
     * @return
     */
    public Response delete(@PathParam("id") String id, EntityManager manager) {
        R record = find(id, manager);
        if (null == record) {
            return Response.status(HTTP_NOT_FOUND).build();
        }
        manager.remove(record);

        logger.debug("DELETE removed " + record);
        ResponseBuilder builder = Response.ok(record);
        builder.lastModified(record.getUpdated());
        Response response = builder.build();

        return response;
    }
}
