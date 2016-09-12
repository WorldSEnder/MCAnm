package com.github.worldsender.mcanm.client.model;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Objects;

import com.github.worldsender.mcanm.client.ClientLoader;
import com.github.worldsender.mcanm.client.mcanmmodel.ModelMCMD;
import com.github.worldsender.mcanm.common.CommonLoader;
import com.github.worldsender.mcanm.common.skeleton.AbstractSkeleton;
import com.github.worldsender.mcanm.common.skeleton.SkeletonMCSKL;
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
		private AbstractSkeleton loadSkeleton(boolean legacy, ResourceLocation skeletonLocation) {
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
			ResourceLocation modelLocation = new ResourceLocation(jsonObject.get("mesh").getAsString());
			ResourceLocation skeletonLocation = new ResourceLocation(jsonObject.get("skeleton").getAsString());

			AbstractSkeleton skeleton = loadSkeleton(version == 0, skeletonLocation);
			ModelMCMD mesh = ClientLoader.loadModel(modelLocation, skeleton);
			return new ModelDescription(mesh, skeleton);
		}
	}

	private ModelMCMD model;
	private AbstractSkeleton skeleton;

	private ModelDescription(ModelMCMD model, AbstractSkeleton skeleton) {
		this.model = Objects.requireNonNull(model);
		this.skeleton = Objects.requireNonNull(skeleton);
	}

	public ModelMCMD getModel() {
		return model;
	}

	public AbstractSkeleton getSkeleton() {
		return skeleton;
	}

	public static ModelDescription parse(InputStream input) {
		return GSON.fromJson(new InputStreamReader(input), ModelDescription.class);
	}

}
