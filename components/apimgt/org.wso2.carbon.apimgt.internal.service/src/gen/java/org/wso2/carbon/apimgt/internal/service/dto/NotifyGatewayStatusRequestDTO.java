package org.wso2.carbon.apimgt.internal.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.wso2.carbon.apimgt.internal.service.dto.GatewayPropertiesDTO;
import javax.validation.constraints.*;

/**
 * Payload to register a new gateway instance.
 **/

import io.swagger.annotations.*;
import java.util.Objects;

import javax.xml.bind.annotation.*;
import org.wso2.carbon.apimgt.rest.api.common.annotations.Scope;
import com.fasterxml.jackson.annotation.JsonCreator;

import javax.validation.Valid;

@ApiModel(description = "Payload to register a new gateway instance.")

public class NotifyGatewayStatusRequestDTO   {
  
    private GatewayPropertiesDTO gatewayProperties = null;
    private List<String> environmentLabels = new ArrayList<>();
    private OffsetDateTime timeStamp = null;
    private String gatewayId = null;

  /**
   **/
  public NotifyGatewayStatusRequestDTO gatewayProperties(GatewayPropertiesDTO gatewayProperties) {
    this.gatewayProperties = gatewayProperties;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
      @Valid
  @JsonProperty("gatewayProperties")
  @NotNull
  public GatewayPropertiesDTO getGatewayProperties() {
    return gatewayProperties;
  }
  public void setGatewayProperties(GatewayPropertiesDTO gatewayProperties) {
    this.gatewayProperties = gatewayProperties;
  }

  /**
   * A list of environments this gateway is configured to support.
   **/
  public NotifyGatewayStatusRequestDTO environmentLabels(List<String> environmentLabels) {
    this.environmentLabels = environmentLabels;
    return this;
  }

  
  @ApiModelProperty(example = "[\"default\",\"production\",\"sandbox\"]", required = true, value = "A list of environments this gateway is configured to support.")
  @JsonProperty("environmentLabels")
  @NotNull
  public List<String> getEnvironmentLabels() {
    return environmentLabels;
  }
  public void setEnvironmentLabels(List<String> environmentLabels) {
    this.environmentLabels = environmentLabels;
  }

  /**
   **/
  public NotifyGatewayStatusRequestDTO timeStamp(OffsetDateTime timeStamp) {
    this.timeStamp = timeStamp;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("timeStamp")
  public OffsetDateTime getTimeStamp() {
    return timeStamp;
  }
  public void setTimeStamp(OffsetDateTime timeStamp) {
    this.timeStamp = timeStamp;
  }

  /**
   * The unique identifier assigned to the newly registered gateway.
   **/
  public NotifyGatewayStatusRequestDTO gatewayId(String gatewayId) {
    this.gatewayId = gatewayId;
    return this;
  }

  
  @ApiModelProperty(example = "ID_1", value = "The unique identifier assigned to the newly registered gateway.")
  @JsonProperty("gatewayId")
  public String getGatewayId() {
    return gatewayId;
  }
  public void setGatewayId(String gatewayId) {
    this.gatewayId = gatewayId;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotifyGatewayStatusRequestDTO notifyGatewayStatusRequest = (NotifyGatewayStatusRequestDTO) o;
    return Objects.equals(gatewayProperties, notifyGatewayStatusRequest.gatewayProperties) &&
        Objects.equals(environmentLabels, notifyGatewayStatusRequest.environmentLabels) &&
        Objects.equals(timeStamp, notifyGatewayStatusRequest.timeStamp) &&
        Objects.equals(gatewayId, notifyGatewayStatusRequest.gatewayId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gatewayProperties, environmentLabels, timeStamp, gatewayId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotifyGatewayStatusRequestDTO {\n");
    
    sb.append("    gatewayProperties: ").append(toIndentedString(gatewayProperties)).append("\n");
    sb.append("    environmentLabels: ").append(toIndentedString(environmentLabels)).append("\n");
    sb.append("    timeStamp: ").append(toIndentedString(timeStamp)).append("\n");
    sb.append("    gatewayId: ").append(toIndentedString(gatewayId)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

