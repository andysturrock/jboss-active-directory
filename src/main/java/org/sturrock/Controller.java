package org.sturrock;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jboss.logging.Logger;

@XmlRootElement(name = "Response")
class XMLResponse {
	
	@XmlElement
	private String message;

	public XMLResponse(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
	// For JAXB
	@SuppressWarnings("unused")
	private XMLResponse() {}
}


@Path("/")
public class Controller {

	@Inject
	private Logger log;

	public Controller() throws IOException, JAXBException {
	}

	@POST
	@Path("protected-post")
	@Consumes(MediaType.APPLICATION_XML)
	public Response protectedPost(String xml) {
		log.info("protected-post: XML = " + xml);
		return Response.status(Status.OK).entity(new XMLResponse("protected-post response")).build();
	}
	
	@POST
	@Path("unprotected-post")
	@Consumes(MediaType.APPLICATION_XML)
	public Response unprotectedPost(String xml) {
		log.info("unprotected-post: XML = " + xml);
		return Response.status(Status.OK).entity(new XMLResponse("unprotected-post response")).build();
	}

	@GET
	@Path("/protected-get")
	@Produces(MediaType.APPLICATION_XML)
	public Response protectedGet() {
		log.info("protected-get");
		return Response.status(Status.OK).entity(new XMLResponse("protected-get response")).build();
	}
	
	@GET
	@Path("/unprotected-get")
	@Produces(MediaType.APPLICATION_XML)
	public Response unprotectedGet() {
		log.info("unprotected-get");
		return Response.status(Status.OK).entity(new XMLResponse("unprotected-get response")).build();
	}

	@DELETE
	@Path("/protected-delete")
	@Produces(MediaType.APPLICATION_XML)
	public Response protectedDelete() {
		log.info("protected-delete");
		return Response.status(Status.OK).entity(new XMLResponse("protected-delete response")).build();
	}
	
	@DELETE
	@Path("/unprotected-delete")
	@Produces(MediaType.APPLICATION_XML)
	public Response unprotectedDelete() {
		log.info("unprotected-delete");
		return Response.status(Status.OK).entity(new XMLResponse("unprotected-delete response")).build();
	}
}
