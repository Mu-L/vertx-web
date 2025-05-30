package io.vertx.ext.web.validation;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.validation.impl.ParameterLocation;

/**
 * Represents an exception while processing a parameter
 */
public class ParameterProcessorException extends BadRequestException {

  @VertxGen
  public enum ParameterProcessorErrorType {
    /**
     * Parameter is required but it's missing
     */
    MISSING_PARAMETER_WHEN_REQUIRED_ERROR,
    /**
     * Error during parsing. The cause of the exception is instance of {@link MalformedValueException}
     */
    PARSING_ERROR,
    /**
     * Error during validation
     */
    VALIDATION_ERROR
  }

  private String parameterName;
  private ParameterLocation location;
  private ParameterProcessorErrorType errorType;

  public ParameterProcessorException(String message, boolean customMessage, String parameterName, ParameterLocation location, ParameterProcessorErrorType errorType, Throwable cause) {
    super(message, customMessage, cause);
    this.parameterName = parameterName;
    this.location = location;
    this.errorType = errorType;
  }

  public String getParameterName() {
    return parameterName;
  }

  public ParameterLocation getLocation() {
    return location;
  }

  public ParameterProcessorErrorType getErrorType() {
    return errorType;
  }

  @Override
  public JsonObject toJson() {
    return super.toJson()
      .put("parameterName", this.parameterName)
      .put("errorType", this.errorType.name())
      .put("location", this.location.name());
  }

  public static ParameterProcessorException createMissingParameterWhenRequired(String parameterName, ParameterLocation location, String message) {
    return new ParameterProcessorException(message != null ? message : "Missing parameter " + parameterName + " in " +  location,
      message != null, parameterName, location, ParameterProcessorErrorType.MISSING_PARAMETER_WHEN_REQUIRED_ERROR, null);
  }

  public static ParameterProcessorException createParsingError(String parameterName, ParameterLocation location, MalformedValueException cause, String message) {
    return new ParameterProcessorException(
      message != null ? message : String.format("Parsing error for parameter %s in location %s: %s", parameterName, location, cause.getMessage()), message != null,
      parameterName, location, ParameterProcessorErrorType.PARSING_ERROR, cause
    );
  }

  public static ParameterProcessorException createValidationError(String parameterName, ParameterLocation location, Throwable cause, String message) {
    return new ParameterProcessorException(
      message != null ? message : String.format("Validation error for parameter %s in location %s: %s", parameterName, location, cause.getMessage()),
      message != null, parameterName, location, ParameterProcessorErrorType.VALIDATION_ERROR, cause
    );
  }
}
