package com.proquip.web.provider;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import java.text.SimpleDateFormat;
import java.util.Date;

@Provider
public class JsonbConfigProvider implements ContextResolver<Jsonb> {

    private final Jsonb jsonb;

    public JsonbConfigProvider() {
        JsonbConfig config = new JsonbConfig()
                .withSerializers(new DateSerializer())
                .withNullValues(false);
        this.jsonb = JsonbBuilder.create(config);
    }

    @Override
    public Jsonb getContext(Class<?> type) {
        return jsonb;
    }

    public static class DateSerializer implements JsonbSerializer<Date> {

        private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
        private static final String DATE_FORMAT = "yyyy-MM-dd";

        @Override
        public void serialize(Date date, JsonGenerator generator, SerializationContext ctx) {
            if (date == null) {
                generator.writeNull();
                return;
            }
            SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT);
            String formatted = sdf.format(date);
            if (formatted.endsWith("T00:00:00")) {
                sdf = new SimpleDateFormat(DATE_FORMAT);
                formatted = sdf.format(date);
            }
            generator.write(formatted);
        }
    }
}
