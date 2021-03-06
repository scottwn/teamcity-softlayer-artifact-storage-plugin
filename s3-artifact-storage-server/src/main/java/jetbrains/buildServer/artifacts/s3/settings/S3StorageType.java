package jetbrains.buildServer.artifacts.s3.settings;

import jetbrains.buildServer.artifacts.s3.S3Constants;
import jetbrains.buildServer.artifacts.s3.S3Util;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.ServerSettings;
import jetbrains.buildServer.serverSide.artifacts.ArtifactStorageType;
import jetbrains.buildServer.serverSide.artifacts.ArtifactStorageTypeRegistry;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.artifacts.s3.S3Constants.S3_USE_PRE_SIGNED_URL_FOR_UPLOAD;
import static jetbrains.buildServer.util.amazon.AWSCommonParams.SECURE_SECRET_ACCESS_KEY_PARAM;

/**
 * Created by Nikita.Skvortsov
 * date: 24.02.2016.
 */
public class S3StorageType extends ArtifactStorageType {

  @NotNull private final String mySettingsJSP;
  @NotNull private final ServerSettings myServerSettings;

  public S3StorageType(@NotNull ArtifactStorageTypeRegistry registry,
                       @NotNull PluginDescriptor descriptor,
                       @NotNull ServerSettings serverSettings) {
    mySettingsJSP = descriptor.getPluginResourcesPath(S3Constants.S3_SETTINGS_PATH + ".jsp");
    myServerSettings = serverSettings;
    registry.registerStorageType(this);
  }

  @NotNull
  @Override
  public String getType() {
    return S3Constants.S3_STORAGE_TYPE;
  }

  @NotNull
  @Override
  public String getName() {
    return "S3 Storage";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Uses s3 bucket to store build artifacts";
  }

  @NotNull
  @Override
  public String getEditStorageParametersPath() {
    return mySettingsJSP;
  }

  @Nullable
  @Override
  public Map<String, String> getDefaultParameters() {
    Map<String, String> result = new HashMap<>();
    result.putAll(AWSCommonParams.getDefaults(myServerSettings.getServerUUID()));
    return result;
  }

  @Nullable
  @Override
  public PropertiesProcessor getParametersProcessor() {
    return params -> {
      final ArrayList<InvalidProperty> invalids = new ArrayList<>();
      for (Map.Entry<String, String> e : S3Util.validateParameters(params, true).entrySet()) {
        invalids.add(new InvalidProperty(e.getKey(), e.getValue()));
      }

      final String bucketName = S3Util.getBucketName(params);
      if (bucketName != null) {
        try {
          final String location = S3Util.withS3Client(params, client -> client.getBucketLocation(bucketName));
          if (location == null) {
            invalids.add(new InvalidProperty(S3Constants.S3_BUCKET_NAME, "Bucket does not exist"));
          }
        } catch (Throwable e) {
          invalids.add(new InvalidProperty(S3Constants.S3_BUCKET_NAME, e.getMessage()));
        }
      }

      return invalids;
    };
  }

  @NotNull
  @Override
  public SettingsPreprocessor getSettingsPreprocessor() {
    return input -> {
      final Map<String, String> output = new HashMap<>(input);
      if(Boolean.parseBoolean(input.get(S3_USE_PRE_SIGNED_URL_FOR_UPLOAD))){
        output.remove(SECURE_SECRET_ACCESS_KEY_PARAM);
      }
      return output;
    };
  }
}
