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

import com.github.worldsender.mcanm.client.mcanmmodel.ModelMCMD;
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
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
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
		@Override
		public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part) {
			return Optional.absent();
		}
	}

	private static class ModelWrapper implements IRetexturableModel {

		private final ResourceLocation modelLocation;
		private final ModelMCMD actualModel;
		private final ImmutableMap<String, ResourceLocation> slotToTexture;
		private final Multimap<ResourceLocation, String> textureToSlots;

		public ModelWrapper(ResourceLocation file, ModelDescription description) {
			this.modelLocation = Objects.requireNonNull(file);
			this.actualModel = description.getModel();
			this.slotToTexture = ImmutableMap.of();
			this.textureToSlots = MultimapBuilder.hashKeys().hashSetValues().build();
		}

		private ModelWrapper(
				ResourceLocation file,
				ModelMCMD model,
				Map<String, ResourceLocation> slotToTex,
				Multimap<ResourceLocation, String> textureToSlot) {
			this.modelLocation = Objects.requireNonNull(file);
			this.actualModel = Objects.requireNonNull(model);
			this.slotToTexture = ImmutableMap.copyOf(slotToTex);
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
			// TODO Auto-generated method stub
			// FIXME: use the bakedTextures 
			return null;
		}

		@Override
		public IModelState getDefaultState() {
			return new ModelState();
		}

		@Override
		public Collection<ResourceLocation> getTextures() {
			return textureToSlots.keySet();
		}

		@Override
		public IModel retexture(ImmutableMap<String, String> textures) {
			Map<String, ResourceLocation> newSlotsToTex = new HashMap<>(slotToTexture);
			Multimap<ResourceLocation, String> newTextureToSlot = MultimapBuilder.hashKeys().hashSetValues()
					.build(textureToSlots);
			for (Entry<String, String> remapped : textures.entrySet()) {
				String toRemap = remapped.getKey();
				String after = remapped.getValue();
				boolean remapSlot = toRemap.startsWith("#");
				if (remapSlot) {
					ResourceLocation current = newSlotsToTex.get(toRemap);
					ResourceLocation updated = new ResourceLocation(after);

					Collection<String> currentSlots = newTextureToSlot.get(current);
					Collection<String> updatedSlots = newTextureToSlot.get(updated);
					newSlotsToTex.put(toRemap, updated);
					updatedSlots.add(toRemap);
					currentSlots.remove(toRemap);
				} else {
					ResourceLocation current = new ResourceLocation(toRemap);
					ResourceLocation updated = new ResourceLocation(after);

					Collection<String> currentSlots = newTextureToSlot.get(current);
					Collection<String> updatedSlots = newTextureToSlot.get(updated);
					currentSlots.forEach(slot -> newSlotsToTex.put(slot, updated));
					updatedSlots.addAll(currentSlots);
					currentSlots.clear();
				}
			}
			return new ModelWrapper(modelLocation, actualModel, newSlotsToTex, newTextureToSlot);
		}
	}

	// FIXME: make this extend IPerspectiveAwareModel?
	private static class BakedModelWrapper implements IBakedModel {
		private final ModelMCMD actualModel;

		public BakedModelWrapper(ModelMCMD model) {
			this.actualModel = Objects.requireNonNull(model);
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
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ItemOverrideList getOverrides() {
			// FIXME: can we decide based on item-state here?
			return ItemOverrideList.NONE;
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			// FIXME: return not just any texture
			return null;
		}

	}
}
