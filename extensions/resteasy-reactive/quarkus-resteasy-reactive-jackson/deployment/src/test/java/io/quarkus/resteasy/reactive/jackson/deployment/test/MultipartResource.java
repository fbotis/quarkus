package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.MultipartForm;

import io.smallrye.common.annotation.Blocking;

@Path("/multipart")
public class MultipartResource {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Blocking
    @Path("/json")
    public Map<String, Object> greeting(@Valid @MultipartForm FormData formData) {
        Map<String, Object> result = new HashMap<>(formData.map);
        result.put("person", formData.person);
        result.put("htmlFileSize", formData.getHtmlPart().size());
        result.put("htmlFilePath", formData.getHtmlPart().uploadedFile().toAbsolutePath().toString());
        result.put("htmlFileContentType", formData.getHtmlPart().contentType());
        result.put("names", formData.names);
        result.put("numbers", formData.numbers);
        result.put("numbers2", formData.numbers2);
        result.put("persons", formData.persons);
        result.put("persons2", formData.persons2);
        return result;
    }
}
