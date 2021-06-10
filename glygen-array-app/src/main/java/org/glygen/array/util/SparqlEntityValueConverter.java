package org.glygen.array.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.glygen.array.persistence.SparqlEntity;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class SparqlEntityValueConverter extends JsonSerializer<SparqlEntity> {

	@Override
	public void serialize(SparqlEntity schema, JsonGenerator jgen,
			SerializerProvider provider) throws IOException,
			JsonGenerationException {
        jgen.writeStartObject();
        Set<String> columns = schema.getColumns();
        for (Iterator iterator = columns.iterator(); iterator.hasNext();) {
			String column = (String) iterator.next();
	        jgen.writeStringField(column, schema.getValue(column));
		}
        jgen.writeEndObject();
	}
}