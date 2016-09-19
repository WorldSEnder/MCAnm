package com.github.worldsender.mcanm.client.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import com.github.worldsender.mcanm.client.mcanmmodel.ModelMCMD;
import com.github.worldsender.mcanm.client.model.util.ModelStateInformation;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.IRetexturableModel;
import net.minecraftforge.common.model.IModelPart;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;

/**
 * A model loader for item models (new with 1.9) (IModel)
 * 
 * @author WorldSEnder
 *
 */
public enum ModelLoader implements ICustomModelLoader {
	INSTANCE;

	public static final String MISSING_SLOT_NAME = "missingno";
	public static final String PARTICLE_SLOT_NAME = "#particle_sprite";

	private static final String SUFFIX = ".mcmdl";

	private IResourceManager manager;
	private final LoadingCache<ResourceLocation, ModelDescription> modelCache = CacheBuilder.newBuilder()
			.build(new CacheLoader<ResourceLocation, ModelDescription>() {
				@Override
				public ModelDescription load(ResourceLocation key) throws IOException {
					return ModelLoader.this.readModelUncached(key);
				}
			});

	public void onResourceManagerReload(IResourceManager manager) {
		this.manager = manager;
		this.modelCache.invalidateAll();
	}

	private ModelDescription readModelUncached(ResourceLocation modelLocation) throws IOException {
		try (InputStream is = manager.getResource(modelLocation).getInputStream()) {
			return ModelDescription.parse(is);
		}
	}

	@Override
	public boolean accepts(ResourceLocation modelLocation) {
		return modelLocation.getResourcePath().endsWith(SUFFIX);
	}

	@Override
	public IModel loadModel(ResourceLocation modelLocation) throws Exception {
		// load model cached
		ResourceLocation file = new ResourceLocation(
				modelLocation.getResourceDomain(),
				modelLocation.getResourcePath());
		ModelDescription description = modelCache.get(file);
		return new ModelWrapper(file, description);
	}

	private static class ModelState implements IModelState {
		private ModelStateInformation modelState = new ModelStateInformation();

		@Override
		public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part) {
			return Optional.absent();
		}

		public IModelStateInformation getCurrentPass(IBlockState state) {
			// FIXME: animations?
			return modelState;
		}
	}

	private static class ModelWrapper implements IRetexturableModel {
		private final ResourceLocation modelLocation;
		private final ModelMCMD actualModel;
		private final Map<String, ResourceLocation> slotToTexture;
		private final Multimap<ResourceLocation, String> textureToSlots;

		public ModelWrapper(ResourceLocation file, ModelDescription description) {
			this.modelLocation = Objects.requireNonNull(file);
			this.actualModel = description.getModel();
			this.slotToTexture = new HashMap<>();
			this.textureToSlots = MultimapBuilder.hashKeys().hashSetValues().build();
			for (Map.Entry<String, ResourceLocation> texMapping : description.getTextureLocations().entrySet()) {
				this.updateTextureSlot(texMapping.getKey(), texMapping.getValue());
			}
		}

		private ModelWrapper(
				ResourceLocation file,
				ModelMCMD model,
				Map<String, ResourceLocation> slotToTex,
				Multimap<ResourceLocation, String> textureToSlot) {
			this.modelLocation = Objects.requireNonNull(file);
			this.actualModel = Objects.requireNonNull(model);
			this.slotToTexture = new HashMap<>(slotToTex);
			this.textureToSlots = MultimapBuilder.hashKeys().hashSetValues().build(textureToSlot);
		}

		@Override
		public Collection<ResourceLocation> getDependencies() {
			return Collections.emptyList();
		}

		@Override
		public IBakedModel bake(
				IModelState state,
				VertexFormat format,
				Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
			ImmutableMap.Builder<String, TextureAtlasSprite> slotToTexSprite = ImmutableMap.builder();

			for (Map.Entry<String, ResourceLocation> slotEntry : slotToTexture.entrySet()) {
				slotToTexSprite.put(slotEntry.getKey(), bakedTextureGetter.apply(slotEntry.getValue()));
			}
			// Note the missing leading '#', surely it does not collide
			slotToTexSprite.put(MISSING_SLOT_NAME, bakedTextureGetter.apply(new ResourceLocation(MISSING_SLOT_NAME)));

			return new BakedModelWrapper(actualModel, ModelState.class.cast(state), format, slotToTexSprite.build());
		}

		@Override
		public IModelState getDefaultState() {
			return new ModelState();
		}

		@Override
		public Collection<ResourceLocation> getTextures() {
			return textureToSlots.keySet();
		}

		private void updateTextureSlot(String slot, ResourceLocation updated) {
			ResourceLocation current = slotToTexture.get(slot);

			Collection<String> currentSlots = textureToSlots.get(current);
			Collection<String> updatedSlots = textureToSlots.get(updated);
			slotToTexture.put(slot, updated);
			updatedSlots.add(slot);
			currentSlots.remove(slot);
		}

		private void replaceTexture(ResourceLocation current, ResourceLocation updated) {
			Collection<String> currentSlots = textureToSlots.get(current);
			Collection<String> updatedSlots = textureToSlots.get(updated);

			currentSlots.forEach(slot -> slotToTexture.put(slot, updated));
			updatedSlots.addAll(currentSlots);
			currentSlots.clear();
		}

		@Override
		public IModel retexture(ImmutableMap<String, String> textures) {
			ModelWrapper retextured = new ModelWrapper(modelLocation, actualModel, slotToTexture, textureToSlots);

			for (Entry<String, String> remapped : textures.entrySet()) {
				String toRemap = remapped.getKey();
				String after = remapped.getValue();

				boolean remapSlot = toRemap.startsWith("#");
				if (remapSlot) {
					ResourceLocation updated = new ResourceLocation(after);
					retextured.updateTextureSlot(toRemap, updated);
				} else {
					ResourceLocation current = new ResourceLocation(toRemap);
					ResourceLocation updated = new ResourceLocation(after);
					retextured.replaceTexture(current, updated);
				}
			}

			return retextured;
		}
	}

	private static class BakedModelWrapper implements IBakedModel, IPerspectiveAwareModel {
		private final ModelMCMD actualModel;
		private final ModelState bakedState;
		private final VertexFormat format;
		private final ImmutableMap<String, TextureAtlasSprite> slotToSprite;
		private final TextureAtlasSprite particleSprite;

		public BakedModelWrapper(
				ModelMCMD model,
				ModelState state,
				VertexFormat format,
				ImmutableMap<String, TextureAtlasSprite> slotToSprite) {
			this.actualModel = Objects.requireNonNull(model);
			this.slotToSprite = Objects.requireNonNull(slotToSprite);
			this.bakedState = Objects.requireNonNull(state);
			this.format = Objects.requireNonNull(format);
			// There is at least the "missingno" texture in the list
			particleSprite = getParticleSprite(slotToSprite);
		}

		private TextureAtlasSprite getParticleSprite(ImmutableMap<String, TextureAtlasSprite> slotToSprite) {
			return slotToSprite.getOrDefault(PARTICLE_SLOT_NAME, slotToSprite.entrySet().asList().get(0).getValue());
		}

		@Override
		public boolean isBuiltInRenderer() {
			return false;
		}

		@Override
		public boolean isAmbientOcclusion() {
			return true;
		}

		@Override
		public boolean isGui3d() {
			return true;
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			// Only called for non-perspective-aware models? and when on the ground
			return ItemCameraTransforms.DEFAULT;
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			IModelStateInformation currentPass = bakedState.getCurrentPass(state);
			return actualModel.getAsBakedQuads(currentPass, slotToSprite, format);
		}

		@Override
		public ItemOverrideList getOverrides() {
			// we could decide based on item-modelState here?
			return ItemOverrideList.NONE;
		}

		public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
			return IPerspectiveAwareModel.MapWrapper.handlePerspective(this, bakedState, cameraTransformType);
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return particleSprite;
		}
	}
}
