package com.github.worldsender.mcanm.client;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.Proxy;
import com.github.worldsender.mcanm.client.mcanmmodel.IModel;
import com.github.worldsender.mcanm.client.model.IEntityAnimator;
import com.github.worldsender.mcanm.client.model.ModelLoader;
import com.github.worldsender.mcanm.client.renderer.entity.RenderAnimatedModel;
import com.github.worldsender.mcanm.common.CommonLoader;
import com.github.worldsender.mcanm.common.resource.IResourceLocation;
import com.github.worldsender.mcanm.common.resource.MinecraftResourcePool;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;
import com.github.worldsender.mcanm.test.CubeEntity;
import com.github.worldsender.mcanm.test.CubeEntityV2;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ClientProxy implements Proxy {
	@Override
	public void preInit() {
		ModelLoaderRegistry.registerLoader(ModelLoader.INSTANCE);

		IResourceManager resManager = Minecraft.getMinecraft().getResourceManager();
		if (resManager instanceof IReloadableResourceManager) {
			IReloadableResourceManager registry = (IReloadableResourceManager) resManager;
			registry.registerReloadListener(new IResourceManagerReloadListener() {
				@Override
				public void onResourceManagerReload(IResourceManager p_110549_1_) {
					ClientProxy.reload();
				}
			});
		} else {
			MCAnm.logger()
					.warn("Couldn't register reload managers. Models will not be reloaded on switching resource pack");
		}

		if (MCAnm.isDebug) {
			ResourceLocation modelSrc = new ResourceLocation("mcanm:models/Cube/Cube.mcmd");
			@SuppressWarnings("deprecation")
			ISkeleton skeleton = CommonLoader.loadLegacySkeleton(modelSrc);
			IModel model = ClientLoader.loadModel(modelSrc, skeleton);
			IRenderFactory<CubeEntity> renderer = RenderAnimatedModel.fromModel(model, 1.0f);

			ResourceLocation model2Src = new ResourceLocation("mcanm:models/CubeV2/Cube.mcmd");
			IModel model2 = ClientLoader.loadModel(model2Src, ISkeleton.EMPTY);
			IRenderFactory<CubeEntityV2> renderer2 = RenderAnimatedModel
					.fromModel(makeAnimator("mcanm:textures/models/Cube/Untitled.png"), model2, 1.0f);

			RenderingRegistry.registerEntityRenderingHandler(CubeEntity.class, renderer);
			RenderingRegistry.registerEntityRenderingHandler(CubeEntityV2.class, renderer2);

			Item debug = new Item();
			debug.addPropertyOverride(new ResourceLocation("test"), new IItemPropertyGetter() {
				@Override
				public float apply(ItemStack stack, World worldIn, EntityLivingBase entityIn) {
					return (entityIn.ticksExisted / 100f) % 1f;
				}
			});
			GameRegistry.register(debug.setFull3D().setRegistryName("debug_item"));
			net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(
					debug,
					0,
					new ModelResourceLocation("mcanm:models/item/debug_item.mcmdl#inventory"));
		}
	}

	private static <T extends EntityLiving> IEntityAnimator<T> makeAnimator(String textureDir) {
		LoadingCache<String, ResourceLocation> cachedResourceLoc = CacheBuilder.newBuilder().maximumSize(100)
				.build(new CacheLoader<String, ResourceLocation>() {
					@Override
					public ResourceLocation load(String key) {
						return new ResourceLocation(textureDir + key + ".png");
					}
				});
		IEntityAnimator<T> animator = (entity, buffer, partialTick, _1, _2, _3, _4, _5) -> {
			return buffer.setTextureTransform(cachedResourceLoc::getUnchecked);
		};
		return animator;
	}

	@Override
	public void init() {}

	private static void reload() {
		if (!MCAnm.configuration().isReloadEnabled()) {
			return;
		}
		MinecraftResourcePool.instance.onResourceManagerReloaded();
	}

	@Override
	public IResourceLocation getSidedResource(ResourceLocation resLoc, ClassLoader context) {
		return MinecraftResourcePool.instance.makeResourceLocation(resLoc);
	}
}
