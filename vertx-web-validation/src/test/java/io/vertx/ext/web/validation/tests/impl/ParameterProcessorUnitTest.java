package io.vertx.ext.web.validation.tests.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.validation.MalformedValueException;
import io.vertx.ext.web.validation.ParameterProcessorException;
import io.vertx.ext.web.validation.impl.ParameterLocation;
import io.vertx.ext.web.validation.impl.parameter.ParameterParser;
import io.vertx.ext.web.validation.impl.parameter.ParameterProcessor;
import io.vertx.ext.web.validation.impl.parameter.ParameterProcessorImpl;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.json.schema.Validator;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ParameterProcessorUnitTest {

  @Mock
  ParameterParser mockedParser;
  @Mock
  SchemaRepository mockedSchemaRepository;
  @Mock
  OutputUnit mockedOutputUnit;
  @Mock
  Validator mockedValidator;

  @Test
  public void testRequiredParam() {
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      false,
      mockedParser,
      mockedSchemaRepository,
      new JsonObject()
    );

    when(mockedParser.parseParameter(any())).thenReturn(null);
    assertThatCode(() -> processor.process(new HashMap<>()))
      .isInstanceOf(ParameterProcessorException.class)
      .hasFieldOrPropertyWithValue("errorType",
        ParameterProcessorException.ParameterProcessorErrorType.MISSING_PARAMETER_WHEN_REQUIRED_ERROR)
      .hasFieldOrPropertyWithValue("location", ParameterLocation.QUERY)
      .hasFieldOrPropertyWithValue("parameterName", "myParam")
      .hasNoCause();
  }

  @Test
  public void testRequiredParamCustomMessage() {
    String missingParameterErrorMessage = "Missing myParam";
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      false,
      mockedParser,
      mockedSchemaRepository,
      new JsonObject()
    ).missingParameterErrorMessage(missingParameterErrorMessage);

    when(mockedParser.parseParameter(any())).thenReturn(null);
    assertThatCode(() -> processor.process(new HashMap<>()))
      .isInstanceOf(ParameterProcessorException.class)
      .hasFieldOrPropertyWithValue("errorType",
        ParameterProcessorException.ParameterProcessorErrorType.MISSING_PARAMETER_WHEN_REQUIRED_ERROR)
      .hasFieldOrPropertyWithValue("location", ParameterLocation.QUERY)
      .hasFieldOrPropertyWithValue("parameterName", "myParam")
      .hasFieldOrPropertyWithValue("message", missingParameterErrorMessage)
      .hasNoCause();
  }

  @Test
  public void testOptionalParam(VertxTestContext testContext) {
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      true,
      mockedParser,
      mockedSchemaRepository,
      new JsonObject()
    );

    when(mockedParser.parseParameter(any())).thenReturn(null);

    processor.process(new HashMap<>()).onComplete(testContext.succeeding(value -> {
      testContext.verify(() ->
        assertThat(value).isNull()
      );
      testContext.completeNow();
    }));
  }


  @Test
  public void testOptionalParamWithDefault(VertxTestContext testContext) {
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      true,
      mockedParser,
      mockedSchemaRepository,
      new JsonObject().put("default", "bla")
    );

    when(mockedParser.parseParameter(any())).thenReturn(null);

    processor.process(new HashMap<>()).onComplete(testContext.succeeding(value -> {
      testContext.verify(() ->
        assertThat(value.getString()).isEqualTo("bla")
      );
      testContext.completeNow();
    }));
  }

  @Test
  public void testParsingFailure() {
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      false,
      mockedParser,
      mockedSchemaRepository,
      new JsonObject()
    );

    when(mockedParser.parseParameter(any())).thenThrow(new MalformedValueException("bla"));

    assertThatCode(() -> processor.process(new HashMap<>()))
      .isInstanceOf(ParameterProcessorException.class)
      .hasFieldOrPropertyWithValue("errorType", ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR)
      .hasFieldOrPropertyWithValue("location", ParameterLocation.QUERY)
      .hasFieldOrPropertyWithValue("parameterName", "myParam")
      .hasCauseInstanceOf(MalformedValueException.class);
  }

  @Test
  public void testParsingCustomMessageFailure() {
    String parsingErrorMessage = "Failed to parse myParam";
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      false,
      mockedParser,
      mockedSchemaRepository,
      new JsonObject()
    ).parsingErrorMessage(parsingErrorMessage);

    when(mockedParser.parseParameter(any())).thenThrow(new MalformedValueException("bla"));

    assertThatCode(() -> processor.process(new HashMap<>()))
      .isInstanceOf(ParameterProcessorException.class)
      .hasFieldOrPropertyWithValue("errorType", ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR)
      .hasFieldOrPropertyWithValue("location", ParameterLocation.QUERY)
      .hasFieldOrPropertyWithValue("parameterName", "myParam")
      .hasCauseInstanceOf(MalformedValueException.class)
      .hasFieldOrPropertyWithValue("message", parsingErrorMessage);
  }

  @Test
  public void testValidation(VertxTestContext testContext) {
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      true,
      mockedParser,
      mockedSchemaRepository,
      new JsonObject()
    );

    when(mockedParser.parseParameter(any())).thenReturn("aaa");

    when(mockedSchemaRepository.validator(any(JsonSchema.class))).thenReturn(mockedValidator);
    when(mockedValidator.validate(any())).thenReturn(mockedOutputUnit);
    when(mockedOutputUnit.getValid()).thenReturn(true);

    processor.process(new HashMap<>()).onComplete(testContext.succeeding(rp -> {
      testContext.verify(() -> {
        assertThat(rp.isString()).isTrue();
        assertThat(rp.getString()).isEqualTo("aaa");
      });
      testContext.completeNow();
    }));
  }

  @Test
  public void testValidationFailure(VertxTestContext testContext) {
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      true,
      mockedParser,
      mockedSchemaRepository,
      new JsonObject()
    );

    when(mockedParser.parseParameter(any())).thenReturn("aaa");

    when(mockedSchemaRepository.validator(any(JsonSchema.class))).thenReturn(mockedValidator);
    when(mockedValidator.validate(any())).thenReturn(mockedOutputUnit);
    when(mockedOutputUnit.getValid()).thenReturn(false);

    processor.process(new HashMap<>()).onComplete(testContext.failing(throwable -> {
      testContext.verify(() -> {
        assertThat(throwable)
          .isInstanceOf(ParameterProcessorException.class)
          .hasFieldOrPropertyWithValue("errorType",
            ParameterProcessorException.ParameterProcessorErrorType.VALIDATION_ERROR)
          .hasFieldOrPropertyWithValue("location", ParameterLocation.QUERY)
          .hasFieldOrPropertyWithValue("parameterName", "myParam");
      });
      testContext.completeNow();
    }));
  }

  @Test
  public void testValidationCustomMessageFailure(VertxTestContext testContext) {
    String customValidationErrorMessage = "Failed to validate myParam";
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      true,
      mockedParser,
      mockedSchemaRepository,
      new JsonObject()
    ).validationErrorMessage(customValidationErrorMessage);

    when(mockedParser.parseParameter(any())).thenReturn("aaa");

    when(mockedSchemaRepository.validator(any(JsonSchema.class))).thenReturn(mockedValidator);
    when(mockedValidator.validate(any())).thenReturn(mockedOutputUnit);
    when(mockedOutputUnit.getValid()).thenReturn(false);

    processor.process(new HashMap<>()).onComplete(testContext.failing(throwable -> {
      testContext.verify(() -> {
        assertThat(throwable)
          .isInstanceOf(ParameterProcessorException.class)
          .hasFieldOrPropertyWithValue("errorType",
            ParameterProcessorException.ParameterProcessorErrorType.VALIDATION_ERROR)
          .hasFieldOrPropertyWithValue("location", ParameterLocation.QUERY)
          .hasFieldOrPropertyWithValue("parameterName", "myParam")
          .hasFieldOrPropertyWithValue("message", customValidationErrorMessage);
      });
      testContext.completeNow();
    }));
  }
}
