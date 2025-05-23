package io.vertx.ext.web.api.service.generator.model;

import io.vertx.codegen.processor.*;
import io.vertx.codegen.processor.doc.Doc;
import io.vertx.codegen.processor.doc.Text;
import io.vertx.codegen.processor.type.ClassTypeInfo;
import io.vertx.codegen.processor.type.ParameterizedTypeInfo;
import io.vertx.codegen.processor.type.TypeInfo;
import io.vertx.codegen.processor.type.TypeMirrorFactory;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.serviceproxy.generator.model.ProxyMethodInfo;
import io.vertx.serviceproxy.generator.model.ProxyModel;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="https://github.com/slinkydeveloper">Francesco Guardiani</a>
 */
public class WebApiProxyModel extends ProxyModel {

  private static final String SIGNATURE_CONSTRAINT_ERROR = "Method must respect signature Future<io.vertx.ext.web.api.service.ServiceResponse> foo(extractedParams..., io.vertx.ext.web.api.service.ServiceRequest request) or foo(extractedParams..., io.vertx.ext.web.api.service.ServiceRequest request, Handler<AsyncResult<io.vertx.ext.web.api.service.ServiceResponse>> handler)";

  public WebApiProxyModel(ProcessingEnvironment env, TypeMirrorFactory typeFactory, TypeElement modelElt) {
    super(env, typeFactory, modelElt);
  }

  @Override
  public String getKind() {
    return "webapi_proxy";
  }

  @Override
  protected void checkParamType(ExecutableElement elem, TypeInfo typeInfo, int pos, int numParams) {
    // We allow RequestParameter but not as last or before last parameter
    if (RequestParameter.class.getName().equals(typeInfo.getName()))
      return;
    super.checkParamType(elem, typeInfo, pos, numParams);
  }

  @Override
  protected MethodInfo createMethodInfo(Set<ClassTypeInfo> ownerTypes, String methodName, String comment, Doc doc, TypeInfo returnType, Text returnDescription, boolean isFluent, boolean isCacheReturn, List<ParamInfo> mParams, ExecutableElement methodElt, boolean isStatic, boolean isDefault, ArrayList<TypeParamInfo.Method> typeParams, TypeElement declaringElt, boolean methodDeprecated, Text methodDeprecatedDesc, boolean methodOverride) {
    ProxyMethodInfo baseInfo = (ProxyMethodInfo) super.createMethodInfo(ownerTypes, methodName, comment, doc, returnType, returnDescription, isFluent, isCacheReturn, mParams, methodElt, isStatic, isDefault, typeParams, declaringElt, methodDeprecated, deprecatedDesc, methodOverride);
    if (!isStatic && !baseInfo.isProxyClose()) {
      // Check signature constraints

      if (!returnType.isParameterized()) {
        throw new GenException(methodElt, SIGNATURE_CONSTRAINT_ERROR);
      }
      ParameterizedTypeInfo pt = (ParameterizedTypeInfo) returnType;
      if (pt.getArgs().size() != 1) {
        throw new GenException(methodElt, SIGNATURE_CONSTRAINT_ERROR);
      }
      TypeInfo ret;
      ret = pt.getArg(0);
      if (!ServiceResponse.class.getName().equals(ret.getName())) {
        throw new GenException(methodElt, SIGNATURE_CONSTRAINT_ERROR);
      }

      TypeInfo shouldBeServiceRequest;
      if (mParams.isEmpty()) {
        throw new GenException(methodElt, SIGNATURE_CONSTRAINT_ERROR);
      }
      shouldBeServiceRequest = mParams.get(mParams.size() - 1).getType();
      if (!ServiceRequest.class.getName().equals(shouldBeServiceRequest.getName())) {
        throw new GenException(methodElt, SIGNATURE_CONSTRAINT_ERROR);
      }

      return new WebApiProxyMethodInfo(baseInfo);
    } else {
      return baseInfo;
    }
  }
}
