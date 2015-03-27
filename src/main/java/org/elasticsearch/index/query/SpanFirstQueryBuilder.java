/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.query;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanFirstQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 *
 */
public class SpanFirstQueryBuilder extends BaseQueryBuilder implements SpanQueryBuilder, BoostableQueryBuilder<SpanFirstQueryBuilder> {

    private final SpanQueryBuilder matchBuilder;

    private final int end;

    private float boost = -1;

    private String queryName;
    
    public static final String NAME = "span_first";

    @Inject
    public SpanFirstQueryBuilder() {
        this.matchBuilder = null;
        this.end = 0;
    }

    public SpanFirstQueryBuilder(SpanQueryBuilder matchBuilder, int end) {
        this.matchBuilder = matchBuilder;
        this.end = end;
    }

    @Override
    public SpanFirstQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }

    /**
     * Sets the query name for the filter that can be used when searching for matched_filters per hit.
     */
    public SpanFirstQueryBuilder queryName(String queryName) {
        this.queryName = queryName;
        return this;
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(SpanFirstQueryBuilder.NAME);
        builder.field("match");
        matchBuilder.toXContent(builder, params);
        builder.field("end", end);
        if (boost != -1) {
            builder.field("boost", boost);
        }
        if (queryName != null) {
            builder.field("name", queryName);
        }
        builder.endObject();
    }
    
    @Override
    public String[] names() {
        return new String[]{NAME, Strings.toCamelCase(NAME)};
    }

    @Override
    public Query parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        float boost = 1.0f;

        SpanQuery match = null;
        int end = -1;
        String queryName = null;

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                if ("match".equals(currentFieldName)) {
                    Query query = parseContext.parseInnerQuery();
                    if (!(query instanceof SpanQuery)) {
                        throw new QueryParsingException(parseContext.index(), "spanFirst [match] must be of type span query");
                    }
                    match = (SpanQuery) query;
                } else {
                    throw new QueryParsingException(parseContext.index(), "[span_first] query does not support [" + currentFieldName + "]");
                }
            } else {
                if ("boost".equals(currentFieldName)) {
                    boost = parser.floatValue();
                } else if ("end".equals(currentFieldName)) {
                    end = parser.intValue();
                } else if ("_name".equals(currentFieldName)) {
                    queryName = parser.text();
                } else {
                    throw new QueryParsingException(parseContext.index(), "[span_first] query does not support [" + currentFieldName + "]");
                }
            }
        }
        if (match == null) {
            throw new QueryParsingException(parseContext.index(), "spanFirst must have [match] span query clause");
        }
        if (end == -1) {
            throw new QueryParsingException(parseContext.index(), "spanFirst must have [end] set for it");
        }

        SpanFirstQuery query = new SpanFirstQuery(match, end);
        query.setBoost(boost);
        if (queryName != null) {
            parseContext.addNamedQuery(queryName, query);
        }
        return query;
    }
}
