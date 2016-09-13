package com.github.worldsender.mcanm.client.model;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.Objects;

import com.github.worldsender.mcanm.client.ClientLoader;
import com.github.worldsender.mcanm.client.mcanmmodel.ModelMCMD;
import com.github.worldsender.mcanm.common.CommonLoader;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;
import com.github.worldsender.mcanm.common.skeleton.SkeletonMCSKL;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.util.ResourceLocation;

/**
 * Contains references to all required file to display the whole model. This includes the mesh-model ({@link ModelMCMD})
 * and the skeleton file ({@link SkeletonMCSKL})
 * 
 * @author WorldSEnder
 *
 */
public class ModelDescription {
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(ModelDescription.class, new DescriptionDeserializer()).create();

	private static class DescriptionDeserializer implements JsonDeserializer<ModelDescription> {
		@SuppressWarnings("deprecation") // We handle the legacy, not consume it
		private ISkeleton loadSkeleton(boolean legacy, JsonObject jsonObject) {
			if (!legacy && !jsonObject.has("skeleton")) {
				return ISkeleton.EMPTY;
			}
			ResourceLocation skeletonLocation = legacy
					? new ResourceLocation(jsonObject.get("mesh").getAsString())
					: new ResourceLocation(jsonObject.get("skeleton").getAsString());

			return legacy
					? CommonLoader.loadLegacySkeleton(skeletonLocation)
					: CommonLoader.loadSkeleton(skeletonLocation);
		}

		@Override
		public ModelDescription deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			JsonObject jsonObject = json.getAsJsonObject();
			int version = jsonObject.get("version").getAsInt();
			// Currently version is ignored
			if (version != 0 && version != 1) {
				throw new JsonParseException("Unsupported model version");
			}
			boolean isLegacy = version == 0;
			ISkeleton skeleton = loadSkeleton(isLegacy, jsonObject);

			ResourceLocation modelLocation = new ResourceLocation(jsonObject.get("mesh").getAsString());
			ModelMCMD mesh = ClientLoader.loadModel(modelLocation, skeleton);

			ImmutableMap<String, ResourceLocation> textureLocatons = ImmutableMap.of();
			if (version != 0) {
				Builder<String, ResourceLocation> builder = ImmutableMap.builder();
				JsonObject textureMap = jsonObject.getAsJsonObject("textures");
				for (Entry<String, JsonElement> texEntry : textureMap.entrySet()) {
					String slotName = texEntry.getKey();
					if (!slotName.startsWith("#")) {
						throw new JsonParseException("Slot names must begin with '#'");
					}
					ResourceLocation textureLocation = new ResourceLocation(texEntry.getValue().getAsString());
					builder.put(slotName, textureLocation);
				}
				textureLocatons = builder.build();
			}
			return new ModelDescription(mesh, skeleton, textureLocatons);
		}
	}

	private ModelMCMD model;
	private ISkeleton skeleton;
	private ImmutableMap<String, ResourceLocation> textureMapping;

	private ModelDescription(
			ModelMCMD model,
			ISkeleton skeleton,
			ImmutableMap<String, ResourceLocation> textureMapping) {
		this.model = Objects.requireNonNull(model);
		this.skeleton = Objects.requireNonNull(skeleton);
		this.textureMapping = Objects.requireNonNull(textureMapping);
	}

	public ModelMCMD getModel() {
		return model;
	}

	public ImmutableMap<String, ResourceLocation> getTextureLocations() {
		return textureMapping;
	}

	public ISkeleton getSkeleton() {
		return skeleton;
	}

	public static ModelDescription parse(InputStream input) {
		return GSON.fromJson(new InputStreamReader(input), ModelDescription.class);
	}

}
