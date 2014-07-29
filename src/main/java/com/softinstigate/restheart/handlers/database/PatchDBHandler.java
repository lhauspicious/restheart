/*
 * Copyright SoftInstigate srl. All Rights Reserved.
 *
 *
 * The copyright to the computer program(s) herein is the property of
 * SoftInstigate srl, Italy. The program(s) may be used and/or copied only
 * with the written permission of SoftInstigate srl or in accordance with the
 * terms and conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied. This copyright notice must not be removed.
 */
package com.softinstigate.restheart.handlers.database;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;
import com.softinstigate.restheart.db.MongoDBClientSingleton;
import com.softinstigate.restheart.utils.ChannelReader;
import com.softinstigate.restheart.utils.HttpStatus;
import com.softinstigate.restheart.utils.RequestContext;
import com.softinstigate.restheart.utils.ResponseHelper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.time.Instant;

/**
 *
 * @author uji
 */
public class PatchDBHandler implements HttpHandler
{
    private static final MongoClient client = MongoDBClientSingleton.getInstance().getClient();

    /**
     * Creates a new instance of EntityResource
     */
    public PatchDBHandler()
    {
    }

    /**
     * partial update db metadata
     *
     * @param exchange
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception
    {
        RequestContext rc = new RequestContext(exchange);

        if (rc.getDBName().isEmpty() || rc.getDBName().startsWith("@"))
        {
            ResponseHelper.endExchangeWithError(exchange, HttpStatus.SC_NOT_ACCEPTABLE, new IllegalArgumentException("db name cannot be empty or start with @"));
            return;
        }

        String _content = ChannelReader.read(exchange.getRequestChannel());

        DBObject content;

        try
        {
            content = (DBObject) JSON.parse(_content);
        }
        catch (JSONParseException ex)
        {
            ResponseHelper.endExchangeWithError(exchange, HttpStatus.SC_NOT_ACCEPTABLE, ex);
            return;
        }
        
        if (content == null)
        {
            ResponseHelper.endExchange(exchange, HttpStatus.SC_NOT_ACCEPTABLE);
            return;
        }

        DB db = client.getDB(rc.getDBName());

        DBCollection coll = db.getCollection("@metadata");

        BasicDBObject metadataQuery = new BasicDBObject("_id", "@metadata");

        DBObject metadata = coll.findOne(metadataQuery);

        if (metadata == null)
        {
            metadata = new BasicDBObject();
            metadata.put("_id", "@metadata");
            metadata.put("@type", "metadata");
            metadata.put("@created_on", Instant.now().toString());
        }
        else
        {
            metadata.put("@lastupdated_on", Instant.now().toString());
        }
        
        // apply new values
        
        metadata.putAll(content);
        
        coll.update(metadataQuery, metadata, true, false);

        ResponseHelper.endExchange(exchange, HttpStatus.SC_OK);
    }
}