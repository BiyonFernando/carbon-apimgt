package org.wso2.carbon.apimgt.internal.service;

import org.wso2.carbon.apimgt.internal.service.dto.ErrorDTO;
import org.wso2.carbon.apimgt.internal.service.dto.NotifyGatewayStatusRequestDTO;
import org.wso2.carbon.apimgt.internal.service.dto.NotifyGatewayStatusResponseDTO;
import org.wso2.carbon.apimgt.internal.service.NotifyGatewayStatusApiService;
import org.wso2.carbon.apimgt.internal.service.impl.NotifyGatewayStatusApiServiceImpl;
import org.wso2.carbon.apimgt.api.APIManagementException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.inject.Inject;

import io.swagger.annotations.*;
import java.io.InputStream;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import java.util.Map;
import java.util.List;
import javax.validation.constraints.*;
@Path("/notify-gateway-status")

@Api(description = "the notify-gateway-status API")




public class NotifyGatewayStatusApi  {

  @Context MessageContext securityContext;

NotifyGatewayStatusApiService delegate = new NotifyGatewayStatusApiServiceImpl();


    @POST
    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Gateway Register or Heartbeat", notes = "Accepts a unified payload that includes fields for both registration and heartbeat. Fields not applicable to the operation type should be omitted or left null. ", response = NotifyGatewayStatusResponseDTO.class, tags={ "Gateway Lifecycle" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful response based on the type of request.", response = NotifyGatewayStatusResponseDTO.class),
        @ApiResponse(code = 400, message = "Invalid payload", response = ErrorDTO.class),
        @ApiResponse(code = 500, message = "Internal server error while processing gateway status.", response = ErrorDTO.class),
        @ApiResponse(code = 200, message = "Unexpected error", response = ErrorDTO.class) })
    public Response notifyGatewayStatusPost(@ApiParam(value = "" ,required=true) NotifyGatewayStatusRequestDTO notifyGatewayStatusRequestDTO) throws APIManagementException{
        return delegate.notifyGatewayStatusPost(notifyGatewayStatusRequestDTO, securityContext);
    }
}
