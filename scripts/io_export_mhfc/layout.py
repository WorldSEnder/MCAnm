import bpy

from .operators import ObjectExporter, ArmAnimationExporter, ArmatureUpdater,\
		AddRenderGroup, RemoveRenderGroup, AddFacesToGroup, SelectGroup,\
		UpdateGroupsVisual
from bpy.types import Panel, Menu, UIList, Header

class RenderGroupUIList(UIList):
	def draw_item(self, context, layout, data, item, icon, active_data, active_propname):
		if self.layout_type in {'DEFAULT', 'COMPACT'}:
			if item:
				if item.image in bpy.data.images:
					layout.prop(item, "name", text="", emboss=False, icon_value=icon)
				else:
					row = layout.row()
					row.prop(item, "name", text="", emboss=False, icon_value=icon)
					row.label(icon="ERROR")
			else:
				layout.label(text="", translate=False, icon_value=icon)
		# 'GRID' layout type should be as compact as possible (typically a single icon!).
		elif self.layout_type in {'GRID'}:
			layout.alignment = 'CENTER'
			layout.label(text="", icon_value=icon)

class AnimationExportMenu(Menu):
	bl_label = "Export Action"
	bl_idname = "animation.mc_export"

	def draw(self, context):
		layout = self.layout
		sce = context.scene
		if context.space_data.mode == 'ACTION':
			row = layout.row()
			op = row.operator(ArmAnimationExporter.bl_idname)
			#op.armature = context.object
			op.offset = sce.frame_start
			if not context.space_data.action:
				row.enabled = False
			else:
				op.arm_action = context.space_data.action.name

class AnimationExportHeader(Header):
	bl_label = "Animation Export Menu"
	bl_idname = "animation.mc_export"
	bl_space_type = "DOPESHEET_EDITOR"

	def draw(self, context):
		layout = self.layout
		layout.menu(AnimationExportMenu.bl_idname)

class ObjectExportPanel(Panel):
	# MC Panel under Object
	bl_label = "Monster Hunter Frontier Craft"
	bl_idname = "object.mc_export"
	bl_region_type = "WINDOW"
	bl_space_type = "PROPERTIES"
	bl_context = "object"

	@classmethod
	def poll(cls, context):
		return context.object is not None and context.object.type == 'MESH'

	def draw(self, context):
		# setup vars
		obj = context.object
		data = obj.data
		meshprops = data.mcprops
		sceneprops = context.scene.mcprops
		prefs = context.user_preferences.addons[__package__].preferences
		layout = self.layout
		error = False
		# check for invalidity: #poll
		# Add advanced toggle
		# General properties
		box = layout.box()
		box.label(text="General properties")
		box.prop(prefs, 'directory', text="Resource directory")
		if not prefs.directory:
			error = True
			box.label(text="Select resource path", icon="ERROR")
			box.separator()
		box.prop(prefs, 'mod_id', text="Mod ID")
		# Properties of this object
		box = layout.box()
		box.label(text="Object properties")
		box.prop(meshprops, 'artist', text="Artist name")
		box.prop(meshprops, 'name', text="Model Name")
		if not meshprops.name:
			error = True
			box.label(text="Select model name", icon="ERROR")
			box.separator()
		box.prop_search(meshprops, 'armature', meshprops, 'poss_arms', text="Armature", icon="ARMATURE_DATA")
		if meshprops.armature and meshprops.armature not in meshprops.poss_arms:
			error = True
			box.label(text="Invalid Armature", icon="ERROR")
			box.separator()
		box.operator(ArmatureUpdater.bl_idname)
		box.prop_search(meshprops, 'uv_layer', data, 'uv_layers', text="UV Layer", icon="GROUP_UVS")
		if not meshprops.uv_layer:
			error = True
			box.label(text="Select a UV map.", icon="ERROR")
			box.separator()
		elif meshprops.uv_layer not in data.uv_layers:
			error = True
			box.label(text="Invalid UV Map", icon="ERROR")
			box.separator()
		box = box.box()
		box.label(text="Default group")
		box.prop(meshprops, 'default_group_name', text="Name")
		if not meshprops.default_group_name:
			error = True
			box.label(text="Select a default group name.", icon="ERROR")
			layout.separator()
		elif meshprops.default_group_name in meshprops.render_groups:
			error = True
			box.label(text="Name collision with existing group.", icon="ERROR")
			layout.separator()
		box.prop_search(meshprops, 'default_img', bpy.data, 'images', text="Texture", icon="IMAGE_DATA")
		if not meshprops.default_img:
			error = True
			box.label(text="Select default texture.", icon="ERROR")
			layout.separator()
		elif meshprops.default_img not in bpy.data.images:
			error = True
			layout.label(text="Invalid image texture", icon="ERROR")
			layout.separator()
		row = layout.row()
		row.alignment = 'RIGHT'
		row.prop(sceneprops, 'enable_advanced')
		if sceneprops.enable_advanced:
			layout.prop(meshprops, 'version', text="Version")
			layout.prop(prefs, 'tex_path', text="Tex Path")
			layout.prop(prefs, 'model_path', text="Model Path")
		layout.prop(sceneprops, 'export_tex', text="Export Textures")
		# Add operator
		operator_box = layout.row()
		op = operator_box.operator(ObjectExporter.bl_idname)
		op.object = obj.name
		op.version = meshprops.version
		op.armature = meshprops.armature
		op.uv_layer = meshprops.uv_layer
		op.default_group_name = meshprops.default_group_name
		op.default_img = meshprops.default_img
		op.model_name = meshprops.name
		op.artist = meshprops.artist
		op.export_tex = sceneprops.export_tex
		op.mod_id = prefs.mod_id
		op.tex_path = prefs.tex_path
		op.model_path = prefs.model_path
		op.filepath = prefs.directory
		if error:
			operator_box.enabled = False

class MeshDataPanel(Panel):
	# MC Panel under Object
	bl_idname = "object.mc_meshdata"
	bl_label = "Minecraft Render Groups"
	bl_region_type = "WINDOW"
	bl_space_type = "PROPERTIES"
	bl_context = "data"

	@classmethod
	def poll(cls, context):
		return context.object is not None and context.object.type == 'MESH'

	def draw(self, context):
		layout = self.layout
		props = context.object.data.mcprops
		groups = props.render_groups
		active_idx = props.active_render_group
		
		row = layout.row()
		row.template_list(
				listtype_name='RenderGroupUIList',
				dataptr=props,
				propname='render_groups',
				active_dataptr=props,
				active_propname='active_render_group',
				rows=2,
				type='DEFAULT',
				)
		col = row.column(align=True)
		col.operator(
				AddRenderGroup.bl_idname,
				text="", icon='ZOOMIN')
		col.operator(
				RemoveRenderGroup.bl_idname,
				text="", icon='ZOOMOUT')
		if active_idx >= 0 and active_idx < len(groups):
			active_g = groups[active_idx]
			layout.prop_search(active_g, 'image', bpy.data, 'images', text="Image", icon="IMAGE_DATA")
			if not active_g.image:
				layout.label(text="Select an imagetexture for active group", icon="ERROR")
			elif not active_g.image in bpy.data.images:
				layout.label(text="Invalid image texture", icon="ERROR")
		if context.mode == 'EDIT_MESH':
			row = layout.row()
			row.operator(AddFacesToGroup.bl_idname)
			row.operator(SelectGroup.bl_idname)
			layout.operator_menu_enum(UpdateGroupsVisual.bl_idname, 'mode')

class AnimationExportPanel(Panel):
	# MC Panel under Object
	bl_space_type = "VIEW_3D"
	bl_region_type = "TOOLS"
	bl_category = "Export"
	bl_label = "Minecraft"
	bl_idname = "view3d.mcanm_export"

	@classmethod
	def poll(cls, context):
		return context.object is not None and context.object.type == 'ARMATURE'

	def draw(self, context):
		layout = self.layout
		obj = context.object
		sce = context.scene
		row = layout.row()
		row.operator_context = 'INVOKE_DEFAULT'
		op = row.operator(ArmAnimationExporter.bl_idname)
		op.offset = sce.frame_start
		if obj.animation_data is not None and obj.animation_data.action is not None:
			action = obj.animation_data.action
			op.arm_action = action.name
			op.armature = obj.name
			layout.prop(action.mcprops, 'artist')
			op.artist = action.mcprops.artist
		else:
			row.enabled = False

def menu_func(self, context):
	self.layout.operator(ObjectExporter.bl_idname, text="Export Minecraft model (.mcmd)")

def register():
	bpy.types.INFO_MT_file_export.append(menu_func)

def unregister():
	bpy.types.INFO_MT_file_export.remove(menu_func)
