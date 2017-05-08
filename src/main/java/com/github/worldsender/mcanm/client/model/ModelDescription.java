package com.github.worldsender.mcanm.client.model;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.github.worldsender.mcanm.client.ClientLoader;
import com.github.worldsender.mcanm.client.mcanmmodel.ModelMCMD;
import com.github.worldsender.mcanm.common.CommonLoader;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;
import com.github.worldsender.mcanm.common.skeleton.LegacyModelAsSkeleton;
import com.github.worldsender.mcanm.common.skeleton.SkeletonMCSKL;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MultimapBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrideBridge;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ForgeBlockStateV1;
import net.minecraftforge.common.model.TRSRTransformation;

/**
 * Contains references to all required file to display the whole model. This includes the mesh-model ({@link ModelMCMD})
 * and the skeleton file ({@link SkeletonMCSKL})
 * 
 * @author WorldSEnder
 *
 */
public class ModelDescription {
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(ModelDescription.class, new DescriptionDeserializer())
			.registerTypeAdapter(TRSRTransformation.class, ForgeBlockStateV1.TRSRDeserializer.INSTANCE)
			.registerTypeAdapter(ItemOverride.class, new ItemOverrideBridge.Deserializer()).create();

	private static final ImmutableMap<TransformType, TRSRTransformation> EMPTY_MAP;
	private static final Map<ResourceLocation, ImmutableMap<TransformType, TRSRTransformation>> PREDEFINED_TRANSFORMS;

	static {
		EMPTY_MAP = ImmutableMap.of();

		PREDEFINED_TRANSFORMS = new HashMap<>();
		registerPredefinedTransform("identity", EMPTY_MAP);
		ImmutableMap.Builder<TransformType, TRSRTransformation> DEFAULT_ITEM = ImmutableMap.builder();
		registerPredefinedTransform("mcanm:default_item", DEFAULT_ITEM.build());
	}

	public static boolean registerPredefinedTransform(
			String id,
			Map<TransformType, TRSRTransformation> viewTransformation) {
		return registerPredefinedTransform(new ResourceLocation(id), viewTransformation);
	}

	public static boolean registerPredefinedTransform(
			ResourceLocation id,
			Map<TransformType, TRSRTransformation> viewTransformation) {
		Objects.requireNonNull(id);
		Objects.requireNonNull(viewTransformation);
		return PREDEFINED_TRANSFORMS.putIfAbsent(id, ImmutableMap.copyOf(viewTransformation)) == null;
	}

	private static class DescriptionDeserializer implements JsonDeserializer<ModelDescription> {
		private ISkeleton loadSkeleton(boolean legacy, JsonObject jsonObject) {
			if (!legacy && !jsonObject.has("skeleton")) {
				return ISkeleton.EMPTY;
			}
			ResourceLocation skeletonLocation = legacy
					? new ResourceLocation(jsonObject.get("mesh").getAsString())
					: new ResourceLocation(jsonObject.get("skeleton").getAsString());

			return legacy ? loadLegacySkeleton(skeletonLocation) : CommonLoader.loadSkeleton(skeletonLocation);
		}

		@SuppressWarnings("deprecation") // We handle the legacy, not consume it
		private LegacyModelAsSkeleton loadLegacySkeleton(ResourceLocation skeletonLocation) {
			return CommonLoader.loadLegacySkeleton(skeletonLocation);
		}

		private ImmutableMap<TransformType, TRSRTransformation> parseViewMapping(
				JsonObject parentObject,
				JsonDeserializationContext context) {
			if (!parentObject.has("transforms")) {
				return EMPTY_MAP;
			}
			JsonElement viewElement = parentObject.get("transforms");
			if (viewElement.isJsonPrimitive() && viewElement.getAsJsonPrimitive().isString()) {
				String viewString = viewElement.getAsString();
				ResourceLocation id = new ResourceLocation(viewString);
				return PREDEFINED_TRANSFORMS.getOrDefault(id, EMPTY_MAP);
			}
			if (viewElement.isJsonObject()) {
				JsonObject viewObject = viewElement.getAsJsonObject();

				return parseTransformObject(context, viewObject);
			}
			ImmutableMap.Builder<TransformType, TRSRTransformation> result = ImmutableMap.builder();
			TRSRTransformation onlyTransformation = context.deserialize(viewElement, TRSRTransformation.class);
			for (TransformType transformType : TransformType.values()) {
				result.put(transformType, onlyTransformation);
			}
			return result.build();
		}

		private ImmutableMap<TransformType, TRSRTransformation> parseTransformObject(
				JsonDeserializationContext context,
				JsonObject viewObject) {
			ListMultimap<String, JsonElement> transformEntries = MultimapBuilder.hashKeys().arrayListValues(1).build();
			for (Map.Entry<String, JsonElement> entry : viewObject.entrySet()) {
				transformEntries.put(entry.getKey().toUpperCase(Locale.ROOT), entry.getValue());
			}

			ImmutableMap.Builder<TransformType, TRSRTransformation> result = ImmutableMap.builder();
			for (TransformType transformType : TransformType.values()) {
				String transformName = transformType.name().toUpperCase(Locale.ROOT);
				List<JsonElement> entryList = transformEntries.get(transformName);
				if (entryList.isEmpty()) {
					continue;
				}
				if (entryList.size() > 1) {
					throw new JsonParseException("Two transform entries for " + transformName);
				}
				JsonElement onlyElement = entryList.get(0);
				result.put(transformType, context.deserialize(onlyElement, TRSRTransformation.class));
			}
			return result.build();
		}

		private List<ItemOverride> getItemOverrides(
				JsonObject object,
				JsonDeserializationContext deserializationContext) {
			List<ItemOverride> list = Lists.<ItemOverride>newArrayList();

			if (object.has("overrides")) {
				for (JsonElement jsonelement : JsonUtils.getJsonArray(object, "overrides")) {
					list.add((ItemOverride) deserializationContext.deserialize(jsonelement, ItemOverride.class));
				}
			}

			return list;
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
			ImmutableMap<TransformType, TRSRTransformation> viewMapping = parseViewMapping(jsonObject, context);
			List<ItemOverride> itemOverrides = getItemOverrides(jsonObject, context);
			return new ModelDescription(mesh, skeleton, textureLocatons, viewMapping, itemOverrides);
		}
	}

	private final ModelMCMD model;
	private final ISkeleton skeleton;
	private final ImmutableMap<String, ResourceLocation> textureMapping;
	private final ImmutableMap<TransformType, TRSRTransformation> viewMapping;
	private final ItemOverrideList itemOverrides;

	private ModelDescription(
			ModelMCMD model,
			ISkeleton skeleton,
			ImmutableMap<String, ResourceLocation> textureMapping,
			ImmutableMap<TransformType, TRSRTransformation> viewMapping,
			List<ItemOverride> itemOverrides) {
		this.model = Objects.requireNonNull(model);
		this.skeleton = Objects.requireNonNull(skeleton);
		this.textureMapping = Objects.requireNonNull(textureMapping);
		this.viewMapping = Objects.requireNonNull(viewMapping);
		this.itemOverrides = new ItemOverrideList(itemOverrides);
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

	public Map<TransformType, TRSRTransformation> getCustomTransformations() {
		return viewMapping;
	}

	public ItemOverrideList getItemOverrides() {
		return itemOverrides;
	}

	public static ModelDescription parse(InputStream input) {
		return GSON.fromJson(new InputStreamReader(input), ModelDescription.class);
	}

}
